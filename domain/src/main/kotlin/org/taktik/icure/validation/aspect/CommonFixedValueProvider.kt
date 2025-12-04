package org.taktik.icure.validation.aspect

import org.taktik.icure.entities.base.CodeIdentification
import org.taktik.icure.utils.FuzzyValues
import org.taktik.icure.validation.AutoFix
import org.taktik.icure.validation.DataOwnerProvider
import java.time.Instant
import java.util.*

open class CommonFixedValueProvider(
	private val dataOwnerProvider: DataOwnerProvider,
) : FixedValueProvider {

	protected fun fuzzyNowFixedValue() = FuzzyValues.currentFuzzyDateTime
	protected fun nowFixedValue() = Instant.now().toEpochMilli()
	protected fun uuidFixedValue() = UUID.randomUUID().toString()
	protected open suspend fun currentUserIdFixedValue(): String? = if (!dataOwnerProvider.requestsAutofixAnonymity()) dataOwnerProvider.getCurrentUserId() else null
	protected open suspend fun currentDataOwnerIdFixedValue(): String? = if (!dataOwnerProvider.requestsAutofixAnonymity()) dataOwnerProvider.getCurrentDataOwnerIdOrNull() else null
	protected fun normalizedCodeFixedValue(value: Any?) = (value as? CodeIdentification)?.normalizeIdentification() ?: value

	protected suspend fun getFixedValue(autoFix: AutoFix, value: Any?): Any? = when (autoFix) {
		AutoFix.FUZZYNOW -> fuzzyNowFixedValue()
		AutoFix.NOW -> nowFixedValue()
		AutoFix.UUID -> uuidFixedValue()
		AutoFix.CURRENTUSERID -> currentUserIdFixedValue()
		AutoFix.CURRENTDATAOWNERID -> currentDataOwnerIdFixedValue()
		AutoFix.NOFIX -> value
		AutoFix.NORMALIZECODE -> normalizedCodeFixedValue(value)
	}

	protected suspend fun fix(autoFix: AutoFix, value: Any?, fixer: suspend (autoFix: AutoFix, value: Any?) -> Any?): Any? = when (value) {
		is MutableSet<*> -> value.map { fixer(autoFix, it) }.toMutableSet()
		is MutableList<*> -> value.map { fixer(autoFix, it) }.toMutableList()
		is Set<*> -> value.map { fixer(autoFix, it) }.toSet()
		is Collection<*> -> value.map { fixer(autoFix, it) }
		else -> fixer(autoFix, value)
	}

	override suspend fun fix(autoFix: AutoFix, value: Any?): Any? = fix(autoFix, value, ::getFixedValue)
}
