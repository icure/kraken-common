package org.taktik.icure.services.external.rest.v2.dto.filter.patient

import org.taktik.icure.services.external.rest.v2.dto.PatientDto
import org.taktik.icure.services.external.rest.v2.dto.filter.AbstractFilterDto

data class PatientByDataOwnerModifiedAfterFilter(
	val dataOwnerId: String,
	val startDate: Long?,
	val endDate: Long?,
	val descending: Boolean?,
	override val desc: String?
)  : AbstractFilterDto<PatientDto>