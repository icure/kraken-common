package org.taktik.icure.entities.embed

import org.taktik.icure.annotations.entities.ContentValue
import org.taktik.icure.annotations.entities.ContentValues
import org.taktik.icure.entities.base.CodeStub

/**
 * A range of values that can be used to provide reference ranges for a result.
 *
 * @property low is the lower bound (inclusive) of the reference range
 * @property high is the higher bound (inclusive) of the reference range
 * @property tags are the tags that apply to the reference range
 * @property codes are the codes that apply to the reference range
 * @property notes are the notes to apply to the reference range
 * @property age is the age range for the reference range (e.g. if age is not specified, then the reference range applies to all ages of patients)
 */
data class ReferenceRange(
    @param:ContentValue(ContentValues.ANY_DOUBLE) val low: Double? = null,
    @param:ContentValue(ContentValues.ANY_DOUBLE) val high: Double? = null,
    @param:ContentValue(ContentValues.NESTED_ENTITIES_LIST) val tags: List<CodeStub> = emptyList(),
    @param:ContentValue(ContentValues.NESTED_ENTITIES_LIST) val codes: List<CodeStub> = emptyList(),
    @param:ContentValue(ContentValues.NESTED_ENTITIES_LIST) val notes: List<Annotation> = emptyList(),
    @param:ContentValue(ContentValues.NESTED_ENTITY) val age: Range? = null
)
