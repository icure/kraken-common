/*
 * Copyright (c) 2020. Taktik SA, All rights reserved.
 */

package org.taktik.icure.services.external.rest.v2.controllers.core

import com.fasterxml.jackson.databind.ObjectMapper
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.tags.Tag
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.reactor.mono
import org.slf4j.LoggerFactory
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
import org.taktik.icure.asyncservice.RelatedPersonService
import org.taktik.icure.cache.ReactorCacheInjector
import org.taktik.icure.config.SharedPaginationConfig
import org.taktik.icure.db.PaginationOffset
import org.taktik.icure.entities.conflicts.ConflictResolutionStrategy
import org.taktik.icure.services.external.rest.v2.dto.ListOfIdsAndRevDto
import org.taktik.icure.services.external.rest.v2.dto.ListOfIdsDto
import org.taktik.icure.services.external.rest.v2.dto.PaginatedList
import org.taktik.icure.services.external.rest.v2.dto.RelatedPersonDto
import org.taktik.icure.services.external.rest.v2.dto.conflicts.ConflictResolutionRequestDto
import org.taktik.icure.services.external.rest.v2.dto.conflicts.ConflictResolutionResultDto
import org.taktik.icure.services.external.rest.v2.dto.conflicts.ConflictResolutionStrategyDto
import org.taktik.icure.services.external.rest.v2.dto.conflicts.MergeResultDto
import org.taktik.icure.services.external.rest.v2.dto.couchdb.DocIdentifierDto
import org.taktik.icure.services.external.rest.v2.dto.filter.AbstractFilterDto
import org.taktik.icure.services.external.rest.v2.dto.filter.chain.FilterChain
import org.taktik.icure.services.external.rest.v2.dto.requests.BulkShareOrUpdateMetadataParamsDto
import org.taktik.icure.services.external.rest.v2.dto.requests.EntityBulkShareResultDto
import org.taktik.icure.services.external.rest.v2.mapper.IdWithRevV2Mapper
import org.taktik.icure.services.external.rest.v2.mapper.RelatedPersonV2Mapper
import org.taktik.icure.services.external.rest.v2.mapper.conflicts.ConflictResolutionStrategyV2Mapper
import org.taktik.icure.services.external.rest.v2.mapper.conflicts.ConflictResolutionV2Mapper
import org.taktik.icure.services.external.rest.v2.mapper.conflicts.MergeResultV2Mapper
import org.taktik.icure.services.external.rest.v2.mapper.couchdb.DocIdentifierV2Mapper
import org.taktik.icure.services.external.rest.v2.mapper.filter.FilterChainV2Mapper
import org.taktik.icure.services.external.rest.v2.mapper.filter.FilterV2Mapper
import org.taktik.icure.services.external.rest.v2.mapper.requests.EntityShareOrMetadataUpdateRequestV2Mapper
import org.taktik.icure.services.external.rest.v2.mapper.requests.RelatedPersonBulkShareResultV2Mapper
import org.taktik.icure.services.external.rest.v2.utils.paginatedList
import org.taktik.icure.utils.injectCachedReactorContext
import org.taktik.icure.utils.injectReactorContext
import org.taktik.icure.utils.orThrow
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

@RestController("relatedPersonControllerV2")
@Profile("app")
@RequestMapping("/rest/v2/relatedperson")
@Tag(name = "relatedPerson")
class RelatedPersonController(
	private val relatedPersonService: RelatedPersonService,
	private val relatedPersonV2Mapper: RelatedPersonV2Mapper,
	private val filterChainV2Mapper: FilterChainV2Mapper,
	private val filterV2Mapper: FilterV2Mapper,
	private val bulkShareResultV2Mapper: RelatedPersonBulkShareResultV2Mapper,
	private val entityShareOrMetadataUpdateRequestV2Mapper: EntityShareOrMetadataUpdateRequestV2Mapper,
	private val docIdentifierV2Mapper: DocIdentifierV2Mapper,
	private val idWithRevV2Mapper: IdWithRevV2Mapper,
	private val reactorCacheInjector: ReactorCacheInjector,
	private val objectMapper: ObjectMapper,
	private val paginationConfig: SharedPaginationConfig,
	private val conflictResolutionV2Mapper: ConflictResolutionV2Mapper,
	private val mergeResultV2Mapper: MergeResultV2Mapper,
	private val conflictResolutionStrategyV2Mapper: ConflictResolutionStrategyV2Mapper,
) {
	private val logger = LoggerFactory.getLogger(javaClass)

	@Operation(
		summary = "Create a related person with the current user",
		description = "Returns an instance of created related person.",
	)
	@PostMapping
	fun createRelatedPerson(
		@RequestBody c: RelatedPersonDto,
	): Mono<RelatedPersonDto> = mono {
		relatedPersonV2Mapper.map(relatedPersonService.createRelatedPerson(relatedPersonV2Mapper.map(c)))
	}

	@Operation(summary = "Get a related person")
	@GetMapping("/{relatedPersonId}")
	fun getRelatedPerson(
		@PathVariable relatedPersonId: String,
	): Mono<RelatedPersonDto> = mono {
		val relatedPerson =
			relatedPersonService.getRelatedPerson(relatedPersonId)
				?: throw ResponseStatusException(
					HttpStatus.NOT_FOUND,
					"Getting related person failed. Possible reasons: no such related person exists, or server error. Please try again or read the server log.",
				)

		relatedPersonV2Mapper.map(relatedPerson)
	}

	@Operation(summary = "Get relatedPersons by batch", description = "Get a list of relatedPersons by ids/keys.")
	@PostMapping("/byIds")
	fun getRelatedPersons(
		@RequestBody relatedPersonIds: ListOfIdsDto,
	): Flux<RelatedPersonDto> {
		require(relatedPersonIds.ids.isNotEmpty()) { "You must specify at least one id." }
		return relatedPersonService.getRelatedPersons(relatedPersonIds.ids).map(relatedPersonV2Mapper::map).injectReactorContext()
	}

	@Operation(summary = "Deletes multiple RelatedPersons")
	@PostMapping("/delete/batch")
	fun deleteRelatedPersons(
		@RequestBody relatedPersonIds: ListOfIdsDto,
	): Flux<DocIdentifierDto> = relatedPersonService
		.deleteRelatedPersons(
			relatedPersonIds.ids.map { IdAndRev(it, null) },
		).map { docIdentifierV2Mapper.map(DocIdentifier(it.id, it.rev)) }
		.injectCachedReactorContext(reactorCacheInjector, 100)

	@Operation(summary = "Deletes multiple RelatedPersons if they match the provided revs")
	@PostMapping("/delete/batch/withrev")
	fun deleteRelatedPersonsWithRev(
		@RequestBody relatedPersonIds: ListOfIdsAndRevDto,
	): Flux<DocIdentifierDto> = relatedPersonService
		.deleteRelatedPersons(
			relatedPersonIds.ids.map(idWithRevV2Mapper::map),
		).map { docIdentifierV2Mapper.map(DocIdentifier(it.id, it.rev)) }
		.injectCachedReactorContext(reactorCacheInjector, 100)

	@Operation(summary = "Deletes a RelatedPerson")
	@DeleteMapping("/{relatedPersonId}")
	fun deleteRelatedPerson(
		@PathVariable relatedPersonId: String,
		@RequestParam(required = false) rev: String? = null,
	): Mono<DocIdentifierDto> = reactorCacheInjector.monoWithCachedContext(10) {
		relatedPersonService.deleteRelatedPerson(relatedPersonId, rev).let {
			docIdentifierV2Mapper.map(DocIdentifier(it.id, it.rev))
		}
	}

	@PostMapping("/undelete/{relatedPersonId}")
	fun undeleteRelatedPerson(
		@PathVariable relatedPersonId: String,
		@RequestParam(required = true) rev: String,
	): Mono<RelatedPersonDto> = reactorCacheInjector.monoWithCachedContext(10) {
		relatedPersonV2Mapper.map(relatedPersonService.undeleteRelatedPerson(relatedPersonId, rev))
	}

	@PostMapping("/undelete/batch")
	fun undeleteRelatedPersons(
		@RequestBody relatedPersonIds: ListOfIdsAndRevDto,
	): Flux<RelatedPersonDto> = relatedPersonService
		.undeleteRelatedPersons(
			relatedPersonIds.ids.map(idWithRevV2Mapper::map),
		).map(relatedPersonV2Mapper::map)
		.injectCachedReactorContext(reactorCacheInjector, 100)

	@DeleteMapping("/purge/{relatedPersonId}")
	fun purgeRelatedPerson(
		@PathVariable relatedPersonId: String,
		@RequestParam(required = true) rev: String,
	): Mono<DocIdentifierDto> = reactorCacheInjector.monoWithCachedContext(10) {
		relatedPersonService.purgeRelatedPerson(relatedPersonId, rev)
			.let(docIdentifierV2Mapper::map)
	}

	@PostMapping("/purge/batch")
	fun purgeRelatedPersons(
		@RequestBody relatedPersonIds: ListOfIdsAndRevDto,
	): Flux<DocIdentifierDto> = relatedPersonService
		.purgeRelatedPersons(
			relatedPersonIds.ids.map(idWithRevV2Mapper::map),
		).map(docIdentifierV2Mapper::map)
		.injectCachedReactorContext(reactorCacheInjector, 100)

	@Operation(summary = "Modify a related person", description = "Returns the modified related person.")
	@PutMapping
	fun modifyRelatedPerson(
		@RequestBody relatedPersonDto: RelatedPersonDto,
	): Mono<RelatedPersonDto> = mono {
		val modifiedRelatedPerson =
			relatedPersonService.modifyRelatedPerson(relatedPersonV2Mapper.map(relatedPersonDto))
		relatedPersonV2Mapper.map(modifiedRelatedPerson)
	}

	@Operation(summary = "Modify a batch of related persons", description = "Returns the modified related persons.")
	@PutMapping("/batch")
	fun modifyRelatedPersons(
		@RequestBody relatedPersonDtos: List<RelatedPersonDto>,
	): Flux<RelatedPersonDto> = try {
		val relatedPersons = relatedPersonService.modifyEntities(
			relatedPersonDtos.map { f -> relatedPersonV2Mapper.map(f) }.asFlow(),
		)
		relatedPersons.map { relatedPersonV2Mapper.map(it) }.injectReactorContext()
	} catch (e: Exception) {
		logger.warn(e.message, e)
		throw ResponseStatusException(HttpStatus.BAD_REQUEST, e.message)
	}

	@Operation(summary = "Create a batch of related persons", description = "Returns the created related persons.")
	@PostMapping("/batch")
	fun createRelatedPersons(
		@RequestBody relatedPersonDtos: List<RelatedPersonDto>,
	): Flux<RelatedPersonDto> = relatedPersonService.createEntities(
		relatedPersonDtos.map { f -> relatedPersonV2Mapper.map(f) }.asFlow(),
	).map { relatedPersonV2Mapper.map(it) }.injectReactorContext()

	@Operation(
		summary = "Filter related persons for the current user (data owner)",
		description = "Returns a list of related persons along with next start keys and Document ID. If the nextStartKey is Null it means that this is the last page.",
	)
	@PostMapping("/filter")
	fun filterRelatedPersonsBy(
		@Parameter(description = "A RelatedPerson document ID") @RequestParam(required = false) startDocumentId: String?,
		@Parameter(description = "Number of rows") @RequestParam(required = false) limit: Int?,
		@RequestBody filterChain: FilterChain<RelatedPersonDto>,
	): Mono<PaginatedList<RelatedPersonDto>> = mono {
		val realLimit = limit ?: paginationConfig.defaultLimit
		val paginationOffset = PaginationOffset(null, startDocumentId, null, realLimit + 1)

		val relatedPersons = relatedPersonService.filterRelatedPersons(paginationOffset, filterChainV2Mapper.tryMap(filterChain).orThrow())

		relatedPersons.paginatedList(relatedPersonV2Mapper::map, realLimit, objectMapper = objectMapper)
	}

	@Operation(description = "Shares one or more related persons with one or more data owners")
	@PutMapping("/bulkSharedMetadataUpdate")
	fun bulkShare(
		@RequestBody request: BulkShareOrUpdateMetadataParamsDto,
	): Flux<EntityBulkShareResultDto<RelatedPersonDto>> = flow {
		emitAll(
			relatedPersonService
				.bulkShareOrUpdateMetadata(
					entityShareOrMetadataUpdateRequestV2Mapper.map(request),
				).map { bulkShareResultV2Mapper.map(it) },
		)
	}.injectCachedReactorContext(reactorCacheInjector, 50)

	@Operation(description = "Shares one or more related persons with one or more data owners but does not return the updated entity.")
	@PutMapping("/bulkSharedMetadataUpdateMinimal")
	fun bulkShareMinimal(
		@RequestBody request: BulkShareOrUpdateMetadataParamsDto,
	): Flux<EntityBulkShareResultDto<Nothing>> = flow {
		emitAll(
			relatedPersonService
				.bulkShareOrUpdateMetadata(
					entityShareOrMetadataUpdateRequestV2Mapper.map(request),
				).map { bulkShareResultV2Mapper.map(it).minimal() },
		)
	}.injectCachedReactorContext(reactorCacheInjector, 50)

	@Operation(summary = "Get the ids of the RelatedPersons matching the provided filter.")
	@PostMapping("/match", produces = [APPLICATION_JSON_VALUE])
	fun matchRelatedPersonsBy(
		@RequestBody filter: AbstractFilterDto<RelatedPersonDto>,
	): Flux<String> = relatedPersonService
		.matchRelatedPersonsBy(
			filter = filterV2Mapper.tryMap(filter).orThrow(),
		).injectReactorContext()

	@GetMapping("/conflicts", produces = [APPLICATION_JSON_VALUE])
	fun getConflictingEntitiesIds(): Flux<String> = relatedPersonService.getConflictingEntitiesIds().injectReactorContext()

	@GetMapping("/conflicts/of")
	fun getConflictsForEntity(
		@RequestParam entityId: String,
	): Flux<RelatedPersonDto> = relatedPersonService.getConflictsFor(entityId)
		.map(relatedPersonV2Mapper::map)
		.injectReactorContext()

	@PostMapping("/conflicts/winner")
	fun declareConflictWinner(
		@RequestBody request: ConflictResolutionRequestDto<RelatedPersonDto>,
	): Mono<ConflictResolutionResultDto<RelatedPersonDto>> = mono {
		val result = relatedPersonService.declareConflictWinner(
			entity = relatedPersonV2Mapper.map(request.document),
			conflictsToPurge = request.conflictsToPurge,
		)
		conflictResolutionV2Mapper.map(result, relatedPersonV2Mapper::map)
	}

	@PostMapping("/conflicts/solve")
	fun autoSolveConflicts(
		@RequestBody entityIds: List<String>,
		@RequestParam strategy: ConflictResolutionStrategyDto?,
	): Flux<MergeResultDto> = relatedPersonService
		.solveConflicts(
			limit = null,
			ids = entityIds,
			strategy = strategy?.let {
				conflictResolutionStrategyV2Mapper.map(strategy)
			} ?: ConflictResolutionStrategy.FullMergeability,
		)
		.map(mergeResultV2Mapper::map)
		.injectReactorContext()
}
