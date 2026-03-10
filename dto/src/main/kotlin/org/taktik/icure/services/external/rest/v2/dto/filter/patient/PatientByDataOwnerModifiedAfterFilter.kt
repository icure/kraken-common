package org.taktik.icure.services.external.rest.v2.dto.filter.patient

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.databind.JsonDeserializer
import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import org.taktik.icure.handlers.JsonPolymorphismRoot
import org.taktik.icure.services.external.rest.v2.dto.PatientDto
import org.taktik.icure.services.external.rest.v2.dto.filter.AbstractFilterDto

@JsonPolymorphismRoot(AbstractFilterDto::class)
@JsonDeserialize(using = JsonDeserializer.None::class)
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
/**
 * Filter that matches patients by data owner and modification date range.
 */
data class PatientByDataOwnerModifiedAfterFilter(
	/** The identifier of the data owner. */
	val dataOwnerId: String,
	/** The start of the modification date range (inclusive). */
	val startDate: Long?,
	/** The end of the modification date range (inclusive). */
	val endDate: Long?,
	/** Whether to return results in descending order. */
	val descending: Boolean?,
	/** Optional description of this filter. */
	override val desc: String?,
) : AbstractFilterDto<PatientDto>
