package org.taktik.icure.services.external.rest.v2.dto.filter.contact

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.databind.JsonDeserializer
import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import org.taktik.icure.handlers.JsonPolymorphismRoot
import org.taktik.icure.services.external.rest.v2.dto.ContactDto
import org.taktik.icure.services.external.rest.v2.dto.filter.AbstractFilterDto

@JsonPolymorphismRoot(AbstractFilterDto::class)
@JsonDeserialize(using = JsonDeserializer.None::class)
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
/**
 * Filter that matches contacts by data owner, patient, and opening date range.
 */
data class ContactByDataOwnerPatientOpeningDateFilter(
	/** The identifier of the data owner. */
	val dataOwnerId: String,
	/** The set of secret foreign keys used for secure delegation matching. */
	val secretForeignKeys: Set<String>,
	/** The start of the opening date range (inclusive). */
	val startDate: Long? = null,
	/** The end of the opening date range (inclusive). */
	val endDate: Long? = null,
	/** Whether to return results in descending order. */
	val descending: Boolean? = null,
	/** Optional description of this filter. */
	override val desc: String? = null,
) : AbstractFilterDto<ContactDto>
