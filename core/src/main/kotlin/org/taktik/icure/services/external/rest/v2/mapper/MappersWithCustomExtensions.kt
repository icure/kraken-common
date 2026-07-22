package org.taktik.icure.services.external.rest.v2.mapper

import com.icure.cardinal.customentities.config.StandardRootEntitiesExtensionConfig
import com.icure.cardinal.customentities.config.StandardRootEntityExtensionConfig
import com.icure.cardinal.customentities.config.VersionedCustomEntitiesConfiguration
import com.icure.cardinal.customentities.config.VersionedObjectDefinitionReference
import com.icure.cardinal.customentities.config.typing.ObjectDefinition
import com.icure.cardinal.customentities.mapping.MapperExtensionsValidationContext
import com.icure.cardinal.customentities.util.CachedCustomEntitiesConfigurationProvider
import com.icure.cardinal.customentities.util.CustomEntityConfigResolutionContext
import com.icure.cardinal.customentities.util.CustomEntityValueValidationContext
import com.icure.cardinal.customentities.util.ExtendableBuiltinEntityValidatorMapperConfigsProvider
import com.icure.cardinal.customentities.util.MapperBasedExtendableBuiltinEntityValidator
import com.icure.cardinal.customentities.util.resolveRequiredObjectReference
import com.icure.cardinal.entities.RawJson
import com.icure.cardinal.errorreporting.ErrorCollector
import com.icure.cardinal.errorreporting.ScopePath
import com.icure.cardinal.errorreporting.ScopedErrorCollector
import com.icure.cardinal.errorreporting.appending
import org.springframework.stereotype.Component
import org.taktik.icure.exceptions.NotFoundRequestException
import org.taktik.icure.services.external.rest.v2.dto.base.CustomisableRootDto
import org.taktik.icure.services.external.rest.v2.dto.base.IdentifiableDto


@Component
object MappersWithCustomExtensions {
	class MapperExtensionsValidationContextImpl(
		customEntityConfigResolutionContext: CustomEntityConfigResolutionContext,
		errorCollector: ScopedErrorCollector,
		configsProvider: ExtendableBuiltinEntityValidatorMapperConfigsProvider,
		initialExtensionDefinition: ObjectDefinition,
	) : MapperExtensionsValidationContext {
		private class ExtensionValidationContext(
			initialExtensionDefinition: ObjectDefinition,
			private val resolutionContext: CustomEntityConfigResolutionContext
		) {
			private val nonNullDefinitionsStack = ArrayDeque<ObjectDefinition>(10).also {
				it.addLast(initialExtensionDefinition)
			}
			private var nullTopElementsCount = 0

			fun enterProperty(propertyName: String) {
				if (nullTopElementsCount > 0) {
					nullTopElementsCount++
				} else {
					val nextDefinition = nonNullDefinitionsStack.last().builtinExtension?.extendedBuiltinProperties[propertyName]?.let {
						resolutionContext.resolveRequiredObjectReference(it)
					}
					if (nextDefinition != null) {
						nonNullDefinitionsStack.addLast(nextDefinition)
					} else {
						nullTopElementsCount++
					}
				}
			}

			fun exitProperty() {
				if (nullTopElementsCount > 0) {
					nullTopElementsCount--
				} else {
					nonNullDefinitionsStack.removeLast()
				}
			}

			fun getLastDefinition(): ObjectDefinition? {
				return if (nullTopElementsCount > 0) {
					null
				} else {
					nonNullDefinitionsStack.last()
				}
			}
		}

		private val extensionValidationContext = ExtensionValidationContext(initialExtensionDefinition, customEntityConfigResolutionContext)

		private val fullContext = CustomEntityValueValidationContext(
			resolution = customEntityConfigResolutionContext,
			validation = errorCollector,
			builtinValidation = MapperBasedExtendableBuiltinEntityValidator(
				configsProvider = configsProvider,
				resolutionContext = customEntityConfigResolutionContext,
			),
			isDecryptedContext = false // Validation on kraken does not have the decrypted context
		)

		override fun enterProperty(propertyName: String) {
			fullContext.validation.path?.apply {
				enterScope(".")
				enterScope(propertyName)
			}
			extensionValidationContext.enterProperty(propertyName)
		}

		override fun exitProperty() {
			fullContext.validation.path?.apply {
				exitScope()
				exitScope()
			}
			extensionValidationContext.exitProperty()
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
			fullContext.validation.appending(".extensions") {
				val currentExtensionDefinition = extensionValidationContext.getLastDefinition()
				if (currentExtensionDefinition == null) {
					if (extensionValue != null) {
						fullContext.validation.addError("GEV-BUILTIN-EXTENSIONNOTALLOWED")
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
	}

	suspend inline fun validateBuiltinModelVersionAndGetMapperExtensionsValidationContext(
		dtoModelVersion: Int?,
		customEntitiesConfigurationProvider: CachedCustomEntitiesConfigurationProvider,
		getExtension: StandardRootEntitiesExtensionConfig.() -> StandardRootEntityExtensionConfig?,
		scopePath: ScopePath?,
		builtinValidationConfigsProvider: ExtendableBuiltinEntityValidatorMapperConfigsProvider,
	): MapperExtensionsValidationContext = validateModelVersionAndGetMapperExtensionsValidationContext(
		dtoModelVersion = dtoModelVersion,
		customEntitiesConfigurationProvider = customEntitiesConfigurationProvider,
		getExtension = { it.extensions.getExtension() },
		scopePath = scopePath,
		builtinValidationConfigsProvider = builtinValidationConfigsProvider,
	)

	suspend inline fun validateCustomModelVersionAndGetMapperExtensionsValidationContext(
		dtoModelVersion: Int?,
		customEntitiesConfigurationProvider: CachedCustomEntitiesConfigurationProvider,
		customEntityTypeId: String,
		scopePath: ScopePath?,
		builtinValidationConfigsProvider: ExtendableBuiltinEntityValidatorMapperConfigsProvider,
	): MapperExtensionsValidationContext = validateModelVersionAndGetMapperExtensionsValidationContext(
		dtoModelVersion = dtoModelVersion,
		customEntitiesConfigurationProvider = customEntitiesConfigurationProvider,
		getExtension = {
			it.customEntities[customEntityTypeId] ?: throw NotFoundRequestException("Custom entity type $customEntityTypeId not found")
		},
		scopePath = scopePath,
		builtinValidationConfigsProvider = builtinValidationConfigsProvider,
	)

	suspend inline fun validateModelVersionAndGetMapperExtensionsValidationContext(
		dtoModelVersion: Int?,
		customEntitiesConfigurationProvider: CachedCustomEntitiesConfigurationProvider,
		getExtension: (config: VersionedCustomEntitiesConfiguration) -> VersionedObjectDefinitionReference?,
		scopePath: ScopePath?,
		builtinValidationConfigsProvider: ExtendableBuiltinEntityValidatorMapperConfigsProvider,
	): MapperExtensionsValidationContext {
		val config = customEntitiesConfigurationProvider.getConfigForCurrentUser()
		val extension = config?.let { getExtension(it) }
		return if (extension != null) {
			require (dtoModelVersion == extension.version) {
				"Unexpected customised model version, expected ${extension.version}, got $dtoModelVersion"
			}
			val customEntityConfigResolutionContext = CustomEntityConfigResolutionContext.ofConfig(config)
			MapperExtensionsValidationContextImpl(
				customEntityConfigResolutionContext = customEntityConfigResolutionContext,
				errorCollector = ScopedErrorCollector(ErrorCollector.Throwing, scopePath),
				configsProvider = builtinValidationConfigsProvider,
				initialExtensionDefinition = customEntityConfigResolutionContext.resolveRequiredObjectReference(extension.objectDefinitionReference)
			)
		} else {
			require (dtoModelVersion == null) {
				"Unexpected customised model version, expected null, got $dtoModelVersion"
			}
			MapperExtensionsValidationContext.Empty
		}
	}

	suspend inline fun <DTO : CustomisableRootDto, OBJ> mapFromDtoWithExtension(
		dto: DTO,
		customEntitiesConfigurationProvider: CachedCustomEntitiesConfigurationProvider,
		getExtension: StandardRootEntitiesExtensionConfig.() -> StandardRootEntityExtensionConfig?,
		doMap: (DTO, MapperExtensionsValidationContext) -> OBJ,
		scopePath: ScopePath?,
		builtinValidationConfigsProvider: ExtendableBuiltinEntityValidatorMapperConfigsProvider,
	): OBJ =
		doMap(dto, validateBuiltinModelVersionAndGetMapperExtensionsValidationContext(
			dtoModelVersion = dto.customisedModelVersion,
			customEntitiesConfigurationProvider = customEntitiesConfigurationProvider,
			getExtension = getExtension,
			scopePath = scopePath,
			builtinValidationConfigsProvider = builtinValidationConfigsProvider,
		))

	suspend inline fun <DTO, OBJ> mapFromDtoWithExtension(
		dtos: List<DTO>,
		customEntitiesConfigurationProvider: CachedCustomEntitiesConfigurationProvider,
		getExtension: StandardRootEntitiesExtensionConfig.() -> StandardRootEntityExtensionConfig?,
		doMap: (DTO, MapperExtensionsValidationContext) -> OBJ,
		scopePath: ScopePath?,
		builtinValidationConfigsProvider: ExtendableBuiltinEntityValidatorMapperConfigsProvider,
	): List<OBJ> where DTO : IdentifiableDto<String>, DTO : CustomisableRootDto =
		doMapList(
			dtos,
			doMap,
			scopePath,
		) {
			validateBuiltinModelVersionAndGetMapperExtensionsValidationContext(
				dtoModelVersion = it,
				customEntitiesConfigurationProvider = customEntitiesConfigurationProvider,
				getExtension = getExtension,
				scopePath = scopePath,
				builtinValidationConfigsProvider = builtinValidationConfigsProvider,
			)
		}


	suspend inline fun <DTO : CustomisableRootDto, OBJ> mapCustomFromDto(
		dto: DTO,
		customEntitiesConfigurationProvider: CachedCustomEntitiesConfigurationProvider,
		customEntityTypeId: String,
		doMap: (DTO, MapperExtensionsValidationContext) -> OBJ,
		scopePath: ScopePath?,
		builtinValidationConfigsProvider: ExtendableBuiltinEntityValidatorMapperConfigsProvider,
	): OBJ =
		doMap(dto, validateCustomModelVersionAndGetMapperExtensionsValidationContext(
			dtoModelVersion = dto.customisedModelVersion,
			customEntitiesConfigurationProvider = customEntitiesConfigurationProvider,
			customEntityTypeId = customEntityTypeId,
			scopePath = scopePath,
			builtinValidationConfigsProvider = builtinValidationConfigsProvider,
		))

	suspend inline fun <DTO, OBJ> mapCustomFromDto(
		dtos: List<DTO>,
		customEntitiesConfigurationProvider: CachedCustomEntitiesConfigurationProvider,
		customEntityTypeId: String,
		doMap: (DTO, MapperExtensionsValidationContext) -> OBJ,
		scopePath: ScopePath?,
		builtinValidationConfigsProvider: ExtendableBuiltinEntityValidatorMapperConfigsProvider,
	): List<OBJ> where DTO : IdentifiableDto<String>, DTO : CustomisableRootDto =
		doMapList(
			dtos,
			doMap,
			scopePath
		) {
			validateCustomModelVersionAndGetMapperExtensionsValidationContext(
				dtoModelVersion = it,
				customEntitiesConfigurationProvider = customEntitiesConfigurationProvider,
				customEntityTypeId = customEntityTypeId,
				scopePath = scopePath,
				builtinValidationConfigsProvider = builtinValidationConfigsProvider,
			)
		}

	suspend inline fun <DTO, OBJ> doMapList(
		dtos: List<DTO>,
		doMap: (DTO, MapperExtensionsValidationContext) -> OBJ,
		scopePath: ScopePath?,
		validateModelVersionAndLoadContext: (modelVersion: Int?) -> MapperExtensionsValidationContext,
	): List<OBJ> where DTO : IdentifiableDto<String>, DTO : CustomisableRootDto {
		if (dtos.isEmpty()) return emptyList()
		val modelVersion = dtos.first().customisedModelVersion
		require (dtos.all { it.customisedModelVersion == modelVersion }) {
			"All provided entities of the same type should use the same customisedModelVersion"
		} // In future if we want to support multiple valid versions in parallel this can change to maybe a set.
		val context = validateModelVersionAndLoadContext(modelVersion)
		return dtos.map { dto ->
			scopePath.appending("(", dto.id, ")") {
				doMap(dto, context)
			}
		}
	}
}