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
 * Filter that matches access logs by data owner, patient, and date range.
 */
data class AccessLogByDataOwnerPatientDateFilter(
	/** The identifier of the data owner. */
	val dataOwnerId: String,
	/** The start of the date range (inclusive). */
	val startDate: Instant?,
	/** The end of the date range (inclusive). */
	val endDate: Instant?,
	/** The set of secret patient identifiers used for secure delegation matching. */
	val secretPatientIds: Set<String>,
	/** Whether to return results in descending order. */
	val descending: Boolean?,
	/** Optional description of this filter. */
	override val desc: String? = null,
) : AbstractFilterDto<AccessLogDto>
