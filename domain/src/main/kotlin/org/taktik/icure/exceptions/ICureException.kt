/*
 * Copyright (c) 2020. Taktik SA, All rights reserved.
 */
package org.taktik.icure.exceptions

open class ICureException : Exception {
	constructor() : super()
	constructor(s: String?, t: Throwable?) : super(s, t)
	constructor(s: String?) : super(s)
	constructor(t: Throwable?) : super(t)

	companion object {
		private const val serialVersionUID = -8702376071839870064L
	}
}
