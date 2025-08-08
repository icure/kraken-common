package org.taktik.icure.validation.aspect

import org.taktik.icure.validation.AutoFix

interface FixedValueProvider {
	suspend fun fix(autoFix: AutoFix, value: Any?): Any?
}
