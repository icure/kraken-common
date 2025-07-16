@file:OptIn(ExperimentalCoroutinesApi::class)

package org.taktik.icure.security

import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.slf4j.Logger
import org.slf4j.Marker
import org.springframework.beans.factory.annotation.Autowired


interface LogMarker {
	suspend fun getMarker(): Marker?
}

@Autowired
private var logMarker : LogMarker? = null

suspend fun Logger.trace(msg: suspend () -> String) {
	if (this.isTraceEnabled) {
		logMarker?.getMarker()?.let { marker ->
			this.trace(marker, msg())
		} ?: this.trace(msg())
	}
}

suspend fun Logger.debug(msg: suspend () -> String) {
	if (this.isDebugEnabled) {
		logMarker?.getMarker()?.let { marker ->
			this.debug(marker, msg())
		} ?: this.debug(msg())
	}
}

suspend fun Logger.info(msg: suspend () -> String) {
	if (this.isInfoEnabled) {
		logMarker?.getMarker()?.let { marker ->
			this.info(marker, msg())
		} ?: this.info(msg())
	}
}

suspend fun Logger.warn(msg: suspend () -> String) {
	if (this.isWarnEnabled) {
		logMarker?.getMarker()?.let { marker ->
			this.warn(marker, msg())
		} ?: this.warn(msg())
	}
}

suspend fun Logger.error(msg: suspend () -> String) {
	if (this.isErrorEnabled) {
		logMarker?.getMarker()?.let { marker ->
			this.error(marker, msg())
		} ?: this.error(msg())
	}
}

suspend fun Logger.info(e: Throwable, msg: suspend () -> String?) {
	if (this.isInfoEnabled) {
		logMarker?.getMarker()?.let { marker ->
			this.info(marker, msg() ?: e.message ?: e.localizedMessage, e)
		} ?: this.info(msg(), e)
	}
}

suspend fun Logger.warn(e: Throwable, msg: suspend () -> String?) {
	if (this.isWarnEnabled) {
		logMarker?.getMarker()?.let { marker ->
			this.warn(marker, msg() ?: e.message ?: e.localizedMessage, e)
		} ?: this.warn(msg(), e)
	}
}

suspend fun Logger.error(e: Throwable, msg: suspend () -> String?) {
	if (this.isErrorEnabled) {
		logMarker?.getMarker()?.let { marker ->
			this.error(marker, msg() ?: e.message ?: e.localizedMessage, e)
		} ?: this.error(msg(), e)
	}
}
