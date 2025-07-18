package org.taktik.icure.exceptions

class IllegalEntityException(
	message: String = "Invalid 2FA",
) : IllegalStateException(message)
