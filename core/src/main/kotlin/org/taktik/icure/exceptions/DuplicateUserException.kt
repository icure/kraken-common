/*
 * Copyright (c) 2020. Taktik SA, All rights reserved.
 */
package org.taktik.icure.exceptions

class DuplicateUserException(
	val fieldType: UniqueFieldType,
	val fieldValue: String
) : NonRespectedUnicityException("A user with $fieldType $fieldValue already exists") {
	enum class UniqueFieldType {
		Login, Email, Phone
	}
}
