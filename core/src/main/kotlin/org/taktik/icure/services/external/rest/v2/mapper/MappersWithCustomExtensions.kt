package org.taktik.icure.services.external.rest.v2.mapper

import com.icure.cardinal.customentities.config.StandardRootEntitiesExtensionConfig
import com.icure.cardinal.customentities.config.StandardRootEntityExtensionConfig
import com.icure.cardinal.customentities.config.VersionedCustomEntitiesConfiguration
import com.icure.cardinal.customentities.config.VersionedObjectDefinitionReference
import com.icure.cardinal.customentities.config.typing.ObjectDefinition
import com.icure.cardinal.customentities.mapping.MapperExtensionsValidationContext
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
import org.taktik.icure.services.external.rest.v2.dto.base.CustomisableBuiltinRootDto
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
					val nextDefinition = nonNullDefinitionsStack.last().builtinExtension?.builtinProperties[propertyName]?.extensionDefinition?.let {
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

	/**
	 * Validates the customised type of a builtin entity received by the API, and gets the context to validate and map
	 * its extensions.
	 *
	 * Currently this is the only place where the backend interprets the extensions of builtin entities, so it is where
	 * disabled customised types are rejected (see kraken-cloud ADR 0010): every endpoint that receives an extendable
	 * builtin entity must map it through this method (directly or through [mapFromDtoWithExtension]).
	 * In the future some checks may also be done in the service and/or logic layers, e.g. the rules on writes that
	 * don't receive the whole entity (which need the type of the stored entity), or to prevent direct callers of the
	 * logic from bypassing these checks.
	 *
	 * - If the builtin entity type has no configuration only unextended entities exist: [dtoTypeId] and
	 *   [dtoModelVersion] must be null (and the returned context rejects any extension).
	 * - Otherwise unextended entities are disabled: [dtoTypeId] must be the id of an enabled customised type of the
	 *   builtin entity type, and [dtoModelVersion] its version.
	 *
	 * The type of the received entity may differ from the type of the stored entity: only the received one matters.
	 *
	 * @param extensionsOfType the configured customised types of the builtin entity type, by customised type id
	 * (`null` if the builtin entity type has no configuration).
	 */
	fun validateBuiltinTypeAndGetMapperExtensionsValidationContext(
		dtoTypeId: String?,
		dtoModelVersion: Int?,
		config: VersionedCustomEntitiesConfiguration?,
		extensionsOfType: Map<String, StandardRootEntityExtensionConfig>?,
		scopePath: ScopePath?,
		builtinValidationConfigsProvider: ExtendableBuiltinEntityValidatorMapperConfigsProvider,
	): MapperExtensionsValidationContext {
		require((dtoTypeId == null) == (dtoModelVersion == null)) {
			"customisedTypeId and customisedModelVersion must be both null or both set, got customisedTypeId $dtoTypeId and customisedModelVersion $dtoModelVersion"
		}
		if (config == null || extensionsOfType == null) {
			require(dtoTypeId == null) {
				"Unexpected customisedTypeId $dtoTypeId: no customised types are configured for this entity type"
			}
			return MapperExtensionsValidationContext.Empty
		}
		val enabledTypeIds = extensionsOfType.filterValues { !it.disabled }.keys
		requireNotNull(dtoTypeId) {
			"Missing customisedTypeId: entities of this type must use one of the enabled customised types $enabledTypeIds"
		}
		val extension = requireNotNull(extensionsOfType[dtoTypeId]) {
			"Unknown customisedTypeId $dtoTypeId: entities of this type must use one of the enabled customised types $enabledTypeIds"
		}
		require(!extension.disabled) {
			"Customised type $dtoTypeId is disabled: entities of this type must use one of the enabled customised types $enabledTypeIds"
		}
		require(dtoModelVersion == extension.version) {
			"Unexpected customised model version for customised type $dtoTypeId, expected ${extension.version}, got $dtoModelVersion"
		}
		return extensionValidationContext(config, extension, scopePath, builtinValidationConfigsProvider)
	}

	suspend inline fun validateCustomModelVersionAndGetMapperExtensionsValidationContext(
		dtoModelVersion: Int?,
		config: VersionedCustomEntitiesConfiguration?,
		customEntityTypeId: String,
		scopePath: ScopePath?,
		builtinValidationConfigsProvider: ExtendableBuiltinEntityValidatorMapperConfigsProvider,
	): MapperExtensionsValidationContext = validateModelVersionAndGetMapperExtensionsValidationContext(
		dtoModelVersion = dtoModelVersion,
		getCustomEntitiesConfigForRequest = { config },
		getExtension = { it.customEntities[customEntityTypeId] },
		scopePath = scopePath,
		builtinValidationConfigsProvider = builtinValidationConfigsProvider,
	)

	suspend inline fun validateModelVersionAndGetMapperExtensionsValidationContext(
		dtoModelVersion: Int?,
		getCustomEntitiesConfigForRequest: suspend () -> VersionedCustomEntitiesConfiguration?,
		getExtension: (config: VersionedCustomEntitiesConfiguration) -> VersionedObjectDefinitionReference?,
		scopePath: ScopePath?,
		builtinValidationConfigsProvider: ExtendableBuiltinEntityValidatorMapperConfigsProvider,
	): MapperExtensionsValidationContext {
		val config = getCustomEntitiesConfigForRequest()
		val extension = config?.let { getExtension(it) }
		return if (extension != null) {
			require (dtoModelVersion == extension.version) {
				"Unexpected customised model version, expected ${extension.version}, got $dtoModelVersion"
			}
			extensionValidationContext(config, extension, scopePath, builtinValidationConfigsProvider)
		} else {
			require (dtoModelVersion == null) {
				"Unexpected customised model version, expected null, got $dtoModelVersion"
			}
			MapperExtensionsValidationContext.Empty
		}
	}

	fun extensionValidationContext(
		config: VersionedCustomEntitiesConfiguration,
		extension: VersionedObjectDefinitionReference,
		scopePath: ScopePath?,
		builtinValidationConfigsProvider: ExtendableBuiltinEntityValidatorMapperConfigsProvider,
	): MapperExtensionsValidationContext {
		val customEntityConfigResolutionContext = CustomEntityConfigResolutionContext.ofConfig(config)
		return MapperExtensionsValidationContextImpl(
			customEntityConfigResolutionContext = customEntityConfigResolutionContext,
			errorCollector = ScopedErrorCollector(ErrorCollector.Throwing, scopePath),
			configsProvider = builtinValidationConfigsProvider,
			initialExtensionDefinition = customEntityConfigResolutionContext.resolveRequiredObjectReference(extension.objectDefinitionReference)
		)
	}

	suspend inline fun <DTO : CustomisableBuiltinRootDto, OBJ> mapFromDtoWithExtension(
		dto: DTO,
		getCustomEntitiesConfigForRequest: suspend () -> VersionedCustomEntitiesConfiguration?,
		getExtension: StandardRootEntitiesExtensionConfig.() -> Map<String, StandardRootEntityExtensionConfig>?,
		doMap: (DTO, MapperExtensionsValidationContext) -> OBJ,
		scopePath: ScopePath?,
		builtinValidationConfigsProvider: ExtendableBuiltinEntityValidatorMapperConfigsProvider,
	): OBJ {
		val config = getCustomEntitiesConfigForRequest()
		return doMap(dto, validateBuiltinTypeAndGetMapperExtensionsValidationContext(
			dtoTypeId = dto.customisedTypeId,
			dtoModelVersion = dto.customisedModelVersion,
			config = config,
			extensionsOfType = config?.extensions?.getExtension(),
			scopePath = scopePath,
			builtinValidationConfigsProvider = builtinValidationConfigsProvider,
		))
	}

	/**
	 * Bulk version of [mapFromDtoWithExtension]: each entity is validated independently, so the entities may have
	 * different customised types (and unextended entities are allowed only when the builtin entity type has no
	 * configuration).
	 */
	suspend inline fun <DTO, OBJ> mapFromDtoWithExtension(
		dtos: List<DTO>,
		getCustomEntitiesConfigForRequest: suspend () -> VersionedCustomEntitiesConfiguration?,
		getExtension: StandardRootEntitiesExtensionConfig.() -> Map<String, StandardRootEntityExtensionConfig>?,
		doMap: (DTO, MapperExtensionsValidationContext) -> OBJ,
		scopePath: ScopePath?,
		builtinValidationConfigsProvider: ExtendableBuiltinEntityValidatorMapperConfigsProvider,
	): List<OBJ> where DTO : IdentifiableDto<String>, DTO : CustomisableBuiltinRootDto {
		if (dtos.isEmpty()) return emptyList()
		val config = getCustomEntitiesConfigForRequest()
		val extensionsOfType = config?.extensions?.getExtension()
		val contextsByTypeAndVersion = mutableMapOf<Pair<String?, Int?>, MapperExtensionsValidationContext>()
		return dtos.map { dto ->
			scopePath.appending("(", dto.id, ")") {
				val context = contextsByTypeAndVersion.getOrPut(dto.customisedTypeId to dto.customisedModelVersion) {
					validateBuiltinTypeAndGetMapperExtensionsValidationContext(
						dtoTypeId = dto.customisedTypeId,
						dtoModelVersion = dto.customisedModelVersion,
						config = config,
						extensionsOfType = extensionsOfType,
						scopePath = scopePath,
						builtinValidationConfigsProvider = builtinValidationConfigsProvider,
					)
				}
				doMap(dto, context)
			}
		}
	}


	suspend inline fun <DTO : CustomisableRootDto, OBJ> mapCustomFromDto(
		dto: DTO,
		config: VersionedCustomEntitiesConfiguration?,
		customEntityTypeId: String,
		doMap: (DTO, MapperExtensionsValidationContext) -> OBJ,
		scopePath: ScopePath?,
		builtinValidationConfigsProvider: ExtendableBuiltinEntityValidatorMapperConfigsProvider,
	): OBJ =
		doMap(dto, validateCustomModelVersionAndGetMapperExtensionsValidationContext(
			dtoModelVersion = dto.customisedModelVersion,
			config = config,
			customEntityTypeId = customEntityTypeId,
			scopePath = scopePath,
			builtinValidationConfigsProvider = builtinValidationConfigsProvider,
		))

	suspend inline fun <DTO, OBJ> mapCustomFromDto(
		dtos: List<DTO>,
		config: VersionedCustomEntitiesConfiguration?,
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
				config = config,
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