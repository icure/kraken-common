package org.taktik.icure.services.external.rest.v1.controllers.core

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.tags.Tag
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.toList
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
import org.taktik.icure.asyncservice.DeviceService
import org.taktik.icure.config.SharedPaginationConfig
import org.taktik.icure.exceptions.NotFoundRequestException
import org.taktik.icure.services.external.rest.v1.dto.DeviceDto
import org.taktik.icure.services.external.rest.v1.dto.IdWithRevDto
import org.taktik.icure.services.external.rest.v1.dto.ListOfIdsDto
import org.taktik.icure.services.external.rest.v1.dto.couchdb.DocIdentifierDto
import org.taktik.icure.services.external.rest.v1.dto.filter.AbstractFilterDto
import org.taktik.icure.services.external.rest.v1.dto.filter.chain.FilterChain
import org.taktik.icure.services.external.rest.v1.mapper.DeviceMapper
import org.taktik.icure.services.external.rest.v1.mapper.couchdb.DocIdentifierMapper
import org.taktik.icure.services.external.rest.v1.mapper.filter.FilterChainMapper
import org.taktik.icure.services.external.rest.v1.mapper.filter.FilterMapper
import org.taktik.icure.services.external.rest.v1.utils.paginatedList
import org.taktik.icure.utils.injectReactorContext
import org.taktik.icure.utils.orThrow
import reactor.core.publisher.Flux

@RestController
@RequestMapping("/rest/v1/device")
@Tag(name = "device")
@Profile("app")
class DeviceController(
	private val deviceService: DeviceService,
	private val deviceMapper: DeviceMapper,
	private val filterChainMapper: FilterChainMapper,
	private val filterMapper: FilterMapper,
	private val docIdentifierMapper: DocIdentifierMapper,
	private val paginationConfig: SharedPaginationConfig
) {
	private val log = LoggerFactory.getLogger(javaClass)

	@Operation(summary = "Get Device", description = "It gets device administrative data.")
	@GetMapping("/{deviceId}")
	fun getDevice(@PathVariable deviceId: String) = mono {
		deviceService.getDevice(deviceId)?.let(deviceMapper::map)
			?: throw ResponseStatusException(HttpStatus.NOT_FOUND, "Getting device failed. Possible reasons: no such device exists, or server error. Please try again or read the server log.")
	}

	@Operation(summary = "Get devices by id", description = "It gets device administrative data.")
	@PostMapping("/byIds")
	fun getDevices(@RequestBody deviceIds: ListOfIdsDto): Flux<DeviceDto> =
		deviceService.getDevices(deviceIds.ids)
			.map { deviceMapper.map(it) }
			.injectReactorContext()

	@Suppress("DEPRECATION")
	@Operation(summary = "Get the HcParty encrypted AES keys indexed by owner", description = "(key, value) of the map is as follows: (ID of the owner of the encrypted AES key, encrypted AES key)", deprecated = true)
	@GetMapping("/{deviceId}/keys")
	fun getDeviceHcPartyKeysForDelegate(@Parameter(description = "The deviceId Id for which information is shared") @PathVariable deviceId: String) = mono {
		deviceService.getHcPartyKeysForDelegate(deviceId)
	}

	@Operation(
		summary = "Get the HcParty encrypted AES keys indexed by owner.",
		description = "(key, value) of the map is as follows: (ID of the owner of the encrypted AES key, encrypted AES keys)"
	)
	@GetMapping("/{deviceId}/aesExchangeKeys")
	fun getDeviceAesExchangeKeysForDelegate(@PathVariable deviceId: String) = mono {
		deviceService.getAesExchangeKeysForDelegate(deviceId)
	}

	@Operation(summary = "Create a device", description = "Name, last name, date of birth, and gender are required. After creation of the device and obtaining the ID, you need to create an initial delegation.")
	@PostMapping
	fun createDevice(@RequestBody p: DeviceDto) = mono {
		deviceService.createDevice(deviceMapper.map(p))?.let(deviceMapper::map)
			?: throw ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Device creation failed.")
	}

	@Operation(summary = "Modify a device", description = "Returns the updated device")
	@PutMapping
	fun updateDevice(@RequestBody deviceDto: DeviceDto) = mono {
		deviceService.modifyDevice(deviceMapper.map(deviceDto))?.let(deviceMapper::map)
			?: throw ResponseStatusException(HttpStatus.NOT_FOUND, "Getting device failed. Possible reasons: no such device exists, or server error. Please try again or read the server log.").also { log.error(it.message) }
	}

	@Operation(summary = "Create devices in bulk", description = "Returns the id and _rev of created devices")
	@PostMapping("/bulk", "/batch")
	fun createDevices(@RequestBody deviceDtos: List<DeviceDto>) = mono {
		val devices = deviceService.createDevices(deviceDtos.map(deviceMapper::map).toList())
		devices.map { p -> IdWithRevDto(id = p.id, rev = p.rev) }.toList()
	}

	@Operation(summary = "Modify devices in bulk", description = "Returns the id and _rev of modified devices")
	@PutMapping("/bulk", "/batch")
	fun updateDevices(@RequestBody deviceDtos: List<DeviceDto>) = mono {
		val devices = deviceService.modifyDevices(deviceDtos.map(deviceMapper::map).toList())
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
			.filterDevices(filterChainMapper.tryMap(filterChain).orThrow(), realLimit + 1, startDocumentId)
			.paginatedList(deviceMapper::map, realLimit)
	}

	@Operation(summary = "Get ids of devices matching the provided filter for the current user (HcParty) ")
	@PostMapping("/match", produces = [APPLICATION_JSON_VALUE])
	fun matchDevicesBy(@RequestBody filter: AbstractFilterDto<DeviceDto>) =
		deviceService.matchDevicesBy(filterMapper.tryMap(filter).orThrow()).injectReactorContext()

	@Operation(summary = "Delete device.", description = "Response contains the id/rev of deleted device.")
	@DeleteMapping("/{deviceId}")
	fun deleteDevice(@PathVariable deviceId: String) = mono {
		deviceService.deleteDevice(deviceId)
			?.let(docIdentifierMapper::map)
			?: throw NotFoundRequestException("Device not found")
	}

	@Operation(summary = "Delete devices.", description = "Response is an array containing the id/rev of deleted devices.")
	@PostMapping("/delete/batch")
	fun deleteDevices(@RequestBody deviceIds: ListOfIdsDto): Flux<DocIdentifierDto> =
		deviceService.deleteDevices(deviceIds.ids.toSet())
			.map(docIdentifierMapper::map)
			.injectReactorContext()
}