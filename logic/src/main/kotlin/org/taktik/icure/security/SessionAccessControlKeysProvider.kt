package org.taktik.icure.security

import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.reactor.ReactorContext
import kotlinx.coroutines.withContext
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Service
import org.springframework.web.server.ServerWebExchange
import org.taktik.icure.security.SessionAccessControlKeysProvider.Companion.ACCESS_CONTROL_KEY_LENGTH_BYTES
import java.util.Base64
import kotlin.coroutines.AbstractCoroutineContextElement
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.coroutineContext

interface SessionAccessControlKeysProvider {
	companion object {
		const val ACCESS_CONTROL_KEY_LENGTH_BYTES = 16
	}

	suspend fun getAccessControlKeys(): List<ByteArray>

	/**
	 * Gives a coroutine context element which stores the provided keys so that they can be retrieved later by this
	 * [SessionAccessControlKeysProvider]. Note that injected keys override any previously existing keys.
	 * @throws IllegalArgumentException if [keys] is empty or if any of the provided [keys] are not of length
	 * [ACCESS_CONTROL_KEY_LENGTH_BYTES]
	 */
	fun contextWithInjectedKeys(keys: List<ByteArray>): CoroutineContext.Element
}

@Service
@Profile("app")
class SessionAccessControlKeysProviderImpl : SessionAccessControlKeysProvider {
	companion object {
		const val ACCESS_CONTROL_KEYS_HEADER = "Icure-Access-Control-Keys"
	}

	private class AccessControlKeysContext(
		val accessControlKeys: List<ByteArray>,
	) : AbstractCoroutineContextElement(Key) {
		object Key : CoroutineContext.Key<AccessControlKeysContext>

		init {
			require(accessControlKeys.isNotEmpty()) { "Access control keys should not be empty" }
			require(accessControlKeys.all { it.size == ACCESS_CONTROL_KEY_LENGTH_BYTES }) {
				"All access control keys should be $ACCESS_CONTROL_KEY_LENGTH_BYTES long"
			}
		}
	}

	override suspend fun getAccessControlKeys(): List<ByteArray> = getInjectedAccessControlKeys() ?: getAccessControlKeysFromWebExchange() ?: emptyList()

	override fun contextWithInjectedKeys(keys: List<ByteArray>): CoroutineContext.Element = AccessControlKeysContext(keys)

	private suspend fun getInjectedAccessControlKeys(): List<ByteArray>? = coroutineContext[AccessControlKeysContext.Key]?.accessControlKeys

	private suspend fun getAccessControlKeysFromWebExchange(): List<ByteArray>? = coroutineContext[ReactorContext]
		?.context
		?.getOrEmpty<ServerWebExchange>(ServerWebExchange::class.java)
		?.orElse(null)
		?.request
		?.headers
		?.get(ACCESS_CONTROL_KEYS_HEADER)
		?.let { values ->
			values.flatMap { value ->
				val valueBytes =
					try {
						Base64.getDecoder().decode(value)
					} catch (e: IllegalArgumentException) {
						throw IllegalArgumentException("Access control keys from headers should be encoded in Base64", e)
					}
				require(valueBytes.size % ACCESS_CONTROL_KEY_LENGTH_BYTES == 0) {
					"Access control keys should be ${valueBytes.size} long and concatenated without any separator."
				}
				valueBytes.toList().chunked(ACCESS_CONTROL_KEY_LENGTH_BYTES) { it.toByteArray() }
			}
		}
}
