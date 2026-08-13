package org.taktik.icure.services.external.rest.v2.controllers.core

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
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
import org.taktik.icure.exceptions.NotFoundRequestException
import org.taktik.icure.services.external.rest.v2.dto.CryptoActorStubWithTypeDto
import org.taktik.icure.services.external.rest.v2.dto.DataOwnerWithTypeDto
import org.taktik.icure.services.external.rest.v2.dto.ListOfIdsDto
import org.taktik.icure.services.external.rest.v2.dto.base.DataOwnerHierarchyInfoDto
import org.taktik.icure.services.external.rest.v2.mapper.CryptoActorStubV2Mapper
import org.taktik.icure.services.external.rest.v2.mapper.DataOwnerWithTypeV2Mapper
import org.taktik.icure.services.external.rest.v2.mapper.base.DataOwnerHierarchyInfoV2Mapper
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
	private val dataOwnerHierarchyInfoMapper: DataOwnerHierarchyInfoV2Mapper,
) {
	private suspend fun currentDataOwnerOr404(): String =
		sessionLogic.getCurrentDataOwnerIdOrNull() ?: throw NotFoundRequestException("Current user is not a data owner")

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
		summary = "Get the current data owner and its legacy parentId chain",
		description = "Deprecated: only follows the legacy linear parentId chain, use /current/hierarchies/info instead",
		deprecated = true,
	)
	@Suppress("DEPRECATION")
	@GetMapping("/current/hierarchy")
	fun getCurrentDataOwnerHierarchy(): Flux<DataOwnerWithTypeDto> = flow {
		emitAll(dataOwnerService.getCryptoActorHierarchy(currentDataOwnerOr404()))
	}.map {
		dataOwnerWithTypeMapper.map(it)
	}.injectReactorContext()

	@Operation(
		summary = "Get the crypto-actor stubs of the current data owner and its legacy parentId chain",
		description = "Deprecated: only follows the legacy linear parentId chain, use /current/hierarchies/info instead",
		deprecated = true,
	)
	@Suppress("DEPRECATION")
	@GetMapping("/current/hierarchy/stub")
	fun getCurrentDataOwnerHierarchyStub(): Flux<CryptoActorStubWithTypeDto> = flow {
		emitAll(dataOwnerService.getCryptoActorHierarchyStub(currentDataOwnerOr404()))
	}.map {
		cryptoActorStubMapper.map(it)
	}.injectReactorContext()

	@Operation(
		summary = "Get the type and group hierarchies of the current data owner as a tree of ids",
		description = "The type of the current data owner (shared by every data owner in the hierarchy) and a tree " +
			"of data owner ids rooted at the current data owner: the parents of each node are the data owners it " +
			"is directly linked to, through the legacy parentId or a dataOwnerGroups link (parents, organisations, " +
			"locations, ...), together with the type of that link (parent or simple). A data owner reachable " +
			"through multiple links appears once per path; a link's transitiveLinks may only have the same or a " +
			"weaker link type than the link itself (e.g. a parent link may lead to a simple link, never the " +
			"reverse).",
	)
	@GetMapping("/current/hierarchies/info")
	fun getCurrentDataOwnerHierarchyInfo(): Mono<DataOwnerHierarchyInfoDto> = mono {
		dataOwnerHierarchyInfoMapper.map(dataOwnerService.getCryptoActorHierarchyInfo(currentDataOwnerOr404()))
	}
}
