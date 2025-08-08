/*
 * Copyright (c) 2020. Taktik SA, All rights reserved.
 */
package org.taktik.icure.constants

interface Roles {

	/* Spring Security Authorities */
	interface GrantedAuthority {
		companion object {
			const val ROLE_ADMINISTRATOR = "ROLE_ADMINISTRATOR"
			const val ROLE_USER = "ROLE_USER"
			const val ROLE_ANONYMOUS = "ROLE_ANONYMOUS"
			const val ROLE_HCP = "ROLE_HCP"
			const val ROLE_PATIENT = "ROLE_PATIENT"
			const val ROLE_DEVICE = "ROLE_DEVICE"
		}
	}

	companion object {
		const val DEFAULT_ROLE_NAME = "DEFAULT"
	}
}
