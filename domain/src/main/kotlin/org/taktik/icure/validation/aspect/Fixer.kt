/*
 * Copyright (c) 2020. Taktik SA, All rights reserved.
 */

package org.taktik.icure.validation.aspect

interface Fixer {
	suspend fun <E : Any> fix(doc: E, isCreate: Boolean): E
}
