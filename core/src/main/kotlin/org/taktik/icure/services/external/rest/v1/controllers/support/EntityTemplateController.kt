/*
 * Copyright (c) 2020. Taktik SA, All rights reserved.
 */

package org.taktik.icure.services.external.rest.v1.controllers.support

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.tags.Tag
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.reactor.mono
import org.springframework.context.annotation.Profile
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.server.ResponseStatusException
import org.taktik.couchdb.entity.IdAndRev
import org.taktik.icure.asyncservice.EntityTemplateService
import org.taktik.icure.services.external.rest.v1.dto.EntityTemplateDto
import org.taktik.icure.services.external.rest.v1.mapper.EntityTemplateMapper
import org.taktik.icure.utils.injectReactorContext
import reactor.core.publisher.Flux

@RestController
@Profile("app")
@RequestMapping("/rest/v1/entitytemplate")
@Tag(name = "entitytemplate")
class EntityTemplateController(
	private val entityTemplateService: EntityTemplateService,
	private val entityTemplateMapper: EntityTemplateMapper,
) {
	@Operation(
		summary = "Finding entityTemplates by userId, entityTemplate, type and version.",
		description = "Returns a list of entityTemplates matched with given input.",
	)
	@GetMapping("/find/{userId}/{type}")
	fun findEntityTemplates(
		@PathVariable userId: String,
		@PathVariable type: String,
		@RequestParam(required = false) searchString: String?,
		@RequestParam(required = false) includeEntities: Boolean?,
	) = entityTemplateService
		.listEntityTemplatesBy(userId, type, searchString, includeEntities)
		.map {
			entityTemplateMapper.map(it) // .apply { if (includeEntities == true) entity = it.entity }
		}.injectReactorContext()

	@Operation(
		summary = "Finding entityTemplates by entityTemplate, type and version.",
		description = "Returns a list of entityTemplates matched with given input.",
	)
	@GetMapping("/findAll/{type}")
	fun findAllEntityTemplates(
		@PathVariable type: String,
		@RequestParam(required = false) searchString: String?,
		@RequestParam(required = false) includeEntities: Boolean?,
	) = entityTemplateService
		.listEntityTemplatesBy(type, searchString, includeEntities)
		.map {
			entityTemplateMapper.map(it) // .apply { if (includeEntities == true) entity = it.entity }
		}.injectReactorContext()

	@Operation(
		summary = "Finding entityTemplates by userId, type and keyword.",
		description = "Returns a list of entityTemplates matched with given input.",
	)
	@GetMapping("/find/{userId}/{type}/keyword/{keyword}")
	fun findEntityTemplatesByKeyword(
		@PathVariable userId: String,
		@PathVariable type: String,
		@PathVariable keyword: String,
		@RequestParam(required = false) includeEntities: Boolean?,
	) = entityTemplateService
		.listEntityTemplatesByKeyword(userId, type, keyword, includeEntities)
		.map {
			entityTemplateMapper.map(it) // .apply { if (includeEntities == true) entity = it.entity }
		}.injectReactorContext()

	@Operation(
		summary = "Finding entityTemplates by entityTemplate, type and version.",
		description = "Returns a list of entityTemplates matched with given input.",
	)
	@GetMapping("/findAll/{type}/keyword/{keyword}")
	fun findAllEntityTemplatesByKeyword(
		@PathVariable type: String,
		@PathVariable keyword: String,
		@RequestParam(required = false) includeEntities: Boolean?,
	) = entityTemplateService
		.listEntityTemplatesByKeyword(type, keyword, includeEntities)
		.map {
			entityTemplateMapper.map(it) // .apply { if (includeEntities == true) entity = it.entity }
		}.injectReactorContext()

	@Operation(summary = "Create a EntityTemplate", description = "Type, EntityTemplate and Version are required.")
	@PostMapping
	fun createEntityTemplate(
		@RequestBody c: EntityTemplateDto,
	) = mono {
		val et = entityTemplateMapper.map(c).copy(entity = c.entity)
		val entityTemplate = entityTemplateService.createEntityTemplate(et)
		entityTemplateMapper.map(entityTemplate)
	}

	@Operation(summary = "Get a list of entityTemplates by ids", description = "Keys must be delimited by comma")
	@GetMapping("/byIds/{entityTemplateIds}")
	fun getEntityTemplates(
		@PathVariable entityTemplateIds: String,
	): Flux<EntityTemplateDto> {
		val entityTemplates = entityTemplateService.getEntityTemplates(entityTemplateIds.split(','))

		val entityTemplateDtos =
			entityTemplates.map { f ->
				entityTemplateMapper.map(f) // .apply { entity = f.entity }
			}

		return entityTemplateDtos.injectReactorContext()
	}

	@Operation(
		summary = "Get a entityTemplate",
		description = "Get a entityTemplate based on ID or (entityTemplate,type,version) as query strings. (entityTemplate,type,version) is unique.",
	)
	@GetMapping("/{entityTemplateId}")
	fun getEntityTemplate(
		@Parameter(description = "EntityTemplate id", required = true) @PathVariable entityTemplateId: String,
	) = mono {
		val c =
			entityTemplateService.getEntityTemplate(entityTemplateId)
				?: throw ResponseStatusException(HttpStatus.NOT_FOUND, "A problem regarding fetching the entityTemplate. Read the app logs.")

		val et = entityTemplateMapper.map(c)
		// et.entity = c.entity
		et
	}

	@Operation(summary = "Modify a entityTemplate", description = "Modification of (type, entityTemplate, version) is not allowed.")
	@PutMapping
	fun modifyEntityTemplate(
		@RequestBody entityTemplateDto: EntityTemplateDto,
	) = mono {
		val modifiedEntityTemplate =
			try {
				val et = entityTemplateMapper.map(entityTemplateDto).copy(entity = entityTemplateDto.entity)
				entityTemplateService.modifyEntityTemplate(et)
			} catch (e: Exception) {
				throw ResponseStatusException(
					HttpStatus.INTERNAL_SERVER_ERROR,
					"A problem regarding modification of the entityTemplate. Read the app logs: " + e.message,
				)
			}

		val succeed = modifiedEntityTemplate != null
		if (succeed) {
			entityTemplateMapper.map(modifiedEntityTemplate)
		} else {
			throw ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Modification of the entityTemplate failed. Read the server log.")
		}
	}

	@Operation(summary = "Modify a batch of entityTemplates", description = "Returns the modified entityTemplates.")
	@PutMapping("/batch")
	fun modifyEntityTemplates(
		@RequestBody entityTemplateDtos: List<EntityTemplateDto>,
	): Flux<EntityTemplateDto> {
		val entityTemplates =
			entityTemplateService.modifyEntityTemplates(
				entityTemplateDtos.map(entityTemplateMapper::map),
			)
		return entityTemplates.map(entityTemplateMapper::map).injectReactorContext()
	}

	@Operation(summary = "Create a batch of entityTemplates", description = "Returns the modified entityTemplates.")
	@PostMapping("/batch")
	fun createEntityTemplates(
		@RequestBody entityTemplateDtos: List<EntityTemplateDto>,
	): Flux<EntityTemplateDto> {
		val entityTemplates =
			entityTemplateService.createEntityTemplates(
				entityTemplateDtos.map(entityTemplateMapper::map),
			)
		return entityTemplates.map(entityTemplateMapper::map).injectReactorContext()
	}

	@DeleteMapping("/{entityTemplateIds}")
	@Operation(summary = "Delete entity templates")
	fun deleteEntityTemplate(
		@PathVariable entityTemplateIds: String,
	) = entityTemplateService.deleteEntityTemplates(entityTemplateIds.split(",").toSet().map { IdAndRev(it, null) }).injectReactorContext()
}
