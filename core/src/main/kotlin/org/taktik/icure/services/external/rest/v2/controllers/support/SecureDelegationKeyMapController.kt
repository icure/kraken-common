package org.taktik.icure.services.external.rest.v2.controllers.support

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.reactor.mono
import org.springframework.context.annotation.Profile
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import org.taktik.icure.asyncservice.SecureDelegationKeyMapService
import org.taktik.icure.cache.ReactorCacheInjector
import org.taktik.icure.services.external.rest.v2.dto.ListOfIdsDto
import org.taktik.icure.services.external.rest.v2.dto.SecureDelegationKeyMapDto
import org.taktik.icure.services.external.rest.v2.dto.requests.BulkShareOrUpdateMetadataParamsDto
import org.taktik.icure.services.external.rest.v2.dto.requests.EntityBulkShareResultDto
import org.taktik.icure.services.external.rest.v2.mapper.SecureDelegationKeyMapV2Mapper
import org.taktik.icure.services.external.rest.v2.mapper.requests.EntityShareOrMetadataUpdateRequestV2Mapper
import org.taktik.icure.services.external.rest.v2.mapper.requests.SecureDelegationKeyMapBulkShareResultV2Mapper
import org.taktik.icure.utils.injectCachedReactorContext
import org.taktik.icure.utils.injectReactorContext
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

@RestController("secureDelegationKeyMapControllerV2")
@Profile("app")
@RequestMapping("/rest/v2/securedelegationkeymap")
@Tag(name = "secureDelegationKeyMap")
class SecureDelegationKeyMapController(
	private val secureDelegationKeyMapService: SecureDelegationKeyMapService,
	private val secureDelegationKeyMapV2Mapper: SecureDelegationKeyMapV2Mapper,
	private val bulkShareResultV2Mapper: SecureDelegationKeyMapBulkShareResultV2Mapper,
	private val entityShareOrMetadataUpdateRequestV2Mapper: EntityShareOrMetadataUpdateRequestV2Mapper,
	private val reactorCacheInjector: ReactorCacheInjector,
) {
	@Operation(description = "Create a new secure delegation key map")
	@PostMapping
	fun createSecureDelegationKeyMap(
		@RequestBody secureDelegationKeyMap: SecureDelegationKeyMapDto,
	): Mono<SecureDelegationKeyMapDto> = mono {
		secureDelegationKeyMapService
			.createSecureDelegationKeyMap(
				secureDelegationKeyMapV2Mapper.map(secureDelegationKeyMap),
			).let(secureDelegationKeyMapV2Mapper::map)
	}

	@Operation(description = "Gets the existing secure delegation key maps for some specific keys")
	@PostMapping("/bydelegationkeys")
	fun findByDelegationKeys(
		@RequestBody delegationKeys: ListOfIdsDto,
	): Flux<SecureDelegationKeyMapDto> = flow {
		emitAll(
			secureDelegationKeyMapService
				.findByDelegationKeys(delegationKeys.ids)
				.map { secureDelegationKeyMapV2Mapper.map(it) },
		)
	}.injectReactorContext()

	@Operation(description = "Shares one or more patients with one or more data owners")
	@PutMapping("/bulkSharedMetadataUpdate")
	fun bulkShare(
		@RequestBody request: BulkShareOrUpdateMetadataParamsDto,
	): Flux<EntityBulkShareResultDto<SecureDelegationKeyMapDto>> = flow {
		emitAll(
			secureDelegationKeyMapService
				.bulkShareOrUpdateMetadata(
					entityShareOrMetadataUpdateRequestV2Mapper.map(request),
				).map { bulkShareResultV2Mapper.map(it) },
		)
	}.injectCachedReactorContext(reactorCacheInjector, 50)
}
