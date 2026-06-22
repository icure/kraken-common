package org.taktik.icure.services.external.rest.v2.dto.filter.message

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.databind.JsonDeserializer
import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import org.taktik.icure.handlers.JsonPolymorphismRoot
import org.taktik.icure.services.external.rest.v2.dto.MessageDto
import org.taktik.icure.services.external.rest.v2.dto.filter.AbstractFilterDto
import java.time.Instant
import com.fasterxml.jackson.annotation.JsonFilter
import org.taktik.icure.dto.annotations.filtering.ActiveField

@JsonPolymorphismRoot(AbstractFilterDto::class)
@JsonDeserialize(using = JsonDeserializer.None::class)
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
/**
 * Filter that matches messages by data owner, patient, and sent date range.
 */
@JsonFilter("org.taktik.icure.services.external.rest.v2.dto.filter.message.MessageByDataOwnerPatientSentDateFilter")
data class MessageByDataOwnerPatientSentDateFilter(
	/** The identifier of the data owner. */
	@ActiveField val dataOwnerId: String,
	/** The set of secret patient keys used for secure delegation matching. */
	@ActiveField val secretPatientKeys: Set<String>,
	/** The start of the sent date range (inclusive). */
	@ActiveField val startDate: Instant? = null,
	/** The end of the sent date range (inclusive). */
	@ActiveField val endDate: Instant? = null,
	/** Whether to return results in descending order. */
	@ActiveField val descending: Boolean?,
	/** Optional description of this filter. */
	override val desc: String? = null,
) : AbstractFilterDto<MessageDto>
