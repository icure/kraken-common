/*
 * Copyright (c) 2020. Taktik SA, All rights reserved.
 */
package org.taktik.icure.entities.embed

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonInclude
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
    val value: Double? = null,
    val valueWithPrecision: ValueWithPrecision? = null,
    /**
     * lower bound of the reference range
     * @deprecated use referenceRanges instead
     */
    @Deprecated(message = "min is deprecated, use referenceRanges instead", replaceWith = ReplaceWith("referenceRanges"))
    val min: Double? = null,
    /**
     * higher bound of the reference range
     * @deprecated use referenceRanges instead
     */
    @Deprecated(message = "max is deprecated, use referenceRanges instead", replaceWith = ReplaceWith("referenceRanges"))
    val max: Double? = null,
    val ref: Double? = null,
    val severity: Int? = null,
    val severityCode: String? = null,
    val evolution: Int? = null,
    /**
     * unit of the measure
     */
    val unit: String? = null,
    val sign: String? = null,
    /**
     * unit codes of the measure
     */
    @field:ValidCode(autoFix = AutoFix.NORMALIZECODE)
    val unitCodes: Set<CodeStub>? = null,
    val comment: String? = null,
    val comparator: String? = null,
    /**
     * reference range of the measure
     *
     * conversion from min/max is done at the client side level since most of the data are encrypted, we can't do it at the server level (or we can, but it will be a lot of work for very little data that aren't encrypted)
     */
    val referenceRanges: List<ReferenceRange> = emptyList()
) : Serializable
