/*
 * Copyright (c) 2020. Taktik SA, All rights reserved.
 */
package org.taktik.icure.exceptions

class UserRegistrationException : ICureException {
	constructor()
	constructor(message: String?) : super(message)
	constructor(cause: Throwable?) : super(cause)
	constructor(message: String?, cause: Throwable?) : super(message, cause)

	companion object {
		private const val serialVersionUID = 1L
	}
}
