/*
 * Copyright (c) 2020. Taktik SA, All rights reserved.
 */

package org.taktik.icure.services.external.rest.v2.controllers.support

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.tags.Tag
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.reactor.mono
import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Profile
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.server.ResponseStatusException

import org.taktik.icure.asyncservice.EntityTemplateService
import org.taktik.icure.services.external.rest.v2.dto.EntityTemplateDto
import org.taktik.icure.services.external.rest.v2.dto.ListOfIdsDto
import org.taktik.icure.services.external.rest.v2.dto.couchdb.DocIdentifierDto
import org.taktik.icure.services.external.rest.v2.mapper.EntityTemplateV2Mapper
import org.taktik.icure.services.external.rest.v2.mapper.couchdb.DocIdentifierV2Mapper
import org.taktik.icure.utils.injectReactorContext
import reactor.core.publisher.Flux

@RestController("entityTemplateControllerV2")
@Profile("app")
@RequestMapping("/rest/v2/entitytemplate")
@Tag(name = "entityTemplate")
class EntityTemplateController(
	private val entityTemplateService: EntityTemplateService,
	private val entityTemplateV2Mapper: EntityTemplateV2Mapper,
	private val docIdentifierV2Mapper: DocIdentifierV2Mapper,
) {
	private val logger = LoggerFactory.getLogger(javaClass)

	@Operation(summary = "Finding entityTemplates by userId, entityTemplate, type and version.", description = "Returns a list of entityTemplates matched with given input.")
	@GetMapping("/find/{userId}/{type}")
	fun listEntityTemplatesBy(
		@PathVariable userId: String,
		@PathVariable type: String,
		@RequestParam(required = false) searchString: String?,
		@RequestParam(required = false) includeEntities: Boolean?
	) = entityTemplateService.listEntityTemplatesBy(userId, type, searchString, includeEntities).map { entityTemplateV2Mapper.map(it)/*.apply { if (includeEntities == true) entity = it.entity }*/ }.injectReactorContext()

	@Operation(summary = "Finding entityTemplates by entityTemplate, type and version.", description = "Returns a list of entityTemplates matched with given input.")
	@GetMapping("/findAll/{type}")
	fun listAllEntityTemplatesBy(
		@PathVariable type: String,
		@RequestParam(required = false) searchString: String?,
		@RequestParam(required = false) includeEntities: Boolean?
	) = entityTemplateService.listEntityTemplatesBy(type, searchString, includeEntities).map { entityTemplateV2Mapper.map(it)/*.apply { if (includeEntities == true) entity = it.entity }*/ }.injectReactorContext()

	@Operation(summary = "Finding entityTemplates by userId, type and keyword.", description = "Returns a list of entityTemplates matched with given input.")
	@GetMapping("/find/{userId}/{type}/keyword/{keyword}")
	fun listEntityTemplatesByKeyword(
		@PathVariable userId: String,
		@PathVariable type: String,
		@PathVariable keyword: String,
		@RequestParam(required = false) includeEntities: Boolean?
	) = entityTemplateService.listEntityTemplatesByKeyword(userId, type, keyword, includeEntities).map { entityTemplateV2Mapper.map(it)/*.apply { if (includeEntities == true) entity = it.entity }*/ }.injectReactorContext()

	@Operation(summary = "Finding entityTemplates by entityTemplate, type and version.", description = "Returns a list of entityTemplates matched with given input.")
	@GetMapping("/findAll/{type}/keyword/{keyword}")
	fun findAllEntityTemplatesByKeyword(
		@PathVariable type: String,
		@PathVariable keyword: String,
		@RequestParam(required = false) includeEntities: Boolean?
	) = entityTemplateService.listEntityTemplatesByKeyword(type, keyword, includeEntities).map { entityTemplateV2Mapper.map(it)/*.apply { if (includeEntities == true) entity = it.entity }*/ }.injectReactorContext()

	@Operation(summary = "Create a EntityTemplate", description = "Type, EntityTemplate and Version are required.")
	@PostMapping
	fun createEntityTemplate(@RequestBody c: EntityTemplateDto) = mono {
		val et = entityTemplateV2Mapper.map(c).copy(entity = c.entity)
		val entityTemplate = entityTemplateService.createEntityTemplate(et)
			?: throw ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "EntityTemplate creation failed.")

		entityTemplateV2Mapper.map(entityTemplate)
	}

	@Operation(summary = "Get a list of entityTemplates by ids", description = "Keys must be delimited by comma")
	@PostMapping("/byIds")
	fun getEntityTemplates(@RequestBody entityTemplateIds: ListOfIdsDto): Flux<EntityTemplateDto> {
		return entityTemplateIds.ids.takeIf { it.isNotEmpty() }
			?.let { ids ->
				entityTemplateService
					.getEntityTemplates(ids)
					.map { f -> entityTemplateV2Mapper.map(f) }
					.injectReactorContext()
			}
			?: throw ResponseStatusException(HttpStatus.BAD_REQUEST, "A required query parameter was not specified for this request.").also { logger.error(it.message) }
	}

	@Operation(summary = "Get a entityTemplate", description = "Get a entityTemplate based on ID or (entityTemplate,type,version) as query strings. (entityTemplate,type,version) is unique.")
	@GetMapping("/{entityTemplateId}")
	fun getEntityTemplate(@Parameter(description = "EntityTemplate id", required = true) @PathVariable entityTemplateId: String) = mono {
		val c = entityTemplateService.getEntityTemplate(entityTemplateId)
			?: throw ResponseStatusException(HttpStatus.NOT_FOUND, "A problem regarding fetching the entityTemplate. Read the app logs.")

		val et = entityTemplateV2Mapper.map(c)
		/*et.entity = c.entity*/
		et
	}

	@Operation(summary = "Modify a entityTemplate", description = "Modification of (type, entityTemplate, version) is not allowed.")
	@PutMapping
	fun modifyEntityTemplate(@RequestBody entityTemplateDto: EntityTemplateDto) = mono {
		val modifiedEntityTemplate = try {
			val et = entityTemplateV2Mapper.map(entityTemplateDto).copy(entity = entityTemplateDto.entity)
			entityTemplateService.modifyEntityTemplate(et)
		} catch (e: Exception) {
			throw ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "A problem regarding modification of the entityTemplate. Read the app logs: " + e.message)
		}

		val succeed = modifiedEntityTemplate != null
		if (succeed) {
			modifiedEntityTemplate?.let { entityTemplateV2Mapper.map(it) }
		} else {
			throw ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Modification of the entityTemplate failed. Read the server log.")
		}
	}

	@Operation(summary = "Modify a batch of entityTemplates", description = "Returns the modified entityTemplates.")
	@PutMapping("/batch")
	fun modifyEntityTemplates(@RequestBody entityTemplateDtos: List<EntityTemplateDto>): Flux<EntityTemplateDto> =
		entityTemplateService.modifyEntityTemplates(
			entityTemplateDtos.map(entityTemplateV2Mapper::map))
			.map(entityTemplateV2Mapper::map)
			.injectReactorContext()

	@Operation(summary = "Create a batch of entityTemplates", description = "Returns the modified entityTemplates.")
	@PostMapping("/batch")
	fun createEntityTemplates(@RequestBody entityTemplateDtos: List<EntityTemplateDto>): Flux<EntityTemplateDto> =
		entityTemplateService.createEntityTemplates(
			entityTemplateDtos.map(entityTemplateV2Mapper::map))
			.map(entityTemplateV2Mapper::map)
			.injectReactorContext()

	@Operation(summary = "Delete entity templates")
	@PostMapping("/delete/batch")
	fun deleteEntityTemplate(@RequestBody entityTemplateIds: ListOfIdsDto): Flux<DocIdentifierDto> =
		entityTemplateIds.ids.takeIf { it.isNotEmpty() }?.let { ids ->
			entityTemplateService.deleteEntityTemplates(ids.toSet())
				.map(docIdentifierV2Mapper::map)
				.injectReactorContext()
		} ?: throw ResponseStatusException(HttpStatus.BAD_REQUEST, "A required query parameter was not specified for this request.").also { logger.error(it.message) }

}
