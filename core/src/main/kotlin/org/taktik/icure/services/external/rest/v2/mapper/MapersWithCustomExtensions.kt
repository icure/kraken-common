package org.taktik.icure.services.external.rest.v2.mapper

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import org.taktik.icure.domain.customentities.config.ExtensionsConfiguration
import org.taktik.icure.domain.customentities.config.StandardRootEntityExtensionConfig
import org.taktik.icure.domain.customentities.mapping.MapperExtensionsValidationContext
import org.taktik.icure.domain.customentities.util.CachedCustomEntitiesConfigurationProvider
import org.taktik.icure.domain.customentities.util.CustomEntityConfigResolutionContext
import org.taktik.icure.domain.customentities.util.ResolutionPath
import org.taktik.icure.domain.customentities.util.resolveRequiredObjectReference
import org.taktik.icure.entities.RawJson
import org.taktik.icure.services.external.rest.v2.dto.embed.ExtendableDto
import org.taktik.icure.services.external.rest.v2.dto.embed.ExtendableRootDto

object MappersWithCustomExtensions {
	class MapperExtensionsValidationContextImpl(
		private val customEntityConfigResolutionContext: CustomEntityConfigResolutionContext,
		private val extensionConfig: StandardRootEntityExtensionConfig,
	) : MapperExtensionsValidationContext {
		override fun validateAndMapRootExtensionsForStore(
			entity: ExtendableRootDto
		): RawJson.JsonObject {
			//TODO
			// validate extension version is accepted
			// - Currently we are only accepting one version of entity. If config wants version X of Entity you can't provide Entity at (X-1) or (X+1)
			// - In future should support multiple versions to allow for smoother app upgrades?
			return customEntityConfigResolutionContext
				.resolveRequiredObjectReference(extensionConfig.objectDefinitionReference)
				.validateAndMapValueForStore(
					customEntityConfigResolutionContext,
					ResolutionPath(arrayListOf("?.")), // TODO context from mapper
					entity.extensions ?: RawJson.JsonObject.empty
				)
		}

		override fun validateAndMapEmbeddedExtensionsForStore(
			entity: ExtendableDto,
			entityCanonicalName: String
		): RawJson.JsonObject? {
			val config = extensionConfig.embeddedEntitiesConfigs[entityCanonicalName]
			return if (config == null) {
				MapperExtensionsValidationContext.Empty.validateAndMapEmbeddedExtensionsForStore(entity, entityCanonicalName)
			} else {
				return customEntityConfigResolutionContext
					.resolveRequiredObjectReference(config)
					.validateAndMapValueForStore(
						customEntityConfigResolutionContext,
						ResolutionPath(arrayListOf("?.")), // TODO context from mapper
						entity.extensions ?: RawJson.JsonObject.empty
					)
			}
		}
	}

	suspend inline fun getMapperExtensionsValidationContext(
		customEntitiesConfigurationProvider: CachedCustomEntitiesConfigurationProvider,
		getExtension: ExtensionsConfiguration.() -> StandardRootEntityExtensionConfig?,
	): MapperExtensionsValidationContext {
		val config = customEntitiesConfigurationProvider.getConfigForCurrentUser()
		val extension = config?.extensions?.getExtension()
		return if (extension != null) {
			MapperExtensionsValidationContextImpl(
				customEntityConfigResolutionContext = CustomEntityConfigResolutionContext.ofConfig(config),
				extensionConfig = extension
			)
		} else {
			MapperExtensionsValidationContext.Empty
		}
	}

	suspend inline fun <DTO, OBJ> mapFromDtoWithExtension(
		dto: DTO,
		customEntitiesConfigurationProvider: CachedCustomEntitiesConfigurationProvider,
		getExtension: ExtensionsConfiguration.() -> StandardRootEntityExtensionConfig?,
		doMap: (DTO, MapperExtensionsValidationContext) -> OBJ,
		crossinline getPathRoot: (DTO) -> String // TODO figure out how to use
	): OBJ =
		doMap(dto, getMapperExtensionsValidationContext(customEntitiesConfigurationProvider, getExtension))

	suspend inline fun <DTO, OBJ> mapFromDtoWithExtension(
		dtos: List<DTO>,
		customEntitiesConfigurationProvider: CachedCustomEntitiesConfigurationProvider,
		getExtension: ExtensionsConfiguration.() -> StandardRootEntityExtensionConfig?,
		doMap: (DTO, MapperExtensionsValidationContext) -> OBJ,
		crossinline getPathRoot: (DTO) -> String // TODO figure out how to use
	): List<OBJ> {
		val context = getMapperExtensionsValidationContext(customEntitiesConfigurationProvider, getExtension)
		return dtos.map { dto -> doMap(dto, context) }
	}

	inline fun <DTO, OBJ> mapFromDtoWithExtension(
		dtos: Flow<DTO>,
		customEntitiesConfigurationProvider: CachedCustomEntitiesConfigurationProvider,
		crossinline getExtension: ExtensionsConfiguration.() -> StandardRootEntityExtensionConfig?,
		crossinline doMap: (DTO, MapperExtensionsValidationContext) -> OBJ,
		crossinline getPathRoot: (DTO) -> String // TODO figure out how to use
	): Flow<OBJ> = flow {
		val context = getMapperExtensionsValidationContext(customEntitiesConfigurationProvider, getExtension)
		emitAll(dtos.map { dto -> doMap(dto, context) })
	}
}