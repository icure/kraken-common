/*
 * Copyright (c) 2020. Taktik SA, All rights reserved.
 */

package org.taktik.icure.config

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.ser.FilterProvider
import com.fasterxml.jackson.databind.ser.impl.SimpleBeanPropertyFilter
import com.fasterxml.jackson.databind.ser.impl.SimpleFilterProvider
import com.fasterxml.jackson.module.kotlin.KotlinFeature
import com.fasterxml.jackson.module.kotlin.KotlinModule
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.Ordered
import org.springframework.core.codec.Encoder
import org.springframework.http.codec.ServerCodecConfigurer
import org.springframework.http.codec.json.Jackson2JsonDecoder
import org.springframework.web.reactive.config.CorsRegistry
import org.springframework.web.reactive.config.ResourceHandlerRegistry
import org.springframework.web.reactive.config.WebFluxConfigurer
import org.springframework.web.reactive.handler.SimpleUrlHandlerMapping
import org.springframework.web.reactive.socket.server.WebSocketService
import org.springframework.web.reactive.socket.server.support.HandshakeWebSocketService
import org.springframework.web.reactive.socket.server.support.WebSocketHandlerAdapter
import org.springframework.web.reactive.socket.server.upgrade.ReactorNettyRequestUpgradeStrategy
import org.taktik.icure.config.SharedWebFluxConfiguration.CardinalMappers.MapperConfig
import org.taktik.icure.entities.utils.SemanticVersion
import org.taktik.icure.serialization.IcureDomainObjectMapper.registerMultiplatformSupportModules
import org.taktik.icure.services.external.http.WebSocketOperationHandler
import org.taktik.icure.spring.encoder.FluxStringJsonEncoder
import reactor.netty.http.server.WebsocketServerSpec
import java.util.TreeMap

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
	/**
	 * Mappers for Cardinal SDK, selectively omit some data based on the version of cardinal sdk declared in use by the
	 * user.
	 *
	 * - Version < 2.0.0 or not declared:
	 *   - Include all data, including legacy autofilled fields
	 * - 2.0.0 <= version < 2.4.0 :
	 *   - Does not include DataAttachment.storedDataSize and Document.mainAttachmentStoredDataSize, as they are filled
	 *     in automatically by the server regardless of the CardinalSDK version.
	 * - 2.4.0 <= version, currently:
	 *   - Everything is included except for legacy autofilled fields by default
	 *
	 * # Legacy fields
	 *
	 * Many fields of the data model are considered part of the legacy data model.
	 *
	 * Most of the fields from the legacy data model were never used by cardinal or kraken, so they always have the
	 * default null/empty value, therefore, when we serialize the dto for Cardinal there is no entry for those fields.
	 * However, if the user was using those fields and gave explicit values to them we will include them in the
	 * serialized DTO, and that would cause an error when using new (standard) versions of cardinal.
	 *
	 * Since no one ever used explicitly the legacy fields we haven't incurred in these problems, however, some fields
	 * of the legacy data model were given a value automatically by older versions o of Cardinal and/or older versions
	 * of Kraken.
	 * We are explicitly ignoring these fields, regardless of their value, when the user declares they are using
	 * Cardinal version 2.0.0 or above, unless additional flags are used to request explicitly that they are included.
	 *
	 * This supports transition without migration of data for cardinal users to 2.0.0.
	 * However, if the user ever explicitly set a value for one of these autofilled fields this would cause silent
	 * partial loss of information, since the Cardinal SDK would not reject the data.
	 * Again, this is not a problem for cardinal users since no user of cardinal < 2.0.0 ever used those fields, and
	 * Cardinal >= 2.0.0 does not expose them, but it would be a problem if users of the legacy iCure SDK migrate to
	 * cardinal without using the proper configuration.
	 *
	 * ## Example
	 *
	 * Health element has:
	 * - MedicalLocationId, a legacy field that has been removed from Cardinal 2.0.0 and was never autofilled
	 * - Status, a legacy field that has been removed from Cardinal 2.0.0 but was autofilled to 0
	 *
	 * MedicalLocationId will not be included in the serialized DTO if null, BUT will always be included if not null.
	 * If for some reason there is data that did set the MedicalLocationId to a value the CardinalSDK would throw an
	 * error when parsing the health element. This protects against unintentional loss of data
	 *
	 * Status instead, will not be included, regardless of value, otherwise users of Cardinal would get unexpected
	 * errors even when they did not explicitly set the status value. However, if a Cardinal user had explicitly set
	 * a Status value to let's say 2, the status would be silently reset to 0 when the user updates the HealthElement
	 * through Cardinal 2.0.0
	 *
	 * This is a problem only for users that moved from the legacy iCure SDK. These users should instead use the
	 * legacy-support version of the cardinal SDK and use a header to request that legacy autofilled fields are
	 * included.
	 */
	interface CardinalMappers {
		/**
		 * Mappers associated by minimum version of cardinal that can use that mapper.
		 */
		val byMinVersion: TreeMap<SemanticVersion, MapperConfig>

		data class MapperConfig(
			/**
			 * A mapper configuration that excludes all legacy fields.
			 */
			val default: ObjectMapper,
			/**
			 * A mapper configuration equivalent to [default] excepts it also includes the legacy fields
			 */
			val includingLegacyFields: ObjectMapper
		)

		/**
		 * Get the cardinal mapper configuration to use for the provided [semanticVersion], or null if there is no
		 * mapper supporting that as minimum version.
		 */
		fun getForVersion(semanticVersion: SemanticVersion, includingLegacyFields: Boolean = false): ObjectMapper? =
			byMinVersion.floorEntry(semanticVersion)?.value?.let {
				if (includingLegacyFields) it.includingLegacyFields else it.default
			}
	}

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

	private val legacyJacksonFilter: FilterProvider = SimpleFilterProvider()
		.setDefaultFilter(SimpleBeanPropertyFilter.serializeAll())

	protected val legacyObjectMapper: ObjectMapper =
		ObjectMapper().registerModule(
			KotlinModule.Builder()
				.configure(KotlinFeature.NullIsSameAsDefault, true)
				.build()
		).apply {
			setDefaultPropertyInclusion(JsonInclude.Include.NON_NULL)
			setFilterProvider(legacyJacksonFilter)
		}.registerMultiplatformSupportModules()

	/**
	 * Object mapper that serializes always all fields
	 */
	@Bean
	open fun legacyObjectMapper() = legacyObjectMapper

	// TODO will work well only as long as we have no overlap between lists
	private val excludeLegacyMetadataFilters = listOf(
		// Status is by default filled to 0, and is always serialized
		Pair("healthElementFilter", SimpleBeanPropertyFilter.serializeAllExcept("status")),
		// An older version of kraken used to automatically set a value for type
		Pair("userFilter", SimpleBeanPropertyFilter.serializeAllExcept("type")),
		// An older version of cardinal used to automatically set label as empty map
		Pair("codeStubFilter", SimpleBeanPropertyFilter.serializeAllExcept("label")),
	)
	private val pre_2_4_0_filters = listOf(
		Pair("dataAttachmentFilter", SimpleBeanPropertyFilter.serializeAllExcept("storedDataSize")),
		Pair("documentFilter", SimpleBeanPropertyFilter.serializeAllExcept("mainAttachmentStoredDataSize")),
	)

	private fun makeCardinalObjectMapper(
		filters: List<Pair<String, SimpleBeanPropertyFilter>>
	): MapperConfig {
		fun includingLegacyFields(doInclude: Boolean) =
			ObjectMapper().registerModule(
				KotlinModule.Builder()
					.configure(KotlinFeature.NullIsSameAsDefault, true)
					.build()
			).apply {
				setDefaultPropertyInclusion(JsonInclude.Include.NON_EMPTY)
				val allFilters = if (doInclude) filters else (excludeLegacyMetadataFilters + filters)
				setFilterProvider(
					allFilters.fold(
						SimpleFilterProvider().setDefaultFilter(SimpleBeanPropertyFilter.serializeAll())
					) { filters, (name, filter) ->
						filters.addFilter(name, filter)
					}
				)
			}.registerMultiplatformSupportModules()
		return MapperConfig(
			default = includingLegacyFields(false),
			includingLegacyFields = includingLegacyFields(true)
		)
	}

	protected val cardinalMappers = object : CardinalMappers {
		override val byMinVersion = TreeMap<SemanticVersion, MapperConfig>().apply {
			put(
				CardinalModelInfo.minCardinalModelVersion,
				makeCardinalObjectMapper(pre_2_4_0_filters)
			)
			put(
				SemanticVersion("2.4.0"),
				makeCardinalObjectMapper(emptyList())
			)
		}
	}

	@Bean
	open fun cardinalObjectMapper() = cardinalMappers

	abstract fun getJackson2JsonEncoder(): Encoder<Any>

	override fun configureHttpMessageCodecs(configurer: ServerCodecConfigurer) {
		configurer.defaultCodecs().maxInMemorySize(128 * 1024 * 1024)

		configurer.customCodecs().register(FluxStringJsonEncoder())

		configurer.defaultCodecs().jackson2JsonEncoder(getJackson2JsonEncoder())

		configurer.defaultCodecs().jackson2JsonDecoder(
			Jackson2JsonDecoder(
				ObjectMapper().registerModule(
					KotlinModule
						.Builder()
						.configure(KotlinFeature.NullIsSameAsDefault, true)
						// TODO : may have significant performance impact but provides better error reporting (400 instead of 500), disable in case of issues.
						.configure(KotlinFeature.StrictNullChecks, true)
						.build(),
				).registerMultiplatformSupportModules(),
			).apply { maxInMemorySize = 128 * 1024 * 1024 },
		)
	}

	fun objectMapper(): ObjectMapper = ObjectMapper()
		.registerModule(
			KotlinModule
				.Builder()
				.withReflectionCacheSize(512)
				.configure(KotlinFeature.NullIsSameAsDefault, true)
				.configure(KotlinFeature.NullToEmptyCollection, true)
				.configure(KotlinFeature.NullToEmptyMap, true)
				.build(),
		).apply {
			setSerializationInclusion(JsonInclude.Include.NON_NULL)
		}
}
