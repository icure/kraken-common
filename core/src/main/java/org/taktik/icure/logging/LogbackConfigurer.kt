/*
 * Copyright (c) 2020. Taktik SA, All rights reserved.
 */
package org.taktik.icure.logging

import ch.qos.logback.classic.LoggerContext
import ch.qos.logback.classic.joran.JoranConfigurator
import ch.qos.logback.core.joran.spi.JoranException
import ch.qos.logback.core.util.StatusPrinter
import org.slf4j.LoggerFactory
import java.net.URL

object LogbackConfigurer {
	fun initLogging(configUrl: URL?) {
		val context = LoggerFactory.getILoggerFactory() as LoggerContext
		try {
			val configurator = JoranConfigurator()
			configurator.context = context
			// Call context.reset() to clear any previous configuration, e.g. default
			// configuration. For multi-step configuration, omit calling context.reset().
			context.reset()
			configurator.doConfigure(configUrl)
		} catch (je: JoranException) {
			// StatusPrinter will handle this
		}
		StatusPrinter.printIfErrorsOccured(context)
	}

	fun stopLogging() {
		// Stop the logback context
		val loggerContext = LoggerFactory.getILoggerFactory() as LoggerContext
		loggerContext.stop()
	}
}
