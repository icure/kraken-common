/*
 * Copyright (c) 2020. Taktik SA, All rights reserved.
 */
package org.taktik.icure.security

import java.io.Serializable
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonInclude
import org.taktik.icure.entities.security.Principal

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
data class PermissionSetIdentifier(val principalClass: Class<out Principal>, val principalId: String) : Serializable {
	fun getPrincipalIdOfClass(principalClass: Class<out Principal>): String? {
		return if (this.principalClass == principalClass) principalId else null
	}
}
