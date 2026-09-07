package org.taktik.icure.services.external.rest.v2.dto.filter

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonInclude
import org.taktik.icure.dto.annotations.filtering.ActiveField
import org.taktik.icure.services.external.rest.v2.dto.dao.KeyComponentDto

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
data class GenericByKeysCustomFilterDto(
	@ActiveField
	val viewName: String,
	@ActiveField
	val startKey: List<KeyComponentDto>?,
	@ActiveField
	val startDocumentId: String?,
	@ActiveField
	val limit: Int,
	@ActiveField
	val keyComponents: List<List<KeyComponentDto>>
) : CustomFilterDto