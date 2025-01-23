/*
 * Copyright (c) 2020. Taktik SA, All rights reserved.
 */
package org.taktik.icure.validation

import java.time.Instant
import org.taktik.icure.entities.base.CodeIdentification
import org.taktik.icure.utils.FuzzyValues

enum class AutoFix(private val fixer: suspend (b: Any?, v: Any?, sl: DataOwnerProvider?) -> Any?) {
	FUZZYNOW({ _: Any?, _: Any?, _: DataOwnerProvider? -> FuzzyValues.currentFuzzyDateTime }),
	NOW({ _: Any?, _: Any?, _: DataOwnerProvider? -> Instant.now().toEpochMilli() }),
	UUID({ _: Any?, _: Any?, _: DataOwnerProvider? -> java.util.UUID.randomUUID().toString() }),
	CURRENTUSERID({ _: Any?, _: Any?, sl: DataOwnerProvider? -> sl?.run { if (requestsAutofixAnonymity()) null else getCurrentUserId() } }),
	CURRENTDATAOWNERID({ _: Any?, _: Any?, sl: DataOwnerProvider? -> sl?.run { if (requestsAutofixAnonymity()) null else getCurrentDataOwnerId() } }),
	NOFIX({ _: Any?, v: Any?, _: DataOwnerProvider? -> v }),
	NORMALIZECODE({ _: Any?, v: Any?, _: DataOwnerProvider? -> (v as? CodeIdentification)?.normalizeIdentification() ?: v });

	suspend fun fix(bean: Any?, value: Any?, sessionLogic: DataOwnerProvider?): Any? {
		return (value as? MutableSet<*>)?.let { it.map { v: Any? -> fixer(bean, v, sessionLogic) }.toMutableSet() }
			?: (value as? MutableList<*>)?.let { it.map { v: Any? -> fixer(bean, v, sessionLogic) }.toMutableList() }
			?: (value as? Set<*>)?.let { it.map { v: Any? -> fixer(bean, v, sessionLogic) }.toSet() }
			?: (value as? Collection<*>)?.let { it.map { v: Any? -> fixer(bean, v, sessionLogic) } }
			?: fixer(bean, value, sessionLogic)
	}
}
