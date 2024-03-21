package org.taktik.icure.services.external.rest.v2.dto.requests

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonInclude

@JsonInclude(JsonInclude.Include.NON_DEFAULT)
@JsonIgnoreProperties(ignoreUnknown = false)
data class BulkShareOrUpdateMetadataParamsDto(
    val requestsByEntityId: Map<String, EntityRequestInformationDto>
) {

    @JsonInclude(JsonInclude.Include.NON_DEFAULT)
    @JsonIgnoreProperties(ignoreUnknown = false)
    data class EntityRequestInformationDto(
        val requests: Map<String, EntityShareOrMetadataUpdateRequestDto>,
        /**
         * Which delegations can be parents to any newly requested non-root delegations. Some may be ignored in order to
         * simplify the delegation graph, or if the requested permission is root.
         */
        val potentialParentDelegations: Set<String>
    )

}