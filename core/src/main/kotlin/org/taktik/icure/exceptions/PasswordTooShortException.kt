package org.taktik.icure.exceptions

import org.springframework.security.authentication.BadCredentialsException

class PasswordTooShortException(
	message: String = "Password too short",
) : BadCredentialsException(message)
