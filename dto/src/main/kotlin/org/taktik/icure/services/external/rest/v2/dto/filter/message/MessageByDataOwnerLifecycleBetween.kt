package org.taktik.icure.services.external.rest.v2.dto.filter.message

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.databind.JsonDeserializer
import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import org.taktik.icure.handlers.JsonPolymorphismRoot
import org.taktik.icure.services.external.rest.v2.dto.MessageDto
import org.taktik.icure.services.external.rest.v2.dto.filter.AbstractFilterDto
import com.fasterxml.jackson.annotation.JsonFilter
import org.taktik.icure.dto.annotations.filtering.ActiveField

@JsonPolymorphismRoot(AbstractFilterDto::class)
@JsonDeserialize(using = JsonDeserializer.None::class)
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
/**
 * Filter that matches messages by data owner and lifecycle timestamp range.
 */
data class MessageByDataOwnerLifecycleBetween(
	/** The identifier of the data owner. */
	@ActiveField val dataOwnerId: String,
	/** The start of the lifecycle timestamp range (inclusive). */
	@ActiveField val startTimestamp: Long? = null,
	/** The end of the lifecycle timestamp range (inclusive). */
	@ActiveField val endTimestamp: Long? = null,
	/** Whether to return results in descending order. */
	@ActiveField val descending: Boolean = false,
	/** Optional description of this filter. */
	override val desc: String? = null,
) : AbstractFilterDto<MessageDto>
