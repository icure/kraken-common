package org.taktik.icure.services.external.rest.v2.mapper

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import org.springframework.stereotype.Component
import org.taktik.icure.customentities.util.MapperBasedExtendableBuiltinEntityValidator
import org.taktik.icure.customentities.util.ExtendableBuiltinEntityValidatorMapperConfigsProvider
import org.taktik.icure.domain.customentities.config.StandardRootEntitiesExtensionConfig
import org.taktik.icure.domain.customentities.config.StandardRootEntityExtensionConfig
import org.taktik.icure.domain.customentities.config.typing.ObjectDefinition
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


@Component
object MappersWithCustomExtensions {
	class MapperExtensionsValidationContextImpl(
		customEntityConfigResolutionContext: CustomEntityConfigResolutionContext,
		errorCollector: ScopedErrorCollector,
		builtinDefinitions: BuiltinDefinitionsProvider,
		configsProvider: ExtendableBuiltinEntityValidatorMapperConfigsProvider,
		initialExtensionDefinition: ObjectDefinition,
	) : MapperExtensionsValidationContext {
		private val currentExtensionDefinitionStack = ArrayDeque<ObjectDefinition?>(10).also {
			it.addLast(initialExtensionDefinition)
		}

		private val fullContext = CustomEntityConfigValidationContext(
			resolution = customEntityConfigResolutionContext,
			validation = errorCollector,
			builtinValidation = MapperBasedExtendableBuiltinEntityValidator(
				configsProvider = configsProvider,
				resolutionContext = customEntityConfigResolutionContext,
				errorCollector = errorCollector,
				builtinDefinitions = builtinDefinitions,
			),
			builtinDefinitions = builtinDefinitions,
		)

		override fun enterProperty(propertyName: String) {
			fullContext.validation.path?.apply {
				enterScope(".")
				enterScope(propertyName)
			}
			currentExtensionDefinitionStack.addLast(
				currentExtensionDefinitionStack.last()?.extendedBuiltinProperties?.get(propertyName)?.let {
					fullContext.resolution.resolveRequiredObjectReference(it)
				}
			)
		}

		override fun exitProperty() {
			fullContext.validation.path?.apply {
				exitScope()
				exitScope()
			}
			currentExtensionDefinitionStack.removeLast()
		}

		override fun enterListItem(index: Int) {
			fullContext.validation.path?.apply {
				enterScope("[")
				enterScope(index.toString())
				enterScope("]")
			}
		}

		override fun exitListItem() {
			fullContext.validation.path?.apply {
				exitScope()
				exitScope()
				exitScope()
			}
		}

		override fun enterMapEntry(key: Any) {
			fullContext.validation.path?.apply {
				enterScope("{")
				enterScope(key)
				enterScope("}")
			}
		}

		override fun exitMapEntry() {
			fullContext.validation.path?.apply {
				exitScope()
				exitScope()
				exitScope()
			}
		}

		override fun validateAndMapCurrentExtension(extensionValue: RawJson.JsonObject?): RawJson.JsonObject? {
			val currentExtensionDefinition = currentExtensionDefinitionStack.last()
			if (currentExtensionDefinition == null) {
				if (extensionValue != null) {
					fullContext.validation.addError("GE-BUILTIN-EXTENSIONNOTALLOWED")
				}
				return extensionValue
			} else {
				return currentExtensionDefinition.validateAndMapExtensionValueForStore(
					fullContext,
					extensionValue ?: RawJson.JsonObject.empty
				)
			}
		}
	}

	suspend inline fun getMapperExtensionsValidationContext(
		customEntitiesConfigurationProvider: CachedCustomEntitiesConfigurationProvider,
		getExtension: StandardRootEntitiesExtensionConfig.() -> StandardRootEntityExtensionConfig?,
		scopePath: ScopePath?,
		builtinValidationConfigsProvider: ExtendableBuiltinEntityValidatorMapperConfigsProvider,
		builtinDefinitions: BuiltinDefinitionsProvider,
	): MapperExtensionsValidationContext {
		val config = customEntitiesConfigurationProvider.getConfigForCurrentUser()
		val extension = config?.extensions?.getExtension()
		return if (extension != null) {
			val customEntityConfigResolutionContext = CustomEntityConfigResolutionContext.ofConfig(config)
			MapperExtensionsValidationContextImpl(
				customEntityConfigResolutionContext = CustomEntityConfigResolutionContext.ofConfig(config),
				errorCollector = ScopedErrorCollector(ErrorCollector.Throwing, scopePath),
				builtinDefinitions = builtinDefinitions,
				configsProvider = builtinValidationConfigsProvider,
				initialExtensionDefinition = customEntityConfigResolutionContext.resolveRequiredObjectReference(extension.objectDefinitionReference)
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
		builtinValidationConfigsProvider: ExtendableBuiltinEntityValidatorMapperConfigsProvider,
		builtinDefinitions: BuiltinDefinitionsProvider,
	): OBJ =
		doMap(dto, getMapperExtensionsValidationContext(
			customEntitiesConfigurationProvider = customEntitiesConfigurationProvider,
			getExtension = getExtension,
			scopePath = scopePath,
			builtinValidationConfigsProvider = builtinValidationConfigsProvider,
			builtinDefinitions = builtinDefinitions,
		))

	suspend inline fun <DTO : IdentifiableDto<String>, OBJ> mapFromDtoWithExtension(
		dtos: List<DTO>,
		customEntitiesConfigurationProvider: CachedCustomEntitiesConfigurationProvider,
		getExtension: StandardRootEntitiesExtensionConfig.() -> StandardRootEntityExtensionConfig?,
		doMap: (DTO, MapperExtensionsValidationContext) -> OBJ,
		scopePath: ScopePath?,
		builtinValidationConfigsProvider: ExtendableBuiltinEntityValidatorMapperConfigsProvider,
		builtinDefinitions: BuiltinDefinitionsProvider,
	): List<OBJ> {
		val context = getMapperExtensionsValidationContext(
			customEntitiesConfigurationProvider = customEntitiesConfigurationProvider,
			getExtension = getExtension,
			scopePath = scopePath,
			builtinValidationConfigsProvider = builtinValidationConfigsProvider,
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
		builtinValidationConfigsProvider: ExtendableBuiltinEntityValidatorMapperConfigsProvider,
		builtinDefinitions: BuiltinDefinitionsProvider,
	): Flow<OBJ> = flow {
		val context = getMapperExtensionsValidationContext(
			customEntitiesConfigurationProvider = customEntitiesConfigurationProvider,
			getExtension = getExtension,
			scopePath = scopePath,
			builtinValidationConfigsProvider = builtinValidationConfigsProvider,
			builtinDefinitions = builtinDefinitions,
		)
		emitAll(dtos.map { dto ->
			scopePath.appending("(", dto.id, ")")  {
				doMap(dto, context)
			}
		})
	}
}