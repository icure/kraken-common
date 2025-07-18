/*
 * Copyright (c) 2020. Taktik SA, All rights reserved.
 */

package org.taktik.icure.properties

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Component

@Component
@Profile("app")
@ConfigurationProperties("icure.authentication.shared")
data class AuthenticationProperties(
	var local: Boolean = false,
	var minPasswordLength: Int = 6,
	var recommendedPasswordLength: Int = 8,
)
