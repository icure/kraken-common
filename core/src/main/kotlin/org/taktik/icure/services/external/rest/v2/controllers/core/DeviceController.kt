package org.taktik.icure.services.external.rest.v2.controllers.core

import com.fasterxml.jackson.databind.ObjectMapper
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.tags.Tag
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
import org.taktik.icure.asyncservice.DeviceService
import org.taktik.icure.cache.ReactorCacheInjector
import org.taktik.icure.config.SharedPaginationConfig
import org.taktik.icure.services.external.rest.v2.dto.DeviceDto
import org.taktik.icure.services.external.rest.v2.dto.ListOfIdsAndRevDto
import org.taktik.icure.services.external.rest.v2.dto.ListOfIdsDto
import org.taktik.icure.services.external.rest.v2.dto.PaginatedList
import org.taktik.icure.services.external.rest.v2.dto.couchdb.DocIdentifierDto
import org.taktik.icure.services.external.rest.v2.dto.filter.AbstractFilterDto
import org.taktik.icure.services.external.rest.v2.dto.filter.chain.FilterChain
import org.taktik.icure.services.external.rest.v2.dto.specializations.AesExchangeKeyEncryptionKeypairIdentifierDto
import org.taktik.icure.services.external.rest.v2.dto.specializations.HexStringDto
import org.taktik.icure.services.external.rest.v2.mapper.DeviceV2Mapper
import org.taktik.icure.services.external.rest.v2.mapper.IdWithRevV2Mapper
import org.taktik.icure.services.external.rest.v2.mapper.couchdb.DocIdentifierV2Mapper
import org.taktik.icure.services.external.rest.v2.mapper.filter.FilterChainV2Mapper
import org.taktik.icure.services.external.rest.v2.mapper.filter.FilterV2Mapper
import org.taktik.icure.services.external.rest.v2.utils.paginatedList
import org.taktik.icure.utils.injectCachedReactorContext
import org.taktik.icure.utils.injectReactorContext
import org.taktik.icure.utils.orThrow
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

@RestController("deviceControllerV2")
@RequestMapping("/rest/v2/device")
@Tag(name = "device")
@Profile("app")
class DeviceController(
	private val deviceService: DeviceService,
	private val deviceV2Mapper: DeviceV2Mapper,
	private val filterChainV2Mapper: FilterChainV2Mapper,
	private val filterV2Mapper: FilterV2Mapper,
	private val docIdentifierV2Mapper: DocIdentifierV2Mapper,
	private val paginationConfig: SharedPaginationConfig,
	private val idWithRevV2Mapper: IdWithRevV2Mapper,
	private val objectMapper: ObjectMapper,
	private val reactorCacheInjector: ReactorCacheInjector,
) {

	@Operation(summary = "Get Device", description = "It gets device administrative data.")
	@GetMapping("/{deviceId}")
	fun getDevice(
		@PathVariable deviceId: String,
	): Mono<DeviceDto> = mono {
		deviceService.getDevice(deviceId)?.let(deviceV2Mapper::map)
			?: throw ResponseStatusException(
				HttpStatus.NOT_FOUND,
				"Getting device failed. Possible reasons: no such device exists, or server error. Please try again or read the server log.",
			)
	}

	@Operation(summary = "Get devices by id", description = "It gets device administrative data.")
	@PostMapping("/byIds")
	fun getDevices(
		@RequestBody deviceIds: ListOfIdsDto,
	): Flux<DeviceDto> = deviceService
		.getDevices(deviceIds.ids)
		.map { deviceV2Mapper.map(it) }
		.injectReactorContext()

	@Operation(
		summary = "Create a device",
		description = "Name, last name, date of birth, and gender are required. After creation of the device and obtaining the ID, you need to create an initial delegation.",
	)
	@PostMapping
	fun createDevice(
		@RequestBody p: DeviceDto,
	): Mono<DeviceDto> = mono {
		deviceV2Mapper.map(deviceService.createDevice(deviceV2Mapper.map(p)))
	}

	@Operation(summary = "Modify a device", description = "Returns the updated device")
	@PutMapping
	fun updateDevice(
		@RequestBody deviceDto: DeviceDto,
	): Mono<DeviceDto> = mono {
		deviceService.modifyDevice(deviceV2Mapper.map(deviceDto)).let(deviceV2Mapper::map)
	}

	@Operation(summary = "Create devices in bulk", description = "Returns the id and _rev of created devices")
	@PostMapping("/bulk", "/batch")
	fun createDevices(
		@RequestBody deviceDtos: List<DeviceDto>,
	): Flux<DeviceDto> = deviceService
		.createDevices(deviceDtos.map(deviceV2Mapper::map))
		.map(deviceV2Mapper::map)
		.injectReactorContext()

	@Operation(summary = "Modify devices in bulk", description = "Returns the id and _rev of modified devices")
	@PutMapping("/bulk", "/batch")
	fun updateDevices(
		@RequestBody deviceDtos: List<DeviceDto>,
	): Flux<DeviceDto> = deviceService
		.modifyDevices(deviceDtos.map(deviceV2Mapper::map))
		.map(deviceV2Mapper::map)
		.injectReactorContext()

	@Operation(
		summary = "Filter devices for the current user (HcParty) ",
		description = "Returns a list of devices along with next start keys and Document ID. If the nextStartKey is Null it means that this is the last page.",
	)
	@PostMapping("/filter")
	fun filterDevicesBy(
		@Parameter(description = "A device document ID") @RequestParam(required = false) startDocumentId: String?,
		@Parameter(description = "Number of rows") @RequestParam(required = false) limit: Int?,
		@RequestBody filterChain: FilterChain<DeviceDto>,
	): Mono<PaginatedList<DeviceDto>> = mono {
		val realLimit = limit ?: paginationConfig.defaultLimit

		deviceService
			.filterDevices(filterChainV2Mapper.tryMap(filterChain).orThrow(), realLimit + 1, startDocumentId)
			.paginatedList(deviceV2Mapper::map, realLimit, objectMapper = objectMapper)
	}

	@Operation(
		summary = "Get the HcParty encrypted AES keys indexed by owner.",
		description = "(key, value) of the map is as follows: (ID of the owner of the encrypted AES key, encrypted AES keys)",
	)
	@GetMapping("/{deviceId}/aesExchangeKeys")
	fun getDeviceAesExchangeKeysForDelegate(
		@PathVariable deviceId: String,
	): Mono<Map<String, Map<String, Map<AesExchangeKeyEncryptionKeypairIdentifierDto, HexStringDto>>>> = mono {
		deviceService.getAesExchangeKeysForDelegate(deviceId)
	}

	@Operation(summary = "Get the ids of the Devices matching the provided filter.")
	@PostMapping("/match", produces = [APPLICATION_JSON_VALUE])
	fun matchDevicesBy(
		@RequestBody filter: AbstractFilterDto<DeviceDto>,
	): Flux<String> = deviceService
		.matchDevicesBy(
			filter = filterV2Mapper.tryMap(filter).orThrow(),
		).injectReactorContext()

	@Operation(summary = "Deletes multiple Devices")
	@PostMapping("/delete/batch")
	fun deleteDevices(
		@RequestBody deviceIds: ListOfIdsDto,
	): Flux<DocIdentifierDto> = deviceService
		.deleteDevices(
			deviceIds.ids.map { IdAndRev(it, null) },
		).map { docIdentifierV2Mapper.map(DocIdentifier(it.id, it.rev)) }
		.injectCachedReactorContext(reactorCacheInjector, 100)

	@Operation(summary = "Deletes a multiple Devices if they match the provided revs")
	@PostMapping("/delete/batch/withrev")
	fun deleteDevicesWithRev(
		@RequestBody deviceIds: ListOfIdsAndRevDto,
	): Flux<DocIdentifierDto> = deviceService
		.deleteDevices(
			deviceIds.ids.map(idWithRevV2Mapper::map),
		).map { docIdentifierV2Mapper.map(DocIdentifier(it.id, it.rev)) }
		.injectCachedReactorContext(reactorCacheInjector, 100)

	@Operation(summary = "Delete a Device")
	@DeleteMapping("/{deviceId}")
	fun deleteDevice(
		@PathVariable deviceId: String,
		@RequestParam(required = false) rev: String? = null,
	): Mono<DocIdentifierDto> = reactorCacheInjector.monoWithCachedContext(10) {
		deviceService.deleteDevice(deviceId, rev).let {
			docIdentifierV2Mapper.map(DocIdentifier(it.id, it.rev))
		}
	}

	@PostMapping("/undelete/{deviceId}")
	fun undeleteDevice(
		@PathVariable deviceId: String,
		@RequestParam(required = true) rev: String,
	): Mono<DeviceDto> = reactorCacheInjector.monoWithCachedContext(10) {
		deviceV2Mapper.map(deviceService.undeleteDevice(deviceId, rev))
	}

	@PostMapping("/undelete/batch")
	fun undeleteDevices(
		@RequestBody deviceIds: ListOfIdsAndRevDto,
	): Flux<DeviceDto> = deviceService
		.undeleteDevices(
			deviceIds.ids.map(idWithRevV2Mapper::map),
		).map(deviceV2Mapper::map)
		.injectCachedReactorContext(reactorCacheInjector, 100)

	@DeleteMapping("/purge/{deviceId}")
	fun purgeDevice(
		@PathVariable deviceId: String,
		@RequestParam(required = true) rev: String,
	): Mono<DocIdentifierDto> = reactorCacheInjector.monoWithCachedContext(10) {
		deviceService.purgeDevice(deviceId, rev).let(docIdentifierV2Mapper::map)
	}

	@PostMapping("/purge/batch")
	fun purgeDevices(
		@RequestBody deviceIds: ListOfIdsAndRevDto,
	): Flux<DocIdentifierDto> = deviceService
		.purgeDevices(
			deviceIds.ids.map(idWithRevV2Mapper::map),
		).map(docIdentifierV2Mapper::map)
		.injectCachedReactorContext(reactorCacheInjector, 100)
}
