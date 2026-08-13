/*
 * Copyright (c) 2020. Taktik SA, All rights reserved.
 */

package org.taktik.icure.services.external.rest.v2.controllers.support

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.toList
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
import org.taktik.couchdb.entity.ComplexKey
import org.taktik.icure.asyncservice.ExchangeDataService
import org.taktik.icure.cache.ReactorCacheInjector
import org.taktik.icure.config.SharedPaginationConfig
import org.taktik.icure.db.PaginationOffset
import org.taktik.icure.entities.DataOwnerType
import org.taktik.icure.exceptions.NotFoundRequestException
import org.taktik.icure.pagination.MultiKeyPaginatedFlux
import org.taktik.icure.pagination.PaginatedFlux
import org.taktik.icure.pagination.asMultiKeyPaginatedFlux
import org.taktik.icure.pagination.asPaginatedFlux
import org.taktik.icure.pagination.mapElements
import org.taktik.icure.services.external.rest.v2.dto.ExchangeDataDto
import org.taktik.icure.services.external.rest.v2.dto.IdWithRevDto
import org.taktik.icure.services.external.rest.v2.dto.ListOfIdsDto
import org.taktik.icure.services.external.rest.v2.dto.requests.ExchangeDataPieceCreationRequestDto
import org.taktik.icure.services.external.rest.v2.mapper.ExchangeDataV2Mapper
import org.taktik.icure.services.external.rest.v2.mapper.requests.ExchangeDataPieceCreationRequestV2Mapper
import org.taktik.icure.utils.injectReactorContext
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

@RestController("exchangeDataControllerV2")
@Profile("app")
@RequestMapping("/rest/v2/exchangedata")
@Tag(name = "exchangeData")
class ExchangeDataController(
	private val exchangeDataLogic: ExchangeDataService,
	private val exchangeDataMapper: ExchangeDataV2Mapper,
	private val exchangeDataPieceCreationRequestMapper: ExchangeDataPieceCreationRequestV2Mapper,
	private val paginationConfig: SharedPaginationConfig,
	private val reactorCacheInjector: ReactorCacheInjector,
) {
	@Operation(summary = "Creates new exchange data")
	@PostMapping
	fun createExchangeData(
		@RequestBody exchangeData: ExchangeDataDto,
	): Mono<ExchangeDataDto> = mono {
		exchangeDataMapper.map(exchangeDataLogic.createExchangeData(exchangeDataMapper.map(exchangeData)))
	}

	@Operation(summary = "Creates new exchange data in bulk")
	@PostMapping("/bulk")
	fun createExchangeDataInBulk(
		@RequestBody exchangeDatas: List<ExchangeDataDto>,
	): Flux<IdWithRevDto> = flow {
		emitAll(
			exchangeDataLogic.createExchangeDatas(
				exchangeDatas.map { exchangeDataMapper.map(it) },
			).map {
				IdWithRevDto(it.id, it.rev!!)
			}
		)
	}.injectReactorContext()

	@Operation(summary = "Modifies existing exchange data")
	@PutMapping
	fun modifyExchangeData(
		@RequestBody exchangeData: ExchangeDataDto,
	): Mono<ExchangeDataDto> = reactorCacheInjector.monoWithCachedContext(10) {
		exchangeDataMapper.map(exchangeDataLogic.modifyExchangeData(exchangeDataMapper.map(exchangeData)))
	}

	@Operation(summary = "Get exchange data with a specific id")
	@GetMapping("/{exchangeDataId}")
	fun getExchangeDataById(
		@PathVariable exchangeDataId: String,
	): Mono<ExchangeDataDto> = mono {
		exchangeDataMapper.map(
			exchangeDataLogic.getExchangeDataById(exchangeDataId)
				?: throw NotFoundRequestException("Could not find exchange data with id $exchangeDataId"),
		)
	}

	@Operation(summary = "Get exchange data with specific ids")
	@PostMapping("/byIds")
	fun getExchangeDataByIds(
		@RequestBody exchangeDataIds: ListOfIdsDto,
	): Flux<ExchangeDataDto> = exchangeDataLogic.getExchangeDataByIds(exchangeDataIds.ids.distinct()).map(exchangeDataMapper::map).injectReactorContext()

	@Operation(summary = "Get exchange data with a specific participant")
	@GetMapping("/byParticipant/{dataOwnerId}")
	fun getExchangeDataByParticipant(
		@PathVariable dataOwnerId: String,
		@RequestParam(required = false) startDocumentId: String?,
		@RequestParam(required = false) limit: Int?,
	): PaginatedFlux<ExchangeDataDto> = getExchangeDataByParticipantQuery(dataOwnerId, startDocumentId, limit)

	@Operation(summary = "Get exchange data with a specific participant. Doesn't allow `/` in dataOwnerId.")
	@GetMapping("/byParticipant")
	fun getExchangeDataByParticipantQuery(
		@RequestParam(required = true) dataOwnerId: String,
		@RequestParam(required = false) startDocumentId: String?,
		@RequestParam(required = false) limit: Int?,
	): PaginatedFlux<ExchangeDataDto> {
		val paginationOffset = PaginationOffset<String>(limit ?: paginationConfig.defaultLimit, startDocumentId)
		return exchangeDataLogic
			.findExchangeDataByParticipant(dataOwnerId, paginationOffset)
			.mapElements(exchangeDataMapper::map)
			.asPaginatedFlux()
	}

	@Operation(summary = "Get exchange data with a specific delegator-delegate pair. Doesn't allow `/` in delegator or delegate id")
	@GetMapping("/byDelegatorDelegate/{delegatorId}/{delegateId}")
	fun getExchangeDataByDelegatorDelegate(
		@PathVariable delegatorId: String,
		@PathVariable delegateId: String,
	): Flux<ExchangeDataDto> = getExchangeDataByDelegatorDelegateQuery(delegatorId, delegateId)

	@Operation(summary = "Get exchange data with a specific delegator-delegate pair")
	@GetMapping("/byDelegatorDelegate")
	fun getExchangeDataByDelegatorDelegateQuery(
		@RequestParam(required = true) delegatorId: String,
		@RequestParam(required = true) delegateId: String,
	): Flux<ExchangeDataDto> = flow {
		emitAll(exchangeDataLogic.findExchangeDataByDelegatorDelegatePair(delegatorId, delegateId).map { exchangeDataMapper.map(it) })
	}.injectReactorContext()

	@Operation(
		summary = "Creates the pieces of the exchange data of a simple-type data owner group, one per recipient",
		description =
		"The keys of the body are the recipients of the pieces to create. The first request for an " +
			"exchangeDataGroupId must include an entry for the delegator itself, and any later request to add pieces " +
			"to that same group must not.",
	)
	@PostMapping("/group/{exchangeDataGroupId}/pieces")
	fun createExchangeDataGroupPieces(
		@PathVariable exchangeDataGroupId: String,
		@RequestParam(required = true) delegator: String,
		@RequestParam(required = true) delegate: String,
		@RequestBody piecesByRecipient: Map<String, ExchangeDataPieceCreationRequestDto>,
	): Flux<ExchangeDataDto> = flow {
		emitAll(
			exchangeDataLogic.createExchangeDataGroupPieces(
				exchangeDataGroupId = exchangeDataGroupId,
				delegator = delegator,
				delegate = delegate,
				piecesByRecipient = piecesByRecipient.mapValues { exchangeDataPieceCreationRequestMapper.map(it.value) },
			).map { exchangeDataMapper.map(it) },
		)
	}.injectReactorContext()

	@Operation(
		summary = "Get all the pieces of an exchange data group",
		description =
		"If there is no exchange data with the provided exchangeDataGroupId, returns the single exchange data " +
			"with that id, if any.",
	)
	@GetMapping("/group/{exchangeDataGroupId}")
	fun getExchangeDataGroupById(
		@PathVariable exchangeDataGroupId: String,
		@RequestParam(required = false) startKey: String?,
		@RequestParam(required = false) startDocumentId: String?,
		@RequestParam(required = false) limit: Int?,
	): PaginatedFlux<ExchangeDataDto> {
		val paginationOffset = PaginationOffset(
			startKey = startKey?.let { ComplexKey.of(exchangeDataGroupId, it) },
			startDocumentId = startDocumentId,
			offset = null,
			limit = limit ?: paginationConfig.defaultLimit,
		)
		return exchangeDataLogic
			.findExchangeDataGroupById(exchangeDataGroupId, paginationOffset)
			.mapElements(exchangeDataMapper::map)
			.asPaginatedFlux()
	}

	@Operation(
		summary = "Get the pieces of an exchange data group for the provided recipients",
		description =
		"Use an empty string as a recipient to also get the exchange data that has no recipient, that is the " +
			"exchange data that is not for a simple-type data owner group.",
	)
	@GetMapping("/group/{exchangeDataGroupId}/byRecipients")
	fun getExchangeDataGroupByIdForRecipients(
		@PathVariable exchangeDataGroupId: String,
		@RequestParam(required = true) recipients: String,
		@RequestParam(required = false) startDocumentId: String?,
	): MultiKeyPaginatedFlux<ExchangeDataDto, String?> = exchangeDataLogic
		.findExchangeDataGroupByIdForRecipients(exchangeDataGroupId, recipients.toFilterRecipients(), startDocumentId)
		.mapElements(exchangeDataMapper::map)
		.asMultiKeyPaginatedFlux()

	@Operation(
		summary = "Get the exchange data with a specific participant, for the provided recipients",
		description =
		"Use an empty string as a recipient to also get the exchange data that has no recipient, that is the " +
			"exchange data that is not for a simple-type data owner group.",
	)
	@GetMapping("/byParticipant/byRecipients")
	fun getExchangeDataByParticipantForRecipients(
		@RequestParam(required = true) dataOwnerId: String,
		@RequestParam(required = true) recipients: String,
		@RequestParam(required = false) startDocumentId: String?,
	): MultiKeyPaginatedFlux<ExchangeDataDto, String?> = exchangeDataLogic
		.findExchangeDataByParticipantForRecipients(dataOwnerId, recipients.toFilterRecipients(), startDocumentId)
		.mapElements(exchangeDataMapper::map)
		.asMultiKeyPaginatedFlux()

	@Operation(
		summary = "Get the exchange data with a specific delegator-delegate pair, for the provided recipients",
		description =
		"Use an empty string as a recipient to also get the exchange data that has no recipient, that is the " +
			"exchange data that is not for a simple-type data owner group.",
	)
	@GetMapping("/byDelegatorDelegate/byRecipients")
	fun getExchangeDataByDelegatorDelegateForRecipients(
		@RequestParam(required = true) delegatorId: String,
		@RequestParam(required = true) delegateId: String,
		@RequestParam(required = true) recipients: String,
		@RequestParam(required = false) startDocumentId: String?,
	): MultiKeyPaginatedFlux<ExchangeDataDto, String?> = exchangeDataLogic
		.findExchangeDataByDelegatorDelegateForRecipients(
			delegatorId,
			delegateId,
			recipients.toFilterRecipients(),
			startDocumentId,
		).mapElements(exchangeDataMapper::map)
		.asMultiKeyPaginatedFlux()

	@Operation(
		summary =
		"Get the ids of all delegates in exchange data where the data owner is delegator and all delegators" +
			" in exchange data where the data owner is delegate. Return only counterparts if that are data owners of " +
			"the specified type. Doesn't allow `/` in dataOwnerId",
	)
	@GetMapping("/byParticipant/{dataOwnerId}/counterparts")
	fun getParticipantCounterparts(
		@PathVariable dataOwnerId: String,
		@RequestParam(required = true) counterpartsTypes: String,
		@RequestParam(required = false) ignoreOnEntryForFingerprint: String? = null,
	): Mono<List<String>> = getParticipantCounterpartsQuery(dataOwnerId, counterpartsTypes, ignoreOnEntryForFingerprint)

	@Operation(
		summary =
		"Get the ids of all delegates in exchange data where the data owner is delegator and all delegators" +
			" in exchange data where the data owner is delegate. Return only counterparts if that are data owners of " +
			"the specified type.",
	)
	@GetMapping("/byParticipant/counterparts")
	fun getParticipantCounterpartsQuery(
		@RequestParam(required = true) dataOwnerId: String,
		@RequestParam(required = true) counterpartsTypes: String,
		@RequestParam(required = false) ignoreOnEntryForFingerprint: String? = null,
	): Mono<List<String>> = mono {
		exchangeDataLogic
			.getParticipantCounterparts(
				dataOwnerId,
				counterpartsTypes.split(",").map { DataOwnerType.valueOf(it.uppercase()) },
				ignoreOnEntryForFingerprint,
			).toList()
	}
}

/**
 * Converts the comma-separated `recipients` request parameter of the recipient-filtered searches to the recipients to
 * filter by: since only the exchange data that is not for a simple-type data owner group has no recipient, an empty
 * entry stands for the `null` recipient.
 */
fun String.toFilterRecipients(): List<String?> = split(",").map { it.takeIf { r -> r.isNotEmpty() } }
