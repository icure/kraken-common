package org.taktik.icure.services.external.rest.v2.controllers.core

import com.icure.cardinal.customentities.config.CustomEntityDefinition
import com.icure.cardinal.customentities.config.VersionedCustomEntitiesConfiguration
import com.icure.cardinal.customentities.util.CachedCustomEntitiesConfigurationProvider
import com.icure.cardinal.customentities.util.ExtendableBuiltinEntityValidatorMapperConfigsProvider
import com.icure.cardinal.errorreporting.MapperScopePathProvider
import io.swagger.v3.oas.annotations.tags.Tag
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import org.springframework.context.annotation.Profile
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.server.ResponseStatusException
import org.taktik.couchdb.DocIdentifier
import org.taktik.icure.asyncservice.CustomEntityService
import org.taktik.icure.cache.ReactorCacheInjector
import org.taktik.icure.entities.CustomEntityBase
import org.taktik.icure.entities.requests.EntityBulkShareResult
import org.taktik.icure.exceptions.NotFoundRequestException
import org.taktik.icure.services.external.rest.v2.dto.CustomEntityBaseDto
import org.taktik.icure.services.external.rest.v2.dto.ListOfIdsAndRevDto
import org.taktik.icure.services.external.rest.v2.dto.ListOfIdsDto
import org.taktik.icure.services.external.rest.v2.dto.couchdb.DocIdentifierDto
import org.taktik.icure.services.external.rest.v2.dto.requests.EntityBulkShareResultDto
import org.taktik.icure.services.external.rest.v2.mapper.CustomEntityBaseV2Mapper
import org.taktik.icure.services.external.rest.v2.mapper.IdWithRevV2Mapper
import org.taktik.icure.services.external.rest.v2.mapper.MappersWithCustomExtensions.mapCustomFromDto
import org.taktik.icure.services.external.rest.v2.mapper.couchdb.DocIdentifierV2Mapper
import org.taktik.icure.services.external.rest.v2.mapper.requests.CustomEntityBaseBulkShareResultV2Mapper
import org.taktik.icure.utils.injectCachedReactorContext
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

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
	private val customEntityService: CustomEntityService,
	private val reactorCacheInjector: ReactorCacheInjector,
	private val docIdentifierV2Mapper: DocIdentifierV2Mapper,
	private val idWithRevV2Mapper: IdWithRevV2Mapper,
) {
	private suspend inline fun <T> withCustomConfig(
		entityType: String,
		block: (config: VersionedCustomEntitiesConfiguration, customEntityDefinition: CustomEntityDefinition) -> T,
	): T {
		val config = customEntitiesConfigurationProvider.getConfigForCurrentUser()
		val customEntityConfig = config?.customEntities[entityType]?.takeIf { !it.disabled } ?: throw NotFoundRequestException("No custom entity config found for $entityType")
		return block(config, customEntityConfig)
	}

	private suspend inline fun <T> withCustomDefinition(
		entityType: String,
		block: (customEntityDefinition: CustomEntityDefinition) -> T,
	): T = withCustomConfig(entityType) { _, customEntityDefinition -> block(customEntityDefinition) }

	private inline fun <T> withCustomDefinitionFlow(
		entityType: String,
		crossinline block: (customEntityDefinition: CustomEntityDefinition) -> Flow<T>,
	): Flow<T> = flow {
		emitAll(withCustomConfig(entityType) { _, customEntityDefinition -> block(customEntityDefinition) })
	}

	private suspend inline fun <T> mappingCustom(
		entityType: String,
		dto: CustomEntityBaseDto,
		block: (mappedEntity: CustomEntityBase, customEntityConfig: CustomEntityDefinition) -> T,
	): T = withCustomConfig(entityType) { config, customEntityConfig ->
		validateEntityMetadataWithConfig(dto, customEntityConfig, entityType)
		return block(
			mapCustomFromDto(
				dto = dto,
				config = config,
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
			),
			customEntityConfig
		)
	}

	private suspend inline fun <T> mappingCustom(
		entityType: String,
		dtos: List<CustomEntityBaseDto>,
		block: (mappedEntities: List<CustomEntityBase>, customEntityConfig: CustomEntityDefinition) -> T,
	): T = withCustomConfig(entityType) { config, customEntityConfig ->
		dtos.forEach { dto ->
			validateEntityMetadataWithConfig(dto, customEntityConfig, entityType)
		}
		// Can do extra validation here
		return block(
			mapCustomFromDto(
				dtos = dtos,
				config = config,
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
			),
			customEntityConfig
		)
	}

	private fun validateEntityMetadataWithConfig(
		entity: CustomEntityBaseDto,
		customEntityDefinition: CustomEntityDefinition,
		customEntityType: String,
	) {
		if (customEntityDefinition.accessControl != CustomEntityDefinition.AccessControl.DelegationBased) {
			require (
				entity.secretForeignKeys.isEmpty() &&
					entity.cryptedForeignKeys.isEmpty() &&
					entity.delegations.isEmpty() &&
					entity.encryptionKeys.isEmpty() &&
					entity.securityMetadata == null
			) {
				"Custom entity type $customEntityType does not support delegations and/or encryption"
			}
		}
		if (customEntityDefinition.attachmentsConfiguration == null) {
			require (
				entity.dataAttachments.isEmpty() && entity.deletedAttachments.isEmpty()
			) {
				"Custom entity type $customEntityType does not support attachments"
			}
		}
	}

	private fun CustomEntityBase.toDto(entityType: String): CustomEntityBaseDto =
		mapper.map(this, entityType)

	private fun Flow<CustomEntityBase>.toDto(entityType: String): Flow<CustomEntityBaseDto> =
		map { mapper.map(it, entityType) }

	private fun Flow<EntityBulkShareResult<CustomEntityBase>>.toDtoUpdateResult(entityType: String): Flow<EntityBulkShareResultDto<CustomEntityBaseDto>> =
		map { bulkShareResultV2Mapper.map(it, entityType) }

	@PostMapping("/{entityType}")
	fun createCustomEntity(
		@PathVariable entityType: String,
		@RequestBody entityDto: CustomEntityBaseDto,
	): Mono<CustomEntityBaseDto> = reactorCacheInjector.monoWithCachedContext(10) {
		mappingCustom(entityType, entityDto) { entity, _ ->
			customEntityService.createCustomEntity(entityType = entityType, entity = entity).toDto(entityType)
		}
	}

	@PostMapping("/{entityType}/batch")
	fun createCustomEntities(
		@PathVariable entityType: String,
		@RequestBody entityDtos: List<CustomEntityBaseDto>,
	): Flux<CustomEntityBaseDto> = flow {
		emitAll(
			mappingCustom(entityType, entityDtos) { entities, _ ->
				customEntityService.createCustomEntities(entityType = entityType, entities = entities).toDto(entityType)
			}
		)
	}.injectCachedReactorContext(reactorCacheInjector, 100)

	@GetMapping("/{entityType}/{entityId}")
	fun getCustomEntity(
		@PathVariable entityType: String,
		@PathVariable entityId: String,
	): Mono<CustomEntityBaseDto> = reactorCacheInjector.monoWithCachedContext(10) {
		withCustomDefinition(entityType) {
			customEntityService.getCustomEntity(entityType = entityType, id = entityId)?.toDto(entityType)
				?: throw ResponseStatusException(
					HttpStatus.NOT_FOUND,
					"$entityType $entityId not found.",
				)
		}
	}

	@PostMapping("/{entityType}/byIds")
	fun getCustomEntities(
		@PathVariable entityType: String,
		@RequestBody entityIds: ListOfIdsDto,
	): Flux<CustomEntityBaseDto> = withCustomDefinitionFlow(entityType) {
		customEntityService.getCustomEntities(
			entityType,
			entityIds.ids
		).toDto(entityType)
	}.injectCachedReactorContext(reactorCacheInjector, 100)

	@PutMapping("/{entityType}")
	fun modifyCustomEntity(
		@PathVariable entityType: String,
		@RequestBody customEntityDto: CustomEntityBaseDto,
	): Mono<CustomEntityBaseDto> = reactorCacheInjector.monoWithCachedContext(10) {
		mappingCustom(entityType, customEntityDto) { customEntity, _ ->
			customEntityService.modifyCustomEntity(entityType = entityType, entity = customEntity).toDto(entityType)
		}
	}

	@PutMapping("/{entityType}/batch")
	fun modifyCustomEntities(
		@PathVariable entityType: String,
		@RequestBody customEntityDtos: List<CustomEntityBaseDto>,
	): Flux<CustomEntityBaseDto> = flow {
		emitAll(
			mappingCustom(entityType, customEntityDtos) { entities, _ ->
				customEntityService.modifyCustomEntities(entityType = entityType, entities = entities).toDto(entityType)
			}
		)
	}.injectCachedReactorContext(reactorCacheInjector, 100)

	@DeleteMapping("/{entityType}/{entityId}")
	fun deleteCustomEntity(
		@PathVariable entityType: String,
		@PathVariable entityId: String,
		@RequestParam(required = false) rev: String? = null,
	): Mono<DocIdentifierDto> = reactorCacheInjector.monoWithCachedContext(10) {
		withCustomDefinition(entityType) {
			customEntityService.deleteCustomEntity(entityType = entityType, id = entityId, rev = rev).let {
				docIdentifierV2Mapper.map(DocIdentifier(it.id, it.rev))
			}
		}
	}

	@PostMapping("/{entityType}/delete/batch")
	fun deleteCustomEntities(
		@PathVariable entityType: String,
		@RequestBody entityIds: ListOfIdsAndRevDto,
	): Flux<DocIdentifierDto> = withCustomDefinitionFlow(entityType) {
		customEntityService.deleteCustomEntities(
			entityType = entityType,
			identifiers = entityIds.ids.map(idWithRevV2Mapper::map)
		).map { docIdentifierV2Mapper.map(DocIdentifier(it.id, it.rev)) }
	}.injectCachedReactorContext(reactorCacheInjector, 100)

	@PostMapping("/{entityType}/undelete/{entityId}")
	fun undeleteCustomEntity(
		@PathVariable entityType: String,
		@PathVariable entityId: String,
		@RequestParam(required = true) rev: String,
	): Mono<CustomEntityBaseDto> = reactorCacheInjector.monoWithCachedContext(10) {
		withCustomDefinition(entityType) {
			customEntityService.undeleteCustomEntity(entityType = entityType, id = entityId, rev = rev).toDto(entityType)
		}
	}

	@PostMapping("/{entityType}/undelete/batch")
	fun undeleteCustomEntities(
		@PathVariable entityType: String,
		@RequestBody entityIds: ListOfIdsAndRevDto,
	): Flux<CustomEntityBaseDto> = withCustomDefinitionFlow(entityType) {
		customEntityService.undeleteCustomEntities(
			entityType = entityType,
			identifiers = entityIds.ids.map(idWithRevV2Mapper::map)
		).toDto(entityType)
	}.injectCachedReactorContext(reactorCacheInjector, 100)

	@DeleteMapping("/{entityType}/purge/{entityId}")
	fun purgeCustomEntity(
		@PathVariable entityType: String,
		@PathVariable entityId: String,
		@RequestParam(required = true) rev: String,
	): Mono<DocIdentifierDto> = reactorCacheInjector.monoWithCachedContext(10) {
		withCustomDefinition(entityType) {
			docIdentifierV2Mapper.map(customEntityService.purgeCustomEntity(entityType = entityType, id = entityId, rev = rev))
		}
	}

	@PostMapping("/{entityType}/purge/batch")
	fun purgeCustomEntities(
		@PathVariable entityType: String,
		@RequestBody entityIds: ListOfIdsAndRevDto,
	): Flux<DocIdentifierDto> = withCustomDefinitionFlow(entityType) {
		customEntityService.purgeCustomEntities(
			entityType = entityType,
			identifiers = entityIds.ids.map(idWithRevV2Mapper::map)
		).map(docIdentifierV2Mapper::map)
	}.injectCachedReactorContext(reactorCacheInjector, 100)
}