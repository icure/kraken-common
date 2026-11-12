package org.taktik.icure.services.external.rest.v2.mapper

import com.icure.cardinal.customentities.config.StandardRootEntitiesExtensionConfig
import com.icure.cardinal.customentities.config.StandardRootEntityExtensionConfig
import com.icure.cardinal.customentities.config.VersionedCustomEntitiesConfiguration
import com.icure.cardinal.customentities.config.typing.ObjectDefinition
import com.icure.cardinal.customentities.mapping.MapperExtensionsValidationContext
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.types.shouldBeInstanceOf
import io.kotest.matchers.types.shouldBeSameInstanceAs
import io.kotest.matchers.types.shouldNotBeSameInstanceAs
import com.icure.cardinal.customentities.util.ExtendableBuiltinEntityValidatorMapperConfigsProviderImpl
import org.taktik.icure.services.external.rest.v2.dto.PatientDto

class BuiltinCustomisedTypeValidationTest :
	StringSpec({
		val configsProvider = ExtendableBuiltinEntityValidatorMapperConfigsProviderImpl(emptyMap(), emptyMap())

		fun config(patient: Map<String, StandardRootEntityExtensionConfig>?) = VersionedCustomEntitiesConfiguration(
			owningGroup = "group",
			version = 1,
			objects = listOf("PatientA", "PatientB", "PatientC").associateWith {
				ObjectDefinition(
					properties = emptyMap(),
					builtinExtension = ObjectDefinition.BuiltinExtensionConfiguration("Patient"),
				)
			},
			enums = emptyMap(),
			extensions = StandardRootEntitiesExtensionConfig(patient = patient),
			customEntities = emptyMap(),
			published = true,
		)

		// Multiple customised types per builtin entity type are not allowed by the configuration validation yet, but
		// the validation of entities already supports them.
		val configured = config(
			mapOf(
				"A" to StandardRootEntityExtensionConfig("PatientA", 2),
				"B" to StandardRootEntityExtensionConfig("PatientB", 3, disabled = true),
				"C" to StandardRootEntityExtensionConfig("PatientC", 1),
			),
		)

		fun validate(
			typeId: String?,
			version: Int?,
			config: VersionedCustomEntitiesConfiguration? = configured,
		): MapperExtensionsValidationContext = MappersWithCustomExtensions.validateBuiltinTypeAndGetMapperExtensionsValidationContext(
			dtoTypeId = typeId,
			dtoModelVersion = version,
			config = config,
			extensionsOfType = config?.extensions?.patient,
			scopePath = null,
			builtinValidationConfigsProvider = configsProvider,
		)

		"Without configuration for the builtin entity type only unextended entities are allowed" {
			for (c in listOf(null, config(null))) {
				validate(null, null, c) shouldBe MapperExtensionsValidationContext.Empty
				shouldThrow<IllegalArgumentException> { validate("A", 2, c) }.message shouldContain "no customised types are configured"
			}
		}

		"With a configuration for the builtin entity type unextended entities are disabled" {
			shouldThrow<IllegalArgumentException> { validate(null, null) }.message shouldContain "Missing customisedTypeId"
			// Also when no customised type is enabled
			shouldThrow<IllegalArgumentException> { validate(null, null, config(emptyMap())) }.message shouldContain "Missing customisedTypeId"
		}

		"Entities of an enabled customised type are accepted with the version of the type" {
			validate("A", 2).shouldBeInstanceOf<MappersWithCustomExtensions.MapperExtensionsValidationContextImpl>()
			validate("C", 1).shouldBeInstanceOf<MappersWithCustomExtensions.MapperExtensionsValidationContextImpl>()
			shouldThrow<IllegalArgumentException> { validate("A", 1) }.message shouldContain "expected 2, got 1"
		}

		"Entities of a disabled or unknown customised type are rejected" {
			shouldThrow<IllegalArgumentException> { validate("B", 3) }.message shouldContain "Customised type B is disabled"
			shouldThrow<IllegalArgumentException> { validate("D", 1) }.message shouldContain "Unknown customisedTypeId D"
			// Builtin entity names are not customised type ids
			shouldThrow<IllegalArgumentException> { validate("Patient", 1) }.message shouldContain "Unknown customisedTypeId Patient"
		}

		"customisedTypeId and customisedModelVersion must be both null or both set" {
			shouldThrow<IllegalArgumentException> { validate("A", null) }.message shouldContain "both null or both set"
			shouldThrow<IllegalArgumentException> { validate(null, 2) }.message shouldContain "both null or both set"
			shouldThrow<IllegalArgumentException> { validate(null, 2, null) }.message shouldContain "both null or both set"
		}

		suspend fun mapAll(vararg patients: PatientDto): List<Pair<String, MapperExtensionsValidationContext>> =
			MappersWithCustomExtensions.mapFromDtoWithExtension(
				dtos = patients.toList(),
				getCustomEntitiesConfigForRequest = { configured },
				getExtension = { patient },
				doMap = { dto, context -> dto.id to context },
				scopePath = null,
				builtinValidationConfigsProvider = configsProvider,
			)

		"Bulk mapping validates each entity independently, with one context per customised type and version" {
			val mapped = mapAll(
				PatientDto(id = "a1", customisedTypeId = "A", customisedModelVersion = 2),
				PatientDto(id = "c1", customisedTypeId = "C", customisedModelVersion = 1),
				PatientDto(id = "a2", customisedTypeId = "A", customisedModelVersion = 2),
			).toMap()
			mapped.keys shouldBe setOf("a1", "c1", "a2")
			mapped.getValue("a1") shouldBeSameInstanceAs mapped.getValue("a2")
			mapped.getValue("a1") shouldNotBeSameInstanceAs mapped.getValue("c1")
		}

		"Bulk mapping fails if any entity has a disabled type" {
			shouldThrow<IllegalArgumentException> {
				mapAll(
					PatientDto(id = "a1", customisedTypeId = "A", customisedModelVersion = 2),
					PatientDto(id = "b1", customisedTypeId = "B", customisedModelVersion = 3),
				)
			}.message shouldContain "Customised type B is disabled"
			shouldThrow<IllegalArgumentException> {
				mapAll(
					PatientDto(id = "a1", customisedTypeId = "A", customisedModelVersion = 2),
					PatientDto(id = "u1"),
				)
			}.message shouldContain "Missing customisedTypeId"
		}
	})
