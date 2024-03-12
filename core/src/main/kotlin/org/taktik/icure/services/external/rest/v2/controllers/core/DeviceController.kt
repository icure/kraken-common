package org.taktik.icure.services.external.rest.v2.controllers.core

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.tags.Tag
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.toList
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
import org.taktik.couchdb.exception.DocumentNotFoundException
import org.taktik.icure.asynclogic.impl.filter.Filters
import org.taktik.icure.asyncservice.DeviceService
import org.taktik.icure.config.SharedPaginationConfig
import org.taktik.icure.exceptions.NotFoundRequestException
import org.taktik.icure.services.external.rest.v2.dto.DeviceDto
import org.taktik.icure.services.external.rest.v2.dto.IdWithRevDto
import org.taktik.icure.services.external.rest.v2.dto.ListOfIdsDto
import org.taktik.icure.services.external.rest.v2.dto.couchdb.DocIdentifierDto
import org.taktik.icure.services.external.rest.v2.dto.filter.AbstractFilterDto
import org.taktik.icure.services.external.rest.v2.dto.filter.chain.FilterChain
import org.taktik.icure.services.external.rest.v2.mapper.DeviceV2Mapper
import org.taktik.icure.services.external.rest.v2.mapper.couchdb.DocIdentifierV2Mapper
import org.taktik.icure.services.external.rest.v2.mapper.filter.FilterChainV2Mapper
import org.taktik.icure.services.external.rest.v2.mapper.filter.FilterV2Mapper
import org.taktik.icure.utils.orThrow
import org.taktik.icure.services.external.rest.v2.utils.paginatedList
import org.taktik.icure.utils.injectReactorContext
import reactor.core.publisher.Flux

@RestController("deviceControllerV2")
@RequestMapping("/rest/v2/device")
@Tag(name = "device")
@Profile("app")
class DeviceController(
	private val filters: Filters,
	private val deviceService: DeviceService,
	private val deviceV2Mapper: DeviceV2Mapper,
	private val filterChainV2Mapper: FilterChainV2Mapper,
	private val filterV2Mapper: FilterV2Mapper,
	private val docIdentifierV2Mapper: DocIdentifierV2Mapper,
	private val paginationConfig: SharedPaginationConfig,
) {
	companion object {
		private val log = LoggerFactory.getLogger(DeviceController::class.java)
	}

	@Operation(summary = "Get Device", description = "It gets device administrative data.")
	@GetMapping("/{deviceId}")
	fun getDevice(@PathVariable deviceId: String) = mono {
		deviceService.getDevice(deviceId)?.let(deviceV2Mapper::map)
			?: throw ResponseStatusException(HttpStatus.NOT_FOUND, "Getting device failed. Possible reasons: no such device exists, or server error. Please try again or read the server log.")
	}

	@Operation(summary = "Get devices by id", description = "It gets device administrative data.")
	@PostMapping("/byIds")
	fun getDevices(@RequestBody deviceIds: ListOfIdsDto): Flux<DeviceDto> =
		deviceService.getDevices(deviceIds.ids)
			.map { deviceV2Mapper.map(it) }
			.injectReactorContext()

	@Operation(summary = "Create a device", description = "Name, last name, date of birth, and gender are required. After creation of the device and obtaining the ID, you need to create an initial delegation.")
	@PostMapping
	fun createDevice(@RequestBody p: DeviceDto) = mono {
		deviceService.createDevice(deviceV2Mapper.map(p))?.let(deviceV2Mapper::map)
			?: throw ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Device creation failed.")
	}

	@Operation(summary = "Modify a device", description = "Returns the updated device")
	@PutMapping
	fun updateDevice(@RequestBody deviceDto: DeviceDto) = mono {
		deviceService.modifyDevice(deviceV2Mapper.map(deviceDto))?.let(deviceV2Mapper::map)
			?: throw DocumentNotFoundException("Getting device failed. Possible reasons: no such device exists, or server error. Please try again or read the server log.").also { log.error(it.message) }
	}

	@Operation(summary = "Create devices in bulk", description = "Returns the id and _rev of created devices")
	@PostMapping("/bulk", "/batch")
	fun createDevices(@RequestBody deviceDtos: List<DeviceDto>) = mono {
		val devices = deviceService.createDevices(deviceDtos.map(deviceV2Mapper::map).toList())
		devices.map { p -> IdWithRevDto(id = p.id, rev = p.rev) }.toList()
	}

	@Operation(summary = "Modify devices in bulk", description = "Returns the id and _rev of modified devices")
	@PutMapping("/bulk", "/batch")
	fun updateDevices(@RequestBody deviceDtos: List<DeviceDto>) = mono {
		val devices = deviceService.modifyDevices(deviceDtos.map(deviceV2Mapper::map).toList())
		devices.map { p -> IdWithRevDto(id = p.id, rev = p.rev) }.toList()
	}

	@Operation(summary = "Filter devices for the current user (HcParty) ", description = "Returns a list of devices along with next start keys and Document ID. If the nextStartKey is Null it means that this is the last page.")
	@PostMapping("/filter")
	fun filterDevicesBy(
		@Parameter(description = "A device document ID") @RequestParam(required = false) startDocumentId: String?,
		@Parameter(description = "Number of rows") @RequestParam(required = false) limit: Int?,
		@RequestBody filterChain: FilterChain<DeviceDto>
	) = mono {
		val realLimit = limit ?: paginationConfig.defaultLimit

		deviceService
			.filterDevices(filterChainV2Mapper.tryMap(filterChain).orThrow(), realLimit + 1, startDocumentId)
			.paginatedList(deviceV2Mapper::map, realLimit)
	}

	@Operation(
		summary = "Get the HcParty encrypted AES keys indexed by owner.",
		description = "(key, value) of the map is as follows: (ID of the owner of the encrypted AES key, encrypted AES keys)"
	)
	@GetMapping("/{deviceId}/aesExchangeKeys")
	fun getDeviceAesExchangeKeysForDelegate(@PathVariable deviceId: String) = mono {
		deviceService.getAesExchangeKeysForDelegate(deviceId)
	}

	@Operation(summary = "Get ids of devices matching the provided filter for the current user (HcParty) ")
	@PostMapping("/match")
	fun matchDevicesBy(@RequestBody filter: AbstractFilterDto<DeviceDto>) = mono {
		filters.resolve(filterV2Mapper.tryMap(filter).orThrow()).toList()
	}

	@Operation(summary = "Delete device.", description = "Response contains the id/rev of deleted device.")
	@DeleteMapping("/{deviceId}")
	fun deleteDevice(@PathVariable deviceId: String) = mono {
		deviceService.deleteDevice(deviceId) ?: throw NotFoundRequestException("Device not found")
	}

	@Operation(summary = "Delete devices.", description = "Response is an array containing the id/rev of deleted devices.")
	@PostMapping("/delete/batch")
	fun deleteDevices(@RequestBody deviceIds: ListOfIdsDto): Flux<DocIdentifierDto> {
		return try {
			deviceService.deleteDevices(deviceIds.ids.toSet()).map(docIdentifierV2Mapper::map).injectReactorContext()
		} catch (e: Exception) {
			throw ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Devices deletion failed").also { log.error(it.message) }
		}
	}
}