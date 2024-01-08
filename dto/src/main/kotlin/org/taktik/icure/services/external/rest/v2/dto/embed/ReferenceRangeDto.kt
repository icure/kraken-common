package org.taktik.icure.services.external.rest.v2.dto.embed

import org.taktik.icure.services.external.rest.v2.dto.base.CodeStubDto

data class ReferenceRangeDto(
    val low: Double? = null,
    val high: Double? = null,
    val tags: List<CodeStubDto> = emptyList(),
    val codes: List<CodeStubDto> = emptyList(),
    val notes: List<Annotation> = emptyList(),
    val age: RangeDto? = null
)
