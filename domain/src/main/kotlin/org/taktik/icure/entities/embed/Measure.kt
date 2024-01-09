/*
 * Copyright (c) 2020. Taktik SA, All rights reserved.
 */
package org.taktik.icure.entities.embed

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonInclude
import org.taktik.icure.annotations.entities.ContentValue
import org.taktik.icure.annotations.entities.ContentValues
import org.taktik.icure.entities.base.CodeStub
import org.taktik.icure.validation.AutoFix
import org.taktik.icure.validation.ValidCode
import java.io.Serializable

/**
 * A measure is a value that can be associated to a result.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
data class Measure(
    /**
     * value of the measure
     */
    @param:ContentValue(ContentValues.ANY_DOUBLE) val value: Double? = null,
    /**
     * lower bound of the reference range
     * @deprecated use referenceRange instead
     */
    @param:ContentValue(ContentValues.ANY_DOUBLE) val min: Double? = null,
    /**
     * higher bound of the reference range
     * @deprecated use referenceRange instead
     */
    @param:ContentValue(ContentValues.ANY_DOUBLE) val max: Double? = null,
    @param:ContentValue(ContentValues.ANY_DOUBLE) val ref: Double? = null,
    @param:ContentValue(ContentValues.ANY_INT) val severity: Int? = null,
    @param:ContentValue(ContentValues.ANY_STRING) val severityCode: String? = null,
    @param:ContentValue(ContentValues.ANY_INT) val evolution: Int? = null,
    /**
     * unit of the measure
     */
    @param:ContentValue(ContentValues.ANY_STRING) val unit: String? = null,
    @param:ContentValue(ContentValues.ANY_STRING) val sign: String? = null,
    /**
     * unit codes of the measure
     */
    @field:ValidCode(autoFix = AutoFix.NORMALIZECODE)
    val unitCodes: Set<CodeStub>? = null,
    @param:ContentValue(ContentValues.ANY_STRING) val comment: String? = null,
    val comparator: String? = null,
    /**
     * reference range of the measure
     *
     * conversion from min/max is done at the client side level since most of the data are encrypted, we can't do it at the server level (or we can, but it will be a lot of work for very little data that aren't encrypted)
     */
    @param:ContentValue(ContentValues.NESTED_ENTITIES_LIST) val referenceRange: List<ReferenceRange>
) : Serializable
