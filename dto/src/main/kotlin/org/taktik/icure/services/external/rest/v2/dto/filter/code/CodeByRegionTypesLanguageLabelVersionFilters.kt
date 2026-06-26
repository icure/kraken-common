package org.taktik.icure.services.external.rest.v2.dto.filter.code

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.databind.JsonDeserializer
import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import org.taktik.icure.handlers.JsonPolymorphismRoot
import org.taktik.icure.services.external.rest.v2.dto.CodeDto
import org.taktik.icure.services.external.rest.v2.dto.filter.AbstractFilterDto
import com.fasterxml.jackson.annotation.JsonFilter
import org.taktik.icure.dto.annotations.filtering.ActiveField

@JsonPolymorphismRoot(AbstractFilterDto::class)
@JsonDeserialize(using = JsonDeserializer.None::class)
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
/**
 * Filter that matches codes by region, multiple types, language, label, and version.
 */
data class CodeByRegionTypesLanguageLabelVersionFilters(
	/** The region to filter codes by. */
	@ActiveField val region: String? = null,
	/** The list of code types to match. */
	@ActiveField val types: List<String>,
	/** The language of the label to match. */
	@ActiveField val language: String,
	/** The label text to match. */
	@ActiveField val label: String,
	/** The code version to match. */
	@ActiveField val version: String? = null,
	/** Optional description of this filter. */
	override val desc: String? = null,
) : AbstractFilterDto<CodeDto>
