package org.taktik.icure.services.external.rest.v2.dto.requests

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonInclude

/**
 * Result of a bulk share operation.
 */
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
@JsonIgnoreProperties(ignoreUnknown = false)
data class EntityBulkShareResultDto<T>(
    /**
     * The updated entity. Non-null if at least one of the requests succeeded.
     */
    val updatedEntity: T? = null,
    /**
     * Id of the entity for which the update was requested.
     */
    val entityId: String,
    /**
     * Last known revision of the entity before any update, non-null only if an entity matching the requests could be
     * found. This can help to understand if an error is caused by an outdated version of the entity on the client-side.
     */
    val entityRev: String? = null,
    /**
     * If a `bulkShare` method fails to apply any of the share requests for an entity this map associates the id of the
     * original failed request to the reason of failure.
     */
    val rejectedRequests: Map<String, RejectedShareOrMetadataUpdateRequestDto> = emptyMap()
) {
    /**
     * Represents a reason why a share requests might have failed.
     */
    @JsonInclude(JsonInclude.Include.NON_DEFAULT)
    @JsonIgnoreProperties(ignoreUnknown = false)
        data class RejectedShareOrMetadataUpdateRequestDto(
        /**
         * Code of the error, mimics an http status code (400 general user error, 409 conflict, ...).
         */
        val code: Int,
        /**
         * If true a new share request with the same content may succeed so the user is encouraged to retry. This could
         * happen if the entity to share changed while verifying the validity of the request (correctness, permissions,
         * ...), and if the entity did not change in ways incompatible with the request re-performing the request in
         * the same way may succeed.
         */
        val shouldRetry: Boolean = false,
        /**
         * Human-friendly message explaining the reason of the failure.
         */
        val reason: String
    )
}
