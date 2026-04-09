package org.taktik.icure.services.external.rest.v2.dto.embed

import org.taktik.icure.services.external.rest.v2.dto.base.CodeStubDto

/**
 * Represents a reference range for a measurement, including bounds, applicable age range, tags, codes, and notes.
 */
data class ReferenceRangeDto(
	/** is the lower bound (inclusive) of the reference range */
	val low: Double? = null,
	/** is the higher bound (inclusive) of the reference range */
	val high: Double? = null,
	/** is a text value that can be used to provide reference for a result (e.g. "Negative", "Positive", "Normal", "Abnormal", etc.) */
	val stringValue: String? = null,
	/** are the tags that apply to the reference range */
	val tags: List<CodeStubDto> = emptyList(),
	/** are the codes that apply to the reference range */
	val codes: List<CodeStubDto> = emptyList(),
	/** are the notes to apply to the reference range */
	val notes: List<AnnotationDto> = emptyList(),
	/** is the age range for the reference range (e.g. if age is not specified, then the reference range applies to all ages of patients) */
	val age: RangeDto? = null,
)
