package org.taktik.icure.services.external.rest.v1.dto.embed

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonInclude
import io.swagger.v3.oas.annotations.media.Schema

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
@Schema(description = "Non preferred name information of a person")
data class PersonNameDto(
	@param:Schema(description = "Family name (often called 'Surname')") val lastName: String? = null,
	@param:Schema(
		description = "Given names (not always 'first'). Includes middle names. This repeating element order: Given Names appear in the correct order for presenting the name",
	) val firstNames: List<String> = emptyList(),
	@param:Schema(
		description = "Starting date of time period when name is/was valid for use. Date encoded as a fuzzy date on 8 positions (YYYYMMDD)",
	) val start: Long? = null,
	@param:Schema(
		description = "Ending date of time period when name is/was valid for use. Date encoded as a fuzzy date on 8 positions (YYYYMMDD)",
	) val end: Long? = null,
	@param:Schema(
		description = "Parts that come before the name. This repeating element order: Prefixes appear in the correct order for presenting the name",
	) val prefix: List<String> = emptyList(),
	@param:Schema(
		description = "Parts that come after the name. This repeating element order: Suffixes appear in the correct order for presenting the name",
	) val suffix: List<String> = emptyList(),
	@param:Schema(description = "Text representation of the full name") val text: String? = null,
	@param:Schema(description = "What is the use of this name") val use: PersonNameUseDto? = null,
)
