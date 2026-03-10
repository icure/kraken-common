package org.taktik.icure.services.external.rest.v2.dto.filter.healthelement

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.databind.JsonDeserializer
import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import org.taktik.icure.handlers.JsonPolymorphismRoot
import org.taktik.icure.services.external.rest.v2.dto.HealthElementDto
import org.taktik.icure.services.external.rest.v2.dto.filter.AbstractFilterDto

@JsonPolymorphismRoot(AbstractFilterDto::class)
@JsonDeserialize(using = JsonDeserializer.None::class)
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
/**
 * Filter that matches health elements by data owner, patient, and opening date range.
 */
data class HealthElementByDataOwnerPatientOpeningDate(
	/** Optional description of this filter. */
	override val desc: String? = null,
	/** The identifier of the healthcare party (data owner). */
	val healthcarePartyId: String,
	/** The set of secret foreign keys for patient matching. */
	val patientSecretForeignKeys: Set<String> = emptySet(),
	/** The start of the opening date range (inclusive). */
	val startDate: Long? = null,
	/** The end of the opening date range (inclusive). */
	val endDate: Long? = null,
	/** Whether to return results in descending order. */
	val descending: Boolean = false,
) : AbstractFilterDto<HealthElementDto>
