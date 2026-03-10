package org.taktik.icure.services.external.rest.v2.dto.embed

import org.taktik.icure.services.external.rest.v2.dto.base.CodeStubDto

/**
 * Represents a reference range for a measurement, including bounds, applicable age range, tags, codes, and notes.
 */
data class ReferenceRangeDto(
	/** The lower bound of the reference range. */
	val low: Double? = null,
	/** The upper bound of the reference range. */
	val high: Double? = null,
	/** A string representation of the reference range. */
	val stringValue: String? = null,
	/** Tags associated with this reference range. */
	val tags: List<CodeStubDto> = emptyList(),
	/** Codes associated with this reference range. */
	val codes: List<CodeStubDto> = emptyList(),
	/** Annotations providing additional context. */
	val notes: List<AnnotationDto> = emptyList(),
	/** The age range to which this reference range applies. */
	val age: RangeDto? = null,
)
