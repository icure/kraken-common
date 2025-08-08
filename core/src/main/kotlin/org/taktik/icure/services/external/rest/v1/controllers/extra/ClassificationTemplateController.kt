/*
 * Copyright (c) 2020. Taktik SA, All rights reserved.
 */

package org.taktik.icure.services.external.rest.v1.controllers.extra

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
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.server.ResponseStatusException
import org.taktik.couchdb.DocIdentifier
import org.taktik.icure.asyncservice.ClassificationTemplateService
import org.taktik.icure.config.SharedPaginationConfig
import org.taktik.icure.db.PaginationOffset
import org.taktik.icure.pagination.PaginatedFlux
import org.taktik.icure.pagination.asPaginatedFlux
import org.taktik.icure.pagination.mapElements
import org.taktik.icure.services.external.rest.v1.dto.ClassificationTemplateDto
import org.taktik.icure.services.external.rest.v1.dto.couchdb.DocIdentifierDto
import org.taktik.icure.services.external.rest.v1.mapper.ClassificationTemplateMapper
import org.taktik.icure.services.external.rest.v1.mapper.couchdb.DocIdentifierMapper
import org.taktik.icure.utils.injectReactorContext
import reactor.core.publisher.Flux

@RestController
@Profile("app")
@RequestMapping("/rest/v1/classificationTemplate")
@Tag(name = "classificationTemplate")
class ClassificationTemplateController(
	private val classificationTemplateService: ClassificationTemplateService,
	private val classificationTemplateMapper: ClassificationTemplateMapper,
	private val docIdentifierMapper: DocIdentifierMapper,
	private val paginationConfig: SharedPaginationConfig,
) {
	@Operation(
		summary = "Create a classification Template with the current user",
		description = "Returns an instance of created classification Template.",
	)
	@PostMapping
	fun createClassificationTemplate(
		@RequestBody c: ClassificationTemplateDto,
	) = mono {
		val element =
			classificationTemplateService.createClassificationTemplate(classificationTemplateMapper.map(c))
				?: throw ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Classification Template creation failed.")
		classificationTemplateMapper.map(element)
	}

	@Operation(summary = "Get a classification Template")
	@GetMapping("/{classificationTemplateId}")
	fun getClassificationTemplate(
		@PathVariable classificationTemplateId: String,
	) = mono {
		val element =
			classificationTemplateService.getClassificationTemplate(classificationTemplateId)
				?: throw ResponseStatusException(
					HttpStatus.NOT_FOUND,
					"Getting classification Template failed. Possible reasons: no such classification Template exists, or server error. Please try again or read the server log.",
				)
		classificationTemplateMapper.map(element)
	}

	@Operation(summary = "Get a list of classifications Templates", description = "Ids are seperated by a coma")
	@GetMapping("/byIds/{ids}")
	fun getClassificationTemplateByIds(
		@PathVariable ids: String,
	): Flux<ClassificationTemplateDto> {
		val elements = classificationTemplateService.getClassificationTemplates(ids.split(','))
		return elements.map { classificationTemplateMapper.map(it) }.injectReactorContext()
	}

	@Operation(
		summary = "Delete classification Templates.",
		description = "Response is a set containing the ID's of deleted classification Templates.",
	)
	@DeleteMapping("/{classificationTemplateIds}")
	fun deleteClassificationTemplates(
		@PathVariable classificationTemplateIds: String,
	): Flux<DocIdentifierDto> {
		val ids =
			classificationTemplateIds.split(',').takeUnless { it.isEmpty() }
				?: throw ResponseStatusException(HttpStatus.BAD_REQUEST, "A required query parameter was not specified for this request.")
		return classificationTemplateService
			.deleteClassificationTemplates(ids)
			.map { docIdentifierMapper.map(DocIdentifier(it.id, it.rev)) }
			.injectReactorContext()
	}

	@Operation(summary = "Modify a classification Template", description = "Returns the modified classification Template.")
	@PutMapping
	fun modifyClassificationTemplate(
		@RequestBody classificationTemplateDto: ClassificationTemplateDto,
	) = mono {
		// TODO Ne modifier que le label
		classificationTemplateService.modifyClassificationTemplate(classificationTemplateMapper.map(classificationTemplateDto))
		val modifiedClassificationTemplate =
			classificationTemplateService.getClassificationTemplate(classificationTemplateDto.id)
				?: throw ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Classification Template modification failed")
		classificationTemplateMapper.map(modifiedClassificationTemplate)
	}

	@Operation(summary = "List all classification templates with pagination", description = "Returns a list of classification templates.")
	@GetMapping
	fun listClassificationTemplates(
		@Parameter(description = "A label") @RequestBody(required = false) startKey: String?,
		@Parameter(description = "An classification template document ID") @RequestBody(required = false) startDocumentId: String?,
		@Parameter(description = "Number of rows") @RequestBody(required = false) limit: Int?,
	): PaginatedFlux<ClassificationTemplateDto> {
		val paginationOffset = PaginationOffset(startKey, startDocumentId, null, limit ?: paginationConfig.defaultLimit)

		return classificationTemplateService
			.listClassificationTemplates(paginationOffset)
			.mapElements(classificationTemplateMapper::map)
			.asPaginatedFlux()
	}
}
