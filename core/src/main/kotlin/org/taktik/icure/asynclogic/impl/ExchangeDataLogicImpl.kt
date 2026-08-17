package org.taktik.icure.asynclogic.impl

import com.fasterxml.jackson.databind.ObjectMapper
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.flow.toSet
import org.taktik.couchdb.ViewQueryResultEvent
import org.taktik.couchdb.ViewRowWithDoc
import org.taktik.couchdb.entity.ComplexKey
import org.taktik.icure.asyncdao.EntityInfoDAO
import org.taktik.icure.asyncdao.ExchangeDataDAO
import org.taktik.icure.asyncdao.results.filterSuccessfulUpdates
import org.taktik.icure.asynclogic.ExchangeDataLogic
import org.taktik.icure.datastore.DatastoreInstanceProvider
import org.taktik.icure.datastore.IDatastoreInformation
import org.taktik.icure.db.PaginationOffset
import org.taktik.icure.entities.DataOwnerType
import org.taktik.icure.entities.Device
import org.taktik.icure.entities.ExchangeData
import org.taktik.icure.entities.HealthcareParty
import org.taktik.icure.entities.Patient
import org.taktik.icure.entities.requests.ExchangeDataPieceCreationRequest
import org.taktik.icure.exceptions.ConflictRequestException
import org.taktik.icure.exceptions.NotFoundRequestException
import org.taktik.icure.pagination.MultiKeyPaginationElement
import org.taktik.icure.pagination.NextPageElement
import org.taktik.icure.pagination.PaginationElement
import org.taktik.icure.pagination.PaginationRowElement
import org.taktik.icure.pagination.limitIncludingKey
import org.taktik.icure.pagination.toMultiKeyPaginatedFlow
import org.taktik.icure.pagination.toPaginatedFlow
import org.taktik.icure.services.external.rest.v2.utils.paginatedList
import org.taktik.icure.utils.Hasher
import org.taktik.icure.validation.DataOwnerProvider

open class ExchangeDataLogicImpl(
	private val exchangeDataDAO: ExchangeDataDAO,
	private val datastoreInstanceProvider: DatastoreInstanceProvider,
	private val baseEntityInfoDao: EntityInfoDAO,
	private val patientEntityInfoDao: EntityInfoDAO,
	private val objectMapper: ObjectMapper,
	private val dataOwnerProvider: DataOwnerProvider,
) : ExchangeDataLogic {
	companion object {
		const val PAGE_SIZE = 300

		/**
		 * Smallest page a caller of [findNonGroupPieceCounterparts] may ask for. A page is shortened by the data owner
		 * type and keypair filters rather than refilled, so a minimum is what bounds how many requests a caller can be
		 * forced into to exhaust the search.
		 */
		const val MIN_COUNTERPARTS_PAGE_SIZE = 100

		/**
		 * Largest page a caller of [findNonGroupPieceCounterparts] may ask for, and the page size it uses when the
		 * caller asks for none.
		 */
		const val MAX_COUNTERPARTS_PAGE_SIZE = 1000
	}

	// Using values + when ensures we get compilation errors if we add more types and forget to update this.
	private val dataOwnerTypeToQualifiedName =
		DataOwnerType.entries.associateWith {
			when (it) {
				DataOwnerType.HCP -> HealthcareParty::class.qualifiedName!!
				DataOwnerType.DEVICE -> Device::class.qualifiedName!!
				DataOwnerType.PATIENT -> Patient::class.qualifiedName!!
			}
		}

	override suspend fun getExchangeDataById(id: String): ExchangeData? {
		// Leaks information on exchange data with provided id actually existing, but should not be a security concern
		return exchangeDataDAO.get(datastoreInstanceProvider.getInstanceAndGroup(), id)
	}

	override fun getExchangeDataByIds(ids: List<String>): Flow<ExchangeData> = flow {
		emitAll(exchangeDataDAO.getEntities(datastoreInstanceProvider.getInstanceAndGroup(), ids))
	}

	override fun findExchangeDataByParticipant(
		dataOwnerId: String,
		paginationOffset: PaginationOffset<String>,
	): Flow<PaginationElement> = flow {
		emitAll(
			exchangeDataDAO
				.findExchangeDataByParticipant(
					datastoreInstanceProvider.getInstanceAndGroup(),
					dataOwnerId,
					paginationOffset.limitIncludingKey(),
				).toPaginatedFlow<ExchangeData>(paginationOffset.limit),
		)
	}

	override fun findExchangeDataByDelegatorDelegatePair(
		delegatorId: String,
		delegateId: String,
	): Flow<ExchangeData> = flow {
		emitAll(
			exchangeDataDAO.findExchangeDataByDelegatorDelegatePair(datastoreInstanceProvider.getInstanceAndGroup(), delegatorId, delegateId),
		)
	}

	override suspend fun createExchangeData(exchangeData: ExchangeData): ExchangeData {
		validateExchangeDataSimpleCreate(exchangeData)
		return checkNotNull(exchangeDataDAO.create(datastoreInstanceProvider.getInstanceAndGroup(), exchangeData)) {
			"Exchange data creation returned null."
		}
	}

	override fun createExchangeDatas(exchangeDatas: List<ExchangeData>): Flow<ExchangeData> = flow {
		exchangeDatas.forEach { validateExchangeDataSimpleCreate(it) }
		val datastoreInfo = datastoreInstanceProvider.getInstanceAndGroup()
		emitAll(
			exchangeDataDAO.saveBulk(
				datastoreInfo,
				exchangeDatas,
			).filterSuccessfulUpdates()
		)
	}

	protected fun validateExchangeDataSimpleCreate(exchangeData: ExchangeData) {
		require(exchangeData.rev == null) { "Can't create new exchange data with rev" }
		require(exchangeData.recipient == null && exchangeData.exchangeDataGroupId == null) {
			"You must use the createExchangeDataGroupPieces method to create exchange data pieces"
		}
	}

	protected suspend fun validateModifyExchangeData(datastoreInfo: IDatastoreInformation, updatedExchangeData: ExchangeData) {
		val original =
			exchangeDataDAO.get(datastoreInfo, updatedExchangeData.id) ?: throw NotFoundRequestException(
				"Can't find exchange data ${updatedExchangeData.id}",
			)
		if (original.rev != updatedExchangeData.rev) throw ConflictRequestException("Outdated rev for exchange data")
		require(updatedExchangeData.delegator == original.delegator && updatedExchangeData.delegate == original.delegate) {
			"Can't modify delegator or delegate of exchange data"
		}
		require(updatedExchangeData.recipient == original.recipient) {
			"Can't modify recipient of exchange data"
		}
		require(updatedExchangeData.exchangeDataGroupId == original.exchangeDataGroupId) {
			"Can't modify exchange data group id"
		}
	}

	override suspend fun modifyExchangeData(exchangeData: ExchangeData): ExchangeData {
		val datastoreInfo = datastoreInstanceProvider.getInstanceAndGroup()
		validateModifyExchangeData(datastoreInfo, exchangeData)
		return checkNotNull(exchangeDataDAO.save(datastoreInfo, exchangeData)) {
			"Exchange data modification returned null"
		}
	}

	@Deprecated("Use findNonGroupPieceCounterparts")
	override fun getParticipantCounterparts(
		dataOwnerId: String,
		counterpartsType: List<DataOwnerType>,
		ignoreOnEntryForFingerprint: String?,
	): Flow<String> = flow {
		require(counterpartsType.isNotEmpty()) { "At least one counterpart type should be provided." }
		val datastoreInfo = datastoreInstanceProvider.getInstanceAndGroup()
		val allAnalyzed = mutableSetOf<String>()
		var nextPage: String? = null
		do {
			val dataForParticipantPage =
				exchangeDataDAO
					.findExchangeDataByParticipant(
						datastoreInfo,
						dataOwnerId,
						PaginationOffset(PAGE_SIZE + 1, nextPage),
					).paginatedList<ExchangeData>(PAGE_SIZE, objectMapper = objectMapper)
			nextPage = dataForParticipantPage.nextKeyPair?.startKeyDocId
			val counterpartsIds =
				dataForParticipantPage.rows
					.let { rows ->
						if (ignoreOnEntryForFingerprint != null) {
							rows.filterNot {
								it.exchangeKey.containsKey(ignoreOnEntryForFingerprint) &&
									it.accessControlSecret.containsKey(ignoreOnEntryForFingerprint) &&
									it.sharedSignatureKey.containsKey(ignoreOnEntryForFingerprint)
							}
						} else {
							rows
						}
					}.flatMap { listOf(it.delegator, it.delegate) }
					.filter { '/' !in it } // Ignore references to data owners in other groups
					.toSet() -
					dataOwnerId -
					allAnalyzed
			allAnalyzed.addAll(counterpartsIds)
			emitAll(filterDataOwnersWithTypes(datastoreInfo, counterpartsIds, counterpartsType.toSet()))
		} while (nextPage != null)
	}

	protected fun doFindNonGroupPieceCounterparts(
		datastoreInformation: IDatastoreInformation,
		dataOwnerId: String,
		counterpartsTypes: List<DataOwnerType>,
		ignoreOnEntryForFingerprint: String?,
		startCounterpartId: String?,
		limit: Int?,
	): Flow<PaginationElement> = flow {
		require(counterpartsTypes.isNotEmpty()) { "At least one counterpart type should be provided." }
		val pageSize = limit ?: MAX_COUNTERPARTS_PAGE_SIZE
		require(pageSize in MIN_COUNTERPARTS_PAGE_SIZE..MAX_COUNTERPARTS_PAGE_SIZE) {
			"The limit should be between $MIN_COUNTERPARTS_PAGE_SIZE and $MAX_COUNTERPARTS_PAGE_SIZE."
		}
		/*
		 * One request is one database query. The counterparts dropped by the two filters below shorten the page instead
		 * of triggering another query to refill it, and the caller follows the cursor rather than counting rows. The
		 * extra row is only asked for to tell a full page apart from the last one, and is never part of the page.
		 */
		val rows = exchangeDataDAO
			.findNonGroupPieceCounterparts(datastoreInformation, dataOwnerId, startCounterpartId, pageSize + 1)
			.toList()
		val pageRows = rows.take(pageSize)
		val candidates = pageRows
			.filterNot { ignoreOnEntryForFingerprint != null && ignoreOnEntryForFingerprint in it.usableKeypairFingerprints }
			.map { it.counterpartId }
		/*
		 * filterDataOwnersWithTypes emits the ids grouped by the dao that recognised them, but the cursor is the last
		 * counterpart of the page, so the rows have to keep the order of the view for the two to agree.
		 */
		val acceptedIds = filterDataOwnersWithTypes(datastoreInformation, candidates, counterpartsTypes.toSet()).toSet()
		candidates.forEach { if (it in acceptedIds) emit(PaginationRowElement<String, String>(it)) }
		// The cursor is built from the raw rows, before either filter, so that nothing is skipped across a page boundary.
		if (rows.size > pageSize) {
			emit(NextPageElement<String>(startKey = pageRows.last().counterpartId))
		}
	}

	override fun findNonGroupPieceCounterparts(
		dataOwnerId: String,
		counterpartsTypes: List<DataOwnerType>,
		ignoreOnEntryForFingerprint: String?,
		startCounterpartId: String?,
		limit: Int?,
	): Flow<PaginationElement> = flow {
		emitAll(
			doFindNonGroupPieceCounterparts(
				datastoreInstanceProvider.getInstanceAndGroup(),
				dataOwnerId,
				counterpartsTypes,
				ignoreOnEntryForFingerprint,
				startCounterpartId,
				limit,
			),
		)
	}

	private fun filterDataOwnersWithTypes(
		datastoreInformation: IDatastoreInformation,
		dataOwnerIds: Collection<String>,
		dataOwnerTypes: Set<DataOwnerType>,
	): Flow<String> = if (dataOwnerTypes.toSet() == DataOwnerType.entries.toSet()) {
		dataOwnerIds.asFlow()
	} else {
		flow {
			var remainingIds = dataOwnerIds
			val acceptableTypes = dataOwnerTypes.map { dataOwnerTypeToQualifiedName.getValue(it) }.toSet()
			listOfNotNull(
				baseEntityInfoDao.takeIf { DataOwnerType.HCP in dataOwnerTypes || DataOwnerType.DEVICE in dataOwnerTypes },
				patientEntityInfoDao.takeIf { DataOwnerType.PATIENT in dataOwnerTypes },
			).forEach { entityInfoDao ->
				if (remainingIds.isNotEmpty()) {
					val infoForCurrentType = entityInfoDao.getEntitiesInfo(datastoreInformation, remainingIds).toList()
					val idsForCurrentType =
						infoForCurrentType
							.filter { it.fullyQualifiedName in acceptableTypes }
							.map { it.id }
							.toSet()

					idsForCurrentType.forEach { emit(it) }
					remainingIds -= idsForCurrentType
				}
			}
		}
	}

	/**
	 * Converts the raw view results of a recipient-filtered "by_keys" DAO query into a [MultiKeyPaginationElement]
	 * flow, requesting one more entity than [PAGE_SIZE] from [daoQuery] so that [toMultiKeyPaginatedFlow] can build
	 * the [MultiKeyPaginationElement.NextPage] cursor from it.
	 *
	 * Which component of the view key holds the recipient depends on the view, so it is up to [recipientOfKey] to
	 * extract it: every recipient-filtered view keys the recipient last today, but that is a property of each view
	 * and not something this method assumes.
	 */
	private inline fun multiKeyPaginatedFlow(
		filterRecipients: List<String?>,
		crossinline recipientOfKey: (key: ComplexKey) -> String?,
		daoQuery: (limit: Int) -> Flow<ViewQueryResultEvent>,
	): Flow<MultiKeyPaginationElement<ExchangeData, String?>> = daoQuery(PAGE_SIZE + 1).toMultiKeyPaginatedFlow(
		pageSize = PAGE_SIZE,
		keys = filterRecipients,
		keyOfRow = { recipientOfKey(it.key as ComplexKey) },
		rowTransform = { it.doc as ExchangeData },
	)

	protected fun doFindExchangeDataGroupByIdForRecipients(
		datastoreInformation: IDatastoreInformation,
		exchangeDataOrGroupId: String,
		filterRecipients: List<String?>,
		startDocumentId: String?,
	): Flow<MultiKeyPaginationElement<ExchangeData, String?>> = multiKeyPaginatedFlow(
		filterRecipients = filterRecipients,
		// by_exchange_data_group_id_recipient is keyed by [exchangeDataGroupId ?: _id, recipient].
		recipientOfKey = { it.components[1] as String? },
	) { limit ->
		exchangeDataDAO.findExchangeDataGroupByIdForRecipients(datastoreInformation, exchangeDataOrGroupId, filterRecipients, startDocumentId, limit)
	}

	override fun findExchangeDataGroupByIdForRecipients(
		exchangeDataOrGroupId: String,
		filterRecipients: List<String?>,
		startDocumentId: String?,
	): Flow<MultiKeyPaginationElement<ExchangeData, String?>> = flow {
		emitAll(
			doFindExchangeDataGroupByIdForRecipients(
				datastoreInstanceProvider.getInstanceAndGroup(),
				exchangeDataOrGroupId,
				filterRecipients,
				startDocumentId,
			),
		)
	}

	protected fun doFindExchangeDataByParticipantForRecipients(
		datastoreInformation: IDatastoreInformation,
		dataOwnerId: String,
		filterRecipients: List<String?>,
		startDocumentId: String?,
	): Flow<MultiKeyPaginationElement<ExchangeData, String?>> = multiKeyPaginatedFlow(
		filterRecipients = filterRecipients,
		// by_participant_recipient is keyed by [participant, recipient].
		recipientOfKey = { it.components[1] as String? },
	) { limit ->
		exchangeDataDAO.findExchangeDataByParticipantForRecipients(datastoreInformation, dataOwnerId, filterRecipients, startDocumentId, limit)
	}

	override fun findExchangeDataByParticipantForRecipients(
		dataOwnerId: String,
		filterRecipients: List<String?>,
		startDocumentId: String?,
	): Flow<MultiKeyPaginationElement<ExchangeData, String?>> = flow {
		emitAll(
			doFindExchangeDataByParticipantForRecipients(
				datastoreInstanceProvider.getInstanceAndGroup(),
				dataOwnerId,
				filterRecipients,
				startDocumentId,
			),
		)
	}

	protected fun doFindExchangeDataByDelegatorDelegateForRecipients(
		datastoreInformation: IDatastoreInformation,
		delegatorId: String,
		delegateId: String,
		filterRecipients: List<String?>,
		startDocumentId: String?,
	): Flow<MultiKeyPaginationElement<ExchangeData, String?>> = multiKeyPaginatedFlow(
		filterRecipients = filterRecipients,
		// by_delegator_delegate_recipient is keyed by [delegator, delegate, recipient].
		recipientOfKey = { it.components[2] as String? },
	) { limit ->
		exchangeDataDAO.findExchangeDataByDelegatorDelegateForRecipients(datastoreInformation, delegatorId, delegateId, filterRecipients, startDocumentId, limit)
	}

	override fun findExchangeDataByDelegatorDelegateForRecipients(
		delegatorId: String,
		delegateId: String,
		filterRecipients: List<String?>,
		startDocumentId: String?,
	): Flow<MultiKeyPaginationElement<ExchangeData, String?>> = flow {
		emitAll(
			doFindExchangeDataByDelegatorDelegateForRecipients(
				datastoreInstanceProvider.getInstanceAndGroup(),
				delegatorId,
				delegateId,
				filterRecipients,
				startDocumentId,
			),
		)
	}

	protected fun doFindExchangeDataGroupById(
		datastoreInformation: IDatastoreInformation,
		exchangeDataOrGroupId: String,
		paginationOffset: PaginationOffset<ComplexKey>,
	): Flow<PaginationElement> {
		// Never request more than a page at a time from the db, no matter what the caller asked for.
		val cappedOffset = paginationOffset.copy(limit = paginationOffset.limit.coerceAtMost(PAGE_SIZE))
		return exchangeDataDAO
			.findExchangeDataGroupById(datastoreInformation, exchangeDataOrGroupId, cappedOffset.limitIncludingKey())
			.toPaginatedFlow<ExchangeData>(cappedOffset.limit)
	}

	override fun findExchangeDataGroupById(
		exchangeDataOrGroupId: String,
		paginationOffset: PaginationOffset<ComplexKey>,
	): Flow<PaginationElement> = flow {
		emitAll(doFindExchangeDataGroupById(datastoreInstanceProvider.getInstanceAndGroup(), exchangeDataOrGroupId, paginationOffset))
	}

	override fun createExchangeDataGroupPieces(
		exchangeDataGroupId: String,
		delegator: String,
		delegate: String,
		piecesByRecipient: Map<String, ExchangeDataPieceCreationRequest>,
	): Flow<ExchangeData> = flow {
		emitAll(
			doCreateExchangeDataGroupPieces(
				datastoreInstanceProvider.getInstanceAndGroup(),
				exchangeDataGroupId,
				delegator,
				delegate,
				piecesByRecipient
			)
		)
	}

	protected fun doCreateExchangeDataGroupPieces(
		datastoreInfo: IDatastoreInformation,
		exchangeDataGroupId: String,
		delegator: String,
		delegate: String,
		piecesByRecipient: Map<String, ExchangeDataPieceCreationRequest>,
	): Flow<ExchangeData> = flow {
		require (piecesByRecipient.isNotEmpty()) {
			"At least one piece of exchange data should be provided."
		}
		// For a group we must have a piece that has the same id has the group: for that group the recipient must be the delegator
		val existingForGroup = exchangeDataDAO.get(datastoreInfo, exchangeDataGroupId)
		/*
		 * Note: this check is not 100% safe: due to replication and in general concurrency it is possible to have 2
		 * concurrent requests with the same exchangeDataGroupId but conflicting delegator/delegate that both pass the
		 * check below and create data that is not valid.
		 * This should not be a problem:
		 * - Only adversarial clients can do this, good clients will not have this issue
		 * - Adversarial clients will not be able to corrupt valid exchange data of other data owners by abusing this
		 *   (assuming prediction of exchangeDataGroupId is impossible)
		 * - Good clients may not be able to access corrupt exchange data created by abusing this, but that does not
		 *   prevent decryption of valid data.
		 */
		if (piecesByRecipient.containsKey(delegator)) {
			// The first request to create pieces should contain an entry for the delegator, future ones should not; therefore the first time there should be no existingForGroup
			if (existingForGroup != null) throw ConflictRequestException(
				"There is already some exchange data for the provided exchangeDataGroupId. If you want to create new pieces for an existing group the pieces should not include a recipient entry for the delegator"
			)
		} else {
			requireNotNull(existingForGroup) {
				"The first request to create pieces should contain an entry for the delegator."
			}
			require(
				existingForGroup.recipient == delegator
					&& existingForGroup.exchangeDataGroupId == exchangeDataGroupId
					&& existingForGroup.delegator == delegator
					&& existingForGroup.delegate == delegate
			) {
				"The request does not match the existing exchange data for the provided exchangeDataGroupId."
			}
		}
		val toCreate = piecesByRecipient.map { (recipient, piece) ->
			require(recipient == delegator || piece.delegatorSignature.isEmpty()) {
				"Delegator signature should only be present on the piece of exchange data for the delegator."
			}
			/*
			 * We intentionally allow no delegator signature on the recipient piece; this allows creating exchange data
			 * that will never be used for encryption by the SDK, i.e. exchange data that is already invalidated.
			 * Sample use case: server-side mass migration of unencrypted data to something encrypted. To encrypt the
			 * data the server creates exchange data for the main parent HCP of the group, but can't (and shouldn't)
			 * sign it or decrypt it. The migration process only keeps the aesKey and accessControl secret in volatile
			 * memory for only the time required to perform the migration then forgets it; once completed the hcp can
			 * decrypt the migrated data through this exchange data, but the SDK will never trust the exchange data for
			 * encryption.
			 *
			 * Note that the delegator signature is also how existing exchange data is invalidated: it is deleted, and
			 * there is no `invalidated` flag. A flag could be flipped back by anyone with write access to the database,
			 * while the signature can only be recreated by an actor holding the private key of the delegator. This
			 * means the server never needs to (and never does) enforce that invalidated exchange data stays
			 * invalidated. The shared signature is never the one removed: it protects the whole piece from tampering,
			 * so removing it would void the integrity guarantee rather than the trust needed for encryption.
			 */
			ExchangeData(
				id = if (recipient == delegator) exchangeDataGroupId else Hasher.sha256Alphanumeric("$exchangeDataGroupId|$recipient"),
				rev = null,
				delegator = delegator,
				delegate = delegate,
				recipient = recipient,
				exchangeDataGroupId = exchangeDataGroupId,
				exchangeKey = piece.exchangeKey,
				accessControlSecret = piece.accessControlSecret,
				sharedSignatureKey = piece.sharedSignatureKey,
				delegatorSignature = piece.delegatorSignature,
				sharedSignature = piece.sharedSignature,
			)
		}

		emitAll(
			exchangeDataDAO.saveBulk(
				datastoreInfo,
				toCreate,
			).filterSuccessfulUpdates()
		)
	}
}
