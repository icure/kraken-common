package org.taktik.icure.services.external.rest.v2.controllers.support

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.reactor.mono
import org.springframework.context.annotation.Profile
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import org.taktik.icure.asyncservice.ExchangeDataMapService
import org.taktik.icure.services.external.rest.v2.dto.ExchangeDataMapCreationBatch
import org.taktik.icure.services.external.rest.v2.dto.ListOfIdsDto
import org.taktik.icure.services.external.rest.v2.mapper.ExchangeDataMapV2Mapper
import org.taktik.icure.utils.injectReactorContext

@RestController("exchangeDataMapControllerV2")
@Profile("app")
@RequestMapping("/rest/v2/exchangedatamap")
@Tag(name = "exchangeDataMap")
class ExchangeDataMapController(
	private val exchangeDataMapService: ExchangeDataMapService,
	private val exchangeDataMapV2Mapper: ExchangeDataMapV2Mapper,
) {
	@Operation(description = "Creates a new Exchange Data Map batch, updating the ones that already exist")
	@PutMapping("/batch")
	fun createOrUpdateExchangeDataMapBatch(
		@RequestBody batch: ExchangeDataMapCreationBatch,
	) = mono {
		require(batch.batch.values.all { it.isNotEmpty() }) {
			"Should not create exchange data map with no content"
		}
		exchangeDataMapService
			.createOrUpdateExchangeDataMapBatchByAccessControlKey(
				batch.batch,
			).collect()
		ResponseEntity.ok().body("ok")
	}

	@Operation(description = "Gets an Exchange Data Map batch by ids")
	@PostMapping("/batch")
	fun getExchangeDataMapBatch(
		@RequestBody ids: ListOfIdsDto,
	) = exchangeDataMapService
		.getExchangeDataMapBatch(ids.ids)
		.map(exchangeDataMapV2Mapper::map)
		.injectReactorContext()
}
