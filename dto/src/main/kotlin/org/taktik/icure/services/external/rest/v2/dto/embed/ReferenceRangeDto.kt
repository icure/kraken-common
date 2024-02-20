package org.taktik.icure.services.external.rest.v2.dto.embed

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonInclude
import org.taktik.icure.services.external.rest.v2.dto.base.CodeStubDto
import java.io.Serializable

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
data class ReferenceRangeDto(
    val low: Double? = null,
    val high: Double? = null,
    val stringValue: String? = null,
    @JsonInclude(JsonInclude.Include.NON_EMPTY) val tags: List<CodeStubDto> = emptyList(),
    @JsonInclude(JsonInclude.Include.NON_EMPTY) val codes: List<CodeStubDto> = emptyList(),
    @JsonInclude(JsonInclude.Include.NON_EMPTY) val notes: List<AnnotationDto> = emptyList(),
    val age: RangeDto? = null
): Serializable
