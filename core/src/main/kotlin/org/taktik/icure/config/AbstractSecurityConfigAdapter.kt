package org.taktik.icure.config

import kotlinx.coroutines.reactor.mono
import org.springframework.security.core.Authentication
import org.springframework.security.core.context.SecurityContext
import org.springframework.security.web.server.authentication.ServerHttpBasicAuthenticationConverter
import org.springframework.security.web.server.context.WebSessionServerSecurityContextRepository
import org.springframework.web.server.ServerWebExchange
import org.taktik.icure.exceptions.UnauthorizedRequestException
import org.taktik.icure.security.SecurityToken
import org.taktik.icure.security.jwt.EncodedJwtAuthenticationToken
import org.taktik.icure.spring.asynccache.Cache
import reactor.core.publisher.Mono

abstract class AbstractSecurityConfigAdapter {
	protected abstract val cache: Cache<String, SecurityToken>
	protected abstract val sessionEnabled: Boolean

	protected val sessionLessSecurityContextRepository =
		object : WebSessionServerSecurityContextRepository() {
			override fun save(
				exchange: ServerWebExchange,
				context: SecurityContext,
			) = exchange.request.headers["X-Bypass-Session"]?.let { Mono.empty() } ?: super.save(exchange, context)

			override fun load(exchange: ServerWebExchange) = exchange.request.headers["X-Bypass-Session"]?.let { Mono.empty() } ?: super.load(exchange)
		}

	protected val multiTokenAuthConverter: (ServerWebExchange) -> Mono<Authentication> = { exchange ->
		// First I check for the JWT Header
		(
			exchange.authorizationBearerToken
				?: exchange.webSocketBearerToken
				?: exchange.request.queryParams["jwt"]?.firstOrNull()
			)?.let {
			Mono.just(EncodedJwtAuthenticationToken(encodedJwt = it))
		} ?: getAuthenticationFromPathOneTimeToken(exchange) ?: if (sessionEnabled) {
			exchange.session.flatMap { webSession ->
				// Otherwise, I check the session
				ServerHttpBasicAuthenticationConverter().convert(exchange).flatMap { auth ->
					// Ignore basic auth if SPRING_SECURITY_CONTEXT was loaded from session
					webSession.attributes[WebSessionServerSecurityContextRepository.DEFAULT_SPRING_SECURITY_CONTEXT_ATTR_NAME]?.let {
						(it as? SecurityContext)?.let { context ->
							if (context.authentication.principal != auth.principal) {
								Mono.just(auth)
							} else {
								Mono.empty()
							}
						}
					} ?: Mono.just(auth)
				}
			}
		} else {
			Mono.empty()
		}
	}

	private val ServerWebExchange.authorizationBearerToken: String?
		get() =
			request.headers["Authorization"]?.firstNotNullOfOrNull { h ->
				h?.takeIf { it.contains("Bearer") }?.replace("Bearer ", "")
			}

	private val ServerWebExchange.webSocketBearerToken: String?
		get() =
			request.headers["Sec-WebSocket-Protocol"]?.firstNotNullOfOrNull { secHeader ->
				secHeader?.split(",")?.firstNotNullOfOrNull { h ->
					h.takeIf { it.contains("Bearer") }?.replace("Bearer", "")
				}
			}

	private fun getAuthenticationFromPathOneTimeToken(exchange: ServerWebExchange): Mono<Authentication>? {
		val path = exchange.request.path.toString()
		return if (path.contains(';')) {
			val token = path.split(';')[1]
			token.let {
				if (it.contains('=')) {
					val (key, value) = it.split('=')
					if (key == "tokenid") {
						mono {
							cache.get(value)?.let { restriction ->
								cache.evict(value)
								if (exchange.request.method == restriction.method && path.startsWith(restriction.path)) {
									restriction.authentication
								} else {
									null
								}
							} ?: throw UnauthorizedRequestException("Invalid token")
						}
					} else {
						null
					}
				} else {
					null
				}
			}
		} else {
			null
		}
	}
}
