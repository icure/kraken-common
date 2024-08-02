/*
 * Copyright (c) 2020. Taktik SA, All rights reserved.
 */
@file:Suppress("BlockingMethodInNonBlockingContext")

package org.taktik.icure.services.external.http

import com.fasterxml.jackson.databind.ObjectMapper
import kotlinx.coroutines.reactive.awaitFirstOrNull
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.server.PathContainer
import org.springframework.security.authentication.AuthenticationServiceException
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.reactive.socket.CloseStatus
import org.springframework.web.reactive.socket.WebSocketHandler
import org.springframework.web.reactive.socket.WebSocketMessage
import org.springframework.web.reactive.socket.WebSocketSession
import org.springframework.web.util.pattern.PathPattern
import org.springframework.web.util.pattern.PathPatternParser
import org.taktik.icure.asynclogic.SessionInformationProvider
import org.taktik.icure.services.external.http.websocket.WebSocketInvocation
import org.taktik.icure.services.external.http.websocket.annotation.WSMessage
import org.taktik.icure.services.external.http.websocket.annotation.WSOperation
import org.taktik.icure.services.external.http.websocket.annotation.WSOperationFactory
import org.taktik.icure.services.external.http.websocket.annotation.WSParam
import org.taktik.icure.services.external.http.websocket.annotation.WSRequestMapping
import org.taktik.icure.services.external.http.websocket.factory.DefaultWebSocketOperationFactoryImpl
import org.taktik.icure.services.external.http.websocket.operation.Operation
import org.taktik.icure.services.external.http.websocket.operation.WebSocketOperationFactory
import reactor.core.publisher.Mono
import java.lang.reflect.Method
import java.lang.reflect.Parameter

abstract class WebSocketOperationHandler(
    wsControllers: List<WsController>,
    val objectMapper: ObjectMapper,
    val sessionLogic: SessionInformationProvider,
    private val operationFactories: List<WebSocketOperationFactory>,
    private val defaultFactory: DefaultWebSocketOperationFactoryImpl,
) : WebSocketHandler {

    companion object {
        /**
         * The status code for a custom close status.
         * https://developer.mozilla.org/en-US/docs/Web/API/CloseEvent/code
         */
        const val CUSTOM_CLOSE_STATUS_CODE = 4000
    }

    private val log = LoggerFactory.getLogger(this.javaClass)
    val ppp: PathPatternParser = PathPatternParser.defaultInstance
    val methods: Map<String, Pair<PathPattern, WebSocketInvocation>> =
        wsControllers.fold(mapOf()) { m, c -> m + scanBeanMethods(c) }

    fun scanBeanMethods(bean: WsController): Map<String, Pair<PathPattern, WebSocketInvocation>> {
        return bean.javaClass.getAnnotation(WSRequestMapping::class.java)?.let {
            if (it.name.isNotEmpty()) {
                val basePath: String = it.name
                bean.javaClass.methods.filter { m: Method ->
                    m.getAnnotation(WSOperation::class.java)?.let { _ ->
                        m.getAnnotation(WSRequestMapping::class.java)?.name?.isNotEmpty()
                    } == true
                }.fold(mutableMapOf()) { methods, m ->
                    val path = (basePath + "/" + m.getAnnotation(WSRequestMapping::class.java).name).replace(
                        "//".toRegex(),
                        "/"
                    )
                    val klass = m.getAnnotation(WSOperation::class.java).adapterClass.java
                    val factoryClass = m.getAnnotation(WSOperationFactory::class.java)?.value
                    methods[path] = ppp.parse(path) to WebSocketInvocation(
                        klass as Class<Operation>,
                        factoryClass?.let { fc -> operationFactories.find { fc.isInstance(it) } }
                            ?: defaultFactory,
                        bean, m
                    )
                    methods
                }
            } else {
                null
            }
        } ?: mapOf()
    }

    protected fun launchOperation(
        operation: Operation,
        pathMethod: Pair<PathPattern, WebSocketInvocation>,
        pathContainer: PathContainer,
        wsm: WebSocketMessage? = null,
    ): Mono<*> {
        return pathMethod.let { (pp, invocation) ->
            try {
                val payloadAsText = wsm?.payloadAsText
                val json = payloadAsText?.let { objectMapper.readTree(it) }
                val jsonParameters = if (json?.has("parameters") == true) json.get("parameters") else null
                val variables = pp.matchAndExtract(pathContainer)?.uriVariables ?: mapOf()
                val parameters = invocation.method.parameters.map { p: Parameter ->
                    val pathVarAnnotation = p.getAnnotation(PathVariable::class.java)
                    val paramAnnotation = p.getAnnotation(WSParam::class.java)
                    val bodyAnnotation = p.getAnnotation(WSMessage::class.java)
                    when {
                        pathVarAnnotation != null -> variables[
                            pathVarAnnotation.name.let { it.ifEmpty { null } }
                                ?: pathVarAnnotation.value.let { it.ifEmpty { null } } ?: p.name
                        ]

                        paramAnnotation != null -> jsonParameters?.let {
                            objectMapper.treeToValue(
                                it.get(paramAnnotation.value),
                                p.type
                            )
                        }

                        bodyAnnotation != null -> bodyAnnotation.messageClass.let { kl ->
                            when {
                                kl.isInstance(wsm) -> wsm
                                kl == String::class -> payloadAsText
                                else -> json?.let { objectMapper.treeToValue(it, kl.java) }
                            }
                        }

                        else -> operation
                    }
                }.toTypedArray()
                try {
                    invocation.method.invoke(invocation.bean, *parameters) as Mono<*>
                } catch (e: Exception) {
                    log.error("Cannot call WS invocation", e)
                    Mono.error<Nothing>(IllegalStateException(e))
                }
            } catch (e: Exception) {
                log.debug("Failed to initialize web-socket operation, likely wrong parameters", e)
                Mono.error<Nothing>(IllegalArgumentException("Invalid parameters for method", e))
            }
        }
    }

    protected suspend fun handleOperationError(session: WebSocketSession, e: Throwable) {
        when (e) {
            is UnsupportedOperationException ->
                HttpStatus.BAD_REQUEST to e.message
            is IllegalArgumentException ->
                HttpStatus.BAD_REQUEST to e.message
            is org.springframework.security.access.AccessDeniedException ->
                HttpStatus.FORBIDDEN to e.message
            is AuthenticationServiceException ->
                HttpStatus.UNAUTHORIZED to "You must be authenticated to use this method"
            else -> (HttpStatus.INTERNAL_SERVER_ERROR to "An internal error occurred").also {
                log.error("Error while performing operation ")
            }
        }.let { (httpStatus, message) ->
            session.close(CloseStatus(CUSTOM_CLOSE_STATUS_CODE + httpStatus.value(), message))
        }.awaitFirstOrNull()
        throw e
    }
}

