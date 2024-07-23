package org.taktik.icure.security

import com.icure.kotp.Totp
import com.icure.kryptom.crypto.HmacAlgorithm
import io.jsonwebtoken.ExpiredJwtException
import io.jsonwebtoken.MalformedJwtException
import io.jsonwebtoken.security.SecurityException
import kotlinx.coroutines.reactive.awaitFirstOrNull
import kotlinx.coroutines.reactor.mono
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.Authentication
import org.springframework.security.core.GrantedAuthority
import org.springframework.security.crypto.password.PasswordEncoder
import org.taktik.icure.asyncdao.HealthcarePartyDAO
import org.taktik.icure.asynclogic.datastore.IDatastoreInformation
import org.taktik.icure.entities.HealthcareParty
import org.taktik.icure.entities.base.BaseUser
import org.taktik.icure.entities.embed.AuthenticationClass
import org.taktik.icure.exceptions.IllegalEntityException
import org.taktik.icure.exceptions.InvalidJwtException
import org.taktik.icure.exceptions.MissingRequirementsException
import org.taktik.icure.security.jwt.JwtAuthenticationToken
import org.taktik.icure.security.jwt.JwtDetails
import org.taktik.icure.security.jwt.JwtRefreshDetails
import org.taktik.icure.security.jwt.JwtUtils
import org.taktik.icure.utils.info
import reactor.core.publisher.Mono

abstract class AbstractAuthenticationManager <
    out JWT : JwtDetails,
    out R : JwtRefreshDetails
>(
    protected val healthcarePartyDAO: HealthcarePartyDAO,
    private val passwordEncoder: PasswordEncoder,
    protected val jwtUtils: JwtUtils
) : CustomReactiveAuthenticationManager {
    protected val log: Logger = LoggerFactory.getLogger(javaClass)

    companion object {
        val TOKEN_REGEX = Regex("^[0-9]{6}[0-9]*$")

        sealed interface PasswordValidationStatus {
            fun isSuccess(): Boolean = false

            data class Success(val authenticationClass: AuthenticationClass) : PasswordValidationStatus {
                override fun isSuccess() = true
            }
            data object Failure : PasswordValidationStatus
            data object Missing2fa : PasswordValidationStatus
            data object Failed2fa : PasswordValidationStatus
        }

        interface UserAccumulator

        private data class ContainsTokenResult(
            val containsToken: Boolean,
            val isShortLivedToken: Boolean
        )
    }

    protected abstract fun encodedJwtToAuthentication(encodedJwt: String): JwtAuthenticationToken<JWT>

    /**
     * Refresh the JWT for user. Requires. Should throw an exception if the user is not active anymore or if the
     * refresh token was invalidated, unless the bypassRefreshValidityCheck parameter is set to true. This should be
     * done only when refreshing a JWT for a third party, after verifying the identity of the user who is asking for
     * this operation.
     * @param encodedRefreshToken the JwtRefreshDetails to create a new authentication JWT.
     * @param bypassRefreshValidityCheck whether to bypass the validity check for the refresh token.
     * @param totpToken if the refresh details have authentication class `password` or `2fa` the
     * totp token can be used to make the newly generated token valid for elevated security operations.
     * @return some JwtDetails.
     */
    abstract suspend fun regenerateJwtDetails(encodedRefreshToken: String, bypassRefreshValidityCheck: Boolean = false, totpToken: String? = null): JWT

    /**
     * Throws an exception if the username and password provided are unable to provide a valid token if passed to authentication methods.
     *
     * The purpose of the method is to validate if a user can still login after the addition of the 2FA to their user account.
     */
    abstract suspend fun checkAuthentication(fullGroupAndId: String, password: String)
    override fun authenticate(authentication: Authentication?): Mono<Authentication> = mono {
        try {
            // TODO: the "if" will be removed when the SESSION will be dismissed
            if (authentication is UsernamePasswordAuthenticationToken) {
                authenticateWithUsernameAndPassword(authentication, null).map {
                    JwtAuthenticationToken(
                        claims = it.principal as JwtDetails,
                        authorities = (it.authorities as Collection<Any?>)
                            .fold(setOf<GrantedAuthority>()) { acc, x -> acc + (x as GrantedAuthority) }.toMutableSet(),
                        encodedJwt = jwtUtils.createJWT(it.principal as JwtDetails),
                        authenticated = true
                    )
                }.awaitFirstOrNull()
                    .also {
                        loadSecurityContext()?.map { ctx ->
                            ctx.authentication = it
                        }?.awaitFirstOrNull()
                    }
            } else {
                encodedJwtToAuthentication(authentication?.credentials as String)
                    .also {
                        loadSecurityContext()?.map { ctx ->
                            ctx.authentication = it
                        }?.awaitFirstOrNull()
                    }
            }
        } catch (e: Exception) {
            val message = e.message ?: "An error occurred while decoding Jwt"
            when(e) {
                is ExpiredJwtException -> throw InvalidJwtException(message)
                is SecurityException -> throw InvalidJwtException(message)
                is MalformedJwtException -> throw InvalidJwtException(message)
                else -> throw e
            }
        }
    }

    /**
     * Starting from a [HealthcareParty], retrieves the [HealthcareParty] HCP hierarchy. The first element of the resulting list is the topmost
     * [HealthcareParty] of the hierarchy, while the last is the direct parent of the [HealthcareParty]. This is for compatibility reason with the SDK.
     * @param childHcp the HCP to get the hierarchy for.
     * @param datastore the datastore information to get the HCPs.
     * @param hierarchy the current hierarchy.
     * @return a [List] of [HealthcareParty]
     */
    protected tailrec suspend fun getHcpHierarchy(childHcp: HealthcareParty, datastore: IDatastoreInformation, hierarchy: List<HealthcareParty> = emptyList()): List<HealthcareParty> =
        if(childHcp.parentId == null) hierarchy
        else {
            if (hierarchy.drop(1).any { it.id == childHcp.id }) throw IllegalEntityException("Circular reference in the hcp hierarchy detected. You have set yourself as an ancestor in the hierarchy. Time loop threatening space time fabric detected.")
            if (childHcp.parentId == childHcp.id) hierarchy.also {
                log.info { "Hcp ${childHcp.id} set themself as a parent" }
            } else {
                val parent = healthcarePartyDAO.get(datastore, childHcp.parentId!!)
                if (parent == null) hierarchy else getHcpHierarchy(parent, datastore, listOf(parent) + hierarchy)
            }
        }

    /**
     * Checks if a password is valid, the password can contain the verification code of the 2FA following this format `password|123456`.
     *
     * @returns [PasswordValidationStatus] depending of the status of the validation:
     * - [PasswordValidationStatus.Success]: Everything is checked and good
     * - [PasswordValidationStatus.Failure]: Password isn't correct
     * - [PasswordValidationStatus.Missing2fa]: Password validated, but 2FA verification code is missing
     * - [PasswordValidationStatus.Failed2fa]: Password validated, but 2FA verification code is wrong
     */
    protected suspend fun isPasswordValid(u: BaseUser, password: String): PasswordValidationStatus {
        val containsTokenResult = doesUserContainsToken(u, appToken = password)
        if (containsTokenResult.containsToken) return PasswordValidationStatus.Success(
            if (containsTokenResult.isShortLivedToken)
                AuthenticationClass.SHORT_LIVED_TOKEN
            else
                AuthenticationClass.LONG_LIVED_TOKEN
        )
        return when {
            u.passwordHash == null -> PasswordValidationStatus.Failure
            u.use2fa == true && u.secret?.isNotBlank() == true -> {
                /*
                 * Rarely the user password may end with |[0-9]{6,} which is NOT part of the 2fa code. In addition to
                 * checking the stripped password we should also check the pw in full.
                 * Risks of this approach: assume an attacker tries to log in with password x|123456:
                 * - If the attacker gets an error associated to Failed2fa he knows the password is x (and the current
                 * totp is not 123456)
                 * - If the attacker gets an error associated to Missing2fa he knows the password is x|123456
                 * - If the attacker gets an error associated to Failure he knows the password is neither x nor x|123456
                 * Meaning the attacker is able to try 2 passwords at a time if one of them ends with |[0-9]{6,}. This
                 * is not a significant issue.
                 */
                val strippedPw = strip2fa(password)
                if (strippedPw != null && passwordEncoder.matches(strippedPw, u.passwordHash)) {
                    val (expectedLength, secret) = u.secret?.split(":", limit = 2)?.let { (lenAsStr, s) ->
                        lenAsStr.toIntOrNull()?.let { it to s }
                    } ?: throw MissingRequirementsException("Invalid configuration of 2FA token length and secret in the user.")
                    val verificationCode = password.split("|").last()
                    if (Totp(secret, algorithm = HmacAlgorithm.HmacSha256).verify(verificationCode, length = expectedLength)) {
                        return PasswordValidationStatus.Success(AuthenticationClass.TWO_FACTOR_AUTHENTICATION)
                    }
                    return PasswordValidationStatus.Failed2fa
                }
                if (passwordEncoder.matches(password, u.passwordHash)) {
                    return PasswordValidationStatus.Missing2fa
                }
                return PasswordValidationStatus.Failure
            }
            else -> {
                passwordEncoder
                    .matches(password, u.passwordHash).takeIf { it }
                    ?.let { PasswordValidationStatus.Success(AuthenticationClass.PASSWORD) }
                    ?: PasswordValidationStatus.Failure
            }
        }
    }

    private fun doesUserContainsToken(
        u: BaseUser, appToken: String
    ) = if (u.applicationTokens?.containsValue(appToken) == true) {
        ContainsTokenResult(containsToken = true, isShortLivedToken = false)
    } else {
        u.authenticationTokens.values.asSequence()
            .filter { authToken -> !authToken.isExpired() && authToken.deletionDate == null }
            .firstOrNull { authToken -> passwordEncoder.matches(appToken, authToken.token) }
            ?.let { authToken -> ContainsTokenResult(true, authToken.isShortLived) }
            ?: ContainsTokenResult(containsToken = false, isShortLivedToken = false)
    }

    // returns null if the password does not end with |potentialToken
    private fun strip2fa(password: String) = password
        .split("|").toTypedArray().toList()
        .takeIf { it.size > 1 && it.last().length >= 6 && it.last().toLongOrNull() != null }
        ?.dropLast(1)
        ?.joinToString("|")

}
