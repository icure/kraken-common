/*
 * Copyright (c) 2020. Taktik SA, All rights reserved.
 */

package org.taktik.icure.services.external.rest.v2.controllers.extra

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.tags.Tag
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.reactor.mono
import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Profile
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.server.ResponseStatusException
import org.taktik.couchdb.DocIdentifier
import org.taktik.icure.asyncservice.ClassificationTemplateService
import org.taktik.icure.config.SharedPaginationConfig
import org.taktik.icure.db.PaginationOffset
import org.taktik.icure.pagination.PaginatedFlux
import org.taktik.icure.pagination.asPaginatedFlux
import org.taktik.icure.pagination.mapElements
import org.taktik.icure.services.external.rest.v2.dto.ClassificationTemplateDto
import org.taktik.icure.services.external.rest.v2.dto.ListOfIdsDto
import org.taktik.icure.services.external.rest.v2.dto.couchdb.DocIdentifierDto
import org.taktik.icure.services.external.rest.v2.mapper.ClassificationTemplateV2Mapper
import org.taktik.icure.services.external.rest.v2.mapper.couchdb.DocIdentifierV2Mapper
import org.taktik.icure.utils.injectReactorContext
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

@RestController("classificationTemplateControllerV2")
@Profile("app")
@RequestMapping("/rest/v2/classificationTemplate")
@Tag(name = "classificationTemplate")
class ClassificationTemplateController(
	private val classificationTemplateService: ClassificationTemplateService,
	private val classificationTemplateV2Mapper: ClassificationTemplateV2Mapper,
	private val docIdentifierV2Mapper: DocIdentifierV2Mapper,
	private val paginationConfig: SharedPaginationConfig,
) {
	companion object {
		private val logger = LoggerFactory.getLogger(this::class.java)
	}

	@Operation(
		summary = "Create a classification Template with the current user",
		description = "Returns an instance of created classification Template.",
	)
	@PostMapping
	fun createClassificationTemplate(
		@RequestBody c: ClassificationTemplateDto,
	): Mono<ClassificationTemplateDto> = mono {
		classificationTemplateV2Mapper.map(
			classificationTemplateService.createClassificationTemplate(classificationTemplateV2Mapper.map(c))
		)
	}

	@Operation(summary = "Get a classification Template")
	@GetMapping("/{classificationTemplateId}")
	fun getClassificationTemplate(
		@PathVariable classificationTemplateId: String,
	): Mono<ClassificationTemplateDto> = mono {
		val element =
			classificationTemplateService.getClassificationTemplate(classificationTemplateId)
				?: throw ResponseStatusException(
					HttpStatus.NOT_FOUND,
					"Getting classification Template failed. Possible reasons: no such classification Template exists, or server error. Please try again or read the server log.",
				)
		classificationTemplateV2Mapper.map(element)
	}

	@Operation(summary = "Get a list of classifications Templates", description = "Ids are seperated by a coma")
	@GetMapping("/byIds/{ids}")
	fun getClassificationTemplateByIds(
		@PathVariable ids: String,
	): Flux<ClassificationTemplateDto> {
		val elements = classificationTemplateService.getClassificationTemplates(ids.split(','))
		return elements.map { classificationTemplateV2Mapper.map(it) }.injectReactorContext()
	}

	@Operation(
		summary = "Deletes a batch of ClassificationTemplates.",
		description = "Response is a set containing the ID's of deleted ClassificationTemplates.",
	)
	@PostMapping("/delete/batch")
	fun deleteClassificationTemplates(
		@RequestBody classificationTemplateIds: ListOfIdsDto,
	): Flux<DocIdentifierDto> = classificationTemplateIds.ids.takeIf { it.isNotEmpty() }?.let { ids ->
		classificationTemplateService
			.deleteClassificationTemplates(LinkedHashSet(ids))
			.map { docIdentifierV2Mapper.map(DocIdentifier(it.id, it.rev)) }
			.injectReactorContext()
	}
		?: throw ResponseStatusException(HttpStatus.BAD_REQUEST, "A required query parameter was not specified for this request.").also {
			logger.error(it.message)
		}

	@Operation(summary = "Deletes a ClassificationTemplate.", description = "Deletes a ClassificationTemplate and returns its id and rev.")
	@DeleteMapping("/{classificationTemplateId}")
	fun deleteClassificationTemplate(
		@PathVariable classificationTemplateId: String,
	): Mono<DocIdentifierDto> = mono {
		classificationTemplateService
			.deleteClassificationTemplate(classificationTemplateId)
			.let {
				docIdentifierV2Mapper.map(DocIdentifier(it.id, it.rev))
			}
	}

	@Operation(summary = "Modify a classification Template", description = "Returns the modified classification Template.")
	@PutMapping
	fun modifyClassificationTemplate(
		@RequestBody classificationTemplateDto: ClassificationTemplateDto,
	): Mono<ClassificationTemplateDto> = mono {
		// TODO Ne modifier que le label
		classificationTemplateService.modifyClassificationTemplate(classificationTemplateV2Mapper.map(classificationTemplateDto))
		val modifiedClassificationTemplate =
			classificationTemplateService.getClassificationTemplate(classificationTemplateDto.id)
				?: throw ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Classification Template modification failed")
		classificationTemplateV2Mapper.map(modifiedClassificationTemplate)
	}

	@Operation(summary = "List all classification templates with pagination", description = "Returns a list of classification templates.")
	@GetMapping
	fun findClassificationTemplatesBy(
		@Parameter(description = "A label") @RequestBody(required = false) startKey: String?,
		@Parameter(description = "An classification template document ID") @RequestBody(required = false) startDocumentId: String?,
		@Parameter(description = "Number of rows") @RequestBody(required = false) limit: Int?,
	): PaginatedFlux<ClassificationTemplateDto> {
		val paginationOffset = PaginationOffset(startKey, startDocumentId, null, limit ?: paginationConfig.defaultLimit)

		return classificationTemplateService
			.listClassificationTemplates(paginationOffset)
			.mapElements(classificationTemplateV2Mapper::map)
			.asPaginatedFlux()
	}
}
