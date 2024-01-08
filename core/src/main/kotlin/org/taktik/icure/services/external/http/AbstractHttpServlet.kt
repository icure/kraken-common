/*
 * Copyright (c) 2020. Taktik SA, All rights reserved.
 */
package org.taktik.icure.services.external.http

import java.io.IOException
import javax.servlet.Servlet
import javax.servlet.ServletConfig
import javax.servlet.ServletException
import javax.servlet.ServletRequest
import javax.servlet.ServletResponse
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse
import org.slf4j.LoggerFactory

abstract class AbstractHttpServlet : Servlet {
	protected val log = LoggerFactory.getLogger(javaClass)
	protected var theServletConfig: ServletConfig? = null

	@Throws(ServletException::class)
	override fun init(servletConfig: ServletConfig) {
		this.theServletConfig = servletConfig
	}

	override fun getServletConfig(): ServletConfig {
		return theServletConfig!!
	}

	override fun getServletInfo(): String? {
		return null
	}

	override fun destroy() {
		theServletConfig = null
	}

	@Throws(ServletException::class, IOException::class)
	override fun service(servletRequest: ServletRequest, servletResponse: ServletResponse) {
		if (servletRequest is HttpServletRequest && servletResponse is HttpServletResponse) {
			try {
				handleRequest(servletRequest, servletResponse)
			} catch (se: ServletException) {
				throw se
			} catch (se: IOException) {
				throw se
			} catch (e: Exception) {
				throw ServletException(e)
			}
		}
	}

	@Throws(Exception::class)
	protected abstract fun handleRequest(httpRequest: HttpServletRequest?, httpResponse: HttpServletResponse?)
}
