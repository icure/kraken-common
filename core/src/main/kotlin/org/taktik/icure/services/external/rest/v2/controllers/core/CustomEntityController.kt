package org.taktik.icure.services.external.rest.v2.controllers.core

import com.icure.cardinal.customentities.util.CachedCustomEntitiesConfigurationProvider
import com.icure.cardinal.customentities.util.ExtendableBuiltinEntityValidatorMapperConfigsProvider
import com.icure.cardinal.errorreporting.MapperScopePathProvider
import io.swagger.v3.oas.annotations.tags.Tag
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.springframework.context.annotation.Profile
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import org.taktik.icure.entities.CustomEntityBase
import org.taktik.icure.entities.requests.EntityBulkShareResult
import org.taktik.icure.services.external.rest.v2.dto.CustomEntityBaseDto
import org.taktik.icure.services.external.rest.v2.dto.requests.EntityBulkShareResultDto
import org.taktik.icure.services.external.rest.v2.mapper.CustomEntityBaseV2Mapper
import org.taktik.icure.services.external.rest.v2.mapper.MappersWithCustomExtensions.mapCustomFromDto
import org.taktik.icure.services.external.rest.v2.mapper.requests.CustomEntityBaseBulkShareResultV2Mapper

@RestController("customEntityControllerV2")
@Profile("app")
@RequestMapping("/rest/v2/custom")
@Tag(name = "custom")
class CustomEntityController(
	val mapper: CustomEntityBaseV2Mapper,
	private val customEntitiesConfigurationProvider: CachedCustomEntitiesConfigurationProvider,
	private val scopePathProvider: MapperScopePathProvider,
	private val builtinValidationConfigsProvider: ExtendableBuiltinEntityValidatorMapperConfigsProvider,
	private val bulkShareResultV2Mapper: CustomEntityBaseBulkShareResultV2Mapper,
) {
	private suspend fun CustomEntityBaseDto.toDomain(entityType: String): CustomEntityBase =
		mapCustomFromDto(
			dto = this,
			customEntitiesConfigurationProvider = customEntitiesConfigurationProvider,
			customEntityTypeId = entityType,
			doMap = { a, b ->
				mapper.map(
					customEntityBaseDto = a,
					entityTypeId = entityType,
					mapperExtensionsValidationContext = b
				)
			},
			scopePath = scopePathProvider.getScopePathFor(entityType),
			builtinValidationConfigsProvider = builtinValidationConfigsProvider,
		)

	private suspend fun CustomEntityBase.toDto(entityType: String): CustomEntityBaseDto =
		mapper.map(this, entityType)

	private suspend fun List<CustomEntityBaseDto>.toDomain(entityType: String): List<CustomEntityBase> =
		mapCustomFromDto(
			dtos = this,
			customEntitiesConfigurationProvider = customEntitiesConfigurationProvider,
			customEntityTypeId = entityType,
			doMap = { a, b ->
				mapper.map(
					customEntityBaseDto = a,
					entityTypeId = entityType,
					mapperExtensionsValidationContext = b
				)
			},
			scopePath = scopePathProvider.getScopePathFor(entityType),
			builtinValidationConfigsProvider = builtinValidationConfigsProvider,
		)

	private fun Flow<CustomEntityBase>.toDto(entityType: String): Flow<CustomEntityBaseDto> =
		map { mapper.map(it, entityType) }

	private fun Flow<EntityBulkShareResult<CustomEntityBase>>.toDtoUpdateResult(entityType: String): Flow<EntityBulkShareResultDto<CustomEntityBaseDto>> =
		map { bulkShareResultV2Mapper.map(it, entityType) }

}