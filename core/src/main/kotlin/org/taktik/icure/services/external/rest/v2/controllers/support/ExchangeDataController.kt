/*
 * Copyright (c) 2020. Taktik SA, All rights reserved.
 */

package org.taktik.icure.services.external.rest.v2.controllers.support

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
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
import org.taktik.icure.pagination.PaginatedFlux
import org.taktik.icure.pagination.asPaginationElements
import org.taktik.icure.pagination.asPaginatedFlux
import org.taktik.icure.pagination.mapElements
import org.taktik.icure.services.external.rest.v2.dto.ExchangeDataDto
import org.taktik.icure.services.external.rest.v2.dto.IdWithRevDto
import org.taktik.icure.services.external.rest.v2.dto.ListOfIdsDto
import org.taktik.icure.services.external.rest.v2.dto.requests.ExchangeDataPieceCreationRequestDto
import org.taktik.icure.services.external.rest.v2.mapper.ExchangeDataV2Mapper
import org.taktik.icure.services.external.rest.v2.mapper.requests.ExchangeDataPieceCreationRequestV2Mapper
import org.taktik.icure.utils.FluxString
import org.taktik.icure.utils.injectReactorContext
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

@RestController("exchangeDataControllerV2")
@Profile("app")
@RequestMapping("/rest/v2/exchangedata")
@Tag(name = "exchangeData")
class ExchangeDataController(
	private val exchangeDataService: ExchangeDataService,
	private val exchangeDataMapper: ExchangeDataV2Mapper,
	private val exchangeDataPieceCreationRequestMapper: ExchangeDataPieceCreationRequestV2Mapper,
	private val paginationConfig: SharedPaginationConfig,
	private val reactorCacheInjector: ReactorCacheInjector,
	private val objectMapper: ObjectMapper,
) {
	@Operation(summary = "Creates new exchange data")
	@PostMapping
	fun createExchangeData(
		@RequestBody exchangeData: ExchangeDataDto,
	): Mono<ExchangeDataDto> = mono {
		exchangeDataMapper.map(exchangeDataService.createExchangeData(exchangeDataMapper.map(exchangeData)))
	}

	@Operation(summary = "Creates new exchange data in bulk")
	@PostMapping("/bulk")
	fun createExchangeDataInBulk(
		@RequestBody exchangeDatas: List<ExchangeDataDto>,
	): Flux<IdWithRevDto> = flow {
		emitAll(
			exchangeDataService.createExchangeDatas(
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
		exchangeDataMapper.map(exchangeDataService.modifyExchangeData(exchangeDataMapper.map(exchangeData)))
	}

	@Operation(summary = "Get exchange data with a specific id")
	@GetMapping("/{exchangeDataId}")
	fun getExchangeDataById(
		@PathVariable exchangeDataId: String,
	): Mono<ExchangeDataDto> = mono {
		exchangeDataMapper.map(
			exchangeDataService.getExchangeDataById(exchangeDataId)
				?: throw NotFoundRequestException("Could not find exchange data with id $exchangeDataId"),
		)
	}

	@Operation(summary = "Get exchange data with specific ids")
	@PostMapping("/byIds")
	fun getExchangeDataByIds(
		@RequestBody exchangeDataIds: ListOfIdsDto,
	): Flux<ExchangeDataDto> = exchangeDataService.getExchangeDataByIds(exchangeDataIds.ids.distinct()).map(exchangeDataMapper::map).injectReactorContext()

	@Operation(summary = "Get exchange data with a specific participant")
	@Deprecated("Should use getExchangeDataByParticipantQuery")
	@GetMapping("/byParticipant/{dataOwnerId}")
	fun getExchangeDataByParticipant(
		@PathVariable dataOwnerId: String,
		@RequestParam(required = false) startDocumentId: String?,
		@RequestParam(required = false) limit: Int?,
	): PaginatedFlux<ExchangeDataDto> = getExchangeDataByParticipantQuery(dataOwnerId, startDocumentId, limit)

	@Operation(summary = "Get exchange data with a specific participant. Doesn't allow `/` in dataOwnerId.")
	@Deprecated("Should use getExchangeDataByParticipantForRecipients")
	@GetMapping("/byParticipant")
	fun getExchangeDataByParticipantQuery(
		@RequestParam(required = true) dataOwnerId: String,
		@RequestParam(required = false) startDocumentId: String?,
		@RequestParam(required = false) limit: Int?,
	): PaginatedFlux<ExchangeDataDto> {
		val paginationOffset = PaginationOffset<String>(limit ?: paginationConfig.defaultLimit, startDocumentId)
		return exchangeDataService
			.findExchangeDataByParticipant(dataOwnerId, paginationOffset)
			.mapElements(exchangeDataMapper::map)
			.asPaginatedFlux()
	}

	@Operation(summary = "Get exchange data with a specific delegator-delegate pair. Doesn't allow `/` in delegator or delegate id")
	@GetMapping("/byDelegatorDelegate/{delegatorId}/{delegateId}")
	@Deprecated("Use getExchangeDataByDelegatorDelegateQuery")
	fun getExchangeDataByDelegatorDelegate(
		@PathVariable delegatorId: String,
		@PathVariable delegateId: String,
	): Flux<ExchangeDataDto> = getExchangeDataByDelegatorDelegateQuery(delegatorId, delegateId)

	@Operation(summary = "Get exchange data with a specific delegator-delegate pair")
	@Deprecated("Use getExchangeDataByDelegatorDelegateForRecipients")
	@GetMapping("/byDelegatorDelegate")
	fun getExchangeDataByDelegatorDelegateQuery(
		@RequestParam(required = true) delegatorId: String,
		@RequestParam(required = true) delegateId: String,
	): Flux<ExchangeDataDto> = flow {
		emitAll(exchangeDataService.findExchangeDataByDelegatorDelegatePair(delegatorId, delegateId).map { exchangeDataMapper.map(it) })
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
			exchangeDataService.createExchangeDataGroupPieces(
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
		return exchangeDataService
			.findExchangeDataGroupById(exchangeDataGroupId, paginationOffset)
			.mapElements(exchangeDataMapper::map)
			.asPaginatedFlux()
	}

	@Operation(
		summary = "Get the pieces of an exchange data group for the provided recipients",
		description =
		"The recipients are a json array, so include null in it to also get the exchange data that has no recipient, " +
			"that is the exchange data that is not for a simple-type data owner group. To get the next page, pass the " +
			"start key of the returned cursor back as the recipients and its start document id as startDocumentId.",
	)
	@GetMapping("/group/{exchangeDataGroupId}/byRecipients")
	fun getExchangeDataGroupByIdForRecipients(
		@PathVariable exchangeDataGroupId: String,
		@RequestParam(required = true) recipients: String,
		@RequestParam(required = false) startDocumentId: String?,
	): PaginatedFlux<ExchangeDataDto> = exchangeDataService
		.findExchangeDataGroupByIdForRecipients(exchangeDataGroupId, recipients.toFilterRecipients(objectMapper), startDocumentId)
		.mapElements(exchangeDataMapper::map)
		.asPaginationElements()
		.asPaginatedFlux()

	@Operation(
		summary = "Get the exchange data with a specific participant, for the provided recipients",
		description =
		"The recipients are a json array, so include null in it to also get the exchange data that has no recipient, " +
			"that is the exchange data that is not for a simple-type data owner group. To get the next page, pass the " +
			"start key of the returned cursor back as the recipients and its start document id as startDocumentId.",
	)
	@GetMapping("/byParticipant/byRecipients")
	fun getExchangeDataByParticipantForRecipients(
		@RequestParam(required = true) dataOwnerId: String,
		@RequestParam(required = true) recipients: String,
		@RequestParam(required = false) startDocumentId: String?,
	): PaginatedFlux<ExchangeDataDto> = exchangeDataService
		.findExchangeDataByParticipantForRecipients(dataOwnerId, recipients.toFilterRecipients(objectMapper), startDocumentId)
		.mapElements(exchangeDataMapper::map)
		.asPaginationElements()
		.asPaginatedFlux()

	@Operation(
		summary = "Get the exchange data with a specific delegator-delegate pair, for the provided recipients",
		description =
		"The recipients are a json array, so include null in it to also get the exchange data that has no recipient, " +
			"that is the exchange data that is not for a simple-type data owner group. To get the next page, pass the " +
			"start key of the returned cursor back as the recipients and its start document id as startDocumentId.",
	)
	@GetMapping("/byDelegatorDelegate/byRecipients")
	fun getExchangeDataByDelegatorDelegateForRecipients(
		@RequestParam(required = true) delegatorId: String,
		@RequestParam(required = true) delegateId: String,
		@RequestParam(required = true) recipients: String,
		@RequestParam(required = false) startDocumentId: String?,
	): PaginatedFlux<ExchangeDataDto> = exchangeDataService
		.findExchangeDataByDelegatorDelegateForRecipients(
			delegatorId,
			delegateId,
			recipients.toFilterRecipients(objectMapper),
			startDocumentId,
		).mapElements(exchangeDataMapper::map)
		.asPaginationElements()
		.asPaginatedFlux()

	@Operation(
		summary =
		"Get the ids of all delegates in exchange data where the data owner is delegator and all delegators" +
			" in exchange data where the data owner is delegate. Return only counterparts if that are data owners of " +
			"the specified type. Doesn't allow `/` in dataOwnerId",
		deprecated = true,
	)
	@Deprecated("Use getParticipantCounterpartsQuery")
	@GetMapping("/byParticipant/{dataOwnerId}/counterparts")
	@Suppress("DEPRECATION")
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
		description =
		"Deprecated in favour of /byParticipant/nonGroupPieceCounterparts, which is paginated. Note that the " +
			"replacement also ignores the exchange data of simple-type data owner groups, which this endpoint reports.",
		deprecated = true,
	)
	@Deprecated("Use findNonGroupPieceCounterparts")
	@GetMapping("/byParticipant/counterparts")
	@Suppress("DEPRECATION")
	fun getParticipantCounterpartsQuery(
		@RequestParam(required = true) dataOwnerId: String,
		@RequestParam(required = true) counterpartsTypes: String,
		@RequestParam(required = false) ignoreOnEntryForFingerprint: String? = null,
	): Mono<List<String>> = mono {
		exchangeDataService
			.getParticipantCounterparts(
				dataOwnerId,
				counterpartsTypes.split(",").map { DataOwnerType.valueOf(it.uppercase()) },
				ignoreOnEntryForFingerprint,
			).toList()
	}

	@Operation(
		summary = "Get the data owners that share exchange data with a data owner, excluding the group pieces",
		description =
		"Returns the delegate of the exchange data where dataOwnerId is the delegator, and the other way around, " +
			"keeping only the counterparts that are data owners of one of the counterpartsTypes.\n\n" +
			"The exchange data that has a recipient, that is the pieces of exchange data for a simple-type data owner " +
			"group, is ignored entirely: a simple-type group is never returned as a counterpart. That is what this " +
			"search is for, the exchange data of a simple-type group being created and re-encrypted one piece per " +
			"group member instead. Counterparts of another group, referenced as `dataOwnerGroupId/dataOwnerId`, are " +
			"not returned either, and a data owner is never its own counterpart.\n\n" +
			"To get the next page pass the start key of the returned cursor back as startKey; there is no start " +
			"document id. The limit must be between 100 and 1000 and defaults to 1000, and is only an upper bound: a " +
			"page can hold fewer counterparts, or none at all, while more pages follow.",
	)
	@GetMapping("/byParticipant/nonGroupPieceCounterparts")
	fun findNonGroupPieceCounterparts(
		@RequestParam(required = true) dataOwnerId: String,
		@RequestParam(required = true) counterpartsTypes: String,
		@RequestParam(required = false) ignoreOnEntryForFingerprint: String? = null,
		@RequestParam(required = false) startKey: String? = null,
		@RequestParam(required = false) limit: Int? = null,
	): PaginatedFlux<FluxString> = exchangeDataService
		.findNonGroupPieceCounterparts(
			dataOwnerId,
			counterpartsTypes.toCounterpartsTypes(),
			ignoreOnEntryForFingerprint,
			startKey,
			limit,
		).asPaginatedFlux()
}

/**
 * Parses the `recipients` request parameter of the recipient-filtered searches, a json array of the recipients to filter
 * by. A `null` entry stands for the exchange data that has no recipient, that is the exchange data that is not for a
 * simple-type data owner group.
 *
 * The cursor of a page of these searches carries the recipients still to visit as its start key, in this same shape, so
 * a caller resumes a search by passing that start key back as this parameter.
 */
fun String.toFilterRecipients(objectMapper: ObjectMapper): List<String?> = objectMapper.readValue<List<String?>>(this)

/**
 * Parses the `counterpartsTypes` request parameter of the counterparts searches, a comma-separated list of data owner
 * types. The types are read case-insensitively rather than bound as a data owner type dto parameter, since spring would
 * match the kotlin constant name while clients send the wire value.
 */
fun String.toCounterpartsTypes(): List<DataOwnerType> = split(",").map {
	requireNotNull(DataOwnerType.valueOfOrNullCaseInsensitive(it.trim())) { "Unknown data owner type $it" }
}
