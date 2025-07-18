package org.taktik.icure.asynclogic.impl

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.single
import kotlinx.coroutines.flow.toList
import org.springframework.context.annotation.Profile
import org.springframework.security.access.AccessDeniedException
import org.springframework.stereotype.Service
import org.taktik.icure.asyncdao.ExchangeDataMapDAO
import org.taktik.icure.asyncdao.results.BulkSaveResult
import org.taktik.icure.asynclogic.ExchangeDataMapLogic
import org.taktik.icure.datastore.DatastoreInstanceProvider
import org.taktik.icure.datastore.IDatastoreInformation
import org.taktik.icure.asynclogic.impl.filter.Filters
import org.taktik.icure.entities.ExchangeDataMap
import org.taktik.icure.entities.utils.Base64String
import org.taktik.icure.entities.utils.HexString
import org.taktik.icure.entities.utils.KeypairFingerprintV2String
import org.taktik.icure.security.hashAccessControlKey
import org.taktik.icure.utils.hexStringToByteArray
import org.taktik.icure.validation.aspect.Fixer
import java.lang.IllegalStateException

open class ExchangeDataMapLogicImpl(
    private val exchangeDataMapDAO: ExchangeDataMapDAO,
    private val datastoreInstanceProvider: DatastoreInstanceProvider
) : ExchangeDataMapLogic {

    override suspend fun modifyExchangeDataMap(exchangeDataMap: ExchangeDataMap): ExchangeDataMap? =
        exchangeDataMapDAO.save(datastoreInstanceProvider.getInstanceAndGroup(), exchangeDataMap)

    override fun getExchangeDataMapBatch(ids: Collection<String>): Flow<ExchangeDataMap> = flow {
        emitAll(exchangeDataMapDAO.getEntities(datastoreInstanceProvider.getInstanceAndGroup(), ids))
    }

    override fun createOrUpdateExchangeDataMapBatchById(
        batch: Map<HexString, Map<KeypairFingerprintV2String, Base64String>>
    ): Flow<ExchangeDataMap> = flow {
        emitAll(doCreateOrUpdateExchangeDataMapBatchById(batch, datastoreInstanceProvider.getInstanceAndGroup()))
    }

    override fun createOrUpdateExchangeDataMapBatchByAccessControlKey(batch: Map<HexString, Map<KeypairFingerprintV2String, Base64String>>): Flow<ExchangeDataMap> =
        createOrUpdateExchangeDataMapBatchById(batch.mapKeys { (k, _) -> hashAccessControlKey(k.hexStringToByteArray()) })

    protected fun doCreateOrUpdateExchangeDataMapBatchById(
        batch: Map<HexString, Map<KeypairFingerprintV2String, Base64String>>,
        datastoreInstance: IDatastoreInformation
    ): Flow<ExchangeDataMap> = flow {
        val filteredBatch = batch.filterValues { it.isNotEmpty() }
        if (filteredBatch.isNotEmpty()) {
            val existingBatch = exchangeDataMapDAO.getEntities(datastoreInstance).toList().associateBy { it.id }
            val updatedBatch = filteredBatch.mapNotNull { (secDelKey, encryptedExchangeDataIds) ->
                val originalXDM = existingBatch[secDelKey]
                when {
                    // Does not exist yet, create a new one
                    originalXDM == null -> ExchangeDataMap(
                        id = secDelKey,
                        encryptedExchangeDataIds = encryptedExchangeDataIds
                    )
                    // Exists and contains values encrypted with all requested keys, nothing to do
                    originalXDM.encryptedExchangeDataIds.keys.containsAll(encryptedExchangeDataIds.keys) -> null
                    // Exists but misses values for some keys from the new request, add missing ones, leave others untouched
                    else -> originalXDM.copy(encryptedExchangeDataIds = encryptedExchangeDataIds + originalXDM.encryptedExchangeDataIds)
                }
            }
            if (updatedBatch.isNotEmpty()) {
                emitAll(
                    exchangeDataMapDAO.saveBulk(datastoreInstance, updatedBatch)
                        .mapNotNull {
                            when (it) {
                                is BulkSaveResult.Success<ExchangeDataMap> -> it.entity
                                is BulkSaveResult.Failure -> {
                                    when (it.code) {
                                        500 -> throw IllegalStateException(it.message)
                                        403 -> throw AccessDeniedException(it.message)
                                        409 -> null
                                        else -> throw IllegalStateException("Unexpected exception: ${it.code} - ${it.message}")
                                    }
                                }
                            }
                        }
                )
            }
        }
    }
}
