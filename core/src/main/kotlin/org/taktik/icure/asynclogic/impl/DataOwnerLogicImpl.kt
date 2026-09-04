package org.taktik.icure.asynclogic.impl

import com.fasterxml.jackson.databind.JsonMappingException
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.toCollection
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import org.taktik.couchdb.ViewRowNoDoc
import org.taktik.couchdb.entity.ComplexKey
import org.taktik.couchdb.entity.Versionable
import org.taktik.icure.asyncdao.DataOwnerPublicKeysViewValue
import org.taktik.icure.asyncdao.DeviceDAO
import org.taktik.icure.asyncdao.ExchangeDataDAO
import org.taktik.icure.asyncdao.GenericDAO
import org.taktik.icure.asyncdao.HealthcarePartyDAO
import org.taktik.icure.asyncdao.PatientDAO
import org.taktik.icure.asyncdao.results.BulkSaveResult
import org.taktik.icure.asyncdao.updateRetrying
import org.taktik.icure.asynclogic.DataOwnerLogic
import org.taktik.icure.datastore.DatastoreInstanceProvider
import org.taktik.icure.datastore.IDatastoreInformation
import org.taktik.icure.entities.CryptoActorStub
import org.taktik.icure.entities.CryptoActorStubWithType
import org.taktik.icure.entities.DataOwnerType
import org.taktik.icure.entities.DataOwnerWithType
import org.taktik.icure.entities.Device
import org.taktik.icure.entities.HealthcareParty
import org.taktik.icure.entities.Patient
import org.taktik.icure.entities.base.CryptoActor
import org.taktik.icure.entities.base.DataOwnerGroupLink
import org.taktik.icure.entities.base.DataOwnerGroupLinkType
import org.taktik.icure.entities.base.DataOwnerGroupLinkType.Companion.defaultGroupLinkType
import org.taktik.icure.entities.base.DataOwnerHierarchyInfo
import org.taktik.icure.entities.base.StoredDocument
import org.taktik.icure.entities.base.asCryptoActorStub
import org.taktik.icure.entities.base.effectiveGroupLinkType
import org.taktik.icure.entities.requests.DataOwnerPublicKeys
import org.taktik.icure.entities.requests.LinkedDataOwner
import org.taktik.icure.entities.requests.PublicKeyInfo
import org.taktik.icure.entities.requests.RsaEncryptionAlgorithm
import org.taktik.icure.entities.requests.publicKeysWithAlgorithm
import org.taktik.icure.exceptions.ConflictRequestException
import org.taktik.icure.exceptions.DeserializationTypeException
import org.taktik.icure.exceptions.IllegalEntityException
import org.taktik.icure.exceptions.NotFoundRequestException
import org.taktik.icure.pagination.MultiKeyPaginationElement
import org.taktik.icure.pagination.toMultiKeyPaginatedFlow
import org.taktik.icure.security.filterDataOwnersMembersOf
import org.taktik.icure.security.resolveDataOwnerHierarchyInfo
import org.taktik.icure.security.resolveHcpHierarchyInfo
import org.taktik.icure.utils.PeekChannel

open class DataOwnerLogicImpl(
	protected val patientDao: PatientDAO,
	protected val hcpDao: HealthcarePartyDAO,
	protected val deviceDao: DeviceDAO,
	private val datastoreInstanceProvider: DatastoreInstanceProvider,
	private val exchangeDataDao: ExchangeDataDAO,
) : DataOwnerLogic {
	companion object {
		private const val MAX_HIERARCHY_DEPTH = 5

		/**
		 * Cap for queries that do not require fetching entire documents
		 */
		const val MAX_LIGHT_QUERY_SIZE = 1000

		/**
		 * Cap for queries that require fetching entire documents.
		 */
		const val MAX_HEAVY_QUERY_SIZE = 200

		/**
		 * Hard limit to a request for changing members of a group, causes a 4xx error before doing any operation that
		 * requests to many changes at once
		 */
		const val MAX_GROUP_MEMBERSHIP_CHANGES = MAX_HEAVY_QUERY_SIZE

		/**
		 * How many times an update initiated conflicting with a concurrent change to the entity is retried.
		 * This is only applied when the updated entity is computed by the backend (based on the client request and the
		 * current data owner value); updates request where the updated entity has been at least partially computed by
		 * the client are not retried.
		 */
		private const val CONFLICT_UPDATE_TRIES = 3
	}

	override suspend fun getCryptoActorStub(dataOwnerId: String): CryptoActorStubWithType? = getDataOwner(dataOwnerId)?.retrieveStub()

	override fun getCryptoActorStubs(dataOwnerIds: List<String>): Flow<CryptoActorStubWithType> = getDataOwners(dataOwnerIds).map { it.retrieveStub() }

	override suspend fun getCryptoActorStubWithType(
		dataOwnerId: String,
		dataOwnerType: DataOwnerType,
	): CryptoActorStub? = getDataOwnerWithType(dataOwnerId, dataOwnerType, null, true)?.retrieveStub()?.stub

	override fun getCryptoActorStubsWithType(
		dataOwnerIds: Collection<String>,
		dataOwnerType: DataOwnerType,
	): Flow<CryptoActorStub> = flow {
		emitAll(doGetCryptoActorStubsWithType(datastoreInstanceProvider.getInstanceAndGroup(), dataOwnerIds, dataOwnerType))
	}

	protected fun doGetCryptoActorStubsWithType(
		datastoreInformation: IDatastoreInformation,
		dataOwnerIds: Collection<String>,
		dataOwnerType: DataOwnerType,
	): Flow<CryptoActorStub> = flow {
		when (dataOwnerType) {
			DataOwnerType.HCP -> hcpDao.getEntities(datastoreInformation, dataOwnerIds)
			DataOwnerType.DEVICE -> deviceDao.getEntities(datastoreInformation, dataOwnerIds)
			DataOwnerType.PATIENT -> patientDao.getEntities(datastoreInformation, dataOwnerIds)
		}.collect { if (it.deletionDate == null) emit(it.retrieveStub()) }
	}

	override fun findDataOwnersLinkedToGroups(
		dataOwnerGroupIds: List<String>,
		dataOwnerType: DataOwnerType,
		startDocumentId: String?,
		limit: Int,
	): Flow<MultiKeyPaginationElement<LinkedDataOwner, String>> = flow {
		emitAll(
			doFindDataOwnersLinkedToGroups(
				datastoreInstanceProvider.getInstanceAndGroup(),
				dataOwnerGroupIds,
				dataOwnerType,
				startDocumentId,
				limit,
			),
		)
	}

	protected fun doFindDataOwnersLinkedToGroups(
		datastoreInformation: IDatastoreInformation,
		dataOwnerGroupIds: List<String>,
		dataOwnerType: DataOwnerType,
		startDocumentId: String?,
		limit: Int,
	): Flow<MultiKeyPaginationElement<LinkedDataOwner, String>> = flow {
		require(dataOwnerGroupIds.isNotEmpty()) {
			"At least one data owner group id should be provided."
		}
		require(dataOwnerGroupIds.distinct().size == dataOwnerGroupIds.size) {
			"The data owner group ids should not contain duplicates."
		}
		require(limit > 0) {
			"The limit should be positive."
		}
		/*
		 * Only healthcare parties can currently be the target of a group link: the default effective type of
		 * patients and devices is notAllowed, and groupLinkType can never be changed after creation, so nothing can
		 * ever be linked to them. The answer is empty without any database access.
		 */
		if (dataOwnerType != DataOwnerType.HCP) return@flow
		val cappedLimit = limit.coerceAtMost(MAX_LIGHT_QUERY_SIZE)
		val defaultLinkType = dataOwnerType.defaultGroupLinkType()
		val emittedIds = mutableSetOf<String>()
		emitAll(
			hcpDao
				.findDataOwnersLinkedToGroups(datastoreInformation, dataOwnerGroupIds, startDocumentId, cappedLimit + 1)
				.toMultiKeyPaginatedFlow(
					pageSize = cappedLimit,
					keys = dataOwnerGroupIds,
					// by_data_owner_group is keyed by the id of the group the emitting data owner is linked to.
					keyOfRow = { checkNotNull(it.key as String?) { "The rows of by_data_owner_group always have a key." } },
				) { row ->
					/*
					 * A data owner linked to several of the queried groups has one row per group: dropping all but the
					 * first shortens the page rather than triggering a second query to refill it.
					 */
					if (emittedIds.add(row.id)) {
						val linkType = (row.value as String?)?.let { DataOwnerGroupLinkType.valueOf(it) }
						// Normalized to null when it is the default for the type, so it doesn't have to be serialized
						// for the data owners that don't deviate from it.
						LinkedDataOwner(row.id, linkType.takeIf { it != defaultLinkType })
					} else {
						null
					}
				},
		)
	}

	override fun getDataOwnersPublicKeys(
		dataOwnerIds: List<String>,
		dataOwnerType: DataOwnerType,
	): Flow<DataOwnerPublicKeys> = flow {
		emitAll(
			doGetDataOwnersPublicKeys(datastoreInstanceProvider.getInstanceAndGroup(), dataOwnerIds, dataOwnerType),
		)
	}

	protected fun doGetDataOwnersPublicKeys(
		datastoreInformation: IDatastoreInformation,
		dataOwnerIds: List<String>,
		dataOwnerType: DataOwnerType,
	): Flow<DataOwnerPublicKeys> = flow {
		require(dataOwnerIds.size <= MAX_LIGHT_QUERY_SIZE) {
			"Can't get the public keys of more than $MAX_LIGHT_QUERY_SIZE data owners at once, got ${dataOwnerIds.size}."
		}
		val distinctIds = dataOwnerIds.distinct()
		if (distinctIds.isEmpty()) return@flow
		when (dataOwnerType) {
			DataOwnerType.HCP -> emitAll(healthcarePartiesPublicKeys(datastoreInformation, distinctIds))
			/*
			 * Patients and devices are never group targets, so there is no bulk use case worth a dedicated view for
			 * them: their keys are extracted from the crypto actor stubs instead.
			 */
			DataOwnerType.PATIENT, DataOwnerType.DEVICE -> emitAll(
				doGetCryptoActorStubsWithType(datastoreInformation, distinctIds, dataOwnerType)
					.map { DataOwnerPublicKeys(it.id, it.publicKeysWithAlgorithm()) }
					.filter { it.publicKeys.isNotEmpty() },
			)
		}
	}

	/**
	 * Resolves the algorithm codes of the by_data_owner_public_keys view rows, one row per healthcare party, into
	 * the [RsaEncryptionAlgorithm] each key must be used with.
	 */
	private fun healthcarePartiesPublicKeys(
		datastoreInformation: IDatastoreInformation,
		dataOwnerIds: List<String>,
	): Flow<DataOwnerPublicKeys> = hcpDao
		.listHealthcarePartiesPublicKeys(datastoreInformation, dataOwnerIds)
		.filterIsInstance<ViewRowNoDoc<String, DataOwnerPublicKeysViewValue>>()
		.map { row ->
			val value = checkNotNull(row.value) { "The rows of by_data_owner_public_keys always have a value." }
			DataOwnerPublicKeys(
				row.id,
				value.pubkeys.map { (publicKey, algorithmCode) ->
					PublicKeyInfo(publicKey, RsaEncryptionAlgorithm.fromViewCode(algorithmCode))
				},
			)
		}

	override suspend fun getDataOwner(dataOwnerId: String): DataOwnerWithType? = doGetDataOwner(dataOwnerId, likelyType = null)

	protected suspend fun doGetDataOwner(
		dataOwnerId: String,
		likelyType: DataOwnerType?,
		preloadedDatastoreInfo: IDatastoreInformation? = null,
	): DataOwnerWithType? {
		val datastoreInfo = preloadedDatastoreInfo ?: datastoreInstanceProvider.getInstanceAndGroup()
		val orderToTry =
			when (likelyType) {
				null, DataOwnerType.PATIENT -> listOf(DataOwnerType.PATIENT, DataOwnerType.HCP, DataOwnerType.DEVICE)
				DataOwnerType.HCP -> listOf(DataOwnerType.HCP, DataOwnerType.PATIENT, DataOwnerType.DEVICE)
				DataOwnerType.DEVICE -> listOf(DataOwnerType.DEVICE, DataOwnerType.PATIENT, DataOwnerType.HCP)
			}
		return orderToTry.firstNotNullOfOrNull {
			getDataOwnerWithType(dataOwnerId, it, datastoreInfo, true)
		}
	}

	override fun getDataOwners(dataOwnerIds: List<String>): Flow<DataOwnerWithType> = flow {
		coroutineScope {
			val datastoreInfo = datastoreInstanceProvider.getInstanceAndGroup()
			var currIdIndex = 0
			val patientChannel = PeekChannel<Patient>(1)
			val hcpChannel = PeekChannel<HealthcareParty>(1)
			val deviceChannel = PeekChannel<Device>(1)
			launch {
				patientDao
					.getEntities(datastoreInfo, dataOwnerIds)
					.filter {
						it.deletionDate == null
					}.collect {
						patientChannel.send(it)
					}
				patientChannel.closeSend()
			}
			launch {
				hcpDao
					.getEntities(datastoreInfo, dataOwnerIds)
					.filter {
						it.deletionDate == null
					}.collect {
						hcpChannel.send(it)
					}
				hcpChannel.closeSend()
			}
			launch {
				deviceDao
					.getEntities(datastoreInfo, dataOwnerIds)
					.filter {
						it.deletionDate == null
					}.collect {
						deviceChannel.send(it)
					}
				deviceChannel.closeSend()
			}
			while (currIdIndex < dataOwnerIds.size) {
				val currId = dataOwnerIds[currIdIndex++]
				when {
					patientChannel.peekOrNull()?.id == currId -> {
						emit(DataOwnerWithType.PatientDataOwner(patientChannel.peekOrNull()!!))
						patientChannel.consume()
					}
					hcpChannel.peekOrNull()?.id == currId -> {
						emit(DataOwnerWithType.HcpDataOwner(hcpChannel.peekOrNull()!!))
						hcpChannel.consume()
					}
					deviceChannel.peekOrNull()?.id == currId -> {
						emit(DataOwnerWithType.DeviceDataOwner(deviceChannel.peekOrNull()!!))
						deviceChannel.consume()
					}
					else -> {
						// ignore id: doesn't match an existing data owner
					}
				}
			}
		}
	}

	@Deprecated("Only follows the legacy linear parentId chain, use getCryptoActorHierarchyInfo instead")
	override fun getCryptoActorHierarchy(dataOwnerId: String): Flow<DataOwnerWithType> = flow {
		var nextId: String? = dataOwnerId
		var nextLikelyType: DataOwnerType? = null
		val visited = mutableSetOf<String>()
		while (nextId != null) {
			if (nextId in visited) throw IllegalEntityException("Circular reference in ancestors of $dataOwnerId")
			if (visited.size >
				MAX_HIERARCHY_DEPTH
			) {
				throw IllegalEntityException("Hierarchy of $dataOwnerId exceeds maximum allowed depth of $MAX_HIERARCHY_DEPTH")
			}
			val current =
				doGetDataOwner(nextId, likelyType = nextLikelyType) ?: throw IllegalEntityException(
					"Can't find ancestor $nextId for $dataOwnerId",
				)
			visited.add(current.id)
			nextLikelyType = current.type
			nextId = current.dataOwner.parentId
			emit(current)
		}
	}

	@Deprecated("Only follows the legacy linear parentId chain, use getCryptoActorHierarchyInfo instead")
	@Suppress("DEPRECATION")
	override fun getCryptoActorHierarchyStub(dataOwnerId: String): Flow<CryptoActorStubWithType> = getCryptoActorHierarchy(dataOwnerId).map { it.retrieveStub() }

	override suspend fun getCryptoActorHierarchyInfo(dataOwnerId: String): DataOwnerHierarchyInfo {
		val datastoreInfo = datastoreInstanceProvider.getInstanceAndGroup()
		val self = doGetDataOwner(dataOwnerId, likelyType = null, preloadedDatastoreInfo = datastoreInfo)
			?: throw IllegalEntityException("Can't find data owner $dataOwnerId")
		return when (self) {
			is DataOwnerWithType.HcpDataOwner -> resolveHcpHierarchyInfo(self.dataOwner) { ids ->
				hcpDao.getEntities(datastoreInfo, ids.toList()).toList()
			}
			// Patients and devices have no dataOwnerGroups: only the data owner itself is part of its hierarchies
			else -> DataOwnerHierarchyInfo(self.id, self.type, emptyList())
		}
	}

	private suspend fun getDataOwnerWithType(
		dataOwnerId: String,
		dataOwnerType: DataOwnerType,
		preloadedDatastoreInfo: IDatastoreInformation?,
		ignoreSoftDeleted: Boolean,
	): DataOwnerWithType? {
		val datastoreInfo = preloadedDatastoreInfo ?: datastoreInstanceProvider.getInstanceAndGroup()
		return when (dataOwnerType) {
			DataOwnerType.HCP ->
				wrongTypeAsNull { hcpDao.get(datastoreInfo, dataOwnerId) }
					?.takeIf { !ignoreSoftDeleted || it.deletionDate == null }
					?.let { DataOwnerWithType.HcpDataOwner(it) }
			DataOwnerType.DEVICE ->
				wrongTypeAsNull { deviceDao.get(datastoreInfo, dataOwnerId) }
					?.takeIf { !ignoreSoftDeleted || it.deletionDate == null }
					?.let { DataOwnerWithType.DeviceDataOwner(it) }
			DataOwnerType.PATIENT ->
				wrongTypeAsNull { patientDao.get(datastoreInfo, dataOwnerId) }
					?.takeIf { !ignoreSoftDeleted || it.deletionDate == null }
					?.let { DataOwnerWithType.PatientDataOwner(it) }
		}
	}

	override suspend fun modifyCryptoActor(modifiedCryptoActor: CryptoActorStubWithType): CryptoActorStubWithType {
		val dataOwnerInfo =
			getDataOwnerWithType(modifiedCryptoActor.stub.id, modifiedCryptoActor.type, null, true)
				?: throw NotFoundRequestException(
					"Data owner with id ${modifiedCryptoActor.stub.id} does not exist or is not of type ${modifiedCryptoActor.type}",
				)
		return when (dataOwnerInfo) {
			is DataOwnerWithType.DeviceDataOwner ->
				checkRevAndTagsThenUpdate(
					dataOwnerInfo.dataOwner,
					modifiedCryptoActor,
					{ deviceDao.save(datastoreInstanceProvider.getInstanceAndGroup(), it) },
					{ original, modified ->
						original.copy(
							publicKey = modified.publicKey,
							hcPartyKeys = modified.hcPartyKeys,
							aesExchangeKeys = modified.aesExchangeKeys,
							transferKeys = modified.transferKeys,
							privateKeyShamirPartitions = modified.privateKeyShamirPartitions,
							publicKeysForOaepWithSha256 = modified.publicKeysForOaepWithSha256,
							cryptoActorProperties = modified.cryptoActorProperties,
						)
					},
				)
			is DataOwnerWithType.HcpDataOwner ->
				checkRevAndTagsThenUpdate(
					dataOwnerInfo.dataOwner,
					modifiedCryptoActor,
					{ hcpDao.save(datastoreInstanceProvider.getInstanceAndGroup(), it) },
					{ original, modified ->
						original.copy(
							publicKey = modified.publicKey,
							hcPartyKeys = modified.hcPartyKeys,
							aesExchangeKeys = modified.aesExchangeKeys,
							transferKeys = modified.transferKeys,
							privateKeyShamirPartitions = modified.privateKeyShamirPartitions,
							publicKeysForOaepWithSha256 = modified.publicKeysForOaepWithSha256,
							cryptoActorProperties = modified.cryptoActorProperties,
						)
					},
				)
			is DataOwnerWithType.PatientDataOwner ->
				checkRevAndTagsThenUpdate(
					dataOwnerInfo.dataOwner,
					modifiedCryptoActor,
					{ patientDao.save(datastoreInstanceProvider.getInstanceAndGroup(), it) },
					{ original, modified ->
						original.copy(
							publicKey = modified.publicKey,
							hcPartyKeys = modified.hcPartyKeys,
							aesExchangeKeys = modified.aesExchangeKeys,
							transferKeys = modified.transferKeys,
							privateKeyShamirPartitions = modified.privateKeyShamirPartitions,
							publicKeysForOaepWithSha256 = modified.publicKeysForOaepWithSha256,
							cryptoActorProperties = modified.cryptoActorProperties,
						)
					},
				)
		}
	}

	private inline fun <T> wrongTypeAsNull(block: () -> T): T? = try {
		block()
	} catch (e: JsonMappingException) {
		if (e.cause is DeserializationTypeException) {
			null
		} else {
			throw e
		}
	}

	private inline fun <T> checkRevAndTagsThenUpdate(
		original: T,
		modified: CryptoActorStubWithType,
		save: (T) -> T?,
		updateOriginalWithCryptoActorStubContent: (T, CryptoActorStub) -> T,
	): CryptoActorStubWithType where T : Versionable<String>, T : CryptoActor {
		if (original.rev != modified.stub.rev) {
			throw ConflictRequestException("Outdated revision for entity with id ${original.id}")
		}
		// Compare the normalized (parentId folded into dataOwnerGroups) link sets rather than each field
		// individually: parentId and dataOwnerGroups are just two different wire representations of the same
		// underlying links, and which one carries a given link can differ across SDK versions (e.g. a cardinal
		// 3+ reader is served the legacy parentId folded into dataOwnerGroups, with parentId reported as null).
		// Comparing fields one-to-one would wrongly reject a client echoing back an unchanged link set in a
		// different shape than it was originally stored in.
		require(
			CryptoActor.normalizedDataOwnerGroupLinks(modified.stub.dataOwnerGroups, modified.stub.parentId) ==
				CryptoActor.normalizedDataOwnerGroupLinks(original.dataOwnerGroups, original.parentId)
		) {
			"You can't use this method to change the parent id or data owner group links of a crypto actor"
		}
		// null is tolerated as "not provided" for the same reason; groupLinkType is a logic/correctness invariant
		// (not access-control), so unlike dataOwnerGroups/parentId there is no permission that can ever bypass this.
		require(modified.stub.groupLinkType == null || modified.stub.groupLinkType == original.groupLinkType) {
			"You can't use this method to change the groupLinkType of a crypto actor"
		}
		val saved =
			checkNotNull(save(updateOriginalWithCryptoActorStubContent(original, modified.stub))) {
				"Update returned null for entity with id ${original.id}"
			}
		return CryptoActorStubWithType(modified.type, saved.retrieveStub())
	}

	private fun <T> T.retrieveStub(): CryptoActorStub where T : CryptoActor, T : Versionable<String> = checkNotNull(asCryptoActorStub()) { "Retrieved crypto actor should be stubbable" }

	private fun DataOwnerWithType.retrieveStub(): CryptoActorStubWithType = checkNotNull(asCryptoActorStub()) { "Retrieved crypto actor should be stubbable" }

	override fun addDataOwnersToGroup(
		dataOwnerType: DataOwnerType,
		dataOwnerGroupId: String,
		newMembersIds: List<String>,
	): Flow<String> = flow {
		emitAll(
			doAddDataOwnersToGroup(
				dataOwnerType,
				dataOwnerGroupId,
				newMembersIds,
				datastoreInstanceProvider.getInstanceAndGroup(),
			),
		)
	}

	private fun doAddDataOwnersToGroup(
		dataOwnerType: DataOwnerType,
		dataOwnerGroupId: String,
		newMembersIds: List<String>,
		datastoreInfo: IDatastoreInformation,
	): Flow<String> = flow {
		when (dataOwnerType) {
			DataOwnerType.HCP -> emitAll(
				doAddDataOwnersToGroup(hcpDao, dataOwnerType, dataOwnerGroupId, newMembersIds, datastoreInfo) {
					copy(dataOwnerGroups = it)
				},
			)
			DataOwnerType.DEVICE -> emitAll(
				doAddDataOwnersToGroup(deviceDao, dataOwnerType, dataOwnerGroupId, newMembersIds, datastoreInfo) {
					copy(dataOwnerGroups = it)
				},
			)
			DataOwnerType.PATIENT -> throw IllegalArgumentException("Data owner groups for patients are currently unsupported.")
		}
	}

	private fun <T> doAddDataOwnersToGroup(
		dao: GenericDAO<T>,
		dataOwnerType: DataOwnerType,
		dataOwnerGroupId: String,
		newMembersIds: List<String>,
		datastoreInfo: IDatastoreInformation,
		updateMembers: T.(List<DataOwnerGroupLink>) -> T,
	): Flow<String> where T : CryptoActor, T : StoredDocument = flow {
		require(newMembersIds.size <= MAX_GROUP_MEMBERSHIP_CHANGES) {
			"Can't add more than $MAX_GROUP_MEMBERSHIP_CHANGES data owners to a data owner group at once, got ${newMembersIds.size}."
		}
		require(newMembersIds.all { it != dataOwnerGroupId }) {
			"Can't add a data owner group to itself."
		}
		val dataOwnerGroup = getDataOwnerWithType(
			dataOwnerGroupId,
			dataOwnerType,
			datastoreInfo,
			false
		) ?: throw NotFoundRequestException(
			"Could not find $dataOwnerType data owner group with id $dataOwnerGroupId."
		)
		val groupEffectiveLinkType = dataOwnerGroup.dataOwner.effectiveGroupLinkType(dataOwnerType)
		require(groupEffectiveLinkType != DataOwnerGroupLinkType.notAllowed) {
			"Data owner $dataOwnerGroupId does not represent a data owner group."
		}
		val newLink = DataOwnerGroupLink(dataOwnerGroupId)
		val noUpdateResults = mutableListOf<String>()
		dao.updateRetrying(
			datastoreInfo,
			newMembersIds.toSet(),
			maxChunkSize = MAX_HEAVY_QUERY_SIZE,
			maxTries = CONFLICT_UPDATE_TRIES,
			delayBeforeRetryMs = 2000L..4000L,
		) { existingDataOwner ->
			when {
				existingDataOwner.effectiveGroupLinkType(dataOwnerType).strength < groupEffectiveLinkType.strength -> {
					// Ignored because invalid
					null
				}
				existingDataOwner.parentId == dataOwnerGroupId || existingDataOwner.dataOwnerGroups.any { it.dataOwnerId == dataOwnerGroupId } -> {
					// no need to update because already member; still emit to mark as success
					noUpdateResults.add(existingDataOwner.id)
					null
				}
				else -> {
					existingDataOwner.updateMembers(existingDataOwner.dataOwnerGroups + newLink)
				}
			}
		}.collect { result ->
			when (result) {
				is BulkSaveResult.Failure -> { /* ignore */ }
				is BulkSaveResult.Success -> emit(result.entity.id)
			}
		}
		noUpdateResults.forEach { emit(it) }
	}

	override fun removeDataOwnersFromGroup(
		dataOwnerType: DataOwnerType,
		dataOwnerGroupId: String,
		membersToRemoveIds: List<String>,
		invalidateSharedExchangeDataIfNeeded: Boolean,
	): Flow<String> = flow {
		emitAll(doRemoveDataOwnersFromGroup(
			dataOwnerType,
			dataOwnerGroupId,
			membersToRemoveIds,
			datastoreInstanceProvider.getInstanceAndGroup(),
			invalidateSharedExchangeDataIfNeeded,
		))
	}


	private fun doRemoveDataOwnersFromGroup(
		dataOwnerType: DataOwnerType,
		dataOwnerGroupId: String,
		membersToRemoveIds: List<String>,
		datastoreInfo: IDatastoreInformation,
		invalidateSharedExchangeDataIfNeeded: Boolean,
	): Flow<String> = flow {
		when (dataOwnerType) {
			DataOwnerType.HCP -> emitAll(
				doRemoveDataOwnersFromGroup(hcpDao, dataOwnerType, dataOwnerGroupId, membersToRemoveIds, datastoreInfo, invalidateSharedExchangeDataIfNeeded) { updatedLinks, updatedLegacyParent ->
					copy(
						dataOwnerGroups = updatedLinks,
						parentId = updatedLegacyParent
					)
				},
			)
			DataOwnerType.DEVICE -> emitAll(
				doRemoveDataOwnersFromGroup(deviceDao, dataOwnerType, dataOwnerGroupId, membersToRemoveIds, datastoreInfo, invalidateSharedExchangeDataIfNeeded) { updatedLinks, updatedLegacyParent ->
					copy(
						dataOwnerGroups = updatedLinks,
						parentId = updatedLegacyParent
					)
				},
			)
			DataOwnerType.PATIENT -> throw IllegalArgumentException("Data owner groups for patients are currently unsupported.")
		}
	}

	private fun <T> doRemoveDataOwnersFromGroup(
		dao: GenericDAO<T>,
		dataOwnerType: DataOwnerType,
		dataOwnerGroupId: String,
		membersToRemoveIds: List<String>,
		datastoreInfo: IDatastoreInformation,
		invalidateSharedExchangeDataIfNeeded: Boolean,
		updateGroupLinksAndLegacyParent: T.(List<DataOwnerGroupLink>, String?) -> T,
	): Flow<String> where T : CryptoActor, T : StoredDocument = flow {
		require(membersToRemoveIds.size <= MAX_GROUP_MEMBERSHIP_CHANGES) {
			"Can't remove more than $MAX_GROUP_MEMBERSHIP_CHANGES data owners from a data owner group at once, got ${membersToRemoveIds.size}."
		}
		require(membersToRemoveIds.all { it != dataOwnerGroupId }) {
			"Can't remove a data owner group from itself."
		}
		// Retrieved through the type-specific dao, and not through getDataOwnerWithType, so that it is statically a
		// [T]: the hierarchy of the group below has to be resolved with the same dao, and therefore on the same type.
		val dataOwnerGroup = wrongTypeAsNull { dao.get(datastoreInfo, dataOwnerGroupId) }
			?: throw NotFoundRequestException(
				"Could not find $dataOwnerType data owner group with id $dataOwnerGroupId."
			)
		val groupEffectiveLinkType = dataOwnerGroup.effectiveGroupLinkType(dataOwnerType)
		require(groupEffectiveLinkType != DataOwnerGroupLinkType.notAllowed) {
			"Data owner $dataOwnerGroupId does not represent a data owner group."
		}
		val groupAndSimpleTransitiveLinks = if (invalidateSharedExchangeDataIfNeeded && groupEffectiveLinkType == DataOwnerGroupLinkType.simple) {
			resolveDataOwnerHierarchyInfo(
				childDataOwner = dataOwnerGroup,
				minAcceptedType = null,
				dataOwnerType = dataOwnerType
			) {
				dao.getEntities(datastoreInfo, it).toList()
			}.flattened(setOf(DataOwnerGroupLinkType.simple))
		} else null
		val simpleTypeGroupMembers = mutableSetOf<String>()
		val confirmedRemoval = mutableListOf<String>()
		dao.updateRetrying(
			datastoreInformation = datastoreInfo,
			idsToUpdate = membersToRemoveIds.toSet(),
			maxChunkSize = MAX_HEAVY_QUERY_SIZE,
			maxTries = CONFLICT_UPDATE_TRIES,
			delayBeforeRetryMs = 2000L..4000L,
		) { existingDataOwner ->
			if (existingDataOwner.effectiveGroupLinkType(dataOwnerType) == DataOwnerGroupLinkType.simple) {
				simpleTypeGroupMembers.add(existingDataOwner.id)
			}
			when {
				existingDataOwner.parentId != dataOwnerGroupId && existingDataOwner.dataOwnerGroups.none { it.dataOwnerId == dataOwnerGroupId } -> {
					// already not a member
					confirmedRemoval.add(existingDataOwner.id)
					null
				}
				else -> {
					existingDataOwner.updateGroupLinksAndLegacyParent(
						existingDataOwner.dataOwnerGroups.filter { it.dataOwnerId != dataOwnerGroupId },
						if (existingDataOwner.parentId == dataOwnerGroupId) null else existingDataOwner.parentId
					)
				}
			}
		}.collect {
			when (it) {
				is BulkSaveResult.Failure -> { /* ignore */ }
				is BulkSaveResult.Success -> confirmedRemoval.add(it.entity.id)
			}
		}
		if (groupAndSimpleTransitiveLinks != null && confirmedRemoval.isNotEmpty()) {
			val exchangeDataToInvalidateMainIds = mutableSetOf<String>()
			groupAndSimpleTransitiveLinks.forEach { currentParticipant ->
				/*
				 * Notes:
				 * - We are checking if we should invalidate exchange data even by considering recipients that were already
				 * not in the group; this is to handle situations where a previous call to the method had failed during
				 * execution and only some of the update requests went through
				 * - We do not consider data owners that after removal are still part of the data owner group (through
				 * transitive links)
				 * - The check is on the current participant, and not on dataOwnerGroupId, so it can't be hoisted out of
				 * this loop: every participant is dataOwnerGroupId itself or a group above it, so a data owner still
				 * part of dataOwnerGroupId is still part of all of them, but not the other way around - a data owner
				 * cut off from dataOwnerGroupId may belong to a group above it in its own right, and reusing the
				 * answer for dataOwnerGroupId would invalidate that group's exchange data for nothing
				 */
				val fullyRemovedForParticipant = confirmedRemoval.toSet().let { confirmedRemovalSet ->
					confirmedRemovalSet - filterDataOwnersMembersOf(confirmedRemovalSet, currentParticipant) { toLoad ->
						dao.getEntities(datastoreInfo, toLoad).toList()
					}
				}
				if (fullyRemovedForParticipant.isNotEmpty()) {
					(
						if (fullyRemovedForParticipant.any { it in simpleTypeGroupMembers }) {
							getExchangeDataMainIdsForParticipants(
								datastoreInfo = datastoreInfo,
								participant = currentParticipant,
							)
						} else {
							getExchangeDataMainIdsForParticipantFilteringHasRecipientPiece(
								datastoreInfo = datastoreInfo,
								participant = currentParticipant,
								recipients = fullyRemovedForParticipant.toList(),
							)
						}
						).collect { exchangeDataToInvalidateMainIds += it }
				}
			}
			if (exchangeDataToInvalidateMainIds.isNotEmpty()) {
				var fullySuccessfulInvalidation = true
				exchangeDataDao.updateRetrying(
					datastoreInformation = datastoreInfo,
					idsToUpdate = exchangeDataToInvalidateMainIds,
					maxChunkSize = MAX_HEAVY_QUERY_SIZE,
					maxTries = 3,
					delayBeforeRetryMs = 100..1000L,
				) {
					if (it.delegatorSignature.isEmpty()) null else it.copy(delegatorSignature = emptyMap())
				}.collect {
					if (!it.isSuccess) fullySuccessfulInvalidation = false
				}
				check(fullySuccessfulInvalidation) {
					"Failed to invalidate exchange data for $dataOwnerType data owner group $dataOwnerGroupId following removal of ${membersToRemoveIds.size} members."
				}
			}
		}
		confirmedRemoval.forEach { emit(it) }
	}

	private fun getExchangeDataMainIdsForParticipants(
		datastoreInfo: IDatastoreInformation,
		participant: String
	) = flow {
		var nextDocumentId: String? = null
		do {
			val retrieved = exchangeDataDao.findMainExchangeDataIdsByParticipant(
				datastoreInfo,
				participant,
				nextDocumentId,
				MAX_LIGHT_QUERY_SIZE + 1
			)
			var emitted = 0
			nextDocumentId = null
			retrieved.collect {
				if (emitted++ == MAX_LIGHT_QUERY_SIZE) {
					nextDocumentId = it
				} else {
					emit(it)
				}
			}
		} while (nextDocumentId != null)
	}

	private fun getExchangeDataMainIdsForParticipantFilteringHasRecipientPiece(
		datastoreInfo: IDatastoreInformation,
		participant: String,
		recipients: List<String>,
	): Flow<String> = flow {
		val remainingKeys = recipients.toCollection(ArrayDeque())
		var nextDocumentId: String? = null
		do {
			val retrieved = exchangeDataDao.findMainExchangeDataIdsByParticipantForRecipients(
				datastoreInfo,
				participant,
				remainingKeys,
				nextDocumentId,
				MAX_LIGHT_QUERY_SIZE + 1
			).filterIsInstance<ViewRowNoDoc<*, *>>()
			var emitted = 0
			nextDocumentId = null
			retrieved.collect {
				if (emitted++ == MAX_LIGHT_QUERY_SIZE) {
					nextDocumentId = it.id
					val nextKeysStart = (it.key as ComplexKey).components[1] as String
					while (remainingKeys.first() != nextKeysStart) {
						remainingKeys.removeFirst()
					}
				} else {
					emit(it.value as String? ?: it.id)
				}
			}
		} while (nextDocumentId != null)
	}
}
