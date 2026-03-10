package org.taktik.icure.services.external.rest.v2.dto.filter.pricing

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.databind.JsonDeserializer
import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import org.taktik.icure.handlers.JsonPolymorphismRoot
import org.taktik.icure.services.external.rest.v2.dto.TarificationDto
import org.taktik.icure.services.external.rest.v2.dto.filter.AbstractFilterDto

@JsonPolymorphismRoot(AbstractFilterDto::class)
@JsonDeserialize(using = JsonDeserializer.None::class)
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
/**
 * Filter that matches pricing entries by region, types, language, and label.
 */
data class PricingByRegionTypesLanguageLabelFilter(
	/** The region to filter pricing entries by. */
	val region: String? = null,
	/** The list of pricing types to match. */
	val types: List<String>,
	/** The language of the label to match. */
	val language: String,
	/** The label text to match. */
	val label: String,
	/** Optional description of this filter. */
	override val desc: String? = null,
) : AbstractFilterDto<TarificationDto>
