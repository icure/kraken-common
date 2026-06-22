package org.taktik.icure.services.external.rest.v2.dto.filter.classification

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.databind.JsonDeserializer
import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import org.taktik.icure.handlers.JsonPolymorphismRoot
import org.taktik.icure.services.external.rest.v2.dto.ClassificationDto
import org.taktik.icure.services.external.rest.v2.dto.filter.AbstractFilterDto
import com.fasterxml.jackson.annotation.JsonFilter
import org.taktik.icure.dto.annotations.filtering.ActiveField

@JsonPolymorphismRoot(AbstractFilterDto::class)
@JsonDeserialize(using = JsonDeserializer.None::class)
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
/**
 * Filter that matches classifications by data owner, patient, and creation date range.
 */
data class ClassificationByDataOwnerPatientCreatedDateFilter(
	/** The identifier of the data owner. */
	@ActiveField val dataOwnerId: String,
	/** The start of the creation date range (inclusive). */
	@ActiveField val startDate: Long?,
	/** The end of the creation date range (inclusive). */
	@ActiveField val endDate: Long?,
	/** The set of secret foreign keys used for secure delegation matching. */
	@ActiveField val secretForeignKeys: Set<String>,
	/** Whether to return results in descending order. */
	@ActiveField val descending: Boolean?,
	/** Optional description of this filter. */
	override val desc: String? = null,
) : AbstractFilterDto<ClassificationDto>
