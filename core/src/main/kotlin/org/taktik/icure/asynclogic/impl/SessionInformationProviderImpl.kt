/*
 * Copyright (c) 2020. Taktik SA, All rights reserved.
 */

package org.taktik.icure.asynclogic.impl

import kotlinx.coroutines.reactive.awaitFirstOrNull
import kotlinx.coroutines.reactor.ReactorContext
import org.springframework.security.authentication.AuthenticationServiceException
import org.springframework.security.core.Authentication
import org.springframework.web.server.ServerWebExchange
import org.taktik.icure.asynclogic.SessionInformationProvider
import org.taktik.icure.entities.DataOwnerType
import org.taktik.icure.entities.base.HasEncryptionMetadata
import org.taktik.icure.entities.base.hasDataOwnerOrDelegationKey
import org.taktik.icure.entities.utils.SemanticVersion
import org.taktik.icure.entities.utils.Sha256HexString
import org.taktik.icure.security.DataOwnerAuthenticationDetails
import org.taktik.icure.security.SessionAccessControlKeysProvider
import org.taktik.icure.security.UserDetails
import org.taktik.icure.security.hashAccessControlKey
import org.taktik.icure.security.jwt.JwtDetails
import org.taktik.icure.security.loadSecurityContext
import java.io.Serializable
import kotlin.coroutines.coroutineContext

open class SessionInformationProviderImpl(
	private val sessionAccessControlKeysProvider: SessionAccessControlKeysProvider,
) : SessionInformationProvider {
	override suspend fun getCurrentSessionContext(): SessionInformationProvider.AsyncSessionContext =
		doGetCurrentSessionContext()

	protected open suspend fun doGetCurrentSessionContext() =
		getCurrentAuthentication()?.let { SessionContextImpl(it) } ?: throw AuthenticationServiceException(
			"getCurrentAuthentication() returned null, no SecurityContext in the coroutine context?",
		)

	override suspend fun getCurrentUserId(): String = getCurrentSessionContext().getUserId()

	override suspend fun getCurrentDataOwnerId(): String =
		getCurrentDataOwnerIdOrNull() ?: throw AuthenticationServiceException("Failed to extract current data owner id")

	override suspend fun getCurrentDataOwnerIdOrNull(): String? =
		doGetCurrentSessionContext().getDataOwnerId()

	override suspend fun getSearchKeyMatcher(): (String, HasEncryptionMetadata) -> Boolean {
		val authenticationDetails = getDataOwnerAuthenticationDetailsOrNull()
		return { hcpId, item ->
			if (hcpId == authenticationDetails?.dataOwner?.id) {
				item.hasDataOwnerOrDelegationKey(hcpId) ||
					authenticationDetails.accessControlKeysHashes.any { item.hasDataOwnerOrDelegationKey(it) }
			} else {
				item.hasDataOwnerOrDelegationKey(hcpId)
			}
		}
	}

	override suspend fun getAllSearchKeysIfCurrentDataOwner(dataOwnerId: String): Set<String> {
		val authenticationDetails = getDataOwnerAuthenticationDetailsOrNull()
		return when (authenticationDetails?.dataOwner?.id) {
			dataOwnerId -> authenticationDetails.accessControlKeysHashes + dataOwnerId
			else -> setOf(dataOwnerId)
		}
	}

	override suspend fun getDataOwnerAuthenticationDetails(): DataOwnerAuthenticationDetails = DataOwnerAuthenticationDetailsImpl(
		getDataOwnerDetails(),
		sessionAccessControlKeysProvider.getAccessControlKeys(),
	)

	override suspend fun getDataOwnerAuthenticationDetailsOrNull(): DataOwnerAuthenticationDetails? {
		return DataOwnerAuthenticationDetailsImpl.nullIfNotDataOwner(
			getDataOwnerDetails(),
			sessionAccessControlKeysProvider.getAccessControlKeys(),
		)
	}

	private suspend fun getDataOwnerDetails(): DataOwnerAuthenticationDetails.DataOwnerDetails? =
		doGetCurrentSessionContext().let { sc ->
			when (sc.getDataOwnerType()) {
				null -> null
				DataOwnerType.HCP -> HcpDataOwnerDetails(sc.getDataOwnerId()!!, sc.getDataOwnerParentHierarchy(), sc.getDataOwnerHierarchy())
				DataOwnerType.PATIENT -> PatientDataOwnerDetails(sc.getDataOwnerId()!!)
				DataOwnerType.DEVICE -> DeviceDataOwnerDetails(sc.getDataOwnerId()!!)
			}
		}

	override suspend fun selfOrDataOwnerHierarchyContains(id: String) =
		doGetCurrentSessionContext().let {
			it.getDataOwnerId() == id || id in it.getDataOwnerHierarchy()
		}

	override suspend fun getCallerCardinalVersion(): SemanticVersion? = coroutineContext[ReactorContext]
		?.context
		?.getOrEmpty<ServerWebExchange>(ServerWebExchange::class.java)
		?.orElse(null)
		?.request
		?.headers
		?.get(REQUEST_CARDINAL_VERSION_HEADER)
		?.firstOrNull()
		?.let { SemanticVersion(it) }

	override suspend fun requestsAutofixAnonymity(): Boolean {
		val default =
			when (getCurrentSessionContext().getDataOwnerType()) {
				DataOwnerType.HCP -> false
				DataOwnerType.DEVICE -> true
				DataOwnerType.PATIENT -> true
				null -> throw AuthenticationServiceException("User is not a data owner.")
			}
		val requestAnonHeader =
			coroutineContext[ReactorContext]
				?.context
				?.getOrEmpty<ServerWebExchange>(ServerWebExchange::class.java)
				?.orElse(null)
				?.request
				?.headers
				?.get(REQUEST_AUTOFIX_ANONYMITY_HEADER)
		return when {
			requestAnonHeader == null -> default
			requestAnonHeader.size != 1 -> throw IllegalArgumentException("$REQUEST_AUTOFIX_ANONYMITY_HEADER should not be repeated")
			requestAnonHeader.first().lowercase() == "true" -> true
			requestAnonHeader.first().lowercase() == "false" -> false
			else -> throw IllegalArgumentException("$REQUEST_AUTOFIX_ANONYMITY_HEADER should be true or false")
		}
	}

	protected open class SessionContextImpl(
		protected val _authentication: Authentication,
	) : SessionInformationProvider.AsyncSessionContext,
		Serializable {

		protected var _userDetails: UserDetails = extractUserDetails(_authentication)

		override fun getUserId(): String = (_userDetails as JwtDetails).userId

		override fun getPatientId(): String? = if ((_userDetails as JwtDetails).dataOwnerType == DataOwnerType.PATIENT) {
			getDataOwnerId()
		} else {
			null
		}

		override fun getHealthcarePartyId(): String? = if ((_userDetails as JwtDetails).dataOwnerType == DataOwnerType.HCP) {
			getDataOwnerId()
		} else {
			null
		}

		override fun getDeviceId(): String? = if ((_userDetails as JwtDetails).dataOwnerType == DataOwnerType.DEVICE) {
			getDataOwnerId()
		} else {
			null
		}

		override fun getGlobalUserId(): String = getUserId()

		open fun getDataOwnerId(): String? = (_userDetails as JwtDetails).dataOwnerId

		/**
		 * All the ancestor data owner groups of the data owner, regardless of the rights they grant (membership).
		 */
		fun getDataOwnerHierarchy(): Set<String> = (_userDetails as JwtDetails).let {
			if (it.hcpOtherGroupIds.isEmpty()) it.hcpHierarchyIds else it.hcpHierarchyIds + it.hcpOtherGroupIds
		}

		/**
		 * Only the ancestor data owner groups that grant administrative (parent) rights over the data owner.
		 */
		fun getDataOwnerParentHierarchy(): Set<String> = (_userDetails as JwtDetails).hcpHierarchyIds

		override fun getDataOwnerType(): DataOwnerType? = (_userDetails as JwtDetails).dataOwnerType
	}

	companion object {
		/**
		 * Header that can be used to override the default value of request anonymity for the request. This prevents
		 * the user id / data owner id to be automatically inserted in clear in the data created / modified by the data
		 * owner.
		 */
		const val REQUEST_AUTOFIX_ANONYMITY_HEADER = "Icure-Request-Autofix-Anonymity"

		/**
		 * Header that can be used on the sdk to specify the version of the SDK used to make a request.
		 * Allows enabling some additional integrity checks that are NOT safety critical.
		 */
		const val REQUEST_CARDINAL_VERSION_HEADER = "Icure-Request-Cardinal-Version"

		private suspend fun getCurrentAuthentication() = loadSecurityContext()?.map { it.authentication }?.awaitFirstOrNull()

		private suspend fun invalidateCurrentAuthentication() {
			loadSecurityContext()?.map { it.authentication.isAuthenticated = false }?.awaitFirstOrNull()
				?: throw AuthenticationServiceException("Could not find authentication object in ReactorContext")
		}

		private fun extractUserDetails(authentication: Authentication): UserDetails = authentication.principal?.let { it as? UserDetails }
			?: throw AuthenticationServiceException("Failed extracting user details: ${authentication.principal}")
	}

	private class DataOwnerAuthenticationDetailsImpl private constructor (
		override val dataOwner: DataOwnerAuthenticationDetails.DataOwnerDetails?,
		override val accessControlKeys: List<ByteArray>,
	) : DataOwnerAuthenticationDetails {
		init {
			if (dataOwner == null && accessControlKeys.isEmpty()) {
				throw AuthenticationServiceException(
					"Anonymous data owner must provide at least some secret exchange ids.",
				)
			}
		}

		companion object {
			operator fun invoke(
				dataOwner: DataOwnerAuthenticationDetails.DataOwnerDetails?,
				accessControlKeys: List<ByteArray>,
			): DataOwnerAuthenticationDetails = DataOwnerAuthenticationDetailsImpl(
				dataOwner,
				accessControlKeys,
			)

			fun nullIfNotDataOwner(
				dataOwner: DataOwnerAuthenticationDetails.DataOwnerDetails?,
				accessControlKeys: List<ByteArray>,
			): DataOwnerAuthenticationDetails? = if (dataOwner == null && accessControlKeys.isEmpty()) {
				null
			} else {
				DataOwnerAuthenticationDetailsImpl(
					dataOwner,
					accessControlKeys,
				)
			}
		}

		override val accessControlKeysHashes: Set<Sha256HexString> by lazy {
			accessControlKeys.map { hashAccessControlKey(it) }.toSet()
		}
	}

	private class HcpDataOwnerDetails(
		override val id: String,
		private val parentIds: Set<String>,
		private val ancestorIds: Set<String>,
	) : DataOwnerAuthenticationDetails.DataOwnerDetails {
		override val type: DataOwnerType get() = DataOwnerType.HCP

		override suspend fun ancestorIds(): Set<String> = ancestorIds

		override suspend fun parentIds(): Set<String> = parentIds
	}

	private class PatientDataOwnerDetails(
		override val id: String,
	) : DataOwnerAuthenticationDetails.DataOwnerDetails {
		override val type: DataOwnerType get() = DataOwnerType.PATIENT

		override suspend fun ancestorIds(): Set<String> = emptySet()

		override suspend fun parentIds(): Set<String> = emptySet()
	}

	private class DeviceDataOwnerDetails(
		override val id: String,
	) : DataOwnerAuthenticationDetails.DataOwnerDetails {
		override val type: DataOwnerType get() = DataOwnerType.DEVICE

		override suspend fun ancestorIds(): Set<String> = emptySet()

		override suspend fun parentIds(): Set<String> = emptySet()
	}
}
