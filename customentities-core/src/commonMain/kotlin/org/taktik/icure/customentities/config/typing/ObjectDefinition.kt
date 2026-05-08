package org.taktik.icure.customentities.config.typing

import org.taktik.icure.customentities.util.BuiltinDefinitionsProvider
import org.taktik.icure.jackson.annotations.JsonIgnore
import org.taktik.icure.jackson.annotations.JsonInclude
import org.taktik.icure.jackson.annotations.JsonIncludeValue
import org.taktik.icure.entities.RawJson
import org.taktik.icure.customentities.util.CustomEntityConfigValidationContext
import org.taktik.icure.customentities.util.CustomEntityValueValidationContext
import org.taktik.icure.errorreporting.addError
import org.taktik.icure.errorreporting.addWarning
import org.taktik.icure.errorreporting.appending
import org.taktik.icure.utils.FuzzyDates
import org.taktik.icure.utils.UuidMP
import org.taktik.icure.utils.Validation

/**
 * A definition of a custom or built-in object type
 */
@JsonInclude(JsonIncludeValue.NON_DEFAULT)
data class ObjectDefinition(
	/**
	 * The "data" properties of the object.
	 * These are properties that have only the purpose of storing data and do not have a specific meaning for the system,
	 * as opposed to metadata properties, such as:
	 * - id
	 * - revision
	 * - creation, modification, deletion timestamps
	 * - security metadata, legacy delegations, other access control related metadata
	 * - attachment metadata
	 * - versioning metadata (e.g. healthElementId and endOfLife on health elements)
	 *
	 * If this object definition extends a builtin entity, these properties are in addition to the ones already defined
	 * in the builtin entity.
	 * It is possible, though discouraged, to use the same name as a property already defined in the builtin entity; in
	 * that case the SDK generator will move the standard entity property value in to a different property.
	 */
	val properties: Map<String, PropertyConfiguration>,
	/**
	 * If this configuration extends a builtin entity specifies which entity should be extended, and if the entity has
	 * builtin extendable properties, how its properties should be extended.
	 */
	val builtinExtension: BuiltinExtensionConfiguration? = null,
	/**
	 * If true this object definition should be considered as "Encryptable", regardless of the type of the extended
	 * builtin entity (if any), and encryption configurations of properties.
	 *
	 * If false (default) the type is considered encryptable only if it extends an encryptable builtin entity, or if
	 * it doesn't extend any entity but at least one of the properties specifies a non-null value for
	 * [PropertyConfiguration.encryptionConfiguration].
	 */
	val forceEncryptable: Boolean = false
) {
	data class BuiltinExtensionConfiguration(
		/**
		 * The name of the builtin entity being extended.
		 */
		val entityName: String,
		/**
		 * A map to configure extension on builtin properties of the extended builtin entity.
		 *
		 * The key must be the name of a property with an extendable type defined in the entity corresponding to this
		 * definition [entityName].
		 *
		 * The value is a reference to an object definition that specifies extension properties for the type of the
		 * specified property.
		 *
		 * This can also be applied if the extendable type is nested within a collection or in a map's value, even if
		 * there are multiple levels of nesting.
		 */
		val extendedBuiltinProperties: Map<String, String> = emptyMap(),
		/**
		 * Specifies which builtin properties should be encrypted.
		 *
		 * If null the default encrypted fields property for that the builtin type will apply (depends on the cardinal
		 * SDK version used).
		 *
		 * If empty no builtin properties will be encrypted.
		 *
		 * Can be not-null only if the extended builtin entity is encryptable.
		 */
		@property:JsonInclude(JsonIncludeValue.NON_NULL)
		val propertiesEncryptionConfiguration: Map<String, PropertyEncryptionConfiguration>? = null
	)

	@JsonInclude(JsonIncludeValue.NON_DEFAULT)
	data class PropertyConfiguration(
		/**
		 * Type of the property
		 */
		val type: GenericTypeConfig,
		/**
		 * The default value for the property, if any, used when the user does not provide an explicit value to use.
		 */
		val defaultValue: DefaultValue? = null,
		/**
		 * Not null if this property should be encrypted.
		 *
		 * If this object definition extends a non-encryptable type through [builtinExtension], you can't use
		 * [encryptionConfiguration].
		 *
		 * The value used might alter how the backend validates the properties.
		 * For details refer to the documentation of the specific [PropertyEncryptionConfiguration] used.
		 */
		val encryptionConfiguration: PropertyEncryptionConfiguration? = null
	) {
		/**
		 * If a property defines a default value and is not explicitly provided in the object it is implied that it
		 * should use the default value
		 */
		sealed interface DefaultValue {
			/**
			 * Checks if this default value is valid for the configured property
			 */
			suspend fun validateFor(
				typeConfig: GenericTypeConfig,
				context: CustomEntityConfigValidationContext
			)

			/**
			 * Get a value matching this default configuration; if not null the value must be stored in the DB.
			 * May change at each invocation
			 */
			fun valueForStore(): RawJson?

			/**
			 * If the provided value should be ignored according to this default configuration.
			 */
			fun shouldIgnoreForStore(value: RawJson): Boolean

			@get:JsonIgnore
			val isConstant: Boolean

			/**
			 * Represents a constant default value.
			 * Properties with values equal to the default value are not included in the serialized representation
			 */
			@JsonInclude(JsonIncludeValue.NON_DEFAULT)
			data class Constant(
				/**
				 * The default value
				 */
				@param:JsonInclude(JsonIncludeValue.ALWAYS)
				val value: RawJson,
			) : DefaultValue {
				override suspend fun validateFor(
					typeConfig: GenericTypeConfig,
					context: CustomEntityConfigValidationContext,
				) {
					// Supported on all types, even for id and date types: in those cases might want to use null or
					// other special values.
					typeConfig.validateAndMapValueForStore(context, value)
				}

				override fun valueForStore(): RawJson? = null

				override fun shouldIgnoreForStore(value: RawJson): Boolean =
					value == this.value

				@get:JsonIgnore
				override val isConstant: Boolean = true
			}

			data object GenerateUuidV4 : DefaultValue {
				override suspend fun validateFor(
					typeConfig: GenericTypeConfig,
					context: CustomEntityConfigValidationContext,
				) {
					if (
						typeConfig !is UuidTypeConfig
					) {
						context.validation.addError("GE-OBJECT-DEFAULT-UUIDV4TYPE")
					}
				}

				override fun valueForStore(): RawJson =
					RawJson.JsonString(UuidMP.randomCryptoSafeUuidString())

				override fun shouldIgnoreForStore(value: RawJson): Boolean =
					false

				@get:JsonIgnore
				override val isConstant: Boolean = false
			}

			@JsonInclude(JsonIncludeValue.NON_DEFAULT)
			class NowDateTime(
				val zoneId: String? = null
			) : DefaultValue {
				override suspend fun validateFor(
					typeConfig: GenericTypeConfig,
					context: CustomEntityConfigValidationContext,
				) {
					if (
						typeConfig !is FuzzyDateTimeTypeConfig
					) {
						context.validation.addError("GE-OBJECT-DEFAULT-NOWDATETIMETYPE")
					}
					zoneId?.let {
						if (!Validation.validZoneId(it)) {
							context.validation.addError("GE-ZONEID",  "value" to it)
						}
					}
				}

				override fun valueForStore(): RawJson =
					RawJson.JsonInteger(FuzzyDates.getNowFuzzyDateTime(zoneId))

				override fun shouldIgnoreForStore(value: RawJson): Boolean =
					false

				@get:JsonIgnore
				override val isConstant: Boolean = false
			}

			@JsonInclude(JsonIncludeValue.NON_DEFAULT)
			class NowDate(
				val zoneId: String? = null
			) : DefaultValue {
				override suspend fun validateFor(
					typeConfig: GenericTypeConfig,
					context: CustomEntityConfigValidationContext,
				) {
					if (
						typeConfig !is FuzzyDateTypeConfig
					) {
						context.validation.addError("GE-OBJECT-DEFAULT-NOWDATETYPE")
					}
					zoneId?.let {
						if (!Validation.validZoneId(it)) {
							context.validation.addError("GE-ZONEID", "value" to it)
						}
					}
				}

				override fun valueForStore(): RawJson =
					RawJson.JsonInteger(FuzzyDates.getNowFuzzyDate(zoneId).toLong())

				override fun shouldIgnoreForStore(value: RawJson): Boolean =
					false

				override val isConstant: Boolean = false
			}

			@JsonInclude(JsonIncludeValue.NON_DEFAULT)
			class NowTime(
				val zoneId: String? = null
			) : DefaultValue {
				override suspend fun validateFor(
					typeConfig: GenericTypeConfig,
					context: CustomEntityConfigValidationContext,
				) {
					if (
						typeConfig !is FuzzyTimeTypeConfig
					) {
						context.validation.addError("GE-OBJECT-DEFAULT-NOWTIMETYPE")
					}
					zoneId?.let {
						if (!Validation.validZoneId(it)) {
							context.validation.addError("GE-ZONEID", "value" to it)
						}
					}
				}

				override fun valueForStore(): RawJson =
					RawJson.JsonInteger(FuzzyDates.getNowFuzzyTime(zoneId).toLong())

				override fun shouldIgnoreForStore(value: RawJson): Boolean =
					false

				override val isConstant: Boolean = false
			}
		}
	}

	/**
	 * Specifies how a property should be encrypted, more options might be added in future.
	 *
	 * # Partial encryption of object types
	 *
	 * To encrypt only some properties of a nested object type, you must configuring encryption at the level of that
	 * object's definition. If you configure encryption at the level of the property containing the
	 *
	 * ## Example
	 *
	 * Consider the following object definitions
	 * - Person
	 *   - name: String
	 *   - address: Address
	 * - Address
	 *   - city: String
	 *   - street: String
	 *
	 * If you want to encrypt only the street of Address (leaving the city in clear), and you don't need to encrypt
	 * anything else in Person you must:
	 * - Configure the definition for Address to use Full encryption of the street property. This way your custom SDK
	 *   will have two variants of Address: EncryptedAddress exposing only the city and a DecryptedAddress, with both
	 *   city and street.
	 * - Configure the definition of Person with [forceEncryptable]=true, so that Person is also generated in two
	 *   variants, with EncryptedPerson using EncryptedAddress and DecryptedPerson using DecryptedAddress. If you
	 *   don't only one implementation of Person is generated, depending directly on DecryptedAddress.
	 *
	 * If instead you set at the level of the Person.address configuration to use encryption (e.g.
	 * [ObjectDefinition.PropertyEncryptionConfiguration.Full]) then the whole Address object will be encrypted,
	 * ignoring its configuration, and not even city will be available in the EncryptedPerson's address.
	 */
	sealed interface PropertyEncryptionConfiguration {
		/**
		 * Encrypt the property in full. No information about this property will be available in the encrypted entity.
		 *
		 * # Validation of custom properties
		 *
		 * When a custom property is marked as requiring [Full] encryption the backend will reject any objects that
		 * specify a value for that property, and no other verification is done, regardless of the type of the property.
		 *
		 * Validation is still done client side to help detect programming errors, but malicious users may still create
		 * data that does not respect the validation.
		 *
		 * # Generated model
		 *
		 * In the generated model a property encrypted using [Full] will not be available from the encrypted variant of
		 * the object or from the interface
		 */
		data object Full : PropertyEncryptionConfiguration
	}

	suspend fun validateDefinition(
		context: CustomEntityConfigValidationContext,
	) {
		if (properties.isEmpty() && builtinExtension?.extendedBuiltinProperties.isNullOrEmpty()) {
			context.validation.addWarning("GE-OBJECT-WEMPTY")
		}
		context.validation.appending(".") {
			properties.forEach { (propName, propConfig) ->
				context.validation.appending(propName) {
					validateIdentifier(context.validation, propName)
					propConfig.type.validateConfig(context)
					propConfig.type.objectDefinitionDependencies.forEach { (definitionName, isBuiltin) ->
						val builtinEntityDependencyName = (
							if (isBuiltin) {
								definitionName
							} else {
								context.resolution.resolveObjectReference(definitionName)?.builtinExtension?.entityName
							}
						)
						if (builtinEntityDependencyName?.let { context.builtinDefinitions.getBuiltinObjectDefinition(it)?.isRoot } == true) {
							// Probably no real implementation complexity or other limitation in allowing it, but root
							// entities also include access control and other metadata that does not work when embedded
							context.validation.addError("GE-OBJECT-EMBEDROOT", "object" to definitionName, "entity" to builtinEntityDependencyName)
						}
					}
					context.validation.appending("<DEFAULT>") {
						propConfig.defaultValue?.validateFor(propConfig.type, context)
					}
				}
			}
		}
		if (builtinExtension != null) {
			val builtinDefinition = context.builtinDefinitions.getBuiltinObjectDefinition(builtinExtension.entityName)
			if (builtinDefinition == null) {
				context.validation.addError("GE-OBJECT-BASEENTITYREF", "entity" to builtinExtension.entityName)
			} else if (!builtinDefinition.isSpecializable) {
				context.validation.addError("GE-OBJECT-BASEENTITYSPECIALIZABLE", "entity" to builtinExtension.entityName)
			} else {
				if (!builtinDefinition.isEncryptable) {
					if (
						forceEncryptable || properties.any { it.value.encryptionConfiguration != null } || builtinExtension.propertiesEncryptionConfiguration != null
					) {
						context.validation.addError("GE-OBJECT-BASEENTITYNOTENCRYPTABLE", "entity" to builtinExtension.entityName)
					}
				} else {
					builtinExtension.propertiesEncryptionConfiguration?.keys?.forEach {
						if (!builtinDefinition.properties.contains(it)) {
							context.validation.addError(
								"GE-OBJECT-ENCRYPTEDPROPNOTFOUND",
								"prop" to it,
								"entity" to builtinExtension.entityName
							)
						}
					}
				}
				if (builtinDefinition.isExtendable) {
					context.validation.appending(".") {
						properties.keys.intersect(builtinDefinition.properties.keys).forEach {
							context.validation.appending(it) {
								context.validation.addWarning(
									"GE-OBJECT-WBASEENTITYPROP",
									"prop" to it,
									"entity" to builtinExtension.entityName
								)
							}
						}
					}
				} else if (properties.isNotEmpty()) {
					context.validation.addError("GE-OBJECT-BASEENTITYEXTENDABLE", "entity" to builtinExtension.entityName)
				}
				builtinExtension.extendedBuiltinProperties.forEach { (builtinPropName, targetDefinitionRef) ->
					val propConfig = builtinDefinition.properties[builtinPropName]
					if (propConfig != null) {
						val objectReferenceDependency = propConfig.type.objectDefinitionDependencies.also {
							if (it.size > 1) {
								throw IllegalStateException("Ambiguous builtin property dependency at path ${context.validation.path ?: "<unknown>"} found ${it.size} dependencies")
							} else if (it.firstOrNull()?.second == false) {
								throw IllegalStateException("Ambiguous builtin property dependency at path ${context.validation.path ?: "<unknown>"} found non builtin object dependency")
							}
						}
						if (objectReferenceDependency.isEmpty()) {
							context.validation.addError("GE-OBJECT-EXTENDBUILTINPROPNOTOBJ", "prop" to builtinPropName, "entity" to builtinExtension.entityName)
						} else {
							val extendedBuiltinPropObjectDefinition = checkNotNull(context.builtinDefinitions.getBuiltinObjectDefinition(objectReferenceDependency.first().first)) {
								"Can't resolve builtin object reference for $builtinPropName of entity ${builtinExtension.entityName} at path ${context.validation.path ?: "<unknown>"}"
							}
							if (!extendedBuiltinPropObjectDefinition.isExtendable) {
								context.validation.addError("GE-OBJECT-EXTENDBUILTINPROPEXTENDABLE", "prop" to builtinPropName, "entity" to builtinExtension.entityName, "ref" to objectReferenceDependency.first().first)
							}
							val targetDefinition = context.resolution.resolveObjectReference(targetDefinitionRef)
							if (targetDefinition == null) {
								context.validation.addError("GE-OBJECT-EXTENDBUILTINPROPDEFREF", "prop" to builtinPropName, "entity" to builtinExtension.entityName, "ref" to targetDefinitionRef)
							} else if (targetDefinition.builtinExtension?.entityName != objectReferenceDependency.first().first) {
								context.validation.addError("GE-OBJECT-EXTENDBUILTINPROPDEFREFBUILTIN", "prop" to builtinPropName, "entity" to builtinExtension.entityName, "ref" to targetDefinitionRef)
							}
						}
					} else {
						context.validation.addError("GE-OBJECT-EXTENDBUILTINPROPNOTFOUND", "prop" to builtinPropName, "entity" to builtinExtension.entityName)
					}
				}
			}
		}
		if (forceEncryptable) {
			if (
				properties.any { it.value.encryptionConfiguration != null } || (
					builtinExtension != null &&
					context.builtinDefinitions.getBuiltinObjectDefinition(builtinExtension.entityName)?.isEncryptable == true
				)
			) {
				context.validation.addWarning("GE-OBJECT-WFORCENCRYPTABLEREDUNDANT")
			} else if (
				properties.none { prop ->
					prop.value.type.objectDefinitionDependencies.any { (objectDependencyName, objectDependencyBuiltin) ->
						if (objectDependencyBuiltin) {
							context.builtinDefinitions.getBuiltinObjectDefinition(objectDependencyName)?.isEncryptable == true
						} else {
							context.resolution.resolveObjectReference(objectDependencyName)?.isEncryptable(context.builtinDefinitions) == true
						}
					}
				}
			) {
				context.validation.addWarning("GE-OBJECT-WFORCENCRYPTABLEIDENTICAL")
			}
		}
	}

	fun validateAndMapValueForStore(
		context: CustomEntityValueValidationContext,
		value: RawJson.JsonObject,
	): RawJson =
		if (builtinExtension != null) {
			throw UnsupportedOperationException("Custom objects extending builtin in custom extension or custom object is not currently supported")
//			context.builtinValidation.validateAndMapExtendedBuiltinObject(
//				this,
//				value,
//				context.validation
//			) // Will take care of also mapping extensions
		} else {
			validateAndMapExtensionValueForStore(context, value)
		}

	fun validateAndMapExtensionValueForStore(
		context: CustomEntityValueValidationContext,
		value: RawJson.JsonObject,
	): RawJson.JsonObject {
		val mappedObjectProperties = mutableMapOf<String, RawJson>()
		(properties.keys + value.properties.keys).forEach { propName ->
			val propConfig = properties[propName]
			if (propConfig == null) {
				context.validation.addError("GE-OBJECT-UNKNOWNPROP", "prop" to propName)
			} else {
				val propValue: RawJson? = value.properties[propName]
				when (propConfig.encryptionConfiguration?.takeUnless { context.isDecryptedContext }) {
					null -> {
						val mappedValue = if (propValue == null) {
							if (propConfig.defaultValue == null) {
								context.validation.addError("GE-OBJECT-MISSINGPROP", "prop" to propName)
								null
							} else {
								propConfig.defaultValue.valueForStore()
							}
						} else if (propConfig.defaultValue?.shouldIgnoreForStore(propValue) == true) {
							null
						} else {
							context.validation.appending(".", propName) {
								propConfig.type.validateAndMapValueForStore(context, propValue)
							}
						}
						if (mappedValue != null) {
							mappedObjectProperties[propName] = mappedValue
						}
					}
					PropertyEncryptionConfiguration.Full -> {
						if (propValue != null) {
							context.validation.addError("GE-OBJECT-NOTENCRYPTEDPROP", "prop" to propName)
						}
					}
				}
			}
		}
		return RawJson.JsonObject(mappedObjectProperties)
	}

	/**
	 * Specifies if this object is encryptable.
	 * If true this object definition will exist in 2 variants, an encrypted variant and a decrypted variant (+ a shared
	 * interface).
	 *
	 * When an encryptable object definition uses another encryptable object definition the variants will match, that is
	 * if we have Person with Address, both encryptable, "EncryptedPerson" will use "EncryptedAddress" and
	 * "DecryptedPerson" will use "DecryptedAddress".
	 *
	 * When a non-encryptable object definition uses an encryptable object then the Decrypted variant will be used
	 */
	// TODO in future we might want to allow usage of always decrypted variants in some encryptable object definitions
	fun isEncryptable(
		builtinDefinitionsProvider: BuiltinDefinitionsProvider
	): Boolean =
		forceEncryptable || properties.any { it.value.encryptionConfiguration != null } || builtinExtension?.entityName?.let {
			builtinDefinitionsProvider.getBuiltinObjectDefinition(it)?.isEncryptable
		} == true
}