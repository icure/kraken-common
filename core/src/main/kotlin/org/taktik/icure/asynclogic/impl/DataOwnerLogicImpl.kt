package org.taktik.icure.asynclogic.impl

import com.fasterxml.jackson.databind.JsonMappingException
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Service
import org.taktik.couchdb.entity.Versionable
import org.taktik.icure.asyncdao.DeviceDAO
import org.taktik.icure.asyncdao.HealthcarePartyDAO
import org.taktik.icure.asyncdao.PatientDAO
import org.taktik.icure.asynclogic.DataOwnerLogic
import org.taktik.icure.asynclogic.datastore.DatastoreInstanceProvider
import org.taktik.icure.asynclogic.datastore.IDatastoreInformation
import org.taktik.icure.entities.CryptoActorStub
import org.taktik.icure.entities.CryptoActorStubWithType
import org.taktik.icure.entities.DataOwnerType
import org.taktik.icure.entities.DataOwnerWithType
import org.taktik.icure.entities.Device
import org.taktik.icure.entities.HealthcareParty
import org.taktik.icure.entities.Patient
import org.taktik.icure.entities.base.CryptoActor
import org.taktik.icure.entities.base.HasTags
import org.taktik.icure.entities.base.asCryptoActorStub
import org.taktik.icure.exceptions.ConflictRequestException
import org.taktik.icure.exceptions.DeserializationTypeException
import org.taktik.icure.exceptions.IllegalEntityException
import org.taktik.icure.exceptions.NotFoundRequestException
import org.taktik.icure.utils.PeekChannel

open class DataOwnerLogicImpl(
    protected val patientDao: PatientDAO,
    protected val hcpDao: HealthcarePartyDAO,
    protected val deviceDao: DeviceDAO,
    private val datastoreInstanceProvider: DatastoreInstanceProvider
) : DataOwnerLogic {
    companion object {
        private const val MAX_HIERARCHY_DEPTH = 5
    }

    override suspend fun getCryptoActorStub(dataOwnerId: String): CryptoActorStubWithType? =
        getDataOwner(dataOwnerId)?.retrieveStub()

    override fun getCryptoActorStubs(dataOwnerIds: List<String>): Flow<CryptoActorStubWithType> =
        getDataOwners(dataOwnerIds).map { it.retrieveStub() }

    override suspend fun getCryptoActorStubWithType(
        dataOwnerId: String,
        dataOwnerType: DataOwnerType
    ): CryptoActorStub? = getDataOwnerWithType(dataOwnerId, dataOwnerType, null)?.retrieveStub()?.stub

    override suspend fun getDataOwner(dataOwnerId: String): DataOwnerWithType? =
        doGetDataOwner(dataOwnerId, likelyType = null)

    protected suspend fun doGetDataOwner(
        dataOwnerId: String,
        likelyType: DataOwnerType?,
        preloadedDatastoreInfo: IDatastoreInformation? = null
    ): DataOwnerWithType? {
        val datastoreInfo = preloadedDatastoreInfo ?: datastoreInstanceProvider.getInstanceAndGroup()
        val orderToTry = when (likelyType) {
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
                patientDao.getEntities(datastoreInfo, dataOwnerIds).filter {
                    it.deletionDate == null
                }.collect {
                    patientChannel.send(it)
                }
                patientChannel.closeSend()
            }
            launch {
                hcpDao.getEntities(datastoreInfo, dataOwnerIds).filter {
                    it.deletionDate == null
                }.collect {
                    hcpChannel.send(it)
                }
                hcpChannel.closeSend()
            }
            launch {
                deviceDao.getEntities(datastoreInfo, dataOwnerIds).filter {
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

    override fun getCryptoActorHierarchy(dataOwnerId: String): Flow<DataOwnerWithType> = flow {
        var nextId: String? = dataOwnerId
        var nextLikelyType: DataOwnerType? = null
        val visited = mutableSetOf<String>()
        while (nextId != null) {
            if (nextId in visited) throw IllegalEntityException("Circular reference in ancestors of $dataOwnerId")
            if (visited.size > MAX_HIERARCHY_DEPTH) throw IllegalEntityException("Hierarchy of $dataOwnerId exceeds maximum allowed depth of $MAX_HIERARCHY_DEPTH")
            val current = doGetDataOwner(nextId, likelyType = nextLikelyType) ?: throw IllegalEntityException(
                "Can't find ancestor $nextId for $dataOwnerId"
            )
            visited.add(current.id)
            nextLikelyType = current.type
            nextId = current.dataOwner.parentId
            emit(current)
        }
    }

    override fun getCryptoActorHierarchyStub(dataOwnerId: String): Flow<CryptoActorStubWithType> =
        getCryptoActorHierarchy(dataOwnerId).map { it.retrieveStub() }

    private suspend fun getDataOwnerWithType(
        dataOwnerId: String,
        dataOwnerType: DataOwnerType,
        preloadedDatastoreInfo: IDatastoreInformation?
    ): DataOwnerWithType? {
        val datastoreInfo = preloadedDatastoreInfo ?: datastoreInstanceProvider.getInstanceAndGroup()
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
        val dataOwnerInfo = getDataOwnerWithType(modifiedCryptoActor.stub.id, modifiedCryptoActor.type, null)
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
                        cryptoActorProperties = modified.cryptoActorProperties,
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
                        cryptoActorProperties = modified.cryptoActorProperties,
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
                        cryptoActorProperties = modified.cryptoActorProperties,
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
    ) : CryptoActorStubWithType where T : Versionable<String>, T : CryptoActor {
        if (original.rev != modified.stub.rev) {
            throw ConflictRequestException("Outdated revision for entity with id ${original.id}")
        }
        if (original.tags != modified.stub.tags) {
            throw IllegalArgumentException("It is not possible to change the tags of a crypto actor stub: update the original entity instead")
        }
        require(modified.stub.parentId == original.parentId) {
            "You can't use this method to change the parent id of a crypto actor"
        }
        val saved = checkNotNull(save(updateOriginalWithCryptoActorStubContent(original, modified.stub))) {
            "Update returned null for entity with id ${original.id}"
        }
        return CryptoActorStubWithType(modified.type, saved.retrieveStub())
    }

    private fun <T> T.retrieveStub(): CryptoActorStub where T : CryptoActor, T : Versionable<String> =
        checkNotNull(asCryptoActorStub()) { "Retrieved crypto actor should be stubbable" }

    private fun DataOwnerWithType.retrieveStub(): CryptoActorStubWithType =
        checkNotNull(asCryptoActorStub()) { "Retrieved crypto actor should be stubbable" }
}
