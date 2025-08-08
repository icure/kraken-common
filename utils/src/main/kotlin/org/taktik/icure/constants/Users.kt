/*
 * Copyright (c) 2020. Taktik SA, All rights reserved.
 */
package org.taktik.icure.constants

interface Users {
	enum class Type {
		database,
		ldap,
		token,
	}

	enum class Status {
		ACTIVE,
		DISABLED,
		REGISTERING,
		;

		companion object {
			fun fromInt(value: Int): Status = Status::class.java.enumConstants[value]
		}
	}
}
