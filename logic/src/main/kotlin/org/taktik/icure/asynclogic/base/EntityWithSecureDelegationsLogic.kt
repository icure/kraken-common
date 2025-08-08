package org.taktik.icure.asynclogic.base

import kotlinx.coroutines.flow.Flow
import org.taktik.icure.entities.base.HasSecureDelegationsAccessControl
import org.taktik.icure.entities.embed.AccessLevel
import org.taktik.icure.entities.requests.BulkShareOrUpdateMetadataParams
import org.taktik.icure.entities.requests.EntityBulkShareResult
import org.taktik.icure.entities.requests.EntityShareOrMetadataUpdateRequest
import org.taktik.icure.entities.requests.EntityShareRequest
import org.taktik.icure.entities.utils.Sha256HexString

/**
 * Defines methods which should be common to the logic of all [HasSecureDelegationsAccessControl] entities.
 */
interface EntityWithSecureDelegationsLogic<T : HasSecureDelegationsAccessControl> {
	/**
	 * Shares or updates shared metadata for one or more entities with one or more users each.
	 *
	 * @param requests associates an entity id to the:
	 * - permissions that the data owner has on the entity.
	 * - requests for the entity accessible through custom request id, used for associating any potential errors with
	 * the request which caused the error.
	 * @return the updated entities and information on failed requests.
	 * @throws IllegalArgumentException if any of the requests is invalid for a reason which could not be caused by a
	 * change in the entity which is unknown by the requester.
	 */
	fun bulkShareOrUpdateMetadata(
		requests: BulkShareOrUpdateMetadataParams,
	): Flow<EntityBulkShareResult<T>>
}
