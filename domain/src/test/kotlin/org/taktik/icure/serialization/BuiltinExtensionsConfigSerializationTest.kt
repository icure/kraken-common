package org.taktik.icure.serialization

import com.fasterxml.jackson.module.kotlin.readValue
import com.icure.cardinal.customentities.config.StandardRootEntitiesExtensionConfig
import com.icure.cardinal.customentities.config.StandardRootEntityExtensionConfig
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe

/**
 * In the configuration of builtin entity extensions, a null map (no configuration for the builtin entity type: only
 * unextended entities, enabled) and an empty map (fully disabled builtin entity type) have different meanings.
 *
 * The domain object mapper enables `NullToEmptyMap`, which would turn an explicit null into an empty map; this is
 * prevented only because `NullIsSameAsDefault` is also enabled, and takes precedence for parameters with a default.
 */
class BuiltinExtensionsConfigSerializationTest :
	StringSpec({
		val mapper = IcureDomainObjectMapper.new()

		"An explicit null configuration for a builtin entity type stays null, and is not turned into an empty map" {
			val config = mapper.readValue<StandardRootEntitiesExtensionConfig>(
				"""{"patient": null, "user": {}, "device": {"Dev": {"objectDefinitionReference": "DevExt", "version": 1}}}""",
			)
			config.patient shouldBe null
			config.user shouldBe emptyMap()
			config.device shouldBe mapOf("Dev" to StandardRootEntityExtensionConfig("DevExt", 1))
			config.healthcareParty shouldBe null
		}

		"Null and empty configurations of builtin entity types are preserved through a serialization round-trip" {
			val config = StandardRootEntitiesExtensionConfig(
				user = emptyMap(),
				device = mapOf("Dev" to StandardRootEntityExtensionConfig("DevExt", 1, disabled = true)),
			)
			mapper.readValue<StandardRootEntitiesExtensionConfig>(mapper.writeValueAsString(config)) shouldBe config
		}
	})
