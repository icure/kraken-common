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
import org.springframework.context.annotation.Profile
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType.APPLICATION_JSON_VALUE
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
import org.taktik.couchdb.entity.IdAndRev
import org.taktik.couchdb.exception.DocumentNotFoundException
import org.taktik.icure.asyncservice.ClassificationService
import org.taktik.icure.cache.ReactorCacheInjector
import org.taktik.icure.services.external.rest.v2.dto.ClassificationDto
import org.taktik.icure.services.external.rest.v2.dto.IcureStubDto
import org.taktik.icure.services.external.rest.v2.dto.ListOfIdsAndRevDto
import org.taktik.icure.services.external.rest.v2.dto.ListOfIdsDto
import org.taktik.icure.services.external.rest.v2.dto.couchdb.DocIdentifierDto
import org.taktik.icure.services.external.rest.v2.dto.filter.AbstractFilterDto
import org.taktik.icure.services.external.rest.v2.dto.requests.BulkShareOrUpdateMetadataParamsDto
import org.taktik.icure.services.external.rest.v2.dto.requests.EntityBulkShareResultDto
import org.taktik.icure.services.external.rest.v2.mapper.ClassificationV2Mapper
import org.taktik.icure.services.external.rest.v2.mapper.IdWithRevV2Mapper
import org.taktik.icure.services.external.rest.v2.mapper.StubV2Mapper
import org.taktik.icure.services.external.rest.v2.mapper.couchdb.DocIdentifierV2Mapper
import org.taktik.icure.services.external.rest.v2.mapper.filter.FilterV2Mapper
import org.taktik.icure.services.external.rest.v2.mapper.requests.ClassificationBulkShareResultV2Mapper
import org.taktik.icure.services.external.rest.v2.mapper.requests.EntityShareOrMetadataUpdateRequestV2Mapper
import org.taktik.icure.utils.injectCachedReactorContext
import org.taktik.icure.utils.injectReactorContext
import org.taktik.icure.utils.orThrow
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

@RestController("classificationControllerV2")
@Profile("app")
@RequestMapping("/rest/v2/classification")
@Tag(name = "classification")
class ClassificationController(
	private val classificationService: ClassificationService,
	private val classificationV2Mapper: ClassificationV2Mapper,
	private val bulkShareResultV2Mapper: ClassificationBulkShareResultV2Mapper,
	private val stubV2Mapper: StubV2Mapper,
	private val entityShareOrMetadataUpdateRequestV2Mapper: EntityShareOrMetadataUpdateRequestV2Mapper,
	private val reactorCacheInjector: ReactorCacheInjector,
	private val docIdentifierV2Mapper: DocIdentifierV2Mapper,
	private val filterV2Mapper: FilterV2Mapper,
	private val idWithRevV2Mapper: IdWithRevV2Mapper,
) {
	@Operation(
		summary = "Create a classification with the current user",
		description = "Returns an instance of created classification Template.",
	)
	@PostMapping
	fun createClassification(
		@RequestBody c: ClassificationDto,
	): Mono<ClassificationDto> = mono {
		classificationV2Mapper.map(
			classificationService.createClassification(classificationV2Mapper.map(c))
		)
	}

	@Operation(summary = "Get a classification Template")
	@GetMapping("/{classificationId}")
	fun getClassification(
		@PathVariable classificationId: String,
	): Mono<ClassificationDto> = mono {
		val element =
			classificationService.getClassification(classificationId)
				?: throw ResponseStatusException(
					HttpStatus.NOT_FOUND,
					"Getting classification failed. Possible reasons: no such classification exists, or server error. Please try again or read the server log.",
				)

		classificationV2Mapper.map(element)
	}

	@Operation(summary = "Get a list of classifications")
	@PostMapping("/byIds")
	fun getClassifications(
		@RequestBody classificationIds: ListOfIdsDto,
	): Flux<ClassificationDto> {
		require(classificationIds.ids.isNotEmpty()) { "You must specify at least one id" }
		return classificationService.getClassifications(classificationIds.ids).map(classificationV2Mapper::map).injectReactorContext()
	}

	@Suppress("DEPRECATION")
	@Deprecated("This method is inefficient for high volumes of keys, use listClassificationIdsByDataOwnerPatientCreated instead")
	@Operation(
		summary = "List classification found by healthcare party and secret foreign keys.",
		description = "Keys must be delimited by comma",
	)
	@GetMapping("/byHcPartySecretForeignKeys")
	fun findClassificationsByHCPartyPatientForeignKeys(
		@RequestParam hcPartyId: String,
		@RequestParam secretFKeys: String,
	): Flux<ClassificationDto> {
		val secretPatientKeys = secretFKeys.split(',').map { it.trim() }
		val elementList = classificationService.listClassificationsByHCPartyAndSecretPatientKeys(hcPartyId, secretPatientKeys)

		return elementList.map { classificationV2Mapper.map(it) }.injectReactorContext()
	}

	@Operation(summary = "Find Classification ids by data owner id, patient secret keys and creation date.")
	@PostMapping("/byDataOwnerPatientCreated", produces = [APPLICATION_JSON_VALUE])
	fun listClassificationIdsByDataOwnerPatientCreated(
		@RequestParam dataOwnerId: String,
		@RequestParam(required = false) startDate: Long?,
		@RequestParam(required = false) endDate: Long?,
		@RequestParam(required = false) descending: Boolean?,
		@RequestBody secretPatientKeys: ListOfIdsDto,
	): Flux<String> {
		require(secretPatientKeys.ids.isNotEmpty()) {
			"You need to provide at least one secret patient key"
		}
		return classificationService
			.listClassificationIdsByDataOwnerPatientCreated(
				dataOwnerId = dataOwnerId,
				secretForeignKeys = secretPatientKeys.ids.toSet(),
				startDate = startDate,
				endDate = endDate,
				descending = descending ?: false,
			).injectReactorContext()
	}

	@Operation(summary = "Deletes multiple Classifications")
	@PostMapping("/delete/batch")
	fun deleteClassifications(
		@RequestBody classificationIds: ListOfIdsDto,
	): Flux<DocIdentifierDto> = classificationService
		.deleteClassifications(
			classificationIds.ids.map { IdAndRev(it, null) },
		).map { docIdentifierV2Mapper.map(DocIdentifier(it.id, it.rev)) }
		.injectReactorContext()

	@Operation(summary = "Deletes a multiple Classifications if they match the provided revs")
	@PostMapping("/delete/batch/withrev")
	fun deleteClassificationsWithRev(
		@RequestBody classificationIds: ListOfIdsAndRevDto,
	): Flux<DocIdentifierDto> = classificationService
		.deleteClassifications(
			classificationIds.ids.map(idWithRevV2Mapper::map),
		).map { docIdentifierV2Mapper.map(DocIdentifier(it.id, it.rev)) }
		.injectReactorContext()

	@Operation(summary = "Deletes an Classification")
	@DeleteMapping("/{classificationId}")
	fun deleteClassification(
		@PathVariable classificationId: String,
		@RequestParam(required = false) rev: String? = null,
	): Mono<DocIdentifierDto> = mono {
		classificationService.deleteClassification(classificationId, rev).let {
			docIdentifierV2Mapper.map(DocIdentifier(it.id, it.rev))
		}
	}

	@PostMapping("/undelete/{classificationId}")
	fun undeleteClassification(
		@PathVariable classificationId: String,
		@RequestParam(required = true) rev: String,
	): Mono<ClassificationDto> = mono {
		classificationV2Mapper.map(classificationService.undeleteClassification(classificationId, rev))
	}

	@DeleteMapping("/purge/{classificationId}")
	fun purgeClassification(
		@PathVariable classificationId: String,
		@RequestParam(required = true) rev: String,
	): Mono<DocIdentifierDto> = mono {
		classificationService.purgeClassification(classificationId, rev).let(docIdentifierV2Mapper::map)
	}

	@Operation(summary = "Modify a classification Template", description = "Returns the modified classification Template.")
	@PutMapping
	fun modifyClassification(
		@RequestBody classificationDto: ClassificationDto,
	): Mono<ClassificationDto> = mono {
		val classification =
			classificationService.modifyClassification(classificationV2Mapper.map(classificationDto))
				?: throw DocumentNotFoundException("Classification modification failed.")

		classificationV2Mapper.map(classification)
	}

	@Suppress("DEPRECATION")
	@Deprecated("This method is inefficient for high volumes of keys, use findClassificationsDelegationsStubsByIds instead")
	@Operation(summary = "List classification stubs found by healthcare party and secret foreign keys.")
	@PostMapping("/byHcPartySecretForeignKeys/delegations")
	fun findClassificationsDelegationsStubsByHCPartyPatientForeignKeys(
		@RequestParam hcPartyId: String,
		@RequestBody secretPatientKeys: List<String>,
	): Flux<IcureStubDto> = classificationService
		.listClassificationsByHCPartyAndSecretPatientKeys(hcPartyId, secretPatientKeys)
		.map { classification -> stubV2Mapper.mapToStub(classification) }
		.injectReactorContext()

	@Operation(summary = "List classification stubs found by ids.")
	@PostMapping("/delegations")
	fun findClassificationsDelegationsStubsByIds(
		@RequestBody classificationIds: ListOfIdsDto,
	): Flux<IcureStubDto> = classificationService
		.getClassifications(classificationIds.ids)
		.map { classification -> stubV2Mapper.mapToStub(classification) }
		.injectReactorContext()

	@Operation(summary = "Get the ids of the Classifications matching the provided filter.")
	@PostMapping("/match", produces = [APPLICATION_JSON_VALUE])
	fun matchClassificationBy(
		@RequestBody filter: AbstractFilterDto<ClassificationDto>,
	): Flux<String> = classificationService
		.matchClassificationsBy(
			filter = filterV2Mapper.tryMap(filter).orThrow(),
		).injectReactorContext()

	@Operation(description = "Shares one or more classifications with one or more data owners")
	@PutMapping("/bulkSharedMetadataUpdate")
	fun bulkShare(
		@RequestBody request: BulkShareOrUpdateMetadataParamsDto,
	): Flux<EntityBulkShareResultDto<ClassificationDto>> = flow {
		emitAll(
			classificationService
				.bulkShareOrUpdateMetadata(
					entityShareOrMetadataUpdateRequestV2Mapper.map(request),
				).map { bulkShareResultV2Mapper.map(it) },
		)
	}.injectCachedReactorContext(reactorCacheInjector, 50)

	@Operation(description = "Shares one or more classifications with one or more data owners but does not return the updated entity.")
	@PutMapping("/bulkSharedMetadataUpdateMinimal")
	fun bulkShareMinimal(
		@RequestBody request: BulkShareOrUpdateMetadataParamsDto,
	): Flux<EntityBulkShareResultDto<Nothing>> = flow {
		emitAll(
			classificationService
				.bulkShareOrUpdateMetadata(
					entityShareOrMetadataUpdateRequestV2Mapper.map(request),
				).map { bulkShareResultV2Mapper.map(it).minimal() },
		)
	}.injectCachedReactorContext(reactorCacheInjector, 50)
}
