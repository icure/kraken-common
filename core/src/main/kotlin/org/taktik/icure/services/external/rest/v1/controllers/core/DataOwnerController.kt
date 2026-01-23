package org.taktik.icure.services.external.rest.v1.controllers.core

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import kotlinx.coroutines.reactor.awaitSingle
import kotlinx.coroutines.reactor.mono
import org.springframework.context.annotation.Profile
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.server.ResponseStatusException
import org.taktik.icure.asynclogic.SessionInformationProvider
import org.taktik.icure.asyncservice.DataOwnerService
import org.taktik.icure.exceptions.NotFoundRequestException
import org.taktik.icure.services.external.rest.v1.dto.CryptoActorStubWithTypeDto
import org.taktik.icure.services.external.rest.v1.dto.DataOwnerWithTypeDto
import org.taktik.icure.services.external.rest.v1.mapper.CryptoActorStubMapper
import org.taktik.icure.services.external.rest.v1.mapper.DataOwnerWithTypeMapper
import reactor.core.publisher.Mono

@RestController
@Profile("app")
@RequestMapping("/rest/v1/dataowner")
@Tag(name = "dataowner")
class DataOwnerController(
	private val dataOwnerService: DataOwnerService,
	private val sessionLogic: SessionInformationProvider,
	private val dataOwnerWithTypeMapper: DataOwnerWithTypeMapper,
	private val cryptoActorStubMapper: CryptoActorStubMapper,
) {
	@Operation(summary = "Get a data owner by his ID", description = "General information about the data owner")
	@GetMapping("/{dataOwnerId}")
	fun getDataOwner(
		@PathVariable dataOwnerId: String,
	): Mono<DataOwnerWithTypeDto> = mono {
		dataOwnerService.getDataOwner(dataOwnerId)?.let { dataOwnerWithTypeMapper.map(it) }
			?: throw NotFoundRequestException("Data owner with id $dataOwnerId not found")
	}

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
		sessionLogic.getCurrentDataOwnerIdOrNull()?.let { getDataOwner(it) }?.awaitSingle()
			?: throw ResponseStatusException(HttpStatus.NOT_FOUND, "Could not find any data owner associated to the current user.")
	}
}
