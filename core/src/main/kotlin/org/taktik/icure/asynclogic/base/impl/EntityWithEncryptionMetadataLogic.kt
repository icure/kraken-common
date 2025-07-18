package org.taktik.icure.asynclogic.base.impl

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.toList
import org.taktik.couchdb.entity.Versionable
import org.taktik.icure.asyncdao.GenericDAO
import org.taktik.icure.asyncdao.results.BulkSaveResult
import org.taktik.icure.asyncdao.results.filterSuccessfulUpdates
import org.taktik.icure.asynclogic.ExchangeDataMapLogic
import org.taktik.icure.asynclogic.SessionInformationProvider
import org.taktik.icure.asynclogic.base.EntityWithSecureDelegationsLogic
import org.taktik.icure.datastore.DatastoreInstanceProvider
import org.taktik.icure.asynclogic.impl.GenericLogicImpl
import org.taktik.icure.asynclogic.impl.filter.Filters
import org.taktik.icure.entities.ExchangeDataMap
import org.taktik.icure.entities.base.HasEncryptionMetadata
import org.taktik.icure.entities.base.HasSecureDelegationsAccessControl
import org.taktik.icure.entities.embed.AccessLevel
import org.taktik.icure.entities.embed.SecureDelegation
import org.taktik.icure.entities.embed.SecurityMetadata
import org.taktik.icure.entities.embed.parentsGraph
import org.taktik.icure.entities.requests.BulkShareOrUpdateMetadataParams
import org.taktik.icure.entities.requests.EntityBulkShareResult
import org.taktik.icure.entities.requests.EntityBulkShareResult.RejectedShareOrMetadataUpdateRequest
import org.taktik.icure.entities.requests.EntityShareRequest
import org.taktik.icure.entities.requests.EntitySharedMetadataUpdateRequest
import org.taktik.icure.entities.requests.ShareEntityRequestDetails
import org.taktik.icure.entities.utils.Base64String
import org.taktik.icure.entities.utils.HexString
import org.taktik.icure.entities.utils.KeypairFingerprintV2String
import org.taktik.icure.entities.utils.Sha256HexString
import org.taktik.icure.exceptions.ConflictRequestException
import org.taktik.icure.exceptions.NotFoundRequestException
import org.taktik.icure.security.hashAccessControlKey
import org.taktik.icure.utils.hexStringToByteArray
import org.taktik.icure.utils.reachSetExcludingZeroLength
import org.taktik.icure.validation.aspect.Fixer

abstract class EntityWithEncryptionMetadataLogic<E, D>(
    fixer: Fixer,
    private val sessionLogic: SessionInformationProvider,
    private val datastoreInstanceProvider: DatastoreInstanceProvider,
    private val exchangeDataMapLogic: ExchangeDataMapLogic,
    filters: Filters
) : GenericLogicImpl<E, D>(fixer, datastoreInstanceProvider, filters),
    EntityWithSecureDelegationsLogic<E>
where
    E : HasEncryptionMetadata, E : Versionable<String>,
    D : GenericDAO<E>
{
    /**
     * Creates a copy of the entity with updated security metadata.
     */
    protected abstract fun entityWithUpdatedSecurityMetadata(
        entity: E,
        updatedMetadata: SecurityMetadata
    ): E

    private val helper = EntityWithEncryptionMetadataLogicHelper<E, D> {
        entityWithUpdatedSecurityMetadata(this@EntityWithEncryptionMetadataLogicHelper, it)
    }

    /**
     * @see [SessionInformationProvider.getAllSearchKeysIfCurrentDataOwner]
     */
    suspend fun getAllSearchKeysIfCurrentDataOwner(dataOwnerId: String): Set<String> =
        sessionLogic.getAllSearchKeysIfCurrentDataOwner(dataOwnerId)

    override fun modifyEntities(entities: Collection<E>): Flow<E> = flow {
        val dao = getGenericDAO()
        val datastoreInfo = datastoreInstanceProvider.getInstanceAndGroup()
        emitAll(
            dao.saveBulk(
                datastoreInformation = datastoreInfo,
                entities = helper.filterValidEntityChanges(
                    entities.map { fix(it, isCreate = false) },
                    dao.getEntities(datastoreInfo, entities.map { it.id })
                ).toList()
            ).filterSuccessfulUpdates()
        )
    }

    override fun modifyEntities(entities: Flow<E>): Flow<E> = flow {
        emitAll(modifyEntities(entities.toList()))
    }

    override fun bulkShareOrUpdateMetadata(
        requests: BulkShareOrUpdateMetadataParams
    ): Flow<EntityBulkShareResult<E>> = flow {
        val datastoreInfo = datastoreInstanceProvider.getInstanceAndGroup()
        emitAll(helper.doBulkShareOrUpdateMetadata(
            requests = requests,
            entities = getGenericDAO().getEntities(datastoreInfo, requests.requestsByEntityId.keys).toList().associateBy { it.id },
            doSaveBulk = { getGenericDAO().saveBulk(datastoreInfo, it) },
            doCreateExchangeDataMapById = { exchangeDataMapLogic.createOrUpdateExchangeDataMapBatchById(it) }
        ))
    }


    /**
     * Throws error if the updated entity has some changes from the current version of the entity which are not allowed.
     */
    protected suspend fun checkValidEntityChange(
        updatedEntity: E,
        currentEntity: E?
    ) {
        helper.doValidateEntityChange(
            updatedEntity,
            currentEntity
                ?: getGenericDAO()
                    .getEntities(datastoreInstanceProvider.getInstanceAndGroup(), listOf(updatedEntity.id))
                    .toList()
                    .firstOrNull()
                ?: throw NotFoundRequestException("Could not find entity with id ${updatedEntity.id}"),
            throwErrorOnInvalidRev = true
        )
    }

    protected fun filterValidEntityChanges(
        updatedEntities: Collection<E>
    ): Flow<E> = flow {
        emitAll(helper.filterValidEntityChanges(
            updatedEntities.map { fix(it, isCreate = false) },
            getGenericDAO().getEntities(datastoreInstanceProvider.getInstanceAndGroup(), updatedEntities.map { it.id })
        ))
    }
}

val EntityShareRequest.canonicalHash get() = accessControlKeys.map { hashAccessControlKey(it.hexStringToByteArray()) }.minOf { it }

class EntityWithEncryptionMetadataLogicHelper<E, D> (
    private val withUpdatedSecurityMetadata: E.(updatedMetadata: SecurityMetadata) -> E
)
    where
E : HasEncryptionMetadata, E : Versionable<String>,
D : GenericDAO<E> {

    fun doBulkShareOrUpdateMetadata(
        requests: BulkShareOrUpdateMetadataParams,
        entities: Map<String, E>,
        doSaveBulk:  (updatedEntities: Collection<E>) -> Flow<BulkSaveResult<E>>,
        doCreateExchangeDataMapById: (batch: Map<HexString, Map<KeypairFingerprintV2String, Base64String>>) -> Flow<ExchangeDataMap>
    ): Flow<EntityBulkShareResult<E>> = flow {
        val validatedRequests = requests.requestsByEntityId.mapNotNull { (entityId, entityRequests) ->
            entities[entityId]?.let { verifyAndApplyShareOrUpdateRequest(entityRequests, it) }
        }
        val approvedChangesByEntityId = validatedRequests.mapNotNull {
            it.entityWithAppliedRequestsIds?.first?.id?.to(it)
        }.toMap()
        emitAll(
            doSaveBulk(approvedChangesByEntityId.map { it.value.entityWithAppliedRequestsIds!!.first }).map { bulkSaveResult ->
                when (bulkSaveResult) {
                    is BulkSaveResult.Failure -> {
                        val validatedRequest = approvedChangesByEntityId.getValue(bulkSaveResult.entityId)
                        EntityBulkShareResult(
                            null,
                            bulkSaveResult.entityId,
                            validatedRequest.entityWithAppliedRequestsIds!!.first.rev,
                            validatedRequest.entityWithAppliedRequestsIds.second.associateWith {
                                RejectedShareOrMetadataUpdateRequest(
                                    bulkSaveResult.code,
                                    true,
                                    "Failed to save entity to database: ${bulkSaveResult.message}"
                                )
                            }
                        )
                    }
                    is BulkSaveResult.Success -> {
                        val validatedRequest = approvedChangesByEntityId.getValue(bulkSaveResult.entity.id)
                        EntityBulkShareResult(
                            bulkSaveResult.entity,
                            bulkSaveResult.entity.id,
                            validatedRequest.entityWithAppliedRequestsIds!!.first.rev,
                            validatedRequest.rejectedRequests
                        )
                    }
                }
            }
        )

        doCreateExchangeDataMapById(getExchangeDataMapsToCreate(requests, validatedRequests)).collect()

        validatedRequests.forEach {
            if (it.entityWithAppliedRequestsIds == null) emit(EntityBulkShareResult(
                null,
                it.entityId,
                it.entityRev,
                it.rejectedRequests
            ))
        }
        requests.requestsByEntityId.forEach { (entityId, entityRequests) ->
            if (entities[entityId] === null) {
                emit(
                    EntityBulkShareResult(
                        null,
                        entityId,
                        null,
                        entityRequests.requests.keys.associateWith {
                            RejectedShareOrMetadataUpdateRequest(404, false, "There is no entity with id $entityId")
                        }
                    )
                )
            }
        }
    }

    private fun <T : HasEncryptionMetadata> getExchangeDataMapsToCreate(
        requests: BulkShareOrUpdateMetadataParams,
        validatedRequests: List<ValidatedShareRequest<T>>,
    ) =
        validatedRequests.mapNotNull { validatedRequest ->
            validatedRequest.entityWithAppliedRequestsIds?.let {
                validatedRequest.entityId to it.second
            }
        }.fold(emptyMap<HexString, Map<KeypairFingerprintV2String, Base64String>>()) { exchangeMaps, (entityId, requestsId) ->
            exchangeMaps + (requests.requestsByEntityId[entityId]?.requests?.filterKeys {
                requestsId.contains(it)
            }?.filterValues {
                it is EntityShareRequest
            }?.mapNotNull { (_, request) ->
                (request as EntityShareRequest).canonicalHash to request.encryptedExchangeDataId
            }?.toMap() ?: emptyMap())
        }

    private data class ValidatedShareRequest<E : HasEncryptionMetadata>(
        val rejectedRequests: Map<String, RejectedShareOrMetadataUpdateRequest>,
        val entityId: String,
        val entityRev: String,
        val entityWithAppliedRequestsIds: Pair<E, Set<String>>?
    )
    private fun verifyAndApplyShareOrUpdateRequest(
        requestInfo: ShareEntityRequestDetails,
        currentEntity: E
    ): ValidatedShareRequest<E> {
        val shareRequests = mutableMapOf<String, EntityShareRequest>()
        val updateRequests = mutableMapOf<String, EntitySharedMetadataUpdateRequest>()
        requestInfo.requests.forEach { (requestId, request) ->
            when (request) {
                is EntityShareRequest -> shareRequests[requestId] = request
                is EntitySharedMetadataUpdateRequest -> updateRequests[requestId] = request
            }
        }
        val appliedRequestsInfo = PartialRequestApplication(
            shareRequests,
            updateRequests,
            emptySet(),
            emptyMap(),
            currentEntity,
            currentEntity,
            requestInfo.potentialParentDelegations.filterTo(mutableSetOf()) {
                currentEntity.securityMetadata?.secureDelegations?.containsKey(it) == true
            },
            if (requestInfo.potentialParentDelegations.any {
                    currentEntity.securityMetadata?.secureDelegations?.get(it)?.permissions == AccessLevel.WRITE
                }) {
                AccessLevel.WRITE
            } else AccessLevel.READ
        ).rejectShareRequestsForExistingDelegations()
            .createRootDelegations()
            .createNonRootDelegations()
            .applyUpdateRequests()
        return ValidatedShareRequest(
            appliedRequestsInfo.rejectedRequests,
            currentEntity.id,
            checkNotNull(currentEntity.rev) { "Retrieved entity should have a rev" },
            if (appliedRequestsInfo.appliedRequestsIds.isNotEmpty()) {
                appliedRequestsInfo.updatedEntity to appliedRequestsInfo.appliedRequestsIds
            } else null
        )
    }

    private data class PartialRequestApplication<E : HasSecureDelegationsAccessControl>(
        val remainingShareRequests: Map<String, EntityShareRequest>,
        val remainingUpdateRequest: Map<String, EntitySharedMetadataUpdateRequest>,
        val appliedRequestsIds: Set<String>,
        val rejectedRequests: Map<String, RejectedShareOrMetadataUpdateRequest>,
        val updatedEntity: E,
        val currentEntity: E,
        val potentialParentDelegations: Set<String>,
        val maxPermissionFromParents: AccessLevel
    )

    private fun PartialRequestApplication<E>.rejectShareRequestsForExistingDelegations(): PartialRequestApplication<E> {
        val validRequests = currentEntity.securityMetadata?.secureDelegations?.keys?.let { existingDelegationKeys ->
            remainingShareRequests.filter { (_, request) ->
                request.accessControlKeys.map { hashAccessControlKey(it.hexStringToByteArray()) }.all { it !in existingDelegationKeys }
            }
        } ?: remainingShareRequests
        return copy(
            remainingShareRequests = validRequests,
            rejectedRequests = rejectedRequests + remainingShareRequests
                .filter { (k, _) -> k !in validRequests }
                .mapValues {
                    RejectedShareOrMetadataUpdateRequest(
                        400,
                        false,
                        "There is already a delegation for these hashes, consider modifying it instead"
                    )
                }
        )
    }

    private fun PartialRequestApplication<E>.createRootDelegations(): PartialRequestApplication<E> {
        val rootDelegationsRequests = remainingShareRequests.filter { (_, v) ->
            v.requestedPermissions == EntityShareRequest.RequestedPermission.ROOT
        }
        return when (rootDelegationsRequests.size) {
            0 -> this
            1 -> {
                val (requestId, request) = rootDelegationsRequests.toList().first()
                val (newDelegationKey, updatedEntity) = updatedEntity.withNewSecureDelegation(
                    request,
                    emptySet(),
                    AccessLevel.WRITE
                )
                copy(
                    remainingShareRequests = remainingShareRequests - requestId,
                    appliedRequestsIds = appliedRequestsIds + requestId,
                    updatedEntity = updatedEntity,
                    potentialParentDelegations = potentialParentDelegations + newDelegationKey,
                    maxPermissionFromParents = AccessLevel.WRITE
                )
            }
            else ->
                throw IllegalStateException("Data owners should not request many ROOT delegations for the same entity; this should have already been checked")
        }
    }

    private fun PartialRequestApplication<E>.createNonRootDelegations(): PartialRequestApplication<E> {
        check(remainingShareRequests.all { it.value.requestedPermissions != EntityShareRequest.RequestedPermission.ROOT }) {
            "There are leftover requests to create root delegations"
        }
        return if (potentialParentDelegations.isEmpty()) {
            copy(
                remainingShareRequests = emptyMap(),
                rejectedRequests = rejectedRequests + remainingShareRequests.mapValues {
                    RejectedShareOrMetadataUpdateRequest(
                        400,
                        false,
                        "You must indicate valid potential parent delegations or create a new ROOT delegation"
                    )
                }
            )
        } else {
            copy(
                remainingShareRequests = emptyMap(),
                appliedRequestsIds = appliedRequestsIds + remainingShareRequests.keys,
                updatedEntity = remainingShareRequests.toList().fold(updatedEntity) { latestEntityUpdate, (_, request) ->
                    latestEntityUpdate.withNewSecureDelegation(
                        request,
                        parentsFromAccessibleHashes(latestEntityUpdate, potentialParentDelegations),
                        when (request.requestedPermissions) {
                            EntityShareRequest.RequestedPermission.FULL_WRITE -> AccessLevel.WRITE
                            EntityShareRequest.RequestedPermission.MAX_WRITE -> maxPermissionFromParents
                            else -> AccessLevel.READ
                        }
                    ).second
                }
            )
        }
    }

    private fun PartialRequestApplication<E>.applyUpdateRequests(): PartialRequestApplication<E> {
        val newAppliedRequestsIds = mutableSetOf<String>()
        val updatedDelegations = mutableMapOf<String, SecureDelegation>()
        val newRejectedRequests = mutableMapOf<String, RejectedShareOrMetadataUpdateRequest>()
        remainingUpdateRequest.forEach { (requestId, request) ->
            val canonicalHash = request.metadataAccessControlHash
            val delegation = currentEntity.securityMetadata?.secureDelegations?.get(request.metadataAccessControlHash)
            if (delegation != null) {
                val updatedSecretIds = validateAndApplyUpdateRequests(
                    delegation.secretIds,
                    request.secretIds,
                )
                val updatedEncryptionKeys = validateAndApplyUpdateRequests(
                    delegation.encryptionKeys,
                    request.encryptionKeys,
                )
                val updatedOwningEntityIds = validateAndApplyUpdateRequests(
                    delegation.owningEntityIds,
                    request.owningEntityIds,
                )
                if (updatedSecretIds == null || updatedEncryptionKeys == null || updatedOwningEntityIds == null) {
                    newRejectedRequests[requestId] = RejectedShareOrMetadataUpdateRequest(
                        400,
                        false,
                        "Request attempts to create duplicate entries or delete non-existing entries."
                    )
                } else {
                    newAppliedRequestsIds.add(requestId)
                    updatedDelegations[canonicalHash] = delegation.copy(
                        secretIds = updatedSecretIds,
                        encryptionKeys = updatedEncryptionKeys,
                        owningEntityIds = updatedOwningEntityIds
                    )
                }
            } else {
                newRejectedRequests[requestId] = RejectedShareOrMetadataUpdateRequest(
                    404,
                    false,
                    "Metadata ${request.metadataAccessControlHash} does not exist on entity ${updatedEntity.id}."
                )
            }
        }
        return copy(
            remainingUpdateRequest = emptyMap(),
            appliedRequestsIds = appliedRequestsIds + newAppliedRequestsIds,
            updatedEntity =
                if (updatedDelegations.isNotEmpty())
                    updatedEntity.withUpdatedSecurityMetadata(
                        updatedEntity.securityMetadata!!.let {
                            it.copy(secureDelegations = it.secureDelegations + updatedDelegations)
                        }
                    )
                else
                    updatedEntity,
            rejectedRequests = rejectedRequests + newRejectedRequests
        )
    }

    private fun validateAndApplyUpdateRequests(
        existingEncryptedData: Set<String>,
        updateRequests: Map<String, EntitySharedMetadataUpdateRequest.EntryUpdateType>,
    ): Set<String>? {
        val toCreate = updateRequests.filterValues { it == EntitySharedMetadataUpdateRequest.EntryUpdateType.CREATE }.keys
        val toDelete = updateRequests.filterValues { it == EntitySharedMetadataUpdateRequest.EntryUpdateType.DELETE }.keys
        return if (toCreate.any { it in existingEncryptedData } || toDelete.any { it !in existingEncryptedData })
            null
        else
            existingEncryptedData + toCreate - toDelete
    }

    private fun parentsFromAccessibleHashes(
        updatedEntity: E,
        accessibleDelegationsHashes: Set<Sha256HexString>
    ): Set<Sha256HexString> {
        val metadata = checkNotNull(updatedEntity.securityMetadata)
        val parentsGraph = metadata.secureDelegations.parentsGraph
        return accessibleDelegationsHashes.filterNot {
            parentsGraph.reachSetExcludingZeroLength(it).any { parent -> parent in accessibleDelegationsHashes }
        }.toSet()
    }

    private fun E.withNewSecureDelegation(
        request: EntityShareRequest,
        parents: Set<Sha256HexString>,
        permissions: AccessLevel
    ): Pair<String, E> {
        val newCanonicalHash = request.canonicalHash
        val newDelegation = SecureDelegation(
            delegator = request.explicitDelegator,
            delegate = request.explicitDelegate,
            secretIds = request.secretIds,
            encryptionKeys = request.encryptionKeys,
            owningEntityIds = request.owningEntityIds,
            parentDelegations = parents,
            exchangeDataId = request.exchangeDataId,
            permissions = permissions
        )
        val newSecurityMetadata = securityMetadata?.let {
            it.copy(
                secureDelegations = it.secureDelegations + (newCanonicalHash to newDelegation),
            )
        } ?: SecurityMetadata(
            secureDelegations = mapOf(newCanonicalHash to newDelegation),
        )
        return newCanonicalHash to withUpdatedSecurityMetadata(newSecurityMetadata)
    }

    /*TODO
     * any way of avoiding load all entities in memory for the comparison without making too many requests to couchdb
     * to retrieve the current versions of the entities? Batch and deal with only up to x at a time?
     */
    fun filterValidEntityChanges(
        updatedEntities: Collection<E>,
        originalEntities: Flow<E>
    ): Flow<E> {
        val updatedEntitiesById = updatedEntities.associateBy { it.id }
        return flow {
            originalEntities.collect { currentEntity ->
                val updatedEntity = updatedEntitiesById.getValue(currentEntity.id)
                if (doValidateEntityChange(updatedEntity, currentEntity, throwErrorOnInvalidRev = false)) {
                    emit(updatedEntity)
                }
            }
        }
    }

    fun doValidateEntityChange(
        updatedEntity: E,
        currentEntity: E,
        throwErrorOnInvalidRev: Boolean
    ): Boolean {
        if (updatedEntity.rev != currentEntity.rev) {
            if (throwErrorOnInvalidRev) throw ConflictRequestException(
                "Rev of current entity is ${currentEntity.rev} but rev of updated is ${updatedEntity.rev}."
            ) else return false
        }
        if (currentEntity.securityMetadata != null && updatedEntity.securityMetadata != currentEntity.securityMetadata) {
            throw IllegalArgumentException(
                "Impossible to modify directly security metadata: use `share` methods instead."
            )
        }
        return true
    }
}
