/*
 * Copyright (c) 2020. Taktik SA, All rights reserved.
 */

package org.taktik.icure.config

import io.swagger.v3.oas.annotations.enums.SecuritySchemeType
import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.info.Info
import org.springdoc.core.GroupedOpenApi
import org.springdoc.core.SpringDocUtils
import org.springdoc.core.customizers.OperationCustomizer
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile
import org.springframework.web.bind.annotation.RequestParam

@Configuration
@Profile("app")
@io.swagger.v3.oas.annotations.security.SecurityScheme(name = "basicSchema", type = SecuritySchemeType.HTTP, scheme = "basic")
class SwaggerConfig {
	companion object {
		init {
			SpringDocUtils.getConfig().removeRequestWrapperToIgnore(Map::class.java)
		}
	}

	@Bean
	fun iCureV1Api(springOperationCustomizer: OperationCustomizer): GroupedOpenApi = GroupedOpenApi
		.builder()
		.group(
			"v1",
		).pathsToMatch("/rest/v1/**")
		.packagesToScan("org.taktik.icure.services.external.rest.v1")
		.addOpenApiCustomiser { openApi ->
			openApi.info(
				Info()
					.title("iCure Data Stack API Documentation")
					.description("The iCure Data Stack Application API is the native interface to iCure. This version is obsolete, please use v2.")
					.version("v1"),
			)
		}.addOperationCustomizer(springOperationCustomizer)
		.build()

	@Bean
	fun iCureV2Api(springOperationCustomizer: OperationCustomizer): GroupedOpenApi = GroupedOpenApi
		.builder()
		.group(
			"v2",
		).pathsToMatch("/rest/v2/**")
		.packagesToScan("org.taktik.icure.services.external.rest.v2")
		.addOpenApiCustomiser { openApi ->
			openApi.info(
				Info()
					.title("iCure Data Stack API Documentation")
					.description("The iCure Data Stack Application API is the native interface to iCure.")
					.version("v2"),
			)
		}.addOperationCustomizer(springOperationCustomizer)
		.build()

	@Bean
	fun springOperationCustomizer() = OperationCustomizer { operation, handlerMethod ->
		operation.also {
			try {
				if (it.parameters != null) {
					it.parameters =
						it.parameters.sortedWith(
							compareBy { p ->
								handlerMethod.methodParameters.indexOfFirst { mp ->
									(
										mp.parameterAnnotations
											.find { it is RequestParam }
											?.let { it as? RequestParam }
											?.name
											?.takeIf { it.isNotEmpty() }
											?: mp.parameter.name
										) ==
										p.name
								}
							},
						)
				}
				it.description =
					buildString {
						append(it.description?.let { desc -> "$desc<br>" } ?: "")
					}
			} catch (_: IllegalStateException) {
			}
		}
	}

	@Bean
	fun customOpenAPI(): OpenAPI = OpenAPI()
		.info(
			Info()
				.title("iCure Data Stack API Documentation")
				.version("v0")
				.description("The iCure Data Stack Application API is the native interface to iCure."),
		)
}
