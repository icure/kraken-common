package org.taktik.icure.services.external.rest.v2.controllers.core

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
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
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import org.taktik.icure.asynclogic.SessionInformationProvider
import org.taktik.icure.asyncservice.DataOwnerService
import org.taktik.icure.config.SharedPaginationConfig
import org.taktik.icure.entities.DataOwnerType
import org.taktik.icure.exceptions.NotFoundRequestException
import org.taktik.icure.pagination.PaginatedFlux
import org.taktik.icure.pagination.asPaginatedFlux
import org.taktik.icure.pagination.asPaginationElements
import org.taktik.icure.pagination.mapElements
import org.taktik.icure.services.external.rest.v2.dto.CryptoActorStubWithTypeDto
import org.taktik.icure.services.external.rest.v2.dto.DataOwnerWithTypeDto
import org.taktik.icure.services.external.rest.v2.dto.ListOfIdsDto
import org.taktik.icure.services.external.rest.v2.dto.base.DataOwnerHierarchyInfoDto
import org.taktik.icure.services.external.rest.v2.dto.requests.DataOwnerPublicKeysDto
import org.taktik.icure.services.external.rest.v2.dto.requests.LinkedDataOwnerDto
import org.taktik.icure.services.external.rest.v2.mapper.CryptoActorStubV2Mapper
import org.taktik.icure.services.external.rest.v2.mapper.DataOwnerWithTypeV2Mapper
import org.taktik.icure.services.external.rest.v2.mapper.base.DataOwnerHierarchyInfoV2Mapper
import org.taktik.icure.services.external.rest.v2.mapper.requests.DataOwnerPublicKeysV2Mapper
import org.taktik.icure.services.external.rest.v2.mapper.requests.LinkedDataOwnerV2Mapper
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
	private val linkedDataOwnerMapper: LinkedDataOwnerV2Mapper,
	private val dataOwnerPublicKeysMapper: DataOwnerPublicKeysV2Mapper,
	private val paginationConfig: SharedPaginationConfig,
	private val objectMapper: ObjectMapper,
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

	@Operation(
		summary = "Get the data owners directly linked to any of the provided data owner groups",
		description =
		"The data owners that declare a link to one of the provided groups, through the legacy parentId or a " +
			"dataOwnerGroups link, each with its own group link type - the type that any link pointing at it " +
			"has. The group link type is omitted when it is the default for the requested data owner type " +
			"(parent for hcp, notAllowed for patient and device). This does not follow the links transitively: " +
			"a data owner linked to a group only through another data owner is not returned, and a client that " +
			"wants the full membership queries again with the members it is interested in. A data owner linked " +
			"to several of the requested groups is returned only once per page, so a page may hold fewer rows " +
			"than the requested limit even when there is a next page. Only healthcare parties can be the target " +
			"of a group link, so this is always empty for patients and devices.",
	)
	@GetMapping("/linkedTo")
	fun findDataOwnersLinkedToGroups(
		@Parameter(description = "The type of the provided groups and of the returned data owners")
		@RequestParam(required = true)
		dataOwnerType: String,
		@Parameter(description = "A url encoded json array of the ids of the data owners representing the groups")
		@RequestParam(required = true)
		dataOwnerGroupIds: String,
		@RequestParam(required = false) startDocumentId: String?,
		@RequestParam(required = false) limit: Int?,
	): PaginatedFlux<LinkedDataOwnerDto> = dataOwnerService
		.findDataOwnersLinkedToGroups(
			dataOwnerGroupIds.toDataOwnerGroupIds(objectMapper),
			dataOwnerType.toDataOwnerType(),
			startDocumentId,
			limit ?: paginationConfig.defaultLimit,
		).mapElements(linkedDataOwnerMapper::map)
		.asPaginationElements()
		.asPaginatedFlux()

	@Operation(
		summary = "Get the public keys of the data owners with the provided ids",
		description =
		"Each key comes with the encryption algorithm it must be used with. Data owners that don't exist, are " +
			"not of the provided type, or have no public key at all produce no result. Fails if too many ids " +
			"are requested at once.",
	)
	@PostMapping("/publicKeys/byIds")
	fun getDataOwnersPublicKeys(
		@Parameter(description = "The type of the data owners with the provided ids")
		@RequestParam(required = true)
		dataOwnerType: String,
		@RequestBody dataOwnerIds: ListOfIdsDto,
	): Flux<DataOwnerPublicKeysDto> = dataOwnerService
		.getDataOwnersPublicKeys(dataOwnerIds.ids, dataOwnerType.toDataOwnerType())
		.map { dataOwnerPublicKeysMapper.map(it) }
		.injectReactorContext()
}

/**
 * Parses the `dataOwnerGroupIds` request parameter of the search for the data owners linked to some groups, a json
 * array of the ids of the data owners representing those groups.
 *
 * The cursor of a page of that search carries the groups still to visit as its start key, in this same shape, so a
 * caller resumes the search by passing that start key back as this parameter.
 */
fun String.toDataOwnerGroupIds(objectMapper: ObjectMapper): List<String> = objectMapper.readValue<List<String>>(this)

/**
 * Parses a `dataOwnerType` request parameter, case-insensitively: clients send the lowercase json value of the dto
 * enum, which doesn't match the name spring would bind a [DataOwnerType] parameter by.
 */
fun String.toDataOwnerType(): DataOwnerType = DataOwnerType.valueOfOrNullCaseInsensitive(this)
	?: throw IllegalArgumentException(
		"Invalid data owner type $this, expected one of ${DataOwnerType.entries.joinToString { it.name.lowercase() }}",
	)
