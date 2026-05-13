@file:OptIn(ExperimentalCoroutinesApi::class)

package org.taktik.icure.security

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.reactor.ReactorContext
import org.slf4j.Logger
import org.slf4j.spi.LoggingEventBuilder

/**
 * Suspend logger extensions that read [IcureLogContext] from the current Reactor Context
 * (via the `ReactorContext` element in `coroutineContext`) and decorate each event with
 * `requestId` / `groupId` / `userId` as SLF4J 2.0 key-value pairs.
 *
 * Single propagation channel: Reactor Context, the same mechanism the codebase already
 * uses for `SecurityContext` (see `loadSecurityContext()` in this package). No MDC, no
 * automatic-propagation hooks, no thread-local accessors.
 *
 * When no [IcureLogContext] is present (background tasks, scripts, `GlobalScope.launch`,
 * unauthenticated request paths), events emit without correlation — the kraken-cloud
 * Hyperion appender then drops them as untagged.
 */

suspend fun Logger.trace(msg: suspend () -> String) {
	if (isTraceEnabled) attachContext(atTrace()).log(msg())
}

suspend fun Logger.debug(msg: suspend () -> String) {
	if (isDebugEnabled) attachContext(atDebug()).log(msg())
}

suspend fun Logger.info(msg: suspend () -> String) {
	if (isInfoEnabled) attachContext(atInfo()).log(msg())
}

suspend fun Logger.warn(msg: suspend () -> String) {
	if (isWarnEnabled) attachContext(atWarn()).log(msg())
}

suspend fun Logger.error(msg: suspend () -> String) {
	if (isErrorEnabled) attachContext(atError()).log(msg())
}

suspend fun Logger.info(e: Throwable, msg: suspend () -> String?) {
	if (isInfoEnabled) attachContext(atInfo()).setCause(e).log(msg() ?: e.message ?: e.localizedMessage)
}

suspend fun Logger.warn(e: Throwable, msg: suspend () -> String?) {
	if (isWarnEnabled) attachContext(atWarn()).setCause(e).log(msg() ?: e.message ?: e.localizedMessage)
}

suspend fun Logger.error(e: Throwable, msg: suspend () -> String?) {
	if (isErrorEnabled) attachContext(atError()).setCause(e).log(msg() ?: e.message ?: e.localizedMessage)
}

private suspend fun attachContext(builder: LoggingEventBuilder): LoggingEventBuilder {
	val reactor = currentCoroutineContext()[ReactorContext]?.context ?: return builder
	val ctx = reactor.getOrDefault<IcureLogContext>(IcureLogContext.Key, null) ?: return builder
	ctx.requestId?.let { builder.addKeyValue(KEY_REQUEST_ID, it) }
	ctx.groupId?.let { builder.addKeyValue(KEY_GROUP_ID, it) }
	ctx.userId?.let { builder.addKeyValue(KEY_USER_ID, it) }
	ctx.targetGroupId?.let { builder.addKeyValue(KEY_TARGET_GROUP_ID, it) }
	return builder
}

const val KEY_REQUEST_ID = "requestId"
const val KEY_GROUP_ID = "groupId"
const val KEY_USER_ID = "userId"
const val KEY_TARGET_GROUP_ID = "targetGroupId"
