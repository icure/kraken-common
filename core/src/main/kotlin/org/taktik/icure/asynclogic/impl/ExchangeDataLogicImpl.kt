package org.taktik.icure.asynclogic.impl

import com.fasterxml.jackson.databind.ObjectMapper
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.toList
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Service
import org.taktik.couchdb.ViewQueryResultEvent
import org.taktik.icure.asyncdao.EntityInfoDAO
import org.taktik.icure.asyncdao.ExchangeDataDAO
import org.taktik.icure.asynclogic.SessionInformationProvider
import org.taktik.icure.asynclogic.ExchangeDataLogic
import org.taktik.icure.asynclogic.datastore.DatastoreInstanceProvider
import org.taktik.icure.db.PaginationOffset
import org.taktik.icure.entities.Device
import org.taktik.icure.entities.ExchangeData
import org.taktik.icure.entities.HealthcareParty
import org.taktik.icure.entities.Patient
import org.taktik.icure.entities.DataOwnerType
import org.taktik.icure.pagination.PaginationElement
import org.taktik.icure.pagination.limitIncludingKey
import org.taktik.icure.pagination.toPaginatedFlow
import org.taktik.icure.services.external.rest.v2.utils.paginatedList

@Service
@Profile("app")
class ExchangeDataLogicImpl(
    private val exchangeDataDAO: ExchangeDataDAO,
    private val sessionLogic: SessionInformationProvider,
    private val datastoreInstanceProvider: DatastoreInstanceProvider,
    @Qualifier("baseEntityInfoDao") private val baseEntityInfoDao: EntityInfoDAO,
    @Qualifier("patientEntityInfoDao") private val patientEntityInfoDao: EntityInfoDAO,
    private val objectMapper: ObjectMapper
) : ExchangeDataLogic {
    companion object {
        const val PAGE_SIZE = 100
    }

    // Using values + when ensures we get compilation errors if we add more types and forget to update this.
    private val dataOwnerTypeToQualifiedName = DataOwnerType.entries.associateWith {
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

    override fun findExchangeDataByParticipant(dataOwnerId: String, paginationOffset: PaginationOffset<String>): Flow<PaginationElement> = flow {
        emitAll(exchangeDataDAO
            .findExchangeDataByParticipant(datastoreInstanceProvider.getInstanceAndGroup(), dataOwnerId, paginationOffset.limitIncludingKey())
            .toPaginatedFlow<ExchangeData>(paginationOffset.limit)
        )
    }

    override fun findExchangeDataByDelegatorDelegatePair(delegatorId: String, delegateId: String): Flow<ExchangeData> = flow {
        emitAll(exchangeDataDAO.findExchangeDataByDelegatorDelegatePair(datastoreInstanceProvider.getInstanceAndGroup(), delegatorId, delegateId))
    }

    override suspend fun createExchangeData(exchangeData: ExchangeData): ExchangeData {
        require(sessionLogic.getCurrentDataOwnerId() == exchangeData.delegator) {
            "When creating new exchange data you are the delegator, but provided data has ${exchangeData.delegator} as delegator."
        }
        require(exchangeData.rev == null) { "New exchange data should not have a revision number." }
        return checkNotNull(exchangeDataDAO.create(datastoreInstanceProvider.getInstanceAndGroup(), exchangeData)) {
            "Exchange data creation returned null."
        }
    }

    override suspend fun modifyExchangeData(exchangeData: ExchangeData): ExchangeData {
        require(exchangeData.rev != null) { "Exchange data to modify should have a revision number" }
        return checkNotNull(exchangeDataDAO.save(datastoreInstanceProvider.getInstanceAndGroup(), exchangeData)) {
            "Exchange data modification returned null"
        }
    }

    override fun getParticipantCounterparts(
        dataOwnerId: String,
        counterpartsType: List<DataOwnerType>,
        ignoreOnEntryForFingerprint: String?
    ): Flow<String> = flow {
        require(counterpartsType.isNotEmpty()) { "At least one counterpart type should be provided." }
        val datastoreInfo = datastoreInstanceProvider.getInstanceAndGroup()
        val allAnalyised = mutableSetOf<String>()
        var nextPage: String? = null
        do {
            val dataForParticipantPage = exchangeDataDAO.findExchangeDataByParticipant(
                datastoreInfo,
                dataOwnerId,
                PaginationOffset(PAGE_SIZE + 1, nextPage)
            ).paginatedList<ExchangeData>(PAGE_SIZE, objectMapper = objectMapper)
            nextPage = dataForParticipantPage.nextKeyPair?.startKeyDocId
            val counterpartsIds = dataForParticipantPage.rows
                .let { rows ->
                    if (ignoreOnEntryForFingerprint != null) {
                        rows.filterNot {
                            it.exchangeKey.containsKey(ignoreOnEntryForFingerprint)
                                && it.accessControlSecret.containsKey(ignoreOnEntryForFingerprint)
                                && it.sharedSignatureKey.containsKey(ignoreOnEntryForFingerprint)
                        }
                    } else rows
                }
                .flatMap { listOf(it.delegator, it.delegate) }
                .toSet() - dataOwnerId - allAnalyised
            allAnalyised.addAll(counterpartsIds)
            emitAll(filterDataOwnersWithTypes(counterpartsIds, counterpartsType.toSet()))
        } while (nextPage != null)
    }

    private fun filterDataOwnersWithTypes(
        dataOwnerIds: Collection<String>,
        dataOwnerTypes: Set<DataOwnerType>
    ): Flow<String> = if (dataOwnerTypes.toSet() == DataOwnerType.entries.toSet()) {
        dataOwnerIds.asFlow()
    } else flow {
        val datastoreInfo = datastoreInstanceProvider.getInstanceAndGroup()
        var remainingIds = dataOwnerIds
        val acceptableTypes = dataOwnerTypes.map { dataOwnerTypeToQualifiedName.getValue(it) }.toSet()
        listOfNotNull(
            baseEntityInfoDao.takeIf { DataOwnerType.HCP in dataOwnerTypes || DataOwnerType.DEVICE in dataOwnerTypes },
            patientEntityInfoDao.takeIf { DataOwnerType.PATIENT in dataOwnerTypes }
        ).forEach { entityInfoDao ->
            if (remainingIds.isNotEmpty()) {
                val infoForCurrentType = entityInfoDao.getEntitiesInfo(datastoreInfo, remainingIds).toList()
                val idsForCurrentType = infoForCurrentType
                    .filter { it.fullyQualifiedName in acceptableTypes }
                    .map { it.id }
                    .toSet()

                idsForCurrentType.forEach { emit(it) }
                remainingIds -= idsForCurrentType
            }
        }
    }
}
