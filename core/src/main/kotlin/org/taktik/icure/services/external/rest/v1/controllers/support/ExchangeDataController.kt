/*
 * Copyright (c) 2020. Taktik SA, All rights reserved.
 */

package org.taktik.icure.services.external.rest.v1.controllers.support

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.transform
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
import org.taktik.icure.config.SharedPaginationConfig
import org.taktik.icure.db.PaginationOffset
import org.taktik.icure.entities.ExchangeData
import org.taktik.icure.exceptions.NotFoundRequestException
import org.taktik.icure.services.external.rest.v2.dto.ExchangeDataDto
import org.taktik.icure.services.external.rest.v2.dto.PaginatedList
import org.taktik.icure.services.external.rest.v2.mapper.ExchangeDataV2Mapper
import org.taktik.icure.services.external.rest.v2.utils.paginatedList
import reactor.core.publisher.Mono

@RestController
@Profile("app")
@RequestMapping("/rest/v1/exchangedata")
@Tag(name = "exchangeData")
class ExchangeDataController(
	private val exchangeDataLogic: ExchangeDataService,
	private val exchangeDataMapper: ExchangeDataV2Mapper,
	private val paginationConfig: SharedPaginationConfig
) {

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
	): Mono<PaginatedList<ExchangeDataDto, *>> = mono {
		val realLimit = limit ?: paginationConfig.defaultLimit
		val paginationOffset = PaginationOffset<String>(realLimit + 1, startDocumentId)
		exchangeDataLogic.findExchangeDataByParticipant(dataOwnerId, paginationOffset)
			.paginatedList<ExchangeData, ExchangeDataDto>({ exchangeDataMapper.map(it) }, realLimit)
	}

	@Operation(summary = "Get exchange data with a specific delegator-delegate pair")
	@GetMapping("/byDelegatorDelegate/{delegatorId}/{delegateId}")
	fun getExchangeDataByDelegatorDelegate(@PathVariable delegatorId: String, @PathVariable delegateId: String): Flow<ExchangeDataDto> =
		exchangeDataLogic.findExchangeDataByDelegatorDelegatePair(delegatorId, delegateId).transform { exchangeDataMapper.map(it) }
}
