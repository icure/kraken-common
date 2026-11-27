package org.taktik.icure.security

import com.icure.kotp.ShaVersion
import com.icure.kotp.Totp
import kotlinx.coroutines.reactive.awaitFirstOrNull
import kotlinx.coroutines.reactor.mono
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.Authentication
import org.springframework.security.crypto.password.PasswordEncoder
import org.taktik.icure.asyncdao.HealthcarePartyDAO
import org.taktik.icure.datastore.IDatastoreInformation
import org.taktik.icure.entities.HealthcareParty
import org.taktik.icure.entities.base.BaseUser
import org.taktik.icure.entities.embed.AuthenticationClass
import org.taktik.icure.exceptions.IllegalEntityException
import org.taktik.icure.exceptions.InvalidJwtException
import org.taktik.icure.exceptions.MissingRequirementsException
import org.taktik.icure.security.jwt.JwtAuthentication
import org.taktik.icure.security.jwt.JwtDetails
import org.taktik.icure.security.jwt.JwtRefreshDetails
import org.taktik.icure.security.jwt.JwtUtils
import reactor.core.publisher.Mono
import kotlin.time.ExperimentalTime

abstract class AbstractAuthenticationManager<
	out JWT : JwtDetails,
	out R : JwtRefreshDetails,
	>(
	protected val healthcarePartyDAO: HealthcarePartyDAO,
	private val passwordEncoder: PasswordEncoder,
	protected val jwtUtils: JwtUtils,
) : CustomReactiveAuthenticationManager {
	protected val log: Logger = LoggerFactory.getLogger(javaClass)

	companion object {
		val TOKEN_REGEX = Regex("^[0-9]{6}[0-9]*$")

		sealed interface PasswordValidationStatus {
			fun isSuccess(): Boolean = false

			data class Success(
				val authenticationClass: AuthenticationClass,
			) : PasswordValidationStatus {
				override fun isSuccess() = true
			}

			data object Failure : PasswordValidationStatus

			data object Missing2fa : PasswordValidationStatus

			data object Failed2fa : PasswordValidationStatus
		}

		interface UserAccumulator

		private data class ContainsTokenResult(
			val containsToken: Boolean,
			val isShortLivedToken: Boolean,
		)
	}

	protected abstract suspend fun encodedJwtToAuthentication(encodedJwt: String): JwtAuthentication

	/**
	 * Refresh the JWT for user. Requires. Should throw an exception if the user is not active anymore or if the
	 * refresh token was invalidated, unless the bypassRefreshValidityCheck parameter is set to true. This should be
	 * done only when refreshing a JWT for a third party, after verifying the identity of the user who is asking for
	 * this operation.
	 * @param encodedRefreshToken the JwtRefreshDetails to create a new authentication JWT.
	 * @param bypassRefreshValidityCheck whether to bypass the validity check for the refresh token.
	 * @param totpToken if the refresh details have authentication class `password` or `2fa` the
	 * totp token can be used to make the newly generated token valid for elevated security operations.
	 * @return the JwtDetails and duration in seconds.
	 */
	abstract suspend fun regenerateAuthJwt(
		encodedRefreshToken: String,
		bypassRefreshValidityCheck: Boolean = false,
		totpToken: String? = null,
	): Pair<JWT, Long?>

	override fun authenticate(authentication: Authentication?): Mono<Authentication> = mono {
		try {
			(
				if (authentication is UsernamePasswordAuthenticationToken) {
					authenticateWithUsernameAndPassword(
						authentication = authentication,
						groupId = null,
						applicationId = null,
						scopeDataOwner = null,
						cacheJwtRefreshDetails = false,
						requestedSchemaVersion = null,
					)
				} else {
					encodedJwtToAuthentication(authentication?.credentials as String)
				}
			).also { auth ->
				loadSecurityContext()
					?.awaitFirstOrNull()
					?.also { ctx -> ctx.authentication = auth }
			}
		} catch (e: Exception) {
			val message = e.message ?: "An error occurred while decoding Jwt"
			when (e) {
				is SecurityException -> throw InvalidJwtException(message)
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
	protected tailrec suspend fun getHcpHierarchy(
		childHcp: HealthcareParty,
		datastore: IDatastoreInformation,
		hierarchy: List<HealthcareParty> = emptyList(),
	): List<HealthcareParty> {
		val parentId = childHcp.parentId
		return if (parentId == null) {
			hierarchy
		} else {
			if (hierarchy.drop(1).any { it.id == childHcp.id }) {
				throw IllegalEntityException(
					"Circular reference in the hcp hierarchy starting from ${hierarchy.last()} detected.",
				)
			}
			if (parentId.isBlank()) {
				throw IllegalEntityException(
					"Blank parent id for healthcare party $childHcp",
				)
			}
			if (parentId == childHcp.id) {
				hierarchy
			} else {
				val parent = healthcarePartyDAO.get(datastore, parentId)
				if (parent == null) hierarchy else getHcpHierarchy(parent, datastore, listOf(parent) + hierarchy)
			}
		}
	}

	/**
	 * How the 2fa verification code is provided to [isPasswordValid].
	 */
	protected sealed interface VerificationCodeSource {
		/**
		 * Legacy: the verification code may be appended to the password following the format `password|123456`.
		 * Ambiguous if a user password itself ends with `|[0-9]{6,}`.
		 */
		data object InlineInPassword : VerificationCodeSource

		/**
		 * The password is used verbatim, and the verification code (if any) is provided separately.
		 * If [code] is null the password of a user with 2fa is still validated, but the result will be
		 * [PasswordValidationStatus.Missing2fa].
		 */
		data class Separate(val code: String?) : VerificationCodeSource
	}

	/**
	 * Checks if a password is valid.
	 *
	 * @param verificationCodeSource how the 2fa verification code is provided; by default it can be appended to the
	 * password (see [VerificationCodeSource.InlineInPassword]).
	 * @returns [PasswordValidationStatus] depending of the status of the validation:
	 * - [PasswordValidationStatus.Success]: Everything is checked and good
	 * - [PasswordValidationStatus.Failure]: Password isn't correct
	 * - [PasswordValidationStatus.Missing2fa]: Password validated, but 2FA verification code is missing
	 * - [PasswordValidationStatus.Failed2fa]: Password validated, but 2FA verification code is wrong
	 */
	protected suspend fun isPasswordValid(
		u: BaseUser,
		password: String,
		verificationCodeSource: VerificationCodeSource = VerificationCodeSource.InlineInPassword,
	): PasswordValidationStatus {
		val containsTokenResult = doesUserContainsToken(u, appToken = password)
		if (containsTokenResult.containsToken) {
			return PasswordValidationStatus.Success(
				if (containsTokenResult.isShortLivedToken) {
					AuthenticationClass.SHORT_LIVED_TOKEN
				} else {
					AuthenticationClass.LONG_LIVED_TOKEN
				},
			)
		}
		val uses2fa = u.use2fa == true && u.secret?.isNotBlank() == true
		return when {
			u.passwordHash == null -> PasswordValidationStatus.Failure
			uses2fa && verificationCodeSource is VerificationCodeSource.Separate -> when {
				!passwordEncoder.matches(password, u.passwordHash) -> PasswordValidationStatus.Failure
				verificationCodeSource.code == null -> PasswordValidationStatus.Missing2fa
				isVerificationCodeValid(u, verificationCodeSource.code) -> PasswordValidationStatus.Success(AuthenticationClass.TWO_FACTOR_AUTHENTICATION)
				else -> PasswordValidationStatus.Failed2fa
			}
			uses2fa -> {
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
				val strippedPwAndCode = splitInlineVerificationCode(password)
				if (strippedPwAndCode != null && passwordEncoder.matches(strippedPwAndCode.first, u.passwordHash)) {
					if (isVerificationCodeValid(u, strippedPwAndCode.second)) {
						PasswordValidationStatus.Success(AuthenticationClass.TWO_FACTOR_AUTHENTICATION)
					} else {
						PasswordValidationStatus.Failed2fa
					}
				} else if (passwordEncoder.matches(password, u.passwordHash)) {
					PasswordValidationStatus.Missing2fa
				} else {
					PasswordValidationStatus.Failure
				}
			}
			else -> {
				passwordEncoder
					.matches(password, u.passwordHash)
					.takeIf { it }
					?.let { PasswordValidationStatus.Success(AuthenticationClass.PASSWORD) }
					?: PasswordValidationStatus.Failure
			}
		}
	}

	@OptIn(ExperimentalTime::class)
	private suspend fun isVerificationCodeValid(u: BaseUser, verificationCode: String): Boolean {
		val parsedSecret = u.secret?.let { ParsedTotpSecret.tryParse(it) }
			?: throw MissingRequirementsException("Invalid configuration of 2FA token length and secret in the user.")
		return Totp(parsedSecret.secret, parsedSecret.algorithm).verify(verificationCode, expectedLength = parsedSecret.expectedCount)
	}

	private fun doesUserContainsToken(
		u: BaseUser,
		appToken: String,
	) = if (u.applicationTokens?.containsValue(appToken) == true) {
		ContainsTokenResult(containsToken = true, isShortLivedToken = false)
	} else {
		u.authenticationTokens.values
			.asSequence()
			.filter { authToken -> !authToken.isExpired() && authToken.deletionDate == null }
			.firstOrNull { authToken -> passwordEncoder.matches(appToken, authToken.token) }
			?.let { authToken -> ContainsTokenResult(true, authToken.isShortLived) }
			?: ContainsTokenResult(containsToken = false, isShortLivedToken = false)
	}

	// returns null if the password does not end with |potentialToken
	private fun splitInlineVerificationCode(password: String) = password
		.split("|")
		.toTypedArray()
		.toList()
		.takeIf { it.size > 1 && it.last().length >= 6 && it.last().toLongOrNull() != null }
		?.let {
			it.dropLast(1).joinToString("|") to it.last()
		}

	protected class ParsedTotpSecret(
		val expectedCount: Int,
		val secret: String,
		val algorithm: ShaVersion
	) {
		companion object {
			fun tryParse(secretString: String): ParsedTotpSecret? {
				val parts = secretString.split(":")
				if (parts.size != 3) return null
				val expectedCount = parts[1].toIntOrNull() ?: return null
				val secret = parts[2]
				val algorithm = kotlin.runCatching { ShaVersion.valueOf(parts[0]) }.getOrNull() ?: return null
				return ParsedTotpSecret(expectedCount, secret, algorithm)
			}
		}
	}
}
