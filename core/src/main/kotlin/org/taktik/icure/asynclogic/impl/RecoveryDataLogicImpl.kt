package org.taktik.icure.asynclogic.impl

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.count
import kotlinx.coroutines.flow.single
import kotlinx.coroutines.flow.toList
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Service
import org.taktik.couchdb.DocIdentifier
import org.taktik.couchdb.entity.IdAndRev
import org.taktik.icure.asyncdao.RecoveryDataDAO
import org.taktik.icure.asynclogic.RecoveryDataLogic
import org.taktik.icure.asynclogic.datastore.DatastoreInstanceProvider
import org.taktik.icure.asynclogic.datastore.IDatastoreInformation
import org.taktik.icure.entities.RecoveryData
import org.taktik.icure.exceptions.NotFoundRequestException

@Service
@Profile("app")
class RecoveryDataLogicImpl(
    private val recoveryDataDAO: RecoveryDataDAO,
    private val datastoreInstanceProvider: DatastoreInstanceProvider,
) : RecoveryDataLogic {
    override suspend fun createRecoveryData(recoveryData: RecoveryData): RecoveryData {
        require(recoveryData.expirationInstant.let { it == null || it > System.currentTimeMillis() }) {
            "Recovery data with expiration time must expire in the future"
        }
        return checkNotNull(recoveryDataDAO.create(datastoreInstanceProvider.getInstanceAndGroup(), recoveryData)) {
            "DAO create returned null"
        }
    }

    override suspend fun getRecoveryData(id: String): RecoveryData? =
        recoveryDataDAO.get(datastoreInstanceProvider.getInstanceAndGroup(), id)?.let { data ->
            if (data.expirationInstant?.let { it < System.currentTimeMillis() } == true) {
                recoveryDataDAO.purge(datastoreInstanceProvider.getInstanceAndGroup(), listOf(data)).single()
                null
            } else data
        }

    override suspend fun deleteRecoveryData(id: String): DocIdentifier =
        recoveryDataDAO.get(datastoreInstanceProvider.getInstanceAndGroup(), id)?.let { data ->
            recoveryDataDAO.purge(datastoreInstanceProvider.getInstanceAndGroup(), listOf(data)).single().entityOrThrow()
        } ?: throw NotFoundRequestException("Recovery data with $id not found")

    override suspend fun deleteAllRecoveryDataForRecipient(recipientId: String): Int = deleteMany {
        recoveryDataDAO.findRecoveryDataIdsByRecipientAndType(it, recipientId, null)
    }

    override suspend fun deleteAllRecoveryDataOfTypeForRecipient(
        type: RecoveryData.Type,
        recipientId: String
    ): Int = deleteMany {
        recoveryDataDAO.findRecoveryDataIdsByRecipientAndType(it, recipientId, type)
    }

    private suspend inline fun deleteMany(
        getData: (IDatastoreInformation) -> Flow<RecoveryData>
    ): Int {
        val datastoreInfo = datastoreInstanceProvider.getInstanceAndGroup()
        val toPurge = getData(datastoreInfo).toList()
        return recoveryDataDAO.purge(datastoreInfo, toPurge).count()
    }
}