package org.taktik.icure.services.external.rest.v2.mapper

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import org.springframework.stereotype.Component
import org.taktik.icure.customentities.util.CustomEntityBuiltinValidatorProvider
import org.taktik.icure.domain.customentities.config.ExtendableEntityName
import org.taktik.icure.domain.customentities.config.StandardRootEntitiesExtensionConfig
import org.taktik.icure.domain.customentities.config.StandardRootEntityExtensionConfig
import org.taktik.icure.domain.customentities.mapping.MapperExtensionsValidationContext
import org.taktik.icure.domain.customentities.util.BuiltinDefinitionsProvider
import org.taktik.icure.domain.customentities.util.CachedCustomEntitiesConfigurationProvider
import org.taktik.icure.domain.customentities.util.CustomEntityConfigResolutionContext
import org.taktik.icure.domain.customentities.util.CustomEntityConfigValidationContext
import org.taktik.icure.domain.customentities.util.resolveRequiredObjectReference
import org.taktik.icure.entities.RawJson
import org.taktik.icure.errorreporting.ErrorCollector
import org.taktik.icure.errorreporting.ScopePath
import org.taktik.icure.errorreporting.ScopedErrorCollector
import org.taktik.icure.errorreporting.appending
import org.taktik.icure.services.external.rest.v2.dto.base.IdentifiableDto
import org.taktik.icure.services.external.rest.v2.dto.embed.ExtendableDto
import org.taktik.icure.services.external.rest.v2.dto.embed.ExtendableRootDto


@Component
object MappersWithCustomExtensions {
	class MapperExtensionsValidationContextImpl(
		customEntityConfigResolutionContext: CustomEntityConfigResolutionContext,
		private val extensionConfig: StandardRootEntityExtensionConfig,
		scopePath: ScopePath?,
		builtinValidationProvider: CustomEntityBuiltinValidatorProvider,
		builtinDefinitions: BuiltinDefinitionsProvider,
	) : MapperExtensionsValidationContext {

		private val fullContext = CustomEntityConfigValidationContext(
			resolution = customEntityConfigResolutionContext,
			validation = ScopedErrorCollector(
				ErrorCollector.Throwing,
				scopePath
			),
			builtinValidation = builtinValidationProvider.getValidator(this),
			builtinDefinitions = builtinDefinitions,
		)
		override val collector: ScopedErrorCollector get() = fullContext.validation

		override fun validateAndMapRootExtensionsForStore(
			entity: ExtendableRootDto
		): RawJson.JsonObject {
			//TODO
			// validate extension version is accepted
			// - Currently we are only accepting one version of entity. If config wants version X of Entity you can't provide Entity at (X-1) or (X+1)
			// - In future should support multiple versions to allow for smoother app upgrades?
			return fullContext.resolution
				.resolveRequiredObjectReference(extensionConfig.objectDefinitionReference)
				.validateAndMapValueForStore(
					fullContext,
					entity.extensions ?: RawJson.JsonObject.empty
				)
		}

		override fun validateAndMapEmbeddedExtensionsForStore(
			entity: ExtendableDto,
			entityName: ExtendableEntityName
		): RawJson.JsonObject? {
			val config = extensionConfig.embeddedEntitiesConfigs[entityName]
			return if (config == null) {
				MapperExtensionsValidationContext.Empty.validateAndMapEmbeddedExtensionsForStore(entity, entityName)
			} else {
				fullContext.resolution
					.resolveRequiredObjectReference(config)
					.validateAndMapValueForStore(
						fullContext,
						entity.extensions ?: RawJson.JsonObject.empty
					)
			}
		}
	}

	suspend inline fun getMapperExtensionsValidationContext(
		customEntitiesConfigurationProvider: CachedCustomEntitiesConfigurationProvider,
		getExtension: StandardRootEntitiesExtensionConfig.() -> StandardRootEntityExtensionConfig?,
		scopePath: ScopePath?,
		builtinValidationProvider: CustomEntityBuiltinValidatorProvider,
		builtinDefinitions: BuiltinDefinitionsProvider,
	): MapperExtensionsValidationContext {
		val config = customEntitiesConfigurationProvider.getConfigForCurrentUser()
		val extension = config?.extensions?.getExtension()
		return if (extension != null) {
			MapperExtensionsValidationContextImpl(
				customEntityConfigResolutionContext = CustomEntityConfigResolutionContext.ofConfig(config),
				extensionConfig = extension,
				scopePath = scopePath,
				builtinValidationProvider = builtinValidationProvider,
				builtinDefinitions = builtinDefinitions,
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
		scopePath: ScopePath?,
		builtinValidationProvider: CustomEntityBuiltinValidatorProvider,
		builtinDefinitions: BuiltinDefinitionsProvider,
	): OBJ =
		doMap(dto, getMapperExtensionsValidationContext(
			customEntitiesConfigurationProvider = customEntitiesConfigurationProvider,
			getExtension = getExtension,
			scopePath = scopePath,
			builtinValidationProvider = builtinValidationProvider,
			builtinDefinitions = builtinDefinitions,
		))

	suspend inline fun <DTO : IdentifiableDto<String>, OBJ> mapFromDtoWithExtension(
		dtos: List<DTO>,
		customEntitiesConfigurationProvider: CachedCustomEntitiesConfigurationProvider,
		getExtension: StandardRootEntitiesExtensionConfig.() -> StandardRootEntityExtensionConfig?,
		doMap: (DTO, MapperExtensionsValidationContext) -> OBJ,
		scopePath: ScopePath?,
		builtinValidationProvider: CustomEntityBuiltinValidatorProvider,
		builtinDefinitions: BuiltinDefinitionsProvider,
	): List<OBJ> {
		val context = getMapperExtensionsValidationContext(
			customEntitiesConfigurationProvider = customEntitiesConfigurationProvider,
			getExtension = getExtension,
			scopePath = scopePath,
			builtinValidationProvider = builtinValidationProvider,
			builtinDefinitions = builtinDefinitions,
		)
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
		scopePath: ScopePath?,
		builtinValidationProvider: CustomEntityBuiltinValidatorProvider,
		builtinDefinitions: BuiltinDefinitionsProvider,
	): Flow<OBJ> = flow {
		val context = getMapperExtensionsValidationContext(
			customEntitiesConfigurationProvider = customEntitiesConfigurationProvider,
			getExtension = getExtension,
			scopePath = scopePath,
			builtinValidationProvider = builtinValidationProvider,
			builtinDefinitions = builtinDefinitions,
		)
		emitAll(dtos.map { dto ->
			scopePath.appending("(", dto.id, ")")  {
				doMap(dto, context)
			}
		})
	}
}