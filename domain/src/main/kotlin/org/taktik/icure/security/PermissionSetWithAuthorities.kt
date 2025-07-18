/*
 * Copyright (c) 2020. Taktik SA, All rights reserved.
 */
package org.taktik.icure.security

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonInclude
import org.springframework.security.core.GrantedAuthority
import org.taktik.icure.entities.security.Permission
import java.io.Serializable

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
data class PermissionSetWithAuthorities(
	val permissionSetIdentifier: PermissionSetIdentifier,
	val permissions: Set<Permission>,
	val grantedAuthorities: Set<GrantedAuthority>,
) : Serializable
