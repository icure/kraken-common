/*
 * Copyright (c) 2020. Taktik SA, All rights reserved.
 */
package org.taktik.icure.domain.filter.impl.predicate

import java.lang.reflect.InvocationTargetException
import org.apache.commons.beanutils.PropertyUtilsBean
import org.taktik.couchdb.id.Identifiable
import org.taktik.icure.domain.filter.predicate.Predicate

data class KeyValuePredicate(
	val key: String? = null,
	val operator: Operator? = null,
	val value: Any? = null
) : Predicate {
	val pub = PropertyUtilsBean()

	override fun apply(input: Identifiable<String>): Boolean {
		return try {
			operator!!.apply(pub.getProperty(input, key) as Comparable<Any>?, value as Comparable<Any>?)
		} catch (e: IllegalAccessException) {
			throw RuntimeException(e)
		} catch (e: InvocationTargetException) {
			throw RuntimeException(e)
		} catch (e: NoSuchMethodException) {
			throw RuntimeException(e)
		}
	}

	enum class Operator(val code: String, val lambda: (Comparable<Any>?, Comparable<Any>?) -> Boolean) {
		EQUAL("==", { a, b ->
			if (a != null && a is Number && b != null && b is Number) {
				if (a.toDouble() == b.toDouble()) true else a == b
			} else a == b
		}),
		NOTEQUAL("!=", { a, b -> !EQUAL.apply(a, b) }),
		GREATERTHAN(">", { a, b -> if (a == null && b == null) false else if (a == null) false else if (b == null) true else a > b }),
		SMALLERTHAN("<", { a, b -> if (a == null && b == null) false else if (a == null) false else if (b == null) true else a < b }),
		GREATERTHANOREQUAL(">=", { a, b -> if (a == null && b == null) false else if (a == null) false else if (b == null) true else a >= b }),
		SMALLERTHANOREQUAL("<=", { a, b -> if (a == null && b == null) false else if (a == null) false else if (b == null) true else a <= b }),
		LIKE("%=", { a, b -> b?.let { pattern -> a?.toString()?.matches(Regex(pattern.toString())) } ?: false }),
		ILIKE("%%=", { a, b -> b?.let { pattern -> a?.toString()?.toLowerCase()?.matches(Regex(pattern.toString().toLowerCase())) } ?: false });

		override fun toString(): String {
			return code
		}

		fun apply(a: Comparable<Any>?, b: Comparable<Any>?): Boolean {
			return lambda(a, b)
		}
	}
}
