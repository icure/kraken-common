package org.taktik.icure.entities.embed

import org.taktik.icure.annotations.entities.ContentValue
import org.taktik.icure.annotations.entities.ContentValues

/**
 * A range of values that can be used to provide reference ranges for a result.
 *
 * @property low is the lower bound of the range
 * @property high is the higher bound of the range
 */
data class Range(
    @param:ContentValue(ContentValues.ANY_DOUBLE) val low: Double? = null,
    @param:ContentValue(ContentValues.ANY_DOUBLE) val high: Double? = null,
)
