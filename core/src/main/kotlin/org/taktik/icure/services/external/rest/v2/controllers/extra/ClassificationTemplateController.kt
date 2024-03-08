/*
 * Copyright (c) 2020. Taktik SA, All rights reserved.
 */

package org.taktik.icure.services.external.rest.v2.controllers.extra

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.tags.Tag
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
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
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.server.ResponseStatusException
import org.taktik.couchdb.DocIdentifier
import org.taktik.couchdb.entity.ComplexKey
import org.taktik.icure.asyncservice.ClassificationTemplateService
import org.taktik.icure.cache.ReactorCacheInjector
import org.taktik.icure.config.SharedPaginationConfig
import org.taktik.icure.db.PaginationOffset
import org.taktik.icure.pagination.PaginatedFlux
import org.taktik.icure.pagination.asPaginatedFlux
import org.taktik.icure.pagination.mapElements
import org.taktik.icure.services.external.rest.v2.dto.ClassificationTemplateDto
import org.taktik.icure.services.external.rest.v2.dto.ListOfIdsDto
import org.taktik.icure.services.external.rest.v2.dto.requests.BulkShareOrUpdateMetadataParamsDto
import org.taktik.icure.services.external.rest.v2.dto.requests.EntityBulkShareResultDto
import org.taktik.icure.services.external.rest.v2.mapper.ClassificationTemplateV2Mapper
import org.taktik.icure.services.external.rest.v2.mapper.requests.ClassificationTemplateBulkShareResultV2Mapper
import org.taktik.icure.services.external.rest.v2.mapper.requests.EntityShareOrMetadataUpdateRequestV2Mapper
import org.taktik.icure.utils.JsonString
import org.taktik.icure.utils.injectCachedReactorContext
import org.taktik.icure.utils.injectReactorContext
import reactor.core.publisher.Flux

@RestController("classificationTemplateControllerV2")
@Profile("app")
@RequestMapping("/rest/v2/classificationTemplate")
@Tag(name = "classificationTemplate")
class ClassificationTemplateController(
	private val classificationTemplateService: ClassificationTemplateService,
	private val classificationTemplateV2Mapper: ClassificationTemplateV2Mapper,
	private val bulkShareResultV2Mapper: ClassificationTemplateBulkShareResultV2Mapper,
	private val entityShareOrMetadataUpdateRequestV2Mapper: EntityShareOrMetadataUpdateRequestV2Mapper,
	private val reactorCacheInjector: ReactorCacheInjector,
	private val objectMapper: ObjectMapper,
	private val paginationConfig: SharedPaginationConfig
) {

	companion object {
		private val logger = LoggerFactory.getLogger(this::class.java)
	}

	@Operation(summary = "Create a classification Template with the current user", description = "Returns an instance of created classification Template.")
	@PostMapping
	fun createClassificationTemplate(@RequestBody c: ClassificationTemplateDto) = mono {
		val element = classificationTemplateService.createClassificationTemplate(classificationTemplateV2Mapper.map(c))
			?: throw ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Classification Template creation failed.")
		classificationTemplateV2Mapper.map(element)
	}

	@Operation(summary = "Get a classification Template")
	@GetMapping("/{classificationTemplateId}")
	fun getClassificationTemplate(@PathVariable classificationTemplateId: String) = mono {
		val element = classificationTemplateService.getClassificationTemplate(classificationTemplateId)
			?: throw ResponseStatusException(HttpStatus.NOT_FOUND, "Getting classification Template failed. Possible reasons: no such classification Template exists, or server error. Please try again or read the server log.")
		classificationTemplateV2Mapper.map(element)
	}

	@Operation(summary = "Get a list of classifications Templates", description = "Ids are seperated by a coma")
	@GetMapping("/byIds/{ids}")
	fun getClassificationTemplateByIds(@PathVariable ids: String): Flux<ClassificationTemplateDto> {
		val elements = classificationTemplateService.getClassificationTemplates(ids.split(','))
		return elements.map { classificationTemplateV2Mapper.map(it) }.injectReactorContext()
	}

	@Operation(summary = "List classification Templates found By Healthcare Party and secret foreign keyelementIds.", description = "Keys hast to delimited by comma")
	@GetMapping("/byHcPartySecretForeignKeys")
	fun listClassificationTemplatesByHCPartyPatientForeignKeys(@RequestParam hcPartyId: String, @RequestParam secretFKeys: String): Flux<ClassificationTemplateDto> {
		val secretPatientKeys = secretFKeys.split(',').map { it.trim() }
		val elementList = classificationTemplateService.listClassificationsByHCPartyAndSecretPatientKeys(hcPartyId, ArrayList(secretPatientKeys))

		return elementList.map { classificationTemplateV2Mapper.map(it) }.injectReactorContext()
	}

	@Operation(summary = "List classification Templates found By Healthcare Party and a single secret foreign key.")
	@GetMapping("/byHcPartySecretForeignKey")
	fun findClassificationTemplatesByHCPartyPatientForeignKey(
		@RequestParam hcPartyId: String,
		@RequestParam secretFKey: String,
		@Parameter(description = "The start key for pagination") @RequestBody(required = false) startKey: JsonString?,
		@Parameter(description = "An classification template document ID") @RequestBody(required = false) startDocumentId: String?,
		@Parameter(description = "Number of rows") @RequestBody(required = false) limit: Int?
	): PaginatedFlux<ClassificationTemplateDto> {
		val keyElements = startKey?.let { objectMapper.readValue<ComplexKey>(it) }
		val offset = PaginationOffset(keyElements, startDocumentId, null, limit ?: paginationConfig.defaultLimit)
		return classificationTemplateService
			.listClassificationsByHCPartyAndSecretPatientKey(hcPartyId, secretFKey, offset)
			.mapElements(classificationTemplateV2Mapper::map)
			.asPaginatedFlux()
	}

	@Operation(summary = "Deletes a batch of ClassificationTemplates.", description = "Response is a set containing the ID's of deleted ClassificationTemplates.")
	@PostMapping("/delete/batch")
	fun deleteClassificationTemplates(@RequestBody classificationTemplateIds: ListOfIdsDto): Flux<DocIdentifier> =
		classificationTemplateIds.ids.takeIf { it.isNotEmpty() }?.let { ids ->
			classificationTemplateService.deleteClassificationTemplates(LinkedHashSet(ids)).injectReactorContext()
		} ?: throw ResponseStatusException(HttpStatus.BAD_REQUEST, "A required query parameter was not specified for this request.").also { logger.error(it.message) }

	@Operation(summary = "Deletes a ClassificationTemplate.", description = "Deletes a ClassificationTemplate and returns its id and rev.")
	@DeleteMapping("/{classificationTemplateId}")
	fun deleteClassificationTemplate(@PathVariable classificationTemplateId: String) = mono {
		classificationTemplateService.deleteClassificationTemplate(classificationTemplateId)
	}

	@Operation(summary = "Modify a classification Template", description = "Returns the modified classification Template.")
	@PutMapping
	fun modifyClassificationTemplate(@RequestBody classificationTemplateDto: ClassificationTemplateDto) = mono {
		//TODO Ne modifier que le label
		classificationTemplateService.modifyClassificationTemplate(classificationTemplateV2Mapper.map(classificationTemplateDto))
		val modifiedClassificationTemplate = classificationTemplateService.getClassificationTemplate(classificationTemplateDto.id)
			?: throw ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Classification Template modification failed")
		classificationTemplateV2Mapper.map(modifiedClassificationTemplate)
	}

	@Operation(summary = "List all classification templates with pagination", description = "Returns a list of classification templates.")
	@GetMapping
	fun findClassificationTemplatesBy(
		@Parameter(description = "A label") @RequestBody(required = false) startKey: String?,
		@Parameter(description = "An classification template document ID") @RequestBody(required = false) startDocumentId: String?,
		@Parameter(description = "Number of rows") @RequestBody(required = false) limit: Int?
	): PaginatedFlux<ClassificationTemplateDto> {
		val paginationOffset = PaginationOffset(startKey, startDocumentId, null, limit ?: paginationConfig.defaultLimit)

		return classificationTemplateService
			.listClassificationTemplates(paginationOffset)
			.mapElements(classificationTemplateV2Mapper::map)
			.asPaginatedFlux()
	}

	@Operation(description = "Shares one or more patients with one or more data owners")
	@PutMapping("/bulkSharedMetadataUpdate")
	fun bulkShare(
		@RequestBody request: BulkShareOrUpdateMetadataParamsDto
	): Flux<EntityBulkShareResultDto<ClassificationTemplateDto>> = flow {
		emitAll(classificationTemplateService.bulkShareOrUpdateMetadata(
			entityShareOrMetadataUpdateRequestV2Mapper.map(request)
		).map { bulkShareResultV2Mapper.map(it) })
	}.injectCachedReactorContext(reactorCacheInjector, 50)
}
