/*
 * Copyright (c) 2020. Taktik SA, All rights reserved.
 */

package org.taktik.icure.services.external.rest.v1.controllers.core

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
import org.taktik.icure.config.SharedPaginationConfig
import org.taktik.icure.db.PaginationOffset
import org.taktik.icure.pagination.PaginatedFlux
import org.taktik.icure.pagination.asPaginatedFlux
import org.taktik.icure.pagination.mapElements
import org.taktik.icure.services.external.rest.v1.dto.AccessLogDto
import org.taktik.icure.services.external.rest.v1.mapper.AccessLogMapper
import org.taktik.icure.utils.JsonString
import org.taktik.icure.utils.injectReactorContext
import reactor.core.publisher.Flux

@RestController
@Profile("app")
@RequestMapping("/rest/v1/accesslog")
@Tag(name = "accesslog")
class AccessLogController(
	private val accessLogService: AccessLogService,
	private val accessLogMapper: AccessLogMapper,
	private val objectMapper: ObjectMapper,
	private val paginationConfig: SharedPaginationConfig
) {

	@Operation(summary = "Create an access log")
	@PostMapping
	fun createAccessLog(@RequestBody accessLogDto: AccessLogDto) = mono {
		val accessLog = accessLogService.createAccessLog(accessLogMapper.map(accessLogDto))
			?: throw ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "AccessLog creation failed")
		accessLogMapper.map(accessLog)
	}

	@Operation(summary = "Delete access logs by batch")
	@DeleteMapping("/{accessLogIds}")
	fun deleteAccessLog(@PathVariable accessLogIds: String): Flux<DocIdentifier> {
		return accessLogService.deleteAccessLogs(accessLogIds.split(',')).injectReactorContext()
	}

	@Operation(summary = "Get an access log")
	@GetMapping("/{accessLogId}")
	fun getAccessLog(@PathVariable accessLogId: String) = mono {
		val accessLog = accessLogService.getAccessLog(accessLogId)
			?: throw ResponseStatusException(HttpStatus.NOT_FOUND, "AccessLog fetching failed")

		accessLogMapper.map(accessLog)
	}

	@Operation(summary = "Get paginated list of Access Logs")
	@GetMapping
	fun listAccessLogs(
		@RequestParam(required = false) fromEpoch: Long?,
		@RequestParam(required = false) toEpoch: Long?,
		@RequestParam(required = false) startKey: Long?,
		@RequestParam(required = false) startDocumentId: String?,
		@RequestParam(required = false) limit: Int?,
		@RequestParam(required = false) descending: Boolean?
	): PaginatedFlux<AccessLogDto> {
		val paginationOffset = PaginationOffset(startKey, startDocumentId, null, limit ?: paginationConfig.defaultLimit)
		return accessLogService.listAccessLogsBy(
			if (descending == true) toEpoch ?: Long.MAX_VALUE else fromEpoch ?: 0,
			if (descending == true) fromEpoch ?: 0 else toEpoch ?: Long.MAX_VALUE,
			paginationOffset,
			descending == true
		).mapElements(accessLogMapper::map).asPaginatedFlux()
	}

	@Operation(summary = "Get paginated list of Access Logs")
	@GetMapping("/byUser")
	fun findByUserAfterDate(
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
		return accessLogService
			.findAccessLogsByUserAfterDate(userId, accessType, startDate, paginationOffset, descending ?: false)
			.mapElements(accessLogMapper::map)
			.asPaginatedFlux()
	}

	@Operation(summary = "List access logs found by Healthcare Party id and secret foreign key element ids.")
	@GetMapping("/byHcPartySecretForeignKeys")
	fun findAccessLogsByHCPartyPatientForeignKeys(
		@RequestParam("hcPartyId") hcPartyId: String,
		@RequestParam("secretFKeys") secretFKeys: String
	) = flow {
		val secretPatientKeys = HashSet(secretFKeys.split(",")).toList()
		emitAll(accessLogService.listAccessLogsByHCPartyAndSecretPatientKeys(hcPartyId, secretPatientKeys).map { accessLogMapper.map(it) })
	}.injectReactorContext()

	@Operation(summary = "List access logs found by Healthcare Party id and secret foreign key element id with pagination.")
	@GetMapping("/byHcPartySecretForeignKey")
	fun findAccessLogsByHCPartyPatientForeignKey(
		@RequestParam("hcPartyId") hcPartyId: String,
		@RequestParam("secretFKey") secretFKey: String,
		@Parameter(description = "The start key for pagination") @RequestParam(required = false) startKey: JsonString?,
		@Parameter(description = "A patient document ID") @RequestParam(required = false) startDocumentId: String?,
		@Parameter(description = "Number of rows") @RequestParam(required = false) limit: Int?,
	): PaginatedFlux<AccessLogDto> {
		val startKeyElements = startKey?.let { objectMapper.readValue<ComplexKey>(startKey) }
		val paginationOffset = PaginationOffset(startKeyElements, startDocumentId, null, limit ?: paginationConfig.defaultLimit)
		return accessLogService
			.listAccessLogsBySearchKeyAndSecretPatientKey(hcPartyId, secretFKey, paginationOffset)
			.mapElements(accessLogMapper::map)
			.asPaginatedFlux()
	}

	@Operation(summary = "List access logs found by Healthcare Party and secret foreign key element ids.")
	@PostMapping("/byHcPartySecretForeignKeys")
	fun findAccessLogsByHCPartyPatientForeignKeys(
		@RequestParam("hcPartyId") hcPartyId: String,
		@RequestBody secretPatientKeys: List<String>
	) = flow {
		emitAll(accessLogService.listAccessLogsByHCPartyAndSecretPatientKeys(hcPartyId, secretPatientKeys).map { accessLogMapper.map(it) })
	}.injectReactorContext()

	@Operation(summary = "Modifies an access log")
	@PutMapping
	fun modifyAccessLog(@RequestBody accessLogDto: AccessLogDto) = mono {
		val accessLog = accessLogService.modifyAccessLog(accessLogMapper.map(accessLogDto))
			?: throw ResponseStatusException(HttpStatus.NOT_FOUND, "AccessLog modification failed")
		accessLogMapper.map(accessLog)
	}
}