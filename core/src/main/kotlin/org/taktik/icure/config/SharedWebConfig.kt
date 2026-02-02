/*
 * Copyright (c) 2020. Taktik SA, All rights reserved.
 */

package org.taktik.icure.config

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.databind.MapperFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.json.JsonMapper
import com.fasterxml.jackson.module.kotlin.KotlinFeature
import com.fasterxml.jackson.module.kotlin.KotlinModule
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.Ordered
import org.springframework.http.codec.ServerCodecConfigurer
import org.springframework.http.codec.json.Jackson2JsonDecoder
import org.springframework.http.codec.json.Jackson2JsonEncoder
import org.springframework.web.reactive.config.CorsRegistry
import org.springframework.web.reactive.config.ResourceHandlerRegistry
import org.springframework.web.reactive.config.WebFluxConfigurer
import org.springframework.web.reactive.handler.SimpleUrlHandlerMapping
import org.springframework.web.reactive.socket.server.WebSocketService
import org.springframework.web.reactive.socket.server.support.HandshakeWebSocketService
import org.springframework.web.reactive.socket.server.support.WebSocketHandlerAdapter
import org.springframework.web.reactive.socket.server.upgrade.ReactorNettyRequestUpgradeStrategy
import org.taktik.icure.serialization.JacksonModules
import org.taktik.icure.services.external.http.WebSocketOperationHandler
import org.taktik.icure.spring.encoder.FluxStringJsonEncoder
import reactor.netty.http.server.WebsocketServerSpec

@Configuration
class SharedWebConfig {
	// Do not remove: these beans are not used directly in any of our classes, but they are used by spring.
	@Bean
	fun handlerMapping(webSocketHandler: WebSocketOperationHandler) = SimpleUrlHandlerMapping().apply {
		urlMap = mapOf("/ws/**" to webSocketHandler)
		order = Ordered.HIGHEST_PRECEDENCE
	}

	@Bean
	fun handlerAdapter(webSocketService: WebSocketService) = WebSocketHandlerAdapter(webSocketService)

	@Bean
	fun webSocketService() = HandshakeWebSocketService(
		ReactorNettyRequestUpgradeStrategy(
			WebsocketServerSpec.builder().maxFramePayloadLength(64 * 1024 * 1024),
		),
	)

	// endregion
}

abstract class SharedWebFluxConfiguration : WebFluxConfigurer {
	private val CLASSPATH_RESOURCE_LOCATIONS =
		arrayOf("classpath:/META-INF/resources/", "classpath:/resources/", "classpath:/static/", "classpath:/public/")

	override fun addResourceHandlers(registry: ResourceHandlerRegistry) {
		registry
			.addResourceHandler("/**")
			.addResourceLocations(*CLASSPATH_RESOURCE_LOCATIONS)
	}

	override fun addCorsMappings(registry: CorsRegistry) {
		registry
			.addMapping("/**")
			.allowCredentials(true)
			.allowedOriginPatterns("*")
			.allowedMethods("*")
			.allowedHeaders("*")
	}

	// TODO update also in kraken-lite to use provided mapper instead of instantiating a new one
	abstract fun getJackson2JsonEncoder(objectMapper: ObjectMapper): Jackson2JsonEncoder

	override fun configureHttpMessageCodecs(configurer: ServerCodecConfigurer) {
		configurer.defaultCodecs().maxInMemorySize(128 * 1024 * 1024)

		configurer.customCodecs().register(FluxStringJsonEncoder())

		val encodingMapper: ObjectMapper = JsonMapper.builder().configure(
			MapperFeature.ALLOW_COERCION_OF_SCALARS, true
		).build().registerModule(
			KotlinModule.Builder()
				.configure(KotlinFeature.NullIsSameAsDefault, true)
				.build()
		).registerModule(JacksonModules.strictFloatsModule).apply {
			setSerializationInclusion(JsonInclude.Include.NON_NULL)
		}

		val decodingMapper: ObjectMapper = JsonMapper.builder().configure(
			MapperFeature.ALLOW_COERCION_OF_SCALARS, true
		).build().registerModule(
			KotlinModule
				.Builder()
				.configure(KotlinFeature.NullIsSameAsDefault, true)
				// TODO : may have significant performance impact but provides better error reporting (400 instead of 500), disable in case of issues.
				.configure(KotlinFeature.StrictNullChecks, true)
				.build(),
		).registerModule(JacksonModules.strictFloatsModule)


		configurer.defaultCodecs().jackson2JsonEncoder(
			getJackson2JsonEncoder(encodingMapper)
		)

		configurer.defaultCodecs().jackson2JsonDecoder(
			Jackson2JsonDecoder(decodingMapper).apply { maxInMemorySize = 128 * 1024 * 1024 },
		)
	}
}
