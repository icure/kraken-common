package org.taktik.icure.services.external.rest.v2.controllers.core

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.reactor.awaitSingle
import kotlinx.coroutines.reactor.mono
import org.springframework.context.annotation.Profile
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import org.taktik.icure.asynclogic.SessionInformationProvider
import org.taktik.icure.asyncservice.DataOwnerService
import org.taktik.icure.domain.customentities.util.CachedCustomEntitiesConfigurationProvider
import org.taktik.icure.domain.customentities.util.CustomEntityConfigResolutionContext
import org.taktik.icure.entities.DataOwnerWithType
import org.taktik.icure.exceptions.NotFoundRequestException
import org.taktik.icure.services.external.rest.v2.dto.CryptoActorStubWithTypeDto
import org.taktik.icure.services.external.rest.v2.dto.DataOwnerWithTypeDto
import org.taktik.icure.services.external.rest.v2.dto.ListOfIdsDto
import org.taktik.icure.services.external.rest.v2.mapper.CryptoActorStubV2Mapper
import org.taktik.icure.services.external.rest.v2.mapper.DataOwnerWithTypeV2Mapper
import org.taktik.icure.services.external.rest.v2.mapper.PatientV2Mapper
import org.taktik.icure.utils.injectReactorContext
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

@RestController("dataOwnerControllerV2")
@Profile("app")
@RequestMapping("/rest/v2/dataowner")
@Tag(name = "dataowner")
class DataOwnerController(
	private val dataOwnerService: DataOwnerService,
	private val sessionLogic: SessionInformationProvider,
	private val dataOwnerWithTypeMapper: DataOwnerWithTypeV2Mapper,
	private val patientMapper: PatientV2Mapper,
	private val cryptoActorStubMapper: CryptoActorStubV2Mapper,
	private val customEntitiesConfigurationProvider: CachedCustomEntitiesConfigurationProvider,
) {
	private suspend fun DataOwnerWithType.map(): DataOwnerWithTypeDto {
		val config = customEntitiesConfigurationProvider.getConfigForCurrentUser()
		val patientExtensions = config?.extensions?.patient
		return dataOwnerWithTypeMapper.map(
			this,
			mapPatientForRead = if (patientExtensions != null) ({ p ->
				patientMapper.map(p) {
					if (it != null) patientExtensions.mapValueForRead(CustomEntityConfigResolutionContext.ofConfig(config), it) else null
				}
			}) else ({ p ->
				patientMapper.map(p) { it }
			})
		)
	}

	private fun Flow<DataOwnerWithType>.map(): Flow<DataOwnerWithTypeDto> = flow {
		val config = customEntitiesConfigurationProvider.getConfigForCurrentUser()
		val patientExtensions = config?.extensions?.patient
		if (patientExtensions != null) {
			val context = CustomEntityConfigResolutionContext.ofConfig(config)
			emitAll(map { dataOwner ->
				dataOwnerWithTypeMapper.map(
					dataOwner,
					mapPatientForRead = { p ->
						patientMapper.map(p) {
							if (it != null) patientExtensions.mapValueForRead(context, it) else null
						}
					}
				)
			})
		} else {
			emitAll(map { dataOwner ->
				dataOwnerWithTypeMapper.map(
					dataOwner,
					mapPatientForRead = { p ->
						patientMapper.map(p) { it }
					}
				)
			})
		}
	}

	private suspend fun currentDataOwnerOr404(): String =
		sessionLogic.getCurrentDataOwnerIdOrNull() ?: throw NotFoundRequestException("Current user is not a data owner")

	@Operation(summary = "Get a data owner by his ID", description = "General information about the data owner")
	@GetMapping("/{dataOwnerId}")
	fun getDataOwner(
		@PathVariable dataOwnerId: String,
	): Mono<DataOwnerWithTypeDto> = mono {
		dataOwnerService.getDataOwner(dataOwnerId)?.map()
			?: throw NotFoundRequestException("Data owner with id $dataOwnerId not found")
	}

	@PostMapping("/byIds")
	fun getDataOwners(
		@RequestBody dataOwnerIds: ListOfIdsDto,
	): Flux<DataOwnerWithTypeDto> = dataOwnerService.getDataOwners(dataOwnerIds.ids).map().injectReactorContext()

	@Operation(
		summary = "Get a data owner stub by his ID",
		description = "Key-related information about the data owner",
	)
	@GetMapping("/stub/{dataOwnerId}")
	fun getDataOwnerStub(
		@PathVariable dataOwnerId: String,
	): Mono<CryptoActorStubWithTypeDto> = mono {
		dataOwnerService.getCryptoActorStub(dataOwnerId)?.let { cryptoActorStubMapper.map(it) }
			?: throw NotFoundRequestException("Data owner with id $dataOwnerId not found")
	}

	@PostMapping("/stub/byIds")
	fun getDataOwnerStubs(
		@RequestBody dataOwnerIds: ListOfIdsDto,
	): Flux<CryptoActorStubWithTypeDto> = dataOwnerService.getCryptoActorStubs(dataOwnerIds.ids).map { cryptoActorStubMapper.map(it) }.injectReactorContext()

	@Operation(
		summary = "Update key-related information of a data owner",
		description = "Updates information such as the public keys of a data owner or aes exchange keys",
	)
	@PutMapping("/stub")
	fun modifyDataOwnerStub(
		@RequestBody updated: CryptoActorStubWithTypeDto,
	): Mono<CryptoActorStubWithTypeDto> = mono {
		cryptoActorStubMapper.map(dataOwnerService.modifyCryptoActor(cryptoActorStubMapper.map(updated)))
	}

	@Operation(
		summary = "Get the data owner corresponding to the current user",
		description = "General information about the current data owner",
	)
	@GetMapping("/current")
	fun getCurrentDataOwner() = mono {
		getDataOwner(currentDataOwnerOr404()).awaitSingle()
	}

	@Operation(
		summary = "Get the data owner stub corresponding to the current user",
		description = "General information about the current data owner",
	)
	@GetMapping("/current/stub")
	fun getCurrentDataOwnerStub() = mono {
		getDataOwnerStub(currentDataOwnerOr404()).awaitSingle()
	}

	@Operation(
		summary = "Get the data owner corresponding to the current user",
		description = "General information about the current data owner",
	)
	@GetMapping("/current/hierarchy")
	fun getCurrentDataOwnerHierarchy(): Flux<DataOwnerWithTypeDto> = flow {
		emitAll(dataOwnerService.getCryptoActorHierarchy(currentDataOwnerOr404()))
	}.map().injectReactorContext()

	@Operation(
		summary = "Get the data owner corresponding to the current user",
		description = "General information about the current data owner",
	)
	@GetMapping("/current/hierarchy/stub")
	fun getCurrentDataOwnerHierarchyStub(): Flux<CryptoActorStubWithTypeDto> = flow {
		emitAll(dataOwnerService.getCryptoActorHierarchyStub(currentDataOwnerOr404()))
	}.map {
		cryptoActorStubMapper.map(it)
	}.injectReactorContext()
}
