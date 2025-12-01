package org.taktik.icure.services.external.rest.v2.controllers.core

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.reactor.awaitSingle
import kotlinx.coroutines.reactor.mono
import org.springframework.context.annotation.Profile
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.server.ResponseStatusException
import org.taktik.icure.asynclogic.SessionInformationProvider
import org.taktik.icure.asynclogic.UserLogic
import org.taktik.icure.asyncservice.DataOwnerService
import org.taktik.icure.entities.User
import org.taktik.icure.exceptions.NotFoundRequestException
import org.taktik.icure.services.external.rest.v2.dto.CryptoActorStubWithTypeDto
import org.taktik.icure.services.external.rest.v2.dto.DataOwnerWithTypeDto
import org.taktik.icure.services.external.rest.v2.dto.ListOfIdsDto
import org.taktik.icure.services.external.rest.v2.mapper.CryptoActorStubV2Mapper
import org.taktik.icure.services.external.rest.v2.mapper.DataOwnerWithTypeV2Mapper
import org.taktik.icure.utils.injectCachedReactorContext
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
	private val cryptoActorStubMapper: CryptoActorStubV2Mapper,
) {
	@Operation(summary = "Get a data owner by his ID", description = "General information about the data owner")
	@GetMapping("/{dataOwnerId}")
	fun getDataOwner(
		@PathVariable dataOwnerId: String,
	): Mono<DataOwnerWithTypeDto> = mono {
		dataOwnerService.getDataOwner(dataOwnerId)?.let { dataOwnerWithTypeMapper.map(it) }
			?: throw NotFoundRequestException("Data owner with id $dataOwnerId not found")
	}

	@PostMapping("/byIds")
	fun getDataOwners(
		@RequestBody dataOwnerIds: ListOfIdsDto,
	): Flux<DataOwnerWithTypeDto> = dataOwnerService.getDataOwners(dataOwnerIds.ids).map { dataOwnerWithTypeMapper.map(it) }.injectReactorContext()

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
		getDataOwner(sessionLogic.getCurrentDataOwnerId()).awaitSingle()
	}

	@Operation(
		summary = "Get the data owner stub corresponding to the current user",
		description = "General information about the current data owner",
	)
	@GetMapping("/current/stub")
	fun getCurrentDataOwnerStub() = mono {
		getDataOwnerStub(sessionLogic.getCurrentDataOwnerId()).awaitSingle()
	}

	@Operation(
		summary = "Get the data owner corresponding to the current user",
		description = "General information about the current data owner",
	)
	@GetMapping("/current/hierarchy")
	fun getCurrentDataOwnerHierarchy(): Flux<DataOwnerWithTypeDto> = flow {
		emitAll(dataOwnerService.getCryptoActorHierarchy(sessionLogic.getCurrentDataOwnerId()))
	}.map {
		dataOwnerWithTypeMapper.map(it)
	}.injectReactorContext()

	@Operation(
		summary = "Get the data owner corresponding to the current user",
		description = "General information about the current data owner",
	)
	@GetMapping("/current/hierarchy/stub")
	fun getCurrentDataOwnerHierarchyStub(): Flux<CryptoActorStubWithTypeDto> = flow {
		emitAll(dataOwnerService.getCryptoActorHierarchyStub(sessionLogic.getCurrentDataOwnerId()))
	}.map {
		cryptoActorStubMapper.map(it)
	}.injectReactorContext()
}
