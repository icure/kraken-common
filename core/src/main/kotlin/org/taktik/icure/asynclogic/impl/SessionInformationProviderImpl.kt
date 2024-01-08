/*
 * Copyright (c) 2020. Taktik SA, All rights reserved.
 */

package org.taktik.icure.asynclogic.impl

import kotlinx.coroutines.reactive.awaitFirstOrNull
import kotlinx.coroutines.reactor.ReactorContext
import org.springframework.context.annotation.Profile
import org.springframework.security.authentication.AuthenticationServiceException
import org.springframework.security.core.Authentication
import org.springframework.stereotype.Service
import org.springframework.web.server.ServerWebExchange
import org.taktik.icure.asynclogic.SessionInformationProvider
import org.taktik.icure.entities.base.Encryptable
import org.taktik.icure.entities.base.hasDataOwnerOrDelegationKey
import org.taktik.icure.entities.utils.Sha256HexString
import org.taktik.icure.entities.DataOwnerType
import org.taktik.icure.security.DataOwnerAuthenticationDetails
import org.taktik.icure.security.SessionAccessControlKeysProvider
import org.taktik.icure.security.UserDetails
import org.taktik.icure.security.hashAccessControlKey
import org.taktik.icure.security.jwt.JwtDetails
import org.taktik.icure.security.loadSecurityContext
import java.io.Serializable
import kotlin.coroutines.coroutineContext

open class SessionInformationProviderImpl(
    private val sessionAccessControlKeysProvider: SessionAccessControlKeysProvider
) : SessionInformationProvider {
    override suspend fun getCurrentSessionContext(): SessionInformationProvider.AsyncSessionContext =
        getCurrentAuthentication()?.let { SessionContextImpl(it) }
            ?: throw AuthenticationServiceException("getCurrentAuthentication() returned null, no SecurityContext in the coroutine context?")
    override suspend fun getCurrentUserId(): String = getCurrentSessionContext().getUserId()
    override suspend fun getCurrentHealthcarePartyId(): String {
        return getCurrentSessionContext().getHealthcarePartyId() ?: throw AuthenticationServiceException("Invalid user")
    }

    override suspend fun getCurrentDataOwnerId(): String {
        return getCurrentSessionContext().let {
            it.getHealthcarePartyId() ?: it.getPatientId() ?: it.getDeviceId()
        } ?: throw AuthenticationServiceException("Failed to extract current data owner id")
    }

    override suspend fun getSearchKeyMatcher(): (String, Encryptable) -> Boolean {
        val authenticationDetails = getDataOwnerAuthenticationDetails()
        return { hcpId, item ->
            if (hcpId == authenticationDetails.dataOwner?.id)
                item.hasDataOwnerOrDelegationKey(hcpId) || authenticationDetails.accessControlKeysHashes.any{ item.hasDataOwnerOrDelegationKey(it) }
            else
                item.hasDataOwnerOrDelegationKey(hcpId)
        }
    }

    override suspend fun getDataOwnerAuthenticationDetails(): DataOwnerAuthenticationDetails {
        return DataOwnerAuthenticationDetailsImpl(
            getCurrentSessionContext().let { sc ->
                sc.getHealthcarePartyId()?.let { HcpDataOwnerDetails.fromHierarchy(it, sc.getHcpHierarchy()) }
                    ?: sc.getPatientId()?.let { PatientDataOwnerDetails(it) }
                    ?: sc.getDeviceId()?.let { DeviceDataOwnerDetails(it) }
            },
            sessionAccessControlKeysProvider.getAccessControlKeys()
        )
    }

    override suspend fun requestsAutofixAnonymity(): Boolean {
        val default = when (getCurrentSessionContext().getDataOwnerType()) {
            DataOwnerType.HCP -> false
            DataOwnerType.DEVICE -> true
            DataOwnerType.PATIENT -> true
            null -> throw AuthenticationServiceException("User is not a data owner.")
        }
        val requestAnonHeader = coroutineContext[ReactorContext]
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

    protected open class SessionContextImpl(protected val _authentication: Authentication) :
        SessionInformationProvider.AsyncSessionContext, Serializable {
        protected var _userDetails: UserDetails = extractUserDetails(_authentication)

        override fun getUserId(): String = (_userDetails as JwtDetails).userId
        override fun getPatientId(): String? {
            return if ((_userDetails as JwtDetails).dataOwnerType == DataOwnerType.PATIENT) (_userDetails as JwtDetails).dataOwnerId
            else null
        }

        override fun getHealthcarePartyId(): String? {
            return if ((_userDetails as JwtDetails).dataOwnerType == DataOwnerType.HCP) (_userDetails as JwtDetails).dataOwnerId
            else null
        }

        override fun getDeviceId(): String? {
            return if ((_userDetails as JwtDetails).dataOwnerType == DataOwnerType.DEVICE) (_userDetails as JwtDetails).dataOwnerId
            else null
        }
        override fun getGlobalUserId(): String = getUserId()
        override fun getHcpHierarchy(): List<String> = (_userDetails as JwtDetails).hcpHierarchy
        override fun getDataOwnerType(): DataOwnerType? = (_userDetails as JwtDetails).dataOwnerType
    }

    companion object {
        /**
         * Header that can be used to override the default value of request anonymity for the request. This prevents
         * the user id / data owner id to be automatically inserted in clear in the data created / modified by the data
         * owner.
         */
        const val REQUEST_AUTOFIX_ANONYMITY_HEADER = "Icure-Request-Autofix-Anonymity"

        private suspend fun getCurrentAuthentication() =
            loadSecurityContext()?.map { it.authentication }?.awaitFirstOrNull()

        private suspend fun invalidateCurrentAuthentication() {
            loadSecurityContext()?.map { it.authentication.isAuthenticated = false }?.awaitFirstOrNull()
                ?: throw AuthenticationServiceException("Could not find authentication object in ReactorContext")
        }

        private fun extractUserDetails(authentication: Authentication): UserDetails {
            return authentication.principal?.let { it as? UserDetails }
                ?: throw AuthenticationServiceException("Failed extracting user details: ${authentication.principal}")
        }
    }

    private class DataOwnerAuthenticationDetailsImpl(
        override val dataOwner: DataOwnerAuthenticationDetails.DataOwnerDetails?,
        override val accessControlKeys: List<ByteArray>
    ): DataOwnerAuthenticationDetails {
        init {
            if (dataOwner == null && accessControlKeys.isEmpty()) throw AuthenticationServiceException(
                "Anonymous data owner must provide at least some secret exchange ids."
            )
        }

        override val accessControlKeysHashes: Set<Sha256HexString> by lazy {
            accessControlKeys.map { hashAccessControlKey(it) }.toSet()
        }
    }

    private class HcpDataOwnerDetails(
        override val id: String,
        private val parent: DataOwnerAuthenticationDetails.DataOwnerDetails?
    ): DataOwnerAuthenticationDetails.DataOwnerDetails {
        companion object {
            fun fromHierarchy(self: String, hierarchy: List<String>): DataOwnerAuthenticationDetails.DataOwnerDetails =
                if (hierarchy.isEmpty())
                    HcpDataOwnerDetails(self, null)
                else
                    HcpDataOwnerDetails(self, fromHierarchy(hierarchy.last(), hierarchy.dropLast(1)))
        }

        override val type: DataOwnerType get() = DataOwnerType.HCP

        override suspend fun parent(): DataOwnerAuthenticationDetails.DataOwnerDetails? = parent
    }

    private class PatientDataOwnerDetails(
        override val id: String
    ): DataOwnerAuthenticationDetails.DataOwnerDetails {
        override val type: DataOwnerType get() = DataOwnerType.PATIENT

        override suspend fun parent(): DataOwnerAuthenticationDetails.DataOwnerDetails? = null
    }

    private class DeviceDataOwnerDetails(
        override val id: String
    ): DataOwnerAuthenticationDetails.DataOwnerDetails {
        override val type: DataOwnerType get() = DataOwnerType.DEVICE

        override suspend fun parent(): DataOwnerAuthenticationDetails.DataOwnerDetails? = null
    }
}
