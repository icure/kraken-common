package org.taktik.icure.services.external.rest.v2.dto.requests

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonInclude
import io.swagger.v3.oas.annotations.media.Schema
import org.taktik.icure.services.external.rest.v2.dto.specializations.SecureDelegationKeyStringDto

@JsonInclude(JsonInclude.Include.NON_DEFAULT)
@JsonIgnoreProperties(ignoreUnknown = false)
data class BulkShareOrUpdateMetadataParamsDto(
	@get:Schema(required = true)
	val requestsByEntityId: Map<String, EntityRequestInformationDto>,
) {
	@JsonInclude(JsonInclude.Include.NON_DEFAULT)
	@JsonIgnoreProperties(ignoreUnknown = false)
	data class EntityRequestInformationDto(
		@get:Schema(required = true)
		val requests: Map<String, EntityShareOrMetadataUpdateRequestDto>,
		/**
		 * Which delegations can be parents to any newly requested non-root delegations. Some may be ignored in order to
		 * simplify the delegation graph, or if the requested permission is root.
		 */
		@get:Schema(required = true)
		val potentialParentDelegations: Set<SecureDelegationKeyStringDto>,
	)
}
