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
import org.taktik.icure.asyncservice.ExchangeDataService
import org.taktik.icure.db.PaginationOffset
import org.taktik.icure.entities.ExchangeData
import org.taktik.icure.exceptions.NotFoundRequestException
import org.taktik.icure.entities.DataOwnerType
import org.taktik.icure.services.external.rest.v2.dto.ExchangeDataDto
import org.taktik.icure.services.external.rest.v2.dto.PaginatedList
import org.taktik.icure.services.external.rest.v2.mapper.ExchangeDataV2Mapper
import org.taktik.icure.services.external.rest.v2.utils.paginatedList
import org.taktik.icure.utils.injectReactorContext
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

@RestController("exchangeDataControllerV2")
@Profile("app")
@RequestMapping("/rest/v2/exchangedata")
@Tag(name = "exchangeData")
class ExchangeDataController(
	private val exchangeDataLogic: ExchangeDataService,
	private val exchangeDataMapper: ExchangeDataV2Mapper
) {
	companion object {
		private const val DEFAULT_LIMIT = 1000
	}

	@Operation(summary = "Creates new exchange data")
	@PostMapping
	fun createExchangeData(@RequestBody exchangeData: ExchangeDataDto) = mono {
		exchangeDataMapper.map(exchangeDataLogic.createExchangeData(exchangeDataMapper.map(exchangeData)))
	}

	@Operation(summary = "Modifies existing exchange data")
	@PutMapping
	fun modifyExchangeData(@RequestBody exchangeData: ExchangeDataDto) = mono {
		exchangeDataMapper.map(exchangeDataLogic.modifyExchangeData(exchangeDataMapper.map(exchangeData)))
	}

	@Operation(summary = "Get exchange data with a specific id")
	@GetMapping("/{exchangeDataId}")
	fun getExchangeDataById(@PathVariable exchangeDataId: String) = mono {
		exchangeDataMapper.map(
			exchangeDataLogic.getExchangeDataById(exchangeDataId)
				?: throw NotFoundRequestException("Could not find exchange data with id $exchangeDataId")
		)
	}

	@Operation(summary = "Get exchange data with a specific participant")
	@GetMapping("/byParticipant/{dataOwnerId}")
	fun getExchangeDataByParticipant(
		@PathVariable dataOwnerId: String,
		@RequestParam(required = false) startDocumentId: String?,
		@RequestParam(required = false) limit: Int?
	): Mono<PaginatedList<ExchangeDataDto>> = mono {
		val realLimit = limit ?: DEFAULT_LIMIT
		val paginationOffset = PaginationOffset<String>(realLimit + 1, startDocumentId)
		exchangeDataLogic.findExchangeDataByParticipant(dataOwnerId, paginationOffset)
			.paginatedList<ExchangeData, ExchangeDataDto>({ exchangeDataMapper.map(it) }, realLimit)
	}

	@Operation(summary = "Get exchange data with a specific delegator-delegate pair")
	@GetMapping("/byDelegatorDelegate/{delegatorId}/{delegateId}")
	fun getExchangeDataByDelegatorDelegate(@PathVariable delegatorId: String, @PathVariable delegateId: String): Flux<ExchangeDataDto> = flow {
		emitAll(exchangeDataLogic.findExchangeDataByDelegatorDelegatePair(delegatorId, delegateId).map { exchangeDataMapper.map(it) })
	}.injectReactorContext()

	@Operation(
		summary = "Get the ids of all delegates in exchange data where the data owner is delegator and all delegators" +
			" in exchange data where the data owner is delegate. Return only counterparts if that are data owners of " +
			"the specified type."
	)
	@GetMapping("/byParticipant/{dataOwnerId}/counterparts")
	fun getParticipantCounterparts(
		@PathVariable dataOwnerId: String,
		@RequestParam(required = true) counterpartsTypes: String,
	) = mono {
		exchangeDataLogic.getParticipantCounterparts(
			dataOwnerId,
			counterpartsTypes.split(",").map { DataOwnerType.valueOf(it.uppercase()) }
		).toList()
	}
}
