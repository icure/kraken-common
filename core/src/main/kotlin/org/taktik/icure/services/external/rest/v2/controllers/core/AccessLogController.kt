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
import org.taktik.icure.asyncservice.AccessLogService
import org.taktik.icure.cache.ReactorCacheInjector
import org.taktik.icure.config.SharedPaginationConfig
import org.taktik.icure.db.PaginationOffset
import org.taktik.icure.pagination.PaginatedFlux
import org.taktik.icure.pagination.asPaginatedFlux
import org.taktik.icure.pagination.mapElements
import org.taktik.icure.services.external.rest.v2.dto.AccessLogDto
import org.taktik.icure.services.external.rest.v2.dto.ListOfIdsDto
import org.taktik.icure.services.external.rest.v2.dto.requests.BulkShareOrUpdateMetadataParamsDto
import org.taktik.icure.services.external.rest.v2.dto.requests.EntityBulkShareResultDto
import org.taktik.icure.services.external.rest.v2.mapper.AccessLogV2Mapper
import org.taktik.icure.services.external.rest.v2.mapper.requests.AccessLogBulkShareResultV2Mapper
import org.taktik.icure.services.external.rest.v2.mapper.requests.EntityShareOrMetadataUpdateRequestV2Mapper
import org.taktik.icure.utils.StartKeyJsonString
import org.taktik.icure.utils.injectReactorContext
import org.taktik.icure.utils.injectCachedReactorContext
import reactor.core.publisher.Flux

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
	private val reactorCacheInjector: ReactorCacheInjector,
	private val paginationConfig: SharedPaginationConfig
) {

	@Operation(summary = "Creates an access log")
	@PostMapping
	fun createAccessLog(@RequestBody accessLogDto: AccessLogDto) = mono {
		val accessLog = accessLogService.createAccessLog(accessLogV2Mapper.map(accessLogDto))
			?: throw ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "AccessLog creation failed")
		accessLogV2Mapper.map(accessLog)
	}

	@Operation(summary = "Deletes a multiple access logs")
	@PostMapping("/delete/batch")
	fun deleteAccessLogs(@RequestBody accessLogIds: ListOfIdsDto): Flux<DocIdentifier> =
			accessLogService.deleteAccessLogs(accessLogIds.ids).injectReactorContext()

	@Operation(summary = "Deletes an Access Log")
	@DeleteMapping("/{accessLogId}")
	fun deleteAccessLog(@PathVariable accessLogId: String) = mono {
		accessLogService.deleteAccessLog(accessLogId)
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
	): PaginatedFlux {
		val paginationOffset = PaginationOffset(startKey, startDocumentId, null, limit ?: paginationConfig.defaultLimit)
		return accessLogService
			.listAccessLogsBy(
				if (descending == true) toEpoch ?: Long.MAX_VALUE else fromEpoch ?: 0,
				if (descending == true) fromEpoch ?: 0 else toEpoch ?: Long.MAX_VALUE,
				paginationOffset,
				descending == true
			).mapElements(accessLogV2Mapper::map).asPaginatedFlux()
	}

	@Operation(summary = "Get Paginated List of Access logs by user after date")
	@GetMapping("/byUser")
	fun findAccessLogsByUserAfterDate(
		@Parameter(description = "A User ID", required = true) @RequestParam userId: String,
		@Parameter(description = "The type of access (COMPUTER or USER)") @RequestParam(required = false) accessType: String?,
		@Parameter(description = "The start search epoch") @RequestParam(required = false) startDate: Long?,
		@Parameter(description = "The start key for pagination") @RequestParam(required = false) startKey: StartKeyJsonString?,
		@Parameter(description = "A patient document ID") @RequestParam(required = false) startDocumentId: String?,
		@Parameter(description = "Number of rows") @RequestParam(required = false) limit: Int?,
		@Parameter(description = "Descending order") @RequestParam(required = false) descending: Boolean?
	): PaginatedFlux {
		val startKeyElements = startKey?.let { objectMapper.readValue<ComplexKey>(startKey) }
		val paginationOffset = PaginationOffset(startKeyElements, startDocumentId, null, limit ?: paginationConfig.defaultLimit)
		return accessLogService.findAccessLogsByUserAfterDate(
			userId, accessType, startDate, paginationOffset, descending ?: false
		).mapElements(accessLogV2Mapper::map).asPaginatedFlux()
	}

	@Operation(summary = "List access logs found By Healthcare Party and secret foreign keyelementIds.")
	@GetMapping("/byHcPartySecretForeignKeys")
	fun listAccessLogsByHCPartyAndPatientForeignKeys(@RequestParam("hcPartyId") hcPartyId: String, @RequestParam("secretFKeys") secretFKeys: String) = flow {
		val secretPatientKeys = HashSet(secretFKeys.split(",")).toList()
		emitAll(accessLogService.listAccessLogsByHCPartyAndSecretPatientKeys(hcPartyId, secretPatientKeys).map { accessLogV2Mapper.map(it) })
	}.injectReactorContext()

	@Operation(summary = "List access logs found by Healthcare Party and secret foreign keyelementIds.")
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
}
