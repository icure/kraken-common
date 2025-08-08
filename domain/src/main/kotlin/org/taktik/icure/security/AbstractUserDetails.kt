/*
 * Copyright (c) 2020. Taktik SA, All rights reserved.
 */
package org.taktik.icure.security

import org.springframework.security.core.GrantedAuthority
import org.taktik.icure.utils.DynamicBitArray

abstract class AbstractUserDetails : UserDetails {
	abstract val userId: String
	abstract val principalPermissions: DynamicBitArray
	protected abstract val authorities: Set<GrantedAuthority>

	override fun getAuthorities(): Collection<GrantedAuthority> = authorities

	override var isRealAuth = true
	override var locale: String? = null
	override var logoutURL: String? = null
	override fun getUsername(): String = userId

	override fun getPassword(): String? = null

	override fun isAccountNonExpired(): Boolean = true

	override fun isAccountNonLocked(): Boolean = true

	override fun isCredentialsNonExpired(): Boolean = true

	override fun isEnabled(): Boolean = true

	override fun equals(other: Any?): Boolean {
		if (this === other) return true
		if (other == null || javaClass != other.javaClass) return false
		val that = other as AbstractUserDetails
		if (authorities != that.authorities) return false
		return userId == that.userId
	}

	override fun hashCode(): Int {
		var result = userId.hashCode()
		result = 31 * result + authorities.hashCode()
		return result
	}

	companion object {
		private const val serialVersionUID = 1L
	}
}
