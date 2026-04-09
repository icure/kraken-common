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
import org.taktik.icure.annotations.controllers.CreatesOne
import org.taktik.icure.annotations.controllers.DeletesMany
import org.taktik.icure.annotations.controllers.DeletesOne
import org.taktik.icure.annotations.controllers.RetrievesAll
import org.taktik.icure.annotations.controllers.RetrievesMany
import org.taktik.icure.annotations.controllers.RetrievesOne
import org.taktik.icure.annotations.controllers.UpdatesOne
import org.taktik.icure.annotations.permissions.AccessControl

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
						append(
							handlerMethod.method.annotations
								.firstOrNull { ann -> ann is AccessControl }
								?.let { "<strong>Access Control Policies</strong>: ${(it as AccessControl).rule}<br>" } ?: "",
						)
						append(it.description?.let { desc -> "$desc<br>" } ?: "")
						append(
							handlerMethod.method.annotations
								.firstOrNull { ann -> ann.annotationClass.qualifiedName?.startsWith("org.taktik.icure.annotations.controllers") ?: false }
								?.let { ann ->
									when (ann) {
										is CreatesOne ->
											"<em>This method follows the Creates One behaviour: it receives the entity in the payload and return the saved entity.</em>"
										is DeletesMany ->
											"<em>This method follows the Deletes Many behaviour: of the entity ids passed as parameters, it deletes the ones that exists and that the current user can access.</em>"
										is DeletesOne ->
											"<em>This method follows the Deletes One behaviour: it deletes the entity passed as parameter if it exists and the user can access it.</em>"
										is RetrievesAll ->
											"<em>This method follows the Retrieves All behaviour: it retrieves all the entities that the user can access.</em>"
										is RetrievesMany ->
											"<em>This method follows the Retrieves Many behaviour: it retrieves all the entities which ids are passed as parameter and the user can access.</em>"
										is RetrievesOne ->
											"<em>This method follows the Retrieves One behaviour: it retrieves the entity with the specified id if it exists and the user can access it.</em>"
										is UpdatesOne ->
											"<em>This method follows the Updates One behaviour: it updates the entity if it exists, the user can access it and the update is correct.</em>"
										else -> ""
									}
								} ?: "",
						)
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
