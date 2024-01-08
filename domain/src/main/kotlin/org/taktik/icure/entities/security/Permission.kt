/*
 * Copyright (c) 2020. Taktik SA, All rights reserved.
 */
package org.taktik.icure.entities.security

import java.io.Serializable
import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonInclude

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
data class Permission(val grants: Set<PermissionItem> = emptySet(), val revokes: Set<PermissionItem> = emptySet()) : Cloneable, Serializable {
	fun grant(permissionItem: PermissionItem): Permission {
		return this.copy(grants = if (this.grants.any { it.type == permissionItem.type }) this.grants.map { if (it.type == permissionItem.type) it.merge(permissionItem) else it }.toSet() else this.grants + permissionItem)
	}

	fun revoke(permissionItem: PermissionItem): Permission {
		return this.copy(revokes = if (this.revokes.any { it.type == permissionItem.type }) this.revokes.map { if (it.type == permissionItem.type) it.merge(permissionItem) else it }.toSet() else this.revokes + permissionItem)
	}

	@get:JsonIgnore
	val isUseless: Boolean
		get() = grants.isEmpty() && revokes.isEmpty()
}
