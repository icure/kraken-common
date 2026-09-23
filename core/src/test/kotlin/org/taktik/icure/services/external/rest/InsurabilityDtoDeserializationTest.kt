package org.taktik.icure.services.external.rest

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.KotlinFeature
import com.fasterxml.jackson.module.kotlin.KotlinModule
import com.fasterxml.jackson.module.kotlin.readValue
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import org.taktik.icure.services.external.rest.v1.dto.embed.InsurabilityDto as InsurabilityDtoV1
import org.taktik.icure.services.external.rest.v2.dto.embed.InsurabilityDto as InsurabilityDtoV2

class InsurabilityDtoDeserializationTest :
	StringSpec({
		// Same configuration as the REST decoder in SharedWebConfig
		val mapper = ObjectMapper().registerModule(
			KotlinModule.Builder()
				.configure(KotlinFeature.NullIsSameAsDefault, true)
				.configure(KotlinFeature.StrictNullChecks, true)
				.build(),
		)
		val json = """{ "parameters": { "a": "x", "b": null, "c": { "d": "e" }, "f": ["g"], "h": 1 } }"""

		"v1 - non-string parameter values should be discarded" {
			mapper.readValue<InsurabilityDtoV1>(json).parameters shouldBe mapOf("a" to "x", "h" to "1")
		}

		"v2 - non-string parameter values should be discarded" {
			mapper.readValue<InsurabilityDtoV2>(json).parameters shouldBe mapOf("a" to "x", "h" to "1")
		}
	})
