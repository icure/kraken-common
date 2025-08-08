/*
 * Copyright (c) 2020. Taktik SA, All rights reserved.
 */
package org.taktik.icure.security.database

import org.springframework.security.crypto.password.MessageDigestPasswordEncoder

class ShaAndVerificationCodePasswordEncoder(
	algorithm: String?,
) : MessageDigestPasswordEncoder(algorithm) {
	override fun matches(
		rawPassword: CharSequence,
		encodedPassword: String,
	): Boolean {
		/*
		 * We split only when checking the password, not when encoding it. If the password of the user includes a `|`
		 * the full password with `|` included will be included.
		 */
		// TODO shouldn't we split by `|` DROP the last and join the rest back? Otherwise users with | in the password
		// may have issues.
		return super.matches(rawPassword, encodedPassword) ||
			super.matches(
				rawPassword.toString().split("\\|").toTypedArray()[0],
				encodedPassword,
			)
	}
}
