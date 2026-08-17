package org.taktik.icure.asynclogic.impl

import com.fasterxml.jackson.databind.JsonMappingException
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import org.taktik.couchdb.ViewRowNoDoc
import org.taktik.couchdb.entity.Versionable
import org.taktik.icure.asyncdao.DataOwnerPublicKeysViewValue
import org.taktik.icure.asyncdao.DeviceDAO
import org.taktik.icure.asyncdao.HealthcarePartyDAO
import org.taktik.icure.asyncdao.PatientDAO
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
import org.taktik.icure.entities.base.DataOwnerGroupLinkType
import org.taktik.icure.entities.base.DataOwnerGroupLinkType.Companion.defaultGroupLinkType
import org.taktik.icure.entities.base.DataOwnerHierarchyInfo
import org.taktik.icure.entities.base.asCryptoActorStub
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
import org.taktik.icure.security.resolveHcpHierarchyInfo
import org.taktik.icure.utils.PeekChannel

open class DataOwnerLogicImpl(
	protected val patientDao: PatientDAO,
	protected val hcpDao: HealthcarePartyDAO,
	protected val deviceDao: DeviceDAO,
	private val datastoreInstanceProvider: DatastoreInstanceProvider,
) : DataOwnerLogic {
	companion object {
		private const val MAX_HIERARCHY_DEPTH = 5

		/**
		 * The maximum number of rows a page of [findDataOwnersLinkedToGroups] may hold, and the maximum number of
		 * ids [getDataOwnersPublicKeys] accepts. The page size is a logic-layer decision, so a caller-provided
		 * limit above this is capped; the bulk get has no cursor to resume from, so an oversized request fails
		 * rather than returning a silently incomplete answer.
		 */
		const val MAX_DATA_OWNER_GROUP_QUERY_SIZE = 1000
	}

	override suspend fun getCryptoActorStub(dataOwnerId: String): CryptoActorStubWithType? = getDataOwner(dataOwnerId)?.retrieveStub()

	override fun getCryptoActorStubs(dataOwnerIds: List<String>): Flow<CryptoActorStubWithType> = getDataOwners(dataOwnerIds).map { it.retrieveStub() }

	override suspend fun getCryptoActorStubWithType(
		dataOwnerId: String,
		dataOwnerType: DataOwnerType,
	): CryptoActorStub? = getDataOwnerWithType(dataOwnerId, dataOwnerType, null)?.retrieveStub()?.stub

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
		val cappedLimit = limit.coerceAtMost(MAX_DATA_OWNER_GROUP_QUERY_SIZE)
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
		require(dataOwnerIds.size <= MAX_DATA_OWNER_GROUP_QUERY_SIZE) {
			"Can't get the public keys of more than $MAX_DATA_OWNER_GROUP_QUERY_SIZE data owners at once, got ${dataOwnerIds.size}."
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
			getDataOwnerWithType(dataOwnerId, it, datastoreInfo)
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
	): DataOwnerWithType? {
		val datastoreInfo = preloadedDatastoreInfo ?: datastoreInstanceProvider.getInstanceAndGroup()
		return when (dataOwnerType) {
			DataOwnerType.HCP ->
				wrongTypeAsNull { hcpDao.get(datastoreInfo, dataOwnerId) }
					?.takeIf { it.deletionDate == null }
					?.let { DataOwnerWithType.HcpDataOwner(it) }
			DataOwnerType.DEVICE ->
				wrongTypeAsNull { deviceDao.get(datastoreInfo, dataOwnerId) }
					?.takeIf { it.deletionDate == null }
					?.let { DataOwnerWithType.DeviceDataOwner(it) }
			DataOwnerType.PATIENT ->
				wrongTypeAsNull { patientDao.get(datastoreInfo, dataOwnerId) }
					?.takeIf { it.deletionDate == null }
					?.let { DataOwnerWithType.PatientDataOwner(it) }
		}
	}

	override suspend fun modifyCryptoActor(modifiedCryptoActor: CryptoActorStubWithType): CryptoActorStubWithType {
		val dataOwnerInfo =
			getDataOwnerWithType(modifiedCryptoActor.stub.id, modifiedCryptoActor.type, null)
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
}
