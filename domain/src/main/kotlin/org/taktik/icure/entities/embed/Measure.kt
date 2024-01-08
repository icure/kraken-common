/*
 * Copyright (c) 2020. Taktik SA, All rights reserved.
 */
package org.taktik.icure.entities.embed

import java.io.Serializable
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonInclude
import org.taktik.icure.annotations.entities.ContentValue
import org.taktik.icure.annotations.entities.ContentValues
import org.taktik.icure.entities.base.CodeStub
import org.taktik.icure.validation.AutoFix
import org.taktik.icure.validation.ValidCode

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
data class Measure(
	@param:ContentValue(ContentValues.ANY_DOUBLE) val value: Double? = null,
	@param:ContentValue(ContentValues.ANY_DOUBLE) val min: Double? = null,
	@param:ContentValue(ContentValues.ANY_DOUBLE) val max: Double? = null,
	@param:ContentValue(ContentValues.ANY_DOUBLE) val ref: Double? = null,
	@param:ContentValue(ContentValues.ANY_INT) val severity: Int? = null,
	@param:ContentValue(ContentValues.ANY_STRING) val severityCode: String? = null,
	@param:ContentValue(ContentValues.ANY_INT) val evolution: Int? = null,
	@param:ContentValue(ContentValues.ANY_STRING) val unit: String? = null,
	@param:ContentValue(ContentValues.ANY_STRING) val sign: String? = null,
	@field:ValidCode(autoFix = AutoFix.NORMALIZECODE)
	val unitCodes: Set<CodeStub>? = null,
	@param:ContentValue(ContentValues.ANY_STRING) val comment: String? = null,
	val comparator: String? = null,
	@param:ContentValue(ContentValues.NESTED_ENTITIES_LIST) val referenceRange: List<ReferenceRange>
) : Serializable
