/*
 * Copyright (c) 2020. Taktik SA, All rights reserved.
 */
package org.taktik.icure.exceptions

class DeletionException : ICureException {
	constructor() : super()
	constructor(message: String?) : super(message)
	constructor(message: String?, t: Throwable?) : super(message, t)
}
