/*
 * Copyright (c) 2020. Taktik SA, All rights reserved.
 */
package org.taktik.icure.services.external.rest.v1.dto.filter.predicate

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.databind.JsonDeserializer
import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import org.taktik.icure.handlers.JsonPolymorphismRoot

@JsonPolymorphismRoot(org.taktik.icure.services.external.rest.v1.dto.filter.predicate.Predicate::class)
@JsonDeserialize(using = JsonDeserializer.None::class)
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
data class KeyValuePredicate(
	val key: String? = null,
	val operator: Operator? = null,
	val value: Any? = null,
) : Predicate {
	enum class Operator(
		val code: String,
	) {
		EQUAL("=="),
		NOTEQUAL("!="),
		GREATERTHAN(">"),
		SMALLERTHAN("<"),
		GREATERTHANOREQUAL(">="),
		SMALLERTHANOREQUAL("<="),
		LIKE("%="),
		ILIKE("%%="),
		;

		override fun toString(): String = code
	}
}
