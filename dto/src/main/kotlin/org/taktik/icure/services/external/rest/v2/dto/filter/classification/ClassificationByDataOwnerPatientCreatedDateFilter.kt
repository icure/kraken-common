package org.taktik.icure.services.external.rest.v2.dto.filter.classification

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.databind.JsonDeserializer
import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import org.taktik.icure.handlers.JsonPolymorphismRoot
import org.taktik.icure.services.external.rest.v2.dto.ClassificationDto
import org.taktik.icure.services.external.rest.v2.dto.filter.AbstractFilterDto

@JsonPolymorphismRoot(AbstractFilterDto::class)
@JsonDeserialize(using = JsonDeserializer.None::class)
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
data class ClassificationByDataOwnerPatientCreatedDateFilter(
	val dataOwnerId: String,
	val startDate: Long?,
	val endDate: Long?,
	val secretForeignKeys: Set<String>,
	val descending: Boolean?,
	override val desc: String? = null
): AbstractFilterDto<ClassificationDto>