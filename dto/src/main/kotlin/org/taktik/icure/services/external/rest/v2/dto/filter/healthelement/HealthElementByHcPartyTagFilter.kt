package org.taktik.icure.services.external.rest.v2.dto.filter.healthelement

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.databind.JsonDeserializer
import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import org.taktik.icure.handlers.JsonPolymorphismRoot
import org.taktik.icure.services.external.rest.v2.dto.HealthElementDto
import org.taktik.icure.services.external.rest.v2.dto.filter.AbstractFilterDto
import org.taktik.icure.services.external.rest.v2.dto.filter.VersionFilteringDto

@JsonPolymorphismRoot(AbstractFilterDto::class)
@JsonDeserialize(using = JsonDeserializer.None::class)
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
/**
 * Filter that matches health elements by healthcare party, tag, and date range.
 */
data class HealthElementByHcPartyTagFilter(
	/** Optional description of this filter. */
	override val desc: String? = null,
	/** The identifier of the healthcare party. */
	val healthcarePartyId: String,
	/** The type of the tag to match. */
	val tagType: String,
	/** The tag code value to match. */
	val tagCode: String,
	/** The start of the health element date range. */
	val startOfHealthElementDate: Long? = null,
	/** The end of the health element date range. */
	val endOfHealthElementDate: Long? = null,
	/** Optional version filtering criteria. */
	val versionFiltering: VersionFilteringDto? = null,
) : AbstractFilterDto<HealthElementDto>

