package org.taktik.icure.entities.embed

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.KotlinFeature
import com.fasterxml.jackson.module.kotlin.KotlinModule
import com.fasterxml.jackson.module.kotlin.readValue
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe

class InsurabilityDeserializationTest :
	StringSpec({
		val mapper = ObjectMapper().registerModule(
			KotlinModule.Builder()
				.configure(KotlinFeature.NullIsSameAsDefault, true)
				.configure(KotlinFeature.NullToEmptyCollection, true)
				.configure(KotlinFeature.NullToEmptyMap, true)
				.build(),
		)

		"Null parameter values should be discarded" {
			mapper.readValue<Insurability>(
				"""{ "parameters": { "a": "x", "b": null } }""",
			).parameters shouldBe mapOf("a" to "x")
		}

		"Parameter with null as key and value should be discarded" {
			mapper.readValue<Insurability>(
				"""{ "parameters": { "null": null } }""",
			).parameters shouldBe emptyMap()
			mapper.readValue<Insurability>(
				"""{ "parameters": { "null": null, "a": "x" } }""",
			).parameters shouldBe mapOf("a" to "x")
		}

		"Object and array parameter values should be discarded" {
			mapper.readValue<Insurability>(
				"""{ "parameters": { "a": "x", "b": { "c": "d" }, "e": ["f"] } }""",
			).parameters shouldBe mapOf("a" to "x")
		}

		"Scalar parameter values should be kept as strings" {
			mapper.readValue<Insurability>(
				"""{ "parameters": { "a": "x", "b": 1, "c": 1.5, "d": true } }""",
			).parameters shouldBe mapOf("a" to "x", "b" to "1", "c" to "1.5", "d" to "true")
		}

		"Missing or null parameters should be empty" {
			mapper.readValue<Insurability>("""{ "insuranceId": "i" }""").parameters shouldBe emptyMap()
			mapper.readValue<Insurability>("""{ "parameters": null }""").parameters shouldBe emptyMap()
		}
	})
