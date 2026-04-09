package org.taktik.icure.entities.embed

import org.taktik.icure.entities.base.CodeStub

/**
 * A range of values that can be used to provide reference ranges for a result.
 *
 * @property low is the lower bound (inclusive) of the reference range
 * @property high is the higher bound (inclusive) of the reference range
 * @property stringValue is a text value that can be used to provide reference for a result (e.g. "Negative", "Positive", "Normal", "Abnormal", etc.)
 * @property tags are the tags that apply to the reference range
 * @property codes are the codes that apply to the reference range
 * @property notes are the notes to apply to the reference range
 * @property age is the age range for the reference range (e.g. if age is not specified, then the reference range applies to all ages of patients)
 */
data class ReferenceRange(
	val low: Double? = null,
	val high: Double? = null,
	val stringValue: String? = null,
	val tags: List<CodeStub> = emptyList(),
	val codes: List<CodeStub> = emptyList(),
	val notes: List<Annotation> = emptyList(),
	val age: Range? = null,
)
