package org.taktik.icure.services.external.rest.v2.dto.filter.form

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.databind.JsonDeserializer
import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import org.taktik.icure.handlers.JsonPolymorphismRoot
import org.taktik.icure.services.external.rest.v2.dto.FormDto
import org.taktik.icure.services.external.rest.v2.dto.filter.AbstractFilterDto

@JsonPolymorphismRoot(AbstractFilterDto::class)
@JsonDeserialize(using = JsonDeserializer.None::class)
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
data class FormByDataOwnerPatientOpeningDateFilter(
	val dataOwnerId: String,
	val secretPatientKeys: Set<String>,
	val startDate: Long? = null,
	val endDate: Long? = null,
	val descending: Boolean? = null,
	override val desc: String? = null,
) : AbstractFilterDto<FormDto>
