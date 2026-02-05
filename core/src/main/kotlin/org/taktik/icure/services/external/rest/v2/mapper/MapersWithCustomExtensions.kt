package org.taktik.icure.services.external.rest.v2.mapper

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import org.taktik.icure.domain.customentities.config.StandardRootEntitiesExtensionConfig
import org.taktik.icure.domain.customentities.config.StandardRootEntityExtensionConfig
import org.taktik.icure.domain.customentities.mapping.MapperExtensionsValidationContext
import org.taktik.icure.domain.customentities.util.CachedCustomEntitiesConfigurationProvider
import org.taktik.icure.domain.customentities.util.CustomEntityConfigResolutionContext
import org.taktik.icure.domain.customentities.util.resolveRequiredObjectReference
import org.taktik.icure.entities.RawJson
import org.taktik.icure.errorreporting.ErrorCollector
import org.taktik.icure.errorreporting.ScopePath
import org.taktik.icure.errorreporting.ScopedErrorCollector
import org.taktik.icure.errorreporting.appending
import org.taktik.icure.services.external.rest.v2.dto.base.IdentifiableDto
import org.taktik.icure.services.external.rest.v2.dto.embed.ExtendableDto
import org.taktik.icure.services.external.rest.v2.dto.embed.ExtendableRootDto

object MappersWithCustomExtensions {
	class MapperExtensionsValidationContextImpl(
		private val customEntityConfigResolutionContext: CustomEntityConfigResolutionContext,
		private val extensionConfig: StandardRootEntityExtensionConfig,
		scopePath: ScopePath?
	) : MapperExtensionsValidationContext {
		override val collector: ScopedErrorCollector = ScopedErrorCollector(
			ErrorCollector.Throwing,
			scopePath
		)

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
					collector,
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
						collector,
						entity.extensions ?: RawJson.JsonObject.empty
					)
			}
		}
	}

	suspend inline fun getMapperExtensionsValidationContext(
		customEntitiesConfigurationProvider: CachedCustomEntitiesConfigurationProvider,
		getExtension: StandardRootEntitiesExtensionConfig.() -> StandardRootEntityExtensionConfig?,
		scopePath: ScopePath?
	): MapperExtensionsValidationContext {
		val config = customEntitiesConfigurationProvider.getConfigForCurrentUser()
		val extension = config?.extensions?.getExtension()
		return if (extension != null) {
			MapperExtensionsValidationContextImpl(
				customEntityConfigResolutionContext = CustomEntityConfigResolutionContext.ofConfig(config),
				extensionConfig = extension,
				scopePath = scopePath,
			)
		} else {
			MapperExtensionsValidationContext.Empty
		}
	}

	suspend inline fun <DTO, OBJ> mapFromDtoWithExtension(
		dto: DTO,
		customEntitiesConfigurationProvider: CachedCustomEntitiesConfigurationProvider,
		getExtension: StandardRootEntitiesExtensionConfig.() -> StandardRootEntityExtensionConfig?,
		doMap: (DTO, MapperExtensionsValidationContext) -> OBJ,
		scopePath: ScopePath?
	): OBJ =
		doMap(dto, getMapperExtensionsValidationContext(customEntitiesConfigurationProvider, getExtension, scopePath))

	suspend inline fun <DTO : IdentifiableDto<String>, OBJ> mapFromDtoWithExtension(
		dtos: List<DTO>,
		customEntitiesConfigurationProvider: CachedCustomEntitiesConfigurationProvider,
		getExtension: StandardRootEntitiesExtensionConfig.() -> StandardRootEntityExtensionConfig?,
		doMap: (DTO, MapperExtensionsValidationContext) -> OBJ,
		scopePath: ScopePath?
	): List<OBJ> {
		val context = getMapperExtensionsValidationContext(customEntitiesConfigurationProvider, getExtension, scopePath)
		return dtos.map { dto ->
			scopePath.appending("(", dto.id, ")") {
				doMap(dto, context)
			}
		}
	}

	inline fun <DTO : IdentifiableDto<String>, OBJ> mapFromDtoWithExtension(
		dtos: Flow<DTO>,
		customEntitiesConfigurationProvider: CachedCustomEntitiesConfigurationProvider,
		crossinline getExtension: StandardRootEntitiesExtensionConfig.() -> StandardRootEntityExtensionConfig?,
		crossinline doMap: (DTO, MapperExtensionsValidationContext) -> OBJ,
		scopePath: ScopePath?
	): Flow<OBJ> = flow {
		val context = getMapperExtensionsValidationContext(customEntitiesConfigurationProvider, getExtension, scopePath)
		emitAll(dtos.map { dto ->
			scopePath.appending("(", dto.id, ")")  {
				doMap(dto, context)
			}
		})
	}
}