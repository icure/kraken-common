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
import org.taktik.icure.asynclogic.SessionInformationProvider
import org.taktik.icure.asynclogic.ExchangeDataMapLogic
import org.taktik.icure.asynclogic.base.EntityWithSecureDelegationsLogic
import org.taktik.icure.asynclogic.datastore.DatastoreInstanceProvider
import org.taktik.icure.asynclogic.impl.GenericLogicImpl
import org.taktik.icure.entities.base.HasEncryptionMetadata
import org.taktik.icure.entities.base.HasSecureDelegationsAccessControl
import org.taktik.icure.entities.embed.AccessLevel
import org.taktik.icure.entities.embed.SecureDelegation
import org.taktik.icure.entities.embed.SecurityMetadata
import org.taktik.icure.entities.embed.parentsGraph
import org.taktik.icure.entities.requests.BulkShareOrUpdateMetadataParams
import org.taktik.icure.entities.requests.EntityBulkShareResult
import org.taktik.icure.entities.requests.EntityBulkShareResult.RejectedShareOrMetadataUpdateRequest
import org.taktik.icure.entities.requests.ShareEntityRequestDetails
import org.taktik.icure.entities.requests.EntityShareRequest
import org.taktik.icure.entities.requests.EntitySharedMetadataUpdateRequest
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
    private val exchangeDataMapLogic: ExchangeDataMapLogic
) : GenericLogicImpl<E, D>(fixer, datastoreInstanceProvider),
    EntityWithSecureDelegationsLogic<E>
where
    E : HasEncryptionMetadata, E : Versionable<String>,
    D : GenericDAO<E>
{

    /**
     * This method returns all the keys that can be used by a Data Owner to access their own encryptable entities, given their
     * id. If the Data Owner that is currently logged in is not the one passed as parameter, only the Data Owner id is returned.
     * @param dataOwnerId the id of the DataOwner to retrieve the access keys for.
     * @return a [Set] of [String]
     */
    suspend fun getAllSearchKeysIfCurrentDataOwner(dataOwnerId: String): Set<String> {
        val authenticationDetails = sessionLogic.getDataOwnerAuthenticationDetails()
        return if (dataOwnerId == authenticationDetails.dataOwner?.id)
            setOf(dataOwnerId) + authenticationDetails.accessControlKeysHashes
        else setOf(dataOwnerId)
    }

    override fun modifyEntities(entities: Collection<E>): Flow<E> = flow {
        emitAll(getGenericDAO().save(datastoreInstanceProvider.getInstanceAndGroup(), filterValidEntityChanges(entities.map { fix(it) }).toList()))
    }

    override fun modifyEntities(entities: Flow<E>): Flow<E> = flow {
        emitAll(modifyEntities(entities.toList()))
    }

    override fun bulkShareOrUpdateMetadata(
        requests: BulkShareOrUpdateMetadataParams
    ): Flow<EntityBulkShareResult<E>> = flow {
        val entities = getGenericDAO().getEntities(datastoreInstanceProvider.getInstanceAndGroup(), requests.requestsByEntityId.keys).toList().associateBy { it.id }
        val validatedRequests = requests.requestsByEntityId.mapNotNull { (entityId, entityRequests) ->
            entities[entityId]?.let { verifyAndApplyShareOrUpdateRequest(entityRequests, it) }
        }
        val approvedChangesByEntityId = validatedRequests.mapNotNull {
            it.entityWithAppliedRequestsIds?.first?.id?.to(it)
        }.toMap()
        emitAll(
            getGenericDAO().saveBulk(
                datastoreInstanceProvider.getInstanceAndGroup(),
                approvedChangesByEntityId.map { it.value.entityWithAppliedRequestsIds!!.first }
            ).map { bulkSaveResult ->
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

        createExchangeDataMapIfNotExisting(requests, validatedRequests)

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

    private suspend fun <T : HasEncryptionMetadata> createExchangeDataMapIfNotExisting(
        requests: BulkShareOrUpdateMetadataParams,
        validatedRequests: List<ValidatedShareRequest<T>>
    ) {
        val mapsToCreate = validatedRequests.mapNotNull { validatedRequest ->
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
        exchangeDataMapLogic.createOrUpdateExchangeDataMapBatchById(mapsToCreate).collect()
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
                currentEntity.securityMetadata?.getDelegation(it) != null
            },
            if (requestInfo.potentialParentDelegations.any { currentEntity.securityMetadata?.getDelegation(it)?.second?.permissions == AccessLevel.WRITE }) {
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
        val validRequests = currentEntity.securityMetadata?.let {
            it.secureDelegations.keys + it.keysEquivalences.keys
        }?.let { existingDelegationKeys ->
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
            val canonicalHashAndDelegation = currentEntity.securityMetadata?.getDelegation(request.metadataAccessControlHash)
            if (canonicalHashAndDelegation != null) {
                val (canonicalHash, delegation) = canonicalHashAndDelegation
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
                    entityWithUpdatedSecurityMetadata(
                        updatedEntity,
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
        val accessibleCanonicalDelegations = accessibleDelegationsHashes.map {
            metadata.keysEquivalences[it] ?: it
        }
        val parentsGraph = metadata.secureDelegations.parentsGraph
        return accessibleCanonicalDelegations.filterNot {
            parentsGraph.reachSetExcludingZeroLength(it).any { parent -> parent in accessibleCanonicalDelegations }
        }.toSet()
    }

    private fun E.withNewSecureDelegation(
        request: EntityShareRequest,
        parents: Set<Sha256HexString>,
        permissions: AccessLevel
    ): Pair<String, E> {
        val newCanonicalHash = request.canonicalHash
        val newEquivalences = (request.accessControlKeys.map { hashAccessControlKey(it.hexStringToByteArray()) } - newCanonicalHash)
            .associateWith { newCanonicalHash }
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
                keysEquivalences = it.keysEquivalences + newEquivalences
            )
        } ?: SecurityMetadata(
            secureDelegations = mapOf(newCanonicalHash to newDelegation),
            keysEquivalences = newEquivalences
        )
        return newCanonicalHash to entityWithUpdatedSecurityMetadata(this, newSecurityMetadata)
    }

    /*TODO
     * any way of avoiding load all entities in memory for the comparison without making too many requests to couchdb
     * to retrieve the current versions of the entities? Batch and deal with only up to x at a time?
     */
    protected fun filterValidEntityChanges(
        updatedEntities: Collection<E>
    ): Flow<E> {
        val updatedEntitiesById = updatedEntities.associateBy { it.id }
        return flow {
            getGenericDAO()
                .getEntities(datastoreInstanceProvider.getInstanceAndGroup(), updatedEntities.map { it.id })
                .collect { currentEntity ->
                    val updatedEntity = updatedEntitiesById.getValue(currentEntity.id)
                    if (doValidateEntityChange(updatedEntity, currentEntity, throwErrorOnInvalid = false)) {
                        emit(updatedEntity)
                    }
                }
        }
    }

    /**
     * Creates a copy of the entity with updated security metadata.
     */
    protected abstract fun entityWithUpdatedSecurityMetadata(
        entity: E,
        updatedMetadata: SecurityMetadata
    ): E

    /**
     * Throws error if the updated entity has some changes from the current version of the entity which are not allowed.
     */
    protected suspend fun checkValidEntityChange(
        updatedEntity: E,
        currentEntity: E?
    ) {
        doValidateEntityChange(
            updatedEntity,
            currentEntity
            ?: getGenericDAO()
                .getEntities(datastoreInstanceProvider.getInstanceAndGroup(), listOf(updatedEntity.id))
                .toList()
                .firstOrNull()
            ?: throw NotFoundRequestException("Could not find entity with id ${updatedEntity.id}"),
            throwErrorOnInvalid = true
        )
    }

    private fun doValidateEntityChange(
        updatedEntity: E,
        currentEntity: E,
        throwErrorOnInvalid: Boolean
    ): Boolean {
        if (updatedEntity.rev != currentEntity.rev) {
            if (throwErrorOnInvalid) throw ConflictRequestException(
                "Rev of current entity is ${currentEntity.rev} but rev of updated is ${updatedEntity.rev}."
            ) else return false
        }
        if (currentEntity.securityMetadata != null && updatedEntity.securityMetadata != currentEntity.securityMetadata) {
            if (throwErrorOnInvalid) throw IllegalArgumentException(
                "Impossible to modify directly security metadata: use `share` methods instead."
            ) else return false
        }
        return true
    }
}

val EntityShareRequest.canonicalHash get() = accessControlKeys.map { hashAccessControlKey(it.hexStringToByteArray()) }.minOf { it }