package org.taktik.icure.services.external.rest.v2.dto.embed

import org.taktik.icure.services.external.rest.v2.dto.base.CodeStubDto
import com.fasterxml.jackson.annotation.JsonFilter
import org.taktik.icure.dto.annotations.filtering.ActiveField

/**
 * Represents a reference range for a measurement, including bounds, applicable age range, tags, codes, and notes.
 */
data class ReferenceRangeDto(
	/** The lower bound of the reference range. */
	@ActiveField val low: Double? = null,
	/** The upper bound of the reference range. */
	@ActiveField val high: Double? = null,
	/** A string representation of the reference range. */
	@ActiveField val stringValue: String? = null,
	/** Tags associated with this reference range. */
	@ActiveField val tags: List<CodeStubDto> = emptyList(),
	/** Codes associated with this reference range. */
	@ActiveField val codes: List<CodeStubDto> = emptyList(),
	/** Annotations providing additional context. */
	@ActiveField val notes: List<AnnotationDto> = emptyList(),
	/** The age range to which this reference range applies. */
	@ActiveField val age: RangeDto? = null,
)
