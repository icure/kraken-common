package org.taktik.icure.services.external.rest.v2.dto.filter.contact

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.databind.JsonDeserializer
import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import org.taktik.icure.handlers.JsonPolymorphismRoot
import org.taktik.icure.services.external.rest.v2.dto.ContactDto
import org.taktik.icure.services.external.rest.v2.dto.filter.AbstractFilterDto
import com.fasterxml.jackson.annotation.JsonFilter
import org.taktik.icure.dto.annotations.filtering.ActiveField

@JsonPolymorphismRoot(AbstractFilterDto::class)
@JsonDeserialize(using = JsonDeserializer.None::class)
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
/**
 * Filter that matches contacts by data owner and opening date range.
 */
@JsonFilter("org.taktik.icure.services.external.rest.v2.dto.filter.contact.ContactByDataOwnerOpeningDateFilter")
data class ContactByDataOwnerOpeningDateFilter(
	/** The identifier of the data owner. */
	@ActiveField val dataOwnerId: String,
	/** The start of the opening date range (inclusive). */
	@ActiveField val startDate: Long? = null,
	/** The end of the opening date range (inclusive). */
	@ActiveField val endDate: Long? = null,
	/** Whether to return results in descending order. */
	@ActiveField val descending: Boolean? = null,
	/** Optional description of this filter. */
	override val desc: String? = null,
) : AbstractFilterDto<ContactDto>
