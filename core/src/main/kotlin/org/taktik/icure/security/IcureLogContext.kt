package org.taktik.icure.security

/**
 * Per-request log correlation triple carried in:
 * - the Reactor Context (keyed by [Key]) — for suspend log extensions in `LoggerUtils.kt`
 *   that read `currentCoroutineContext()[ReactorContext]?.context?.getOrDefault(Key, …)`;
 * - the `ServerWebExchange.attributes` map (keyed by [EXCHANGE_ATTRIBUTE]) — for
 *   synchronous Reactor-callback sites that have access to the exchange but not to the
 *   coroutine context (notably `GlobalErrorHandler`, which runs as an
 *   `ErrorWebExceptionHandler` after the filter chain unwinds and emits the bulk of the
 *   error/warn logs in production).
 *
 * Both channels are populated by the cloud-side `HyperionRequestContextFilter` at the
 * top of the request and carry identical values.
 *
 * Plain data — not a `CoroutineContext.Element`. Propagation rides on the existing
 * Reactor Context channel that the codebase already uses for `SecurityContext`.
 */
data class IcureLogContext(
	val requestId: String?,
	val groupId: String?,
	val userId: String?,
) {
	companion object {
		val Key: Class<IcureLogContext> = IcureLogContext::class.java
		const val EXCHANGE_ATTRIBUTE: String = "org.taktik.icure.security.IcureLogContext"
	}
}
