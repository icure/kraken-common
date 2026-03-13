package org.taktik.icure.services.external.rest.v2.dto.filter.accesslog

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.databind.JsonDeserializer
import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import org.taktik.icure.handlers.JsonPolymorphismRoot
import org.taktik.icure.services.external.rest.v2.dto.AccessLogDto
import org.taktik.icure.services.external.rest.v2.dto.filter.AbstractFilterDto
import java.time.Instant

@JsonPolymorphismRoot(AbstractFilterDto::class)
@JsonDeserialize(using = JsonDeserializer.None::class)
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
/**
 * Filter that matches access logs by user identifier, access type, and start date.
 */
data class AccessLogByUserIdUserTypeDateFilter(
	/** The identifier of the user who created the access log. */
	val userId: String,
	/** The type of access to filter on. */
	val accessType: String?,
	/** The start date from which to retrieve access logs. */
	val startDate: Instant?,
	/** Whether to return results in descending order. */
	val descending: Boolean?,
	/** Optional description of this filter. */
	override val desc: String? = null,
) : AbstractFilterDto<AccessLogDto>
