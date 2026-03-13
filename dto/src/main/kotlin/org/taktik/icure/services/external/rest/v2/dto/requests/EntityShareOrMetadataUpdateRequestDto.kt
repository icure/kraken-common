package org.taktik.icure.services.external.rest.v2.dto.requests

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonInclude

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = false)
/**
 * A request that combines an optional entity share operation with an optional shared-metadata update into a single payload.
 * Exactly one of the two fields should be populated for each entry in a bulk request.
 */
data class EntityShareOrMetadataUpdateRequestDto(
	/** The share request to grant access to an entity, if applicable. */
	val share: EntityShareRequestDto? = null,
	/** The metadata update request to modify sharing metadata of an already-shared entity, if applicable. */
	val update: EntitySharedMetadataUpdateRequestDto? = null,
)
