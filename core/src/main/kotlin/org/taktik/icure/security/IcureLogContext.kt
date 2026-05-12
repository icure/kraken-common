package org.taktik.icure.security

/**
 * Per-request log correlation triple carried in the Reactor Context.
 *
 * Populated at the WebFilter boundary by the cloud-side `HyperionRequestContextFilter`,
 * read by the suspend log extensions in `LoggerUtils.kt` to decorate each event with
 * matching SLF4J 2.0 key-value pairs. Plain data — not a `CoroutineContext.Element`;
 * propagation rides on the existing `coroutineContext[ReactorContext]` channel that
 * the codebase already uses for `SecurityContext`.
 */
data class IcureLogContext(
	val requestId: String?,
	val groupId: String?,
	val userId: String?,
) {
	companion object {
		val Key: Class<IcureLogContext> = IcureLogContext::class.java
	}
}
