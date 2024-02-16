/*
 * Copyright (c) 2020. Taktik SA, All rights reserved.
 */

package org.taktik.icure.services.external.rest.v2.controllers.extra

import io.swagger.v3.oas.annotations.Operation
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
import org.taktik.couchdb.exception.DocumentNotFoundException
import org.taktik.icure.asyncservice.ClassificationService
import org.taktik.icure.cache.ReactorCacheInjector
import org.taktik.icure.services.external.rest.v2.dto.ClassificationDto
import org.taktik.icure.services.external.rest.v2.dto.ListOfIdsDto
import org.taktik.icure.services.external.rest.v2.dto.requests.BulkShareOrUpdateMetadataParamsDto
import org.taktik.icure.services.external.rest.v2.dto.requests.EntityBulkShareResultDto
import org.taktik.icure.services.external.rest.v2.mapper.ClassificationV2Mapper
import org.taktik.icure.services.external.rest.v2.mapper.requests.ClassificationBulkShareResultV2Mapper
import org.taktik.icure.services.external.rest.v2.mapper.requests.EntityShareOrMetadataUpdateRequestV2Mapper
import org.taktik.icure.utils.injectReactorContext
import org.taktik.icure.utils.injectCachedReactorContext
import reactor.core.publisher.Flux

@RestController("classificationControllerV2")
@Profile("app")
@RequestMapping("/rest/v2/classification")
@Tag(name = "classification")
class ClassificationController(
	private val classificationService: ClassificationService,
	private val classificationV2Mapper: ClassificationV2Mapper,
	private val bulkShareResultV2Mapper: ClassificationBulkShareResultV2Mapper,
	private val entityShareOrMetadataUpdateRequestV2Mapper: EntityShareOrMetadataUpdateRequestV2Mapper,
	private val reactorCacheInjector: ReactorCacheInjector
) {
	private val logger = LoggerFactory.getLogger(javaClass)

	@Operation(summary = "Create a classification with the current user", description = "Returns an instance of created classification Template.")
	@PostMapping
	fun createClassification(@RequestBody c: ClassificationDto) = mono {
		val element = classificationService.createClassification(classificationV2Mapper.map(c))
			?: throw ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Classification creation failed.")

		classificationV2Mapper.map(element)
	}

	@Operation(summary = "Get a classification Template")
	@GetMapping("/{classificationId}")
	fun getClassification(@PathVariable classificationId: String) = mono {
		val element = classificationService.getClassification(classificationId)
			?: throw ResponseStatusException(HttpStatus.NOT_FOUND, "Getting classification failed. Possible reasons: no such classification exists, or server error. Please try again or read the server log.")

		classificationV2Mapper.map(element)
	}

	@Operation(summary = "Get a list of classifications", description = "Ids are seperated by a coma")
	@GetMapping("/byIds/{ids}")
	fun getClassificationByHcPartyId(@PathVariable ids: String): Flux<ClassificationDto> {
		val elements = classificationService.getClassifications(ids.split(','))

		return elements.map { classificationV2Mapper.map(it) }.injectReactorContext()
	}

	@Operation(summary = "List classification Templates found By Healthcare Party and secret foreign keyelementIds.", description = "Keys hast to delimited by coma")
	@GetMapping("/byHcPartySecretForeignKeys")
	fun findClassificationsByHCPartyPatientForeignKeys(@RequestParam hcPartyId: String, @RequestParam secretFKeys: String): Flux<ClassificationDto> {
		val secretPatientKeys = secretFKeys.split(',').map { it.trim() }
		val elementList = classificationService.listClassificationsByHCPartyAndSecretPatientKeys(hcPartyId, secretPatientKeys)

		return elementList.map { classificationV2Mapper.map(it) }.injectReactorContext()
	}

	@Operation(summary = "Delete a batch of classifications", description = "Response is a set containing the ID's of deleted classifications.")
	@PostMapping("/delete/batch")
	fun deleteClassifications(@RequestBody classificationIds: ListOfIdsDto): Flux<DocIdentifier> =
		classificationIds.ids.takeIf { it.isNotEmpty() }?.let { ids ->
			classificationService.deleteClassifications(ids.toSet()).injectReactorContext()
		} ?: throw ResponseStatusException(HttpStatus.BAD_REQUEST, "A required query parameter was not specified for this request.").also { logger.error(it.message) }

	@Operation(summary = "Delete a classifications", description = "Deletes a classification and returns its id.")
	@DeleteMapping("/{classificationId}")
	fun deleteClassification(@PathVariable classificationId: String) = mono {
		classificationService.deleteClassification(classificationId)
	}

	@Operation(summary = "Modify a classification Template", description = "Returns the modified classification Template.")
	@PutMapping
	fun modifyClassification(@RequestBody classificationDto: ClassificationDto) = mono {
		val classification = classificationService.modifyClassification(classificationV2Mapper.map(classificationDto))
			?: throw DocumentNotFoundException("Classification modification failed.")

		classificationV2Mapper.map(classification)
	}

	@Operation(description = "Shares one or more classifications with one or more data owners")
	@PutMapping("/bulkSharedMetadataUpdate")
	fun bulkShare(
		@RequestBody request: BulkShareOrUpdateMetadataParamsDto
	): Flux<EntityBulkShareResultDto<ClassificationDto>> = flow {
		emitAll(classificationService.bulkShareOrUpdateMetadata(
			entityShareOrMetadataUpdateRequestV2Mapper.map(request)
		).map { bulkShareResultV2Mapper.map(it) })
	}.injectCachedReactorContext(reactorCacheInjector, 50)

	@Operation(description = "Shares one or more classifications with one or more data owners but does not return the updated entity.")
	@PutMapping("/bulkSharedMetadataUpdateMinimal")
	fun bulkShareMinimal(
		@RequestBody request: BulkShareOrUpdateMetadataParamsDto
	): Flux<EntityBulkShareResultDto<ClassificationDto>> = flow {
		emitAll(classificationService.bulkShareOrUpdateMetadata(
			entityShareOrMetadataUpdateRequestV2Mapper.map(request)
		).map { bulkShareResultV2Mapper.map(it).copy(updatedEntity = null) })
	}.injectCachedReactorContext(reactorCacheInjector, 50)
}
