package org.taktik.icure.asynclogic.impl

import com.fasterxml.jackson.databind.JsonMappingException
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Service
import org.taktik.couchdb.entity.Versionable
import org.taktik.icure.asyncdao.DeviceDAO
import org.taktik.icure.asyncdao.HealthcarePartyDAO
import org.taktik.icure.asyncdao.PatientDAO
import org.taktik.icure.asynclogic.DataOwnerLogic
import org.taktik.icure.entities.CryptoActorStub
import org.taktik.icure.entities.CryptoActorStubWithType
import org.taktik.icure.entities.DataOwnerType
import org.taktik.icure.entities.DataOwnerWithType
import org.taktik.icure.entities.base.CryptoActor
import org.taktik.icure.entities.base.HasTags
import org.taktik.icure.entities.base.asCryptoActorStub
import org.taktik.icure.exceptions.ConflictRequestException
import org.taktik.icure.exceptions.DeserializationTypeException
import org.taktik.icure.exceptions.NotFoundRequestException
import java.lang.IllegalArgumentException

@Service
@Profile("app")
class DataOwnerLogicImpl(
    val patientDao: PatientDAO,
    val hcpDao: HealthcarePartyDAO,
    val deviceDao: DeviceDAO,
    private val datastoreInstanceProvider: org.taktik.icure.asynclogic.datastore.DatastoreInstanceProvider
) : DataOwnerLogic {
    override suspend fun getCryptoActorStub(dataOwnerId: String): CryptoActorStubWithType? =
        getDataOwner(dataOwnerId)?.retrieveStub()

    override suspend fun getCryptoActorStubWithType(
        dataOwnerId: String,
        dataOwnerType: DataOwnerType
    ): CryptoActorStub? = getDataOwnerWithType(dataOwnerId, dataOwnerType)?.retrieveStub()?.stub

    override suspend fun getDataOwner(dataOwnerId: String): DataOwnerWithType? {
        val datastoreInfo = datastoreInstanceProvider.getInstanceAndGroup()
        return wrongTypeAsNull { patientDao.get(datastoreInfo, dataOwnerId) }?.takeIf { it.deletionDate == null }?.let {
            DataOwnerWithType.PatientDataOwner(it)
        } ?: wrongTypeAsNull { hcpDao.get(datastoreInfo, dataOwnerId) }?.takeIf { it.deletionDate == null }?.let {
            DataOwnerWithType.HcpDataOwner(it)
        } ?: wrongTypeAsNull { deviceDao.get(datastoreInfo, dataOwnerId) }?.takeIf { it.deletionDate == null }?.let {
            DataOwnerWithType.DeviceDataOwner(it)
        }
    }

    private suspend fun getDataOwnerWithType(
        dataOwnerId: String,
        dataOwnerType: DataOwnerType
    ): DataOwnerWithType? {
        val datastoreInfo = datastoreInstanceProvider.getInstanceAndGroup()
        return when (dataOwnerType) {
            DataOwnerType.HCP -> wrongTypeAsNull { hcpDao.get(datastoreInfo, dataOwnerId) }
                ?.takeIf { it.deletionDate == null }
                ?.let { DataOwnerWithType.HcpDataOwner(it) }
            DataOwnerType.DEVICE -> wrongTypeAsNull { deviceDao.get(datastoreInfo, dataOwnerId) }
                ?.takeIf { it.deletionDate == null }
                ?.let { DataOwnerWithType.DeviceDataOwner(it) }
            DataOwnerType.PATIENT -> wrongTypeAsNull { patientDao.get(datastoreInfo, dataOwnerId) }
                ?.takeIf { it.deletionDate == null }
                ?.let { DataOwnerWithType.PatientDataOwner(it) }
        }
    }

    override suspend fun modifyCryptoActor(modifiedCryptoActor: CryptoActorStubWithType): CryptoActorStubWithType {
        val dataOwnerInfo = getDataOwnerWithType(modifiedCryptoActor.stub.id, modifiedCryptoActor.type)
            ?: throw NotFoundRequestException("Data owner with id ${modifiedCryptoActor.stub.id} does not exist or is not of type ${modifiedCryptoActor.type}")
        return when (dataOwnerInfo) {
            is DataOwnerWithType.DeviceDataOwner -> checkRevAndTagsThenUpdate(
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
                    )
                }
            )
            is DataOwnerWithType.HcpDataOwner -> checkRevAndTagsThenUpdate(
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
                    )
                }
            )
            is DataOwnerWithType.PatientDataOwner -> checkRevAndTagsThenUpdate(
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
                    )
                }
            )
        }
    }

    private inline fun <T> wrongTypeAsNull(block: () -> T): T? =
        try {
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
        updateOriginalWithCryptoActorStubContent: (T, CryptoActorStub) -> T
    ) : CryptoActorStubWithType where T : Versionable<String>, T : CryptoActor, T : HasTags {
        if (original.rev != modified.stub.rev) {
            throw ConflictRequestException("Outdated revision for entity with id ${original.id}")
        }
        if (original.tags != modified.stub.tags) {
            throw IllegalArgumentException("It is not possible to change the tags of a crypto actor stub: update the original entity instead")
        }
        val saved = checkNotNull(save(updateOriginalWithCryptoActorStubContent(original, modified.stub))) {
            "Update returned null for entity with id ${original.id}"
        }
        return CryptoActorStubWithType(modified.type, saved.retrieveStub())
    }

    private fun <T> T.retrieveStub(): CryptoActorStub where T : CryptoActor, T : Versionable<String>, T : HasTags =
        checkNotNull(asCryptoActorStub()) { "Retrieved crypto actor should be stubbable" }

    private fun DataOwnerWithType.retrieveStub(): CryptoActorStubWithType =
        checkNotNull(asCryptoActorStub()) { "Retrieved crypto actor should be stubbable" }
}