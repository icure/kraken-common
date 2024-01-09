package org.taktik.icure.entities.embed

import org.taktik.icure.annotations.entities.ContentValue
import org.taktik.icure.annotations.entities.ContentValues

/**
 * A general range of values.
 *
 * @property low is the lower bound (inclusive) of the range
 * @property high is the higher bound (inclusive) of the range
 */
data class Range(
    @param:ContentValue(ContentValues.ANY_DOUBLE) val low: Double? = null,
    @param:ContentValue(ContentValues.ANY_DOUBLE) val high: Double? = null,
)
