package org.taktik.icure.asyncservice.base

import kotlinx.coroutines.flow.Flow
import org.taktik.icure.entities.base.HasSecureDelegationsAccessControl
import org.taktik.icure.entities.requests.EntityBulkShareResult
import org.taktik.icure.entities.requests.BulkShareOrUpdateMetadataParams

/**
 * Defines methods which should be common to the logic of all [HasSecureDelegationsAccessControl] entities.
 */
interface EntityWithSecureDelegationsService<T : HasSecureDelegationsAccessControl> {
    /**
     * Shares or updates shared metadata for one or more entities with one or more users each.
     *
     * @param requests associates an entity id to the requests for the entity accessible through a custom request
     * id, used for associating any potential errors with the request which caused the error.
     * @return the updated entities and information on failed requests.
     * @throws IllegalArgumentException if any of the requests is invalid for a reason which could not be caused by a
     * change in the entity which is unknown by the requester.
     */
    fun bulkShareOrUpdateMetadata(requests: BulkShareOrUpdateMetadataParams): Flow<EntityBulkShareResult<T>>
}