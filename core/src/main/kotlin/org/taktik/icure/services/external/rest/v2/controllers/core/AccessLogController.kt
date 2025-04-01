/*
 * Copyright (c) 2020. Taktik SA, All rights reserved.
 */

package org.taktik.icure.services.external.rest.v2.controllers.core

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
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
import org.taktik.couchdb.entity.ComplexKey
import org.taktik.couchdb.entity.IdAndRev
import org.taktik.icure.asyncservice.AccessLogService
import org.taktik.icure.cache.ReactorCacheInjector
import org.taktik.icure.config.SharedPaginationConfig
import org.taktik.icure.db.PaginationOffset
import org.taktik.icure.pagination.PaginatedFlux
import org.taktik.icure.pagination.asPaginatedFlux
import org.taktik.icure.pagination.mapElements
import org.taktik.icure.services.external.rest.v2.dto.AccessLogDto
import org.taktik.icure.services.external.rest.v2.dto.ListOfIdsAndRevDto
import org.taktik.icure.services.external.rest.v2.dto.ListOfIdsDto
import org.taktik.icure.services.external.rest.v2.dto.couchdb.DocIdentifierDto
import org.taktik.icure.services.external.rest.v2.dto.filter.AbstractFilterDto
import org.taktik.icure.services.external.rest.v2.dto.requests.BulkShareOrUpdateMetadataParamsDto
import org.taktik.icure.services.external.rest.v2.dto.requests.EntityBulkShareResultDto
import org.taktik.icure.services.external.rest.v2.mapper.AccessLogV2Mapper
import org.taktik.icure.services.external.rest.v2.mapper.IdWithRevV2Mapper
import org.taktik.icure.services.external.rest.v2.mapper.couchdb.DocIdentifierV2Mapper
import org.taktik.icure.services.external.rest.v2.mapper.filter.FilterV2Mapper
import org.taktik.icure.services.external.rest.v2.mapper.requests.AccessLogBulkShareResultV2Mapper
import org.taktik.icure.services.external.rest.v2.mapper.requests.EntityShareOrMetadataUpdateRequestV2Mapper
import org.taktik.icure.utils.JsonString
import org.taktik.icure.utils.injectCachedReactorContext
import org.taktik.icure.utils.injectReactorContext
import org.taktik.icure.utils.orThrow
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

@RestController("accesslLogControllerV2")
@Profile("app")
@RequestMapping("/rest/v2/accesslog")
@Tag(name = "accessLog")
class AccessLogController(
	private val accessLogService: AccessLogService,
	private val accessLogV2Mapper: AccessLogV2Mapper,
	private val objectMapper: ObjectMapper,
	private val bulkShareResultV2Mapper: AccessLogBulkShareResultV2Mapper,
	private val entityShareOrMetadataUpdateRequestV2Mapper: EntityShareOrMetadataUpdateRequestV2Mapper,
	private val docIdentifierV2Mapper: DocIdentifierV2Mapper,
	private val reactorCacheInjector: ReactorCacheInjector,
	private val paginationConfig: SharedPaginationConfig,
	private val filterV2Mapper: FilterV2Mapper,
	private val idWithRevV2Mapper: IdWithRevV2Mapper,
) {

	@Operation(summary = "Creates an access log")
	@PostMapping
	fun createAccessLog(@RequestBody accessLogDto: AccessLogDto) = mono {
		val accessLog = accessLogService.createAccessLog(accessLogV2Mapper.map(accessLogDto))
			?: throw ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "AccessLog creation failed")
		accessLogV2Mapper.map(accessLog)
	}

	@Operation(summary = "Deletes multiple access logs")
	@PostMapping("/delete/batch")
	fun deleteAccessLogs(@RequestBody accessLogIds: ListOfIdsDto): Flux<DocIdentifierDto> =
		accessLogService.deleteAccessLogs(
			accessLogIds.ids.map { IdAndRev(it, null) }
		).map { docIdentifierV2Mapper.map(DocIdentifier(it.id, it.rev)) }.injectReactorContext()

	@Operation(summary = "Deletes multiple access log if they match the provided rev")
	@PostMapping("/delete/batch/withrev")
	fun deleteAccessLogsWithRev(@RequestBody accessLogIds: ListOfIdsAndRevDto): Flux<DocIdentifierDto> =
		accessLogService.deleteAccessLogs(
			accessLogIds.ids.map(idWithRevV2Mapper::map)
		).map { docIdentifierV2Mapper.map(DocIdentifier(it.id, it.rev)) }.injectReactorContext()

	@Operation(summary = "Deletes an Access Log")
	@DeleteMapping("/{accessLogId}")
	fun deleteAccessLog(
		@PathVariable accessLogId: String,
		@RequestParam(required = false) rev: String? = null
	): Mono<DocIdentifierDto> = mono {
		accessLogService.deleteAccessLog(accessLogId, rev).let {
			docIdentifierV2Mapper.map(DocIdentifier(it.id, it.rev))
		}
	}

	@PostMapping("/undelete/{accessLogId}")
	fun undeleteAccessLog(
		@PathVariable accessLogId: String,
		@RequestParam(required=true) rev: String
	): Mono<AccessLogDto> = mono {
		accessLogV2Mapper.map(accessLogService.undeleteAccessLog(accessLogId, rev))
	}

	@DeleteMapping("/purge/{accessLogId}")
	fun purgeAccessLog(
		@PathVariable accessLogId: String,
		@RequestParam(required=true) rev: String
	): Mono<DocIdentifierDto> = mono {
		accessLogService.purgeAccessLog(accessLogId, rev).let(docIdentifierV2Mapper::map)
	}

	@Operation(summary = "Gets an access log")
	@GetMapping("/{accessLogId}")
	fun getAccessLog(@PathVariable accessLogId: String) = mono {
		val accessLog = accessLogService.getAccessLog(accessLogId)
			?: throw ResponseStatusException(HttpStatus.NOT_FOUND, "AccessLog fetching failed")

		accessLogV2Mapper.map(accessLog)
	}

	@Operation(summary = "Get Paginated List of Access logs")
	@GetMapping
	fun findAccessLogsBy(
		@RequestParam(required = false) fromEpoch: Long?,
		@RequestParam(required = false) toEpoch: Long?,
		@RequestParam(required = false) startKey: Long?,
		@RequestParam(required = false) startDocumentId: String?,
		@RequestParam(required = false) limit: Int?,
		@RequestParam(required = false) descending: Boolean?
	): PaginatedFlux<AccessLogDto> {
		val paginationOffset = PaginationOffset(startKey, startDocumentId, null, limit ?: paginationConfig.defaultLimit)

		val (from, to) = when {
			descending == true && fromEpoch != null && toEpoch != null && fromEpoch >= toEpoch -> fromEpoch to toEpoch
			descending == true && fromEpoch != null && toEpoch != null && fromEpoch < toEpoch -> toEpoch to fromEpoch
			fromEpoch != null && toEpoch != null && fromEpoch >= toEpoch -> toEpoch to fromEpoch
			fromEpoch != null && toEpoch != null && fromEpoch < toEpoch -> fromEpoch to toEpoch
			descending == true -> (toEpoch ?: Long.MAX_VALUE) to (fromEpoch ?: 0)
			else -> (fromEpoch ?: 0) to (toEpoch ?: Long.MAX_VALUE)
		}

		return accessLogService
			.listAccessLogsBy(from, to, paginationOffset, descending == true)
			.mapElements(accessLogV2Mapper::map)
			.asPaginatedFlux()
	}

	@Operation(summary = "Get Paginated List of Access logs by user after date")
	@GetMapping("/byUser")
	fun findAccessLogsByUserAfterDate(
		@Parameter(description = "A User ID", required = true) @RequestParam userId: String,
		@Parameter(description = "The type of access (COMPUTER or USER)") @RequestParam(required = false) accessType: String?,
		@Parameter(description = "The start search epoch") @RequestParam(required = false) startDate: Long?,
		@Parameter(description = "The start key for pagination") @RequestParam(required = false) startKey: JsonString?,
		@Parameter(description = "A patient document ID") @RequestParam(required = false) startDocumentId: String?,
		@Parameter(description = "Number of rows") @RequestParam(required = false) limit: Int?,
		@Parameter(description = "Descending order") @RequestParam(required = false) descending: Boolean?
	): PaginatedFlux<AccessLogDto> {
		val startKeyElements = startKey?.let { objectMapper.readValue<ComplexKey>(startKey) }
		val paginationOffset = PaginationOffset(startKeyElements, startDocumentId, null, limit ?: paginationConfig.defaultLimit)
		return accessLogService.findAccessLogsByUserAfterDate(
			userId, accessType, startDate, paginationOffset, descending ?: false
		).mapElements(accessLogV2Mapper::map).asPaginatedFlux()
	}

	@Suppress("DEPRECATION")
	@Deprecated("This method is inefficient for high volumes of keys, use listAccessLogIdsByDataOwnerPatientDate instead")
	@Operation(summary = "List access logs found By Healthcare Party and secret foreign keyelementIds.")
	@GetMapping("/byHcPartySecretForeignKeys")
	fun listAccessLogsByHCPartyAndPatientForeignKeys(@RequestParam("hcPartyId") hcPartyId: String, @RequestParam("secretFKeys") secretFKeys: String) = flow {
		val secretPatientKeys = HashSet(secretFKeys.split(",")).toList()
		emitAll(accessLogService.listAccessLogsByHCPartyAndSecretPatientKeys(hcPartyId, secretPatientKeys).map { accessLogV2Mapper.map(it) })
	}.injectReactorContext()

	@Operation(summary = "Retrieves Access Logs ids by Data Owner id and Patient Foreign keys.")
	@PostMapping("/byDataOwnerPatientDate", produces = [APPLICATION_JSON_VALUE])
	fun listAccessLogIdsByDataOwnerPatientDate(
		@RequestParam dataOwnerId: String,
		@RequestParam(required = false) startDate: Long?,
		@RequestParam(required = false) endDate: Long?,
		@RequestParam(required = false) descending: Boolean?,
		@RequestBody secretPatientKeys: ListOfIdsDto
	): Flux<String> {
		require(secretPatientKeys.ids.isNotEmpty()) {
			"You need to provide at least one secret patient key"
		}
		return accessLogService
			.listAccessLogIdsByDataOwnerPatientDate(dataOwnerId, secretPatientKeys.ids.toSet(), startDate, endDate, descending ?: false)
			.injectReactorContext()
	}

	@Operation(summary = "Get AccessLogs by ids")
	@PostMapping("/byIds")
	fun getAccessLogByIds(@RequestBody accessLogIds: ListOfIdsDto): Flux<AccessLogDto> {
		require(accessLogIds.ids.isNotEmpty()) { "You must specify at least one id." }
		return accessLogService
			.getAccessLogs(accessLogIds.ids)
			.map(accessLogV2Mapper::map)
			.injectReactorContext()
	}

	@Suppress("DEPRECATION")
	@Deprecated("This method is inefficient for high volumes of keys, use listAccessLogIdsByDataOwnerPatientDate instead")
	@Operation(summary = "List access logs found by Healthcare Party and secret foreign key elementIds.")
	@PostMapping("/byHcPartySecretForeignKeys")
	fun findAccessLogsByHCPartyPatientForeignKeys(@RequestParam("hcPartyId") hcPartyId: String, @RequestBody secretPatientKeys: List<String>) = flow {
		emitAll(accessLogService.listAccessLogsByHCPartyAndSecretPatientKeys(hcPartyId, secretPatientKeys).map { accessLogV2Mapper.map(it) })
	}.injectReactorContext()

	@Operation(summary = "Modifies an access log")
	@PutMapping
	fun modifyAccessLog(@RequestBody accessLogDto: AccessLogDto) = mono {
		val accessLog = accessLogService.modifyAccessLog(accessLogV2Mapper.map(accessLogDto))
			?: throw ResponseStatusException(HttpStatus.NOT_FOUND, "AccessLog modification failed")
		accessLogV2Mapper.map(accessLog)
	}

	@Operation(description = "Shares one or more patients with one or more data owners")
	@PutMapping("/bulkSharedMetadataUpdate")
	fun bulkShare(
		@RequestBody request: BulkShareOrUpdateMetadataParamsDto
	): Flux<EntityBulkShareResultDto<AccessLogDto>> = flow {
		emitAll(accessLogService.bulkShareOrUpdateMetadata(
			entityShareOrMetadataUpdateRequestV2Mapper.map(request)
		).map { bulkShareResultV2Mapper.map(it) })
	}.injectCachedReactorContext(reactorCacheInjector, 50)

	@Operation(summary = "Get the ids of the Access Logs matching the provided filter")
	@PostMapping("/match", produces = [APPLICATION_JSON_VALUE])
	fun matchAccessLogsBy(
		@RequestBody filter: AbstractFilterDto<AccessLogDto>
	) = accessLogService.matchAccessLogsBy(
		filter = filterV2Mapper.tryMap(filter).orThrow()
	).injectReactorContext()
}
