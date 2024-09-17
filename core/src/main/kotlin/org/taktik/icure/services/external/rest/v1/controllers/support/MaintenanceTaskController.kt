/*
 * Copyright (c) 2020. Taktik SA, All rights reserved.
 */

package org.taktik.icure.services.external.rest.v1.controllers.support

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.tags.Tag
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.reactor.mono
import org.springframework.context.annotation.Profile
import org.springframework.http.HttpStatus
import org.springframework.security.access.AccessDeniedException
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
import org.taktik.couchdb.entity.IdAndRev
import org.taktik.icure.asyncservice.MaintenanceTaskService
import org.taktik.icure.config.SharedPaginationConfig
import org.taktik.icure.db.PaginationOffset
import org.taktik.icure.services.external.rest.v1.dto.MaintenanceTaskDto
import org.taktik.icure.services.external.rest.v1.dto.filter.chain.FilterChain
import org.taktik.icure.services.external.rest.v1.mapper.MaintenanceTaskMapper
import org.taktik.icure.services.external.rest.v1.mapper.filter.FilterChainMapper
import org.taktik.icure.services.external.rest.v1.utils.paginatedList
import org.taktik.icure.utils.injectReactorContext
import org.taktik.icure.utils.orThrow

@RestController
@Profile("app")
@RequestMapping("/rest/v1/maintenancetask")
@Tag(name = "maintenanceTask")
class MaintenanceTaskController(
	private val maintenanceTaskService: MaintenanceTaskService,
	private val maintenanceTaskMapper: MaintenanceTaskMapper,
	private val filterChainMapper: FilterChainMapper,
	private val paginationConfig: SharedPaginationConfig
) {

	@Operation(summary = "Creates a maintenanceTask")
	@PostMapping
	fun createMaintenanceTask(@RequestBody maintenanceTaskDto: MaintenanceTaskDto) = mono {
		maintenanceTaskService.createMaintenanceTask(maintenanceTaskMapper.map(maintenanceTaskDto))
			?.let {
				maintenanceTaskMapper.map(it)
			} ?: throw ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "MaintenanceTask creation failed.")
	}

	@Operation(summary = "Delete maintenanceTasks")
	@DeleteMapping("/{maintenanceTaskIds}")
	fun deleteMaintenanceTask(@PathVariable maintenanceTaskIds: String) =
		maintenanceTaskService.deleteMaintenanceTasks(maintenanceTaskIds.split(',').map { IdAndRev(it, null) })
			.catch { e ->
				if (e is AccessDeniedException)
					throw ResponseStatusException(HttpStatus.FORBIDDEN, e.message)
				throw ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "MaintenanceTask deletion failed.")
			}
			.injectReactorContext()

	@Operation(summary = "Gets a maintenanceTask")
	@GetMapping("/{maintenanceTaskId}")
	fun getMaintenanceTask(@PathVariable maintenanceTaskId: String) = mono {
		maintenanceTaskService.getMaintenanceTask(maintenanceTaskId)?.let(maintenanceTaskMapper::map)
			?: throw ResponseStatusException(HttpStatus.NOT_FOUND, "MaintenanceTask not found")
	}

	@Operation(summary = "Updates a maintenanceTask")
	@PutMapping
	fun modifyMaintenanceTask(@RequestBody maintenanceTaskDto: MaintenanceTaskDto) = mono {
		maintenanceTaskService.modifyMaintenanceTask(maintenanceTaskMapper.map(maintenanceTaskDto))?.let{ maintenanceTaskMapper.map(it) }
			?: throw ResponseStatusException(HttpStatus.NOT_FOUND, "MaintenanceTask modification failed.")
	}

	@Operation(summary = "Filter maintenanceTasks for the current user (HcParty) ", description = "Returns a list of maintenanceTasks along with next start keys and Document ID. If the nextStartKey is Null it means that this is the last page.")
	@PostMapping("/filter")
	fun filterMaintenanceTasksBy(
		@Parameter(description = "A maintenanceTask document ID") @RequestParam(required = false) startDocumentId: String?,
		@Parameter(description = "Number of rows") @RequestParam(required = false) limit: Int?,
		@RequestBody filterChain: FilterChain<MaintenanceTaskDto>
	) = mono {
		val realLimit = limit ?: paginationConfig.defaultLimit
		val paginationOffset = PaginationOffset(null, startDocumentId, null, realLimit + 1)

		maintenanceTaskService
			.filterMaintenanceTasks(paginationOffset, filterChainMapper.tryMap(filterChain).orThrow())
			.paginatedList(maintenanceTaskMapper::map, realLimit)
	}
}