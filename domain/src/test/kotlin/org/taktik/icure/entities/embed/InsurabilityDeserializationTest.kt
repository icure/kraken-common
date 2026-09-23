package org.taktik.icure.entities.embed

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.KotlinFeature
import com.fasterxml.jackson.module.kotlin.KotlinModule
import com.fasterxml.jackson.module.kotlin.readValue
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe

class InsurabilityDeserializationTest :
	StringSpec({
		val mappers = mapOf(
			"domain mapper" to ObjectMapper().registerModule(
				KotlinModule.Builder()
					.configure(KotlinFeature.NullIsSameAsDefault, true)
					.configure(KotlinFeature.NullToEmptyCollection, true)
					.configure(KotlinFeature.NullToEmptyMap, true)
					.build(),
			),
			"rest mapper" to ObjectMapper().registerModule(
				KotlinModule.Builder()
					.configure(KotlinFeature.NullIsSameAsDefault, true)
					.configure(KotlinFeature.StrictNullChecks, true)
					.build(),
			),
		)

		mappers.forEach { (name, mapper) ->
			"$name - null parameter values should be discarded" {
				mapper.readValue<Insurability>(
					"""{ "parameters": { "a": "x", "b": null } }""",
				).parameters shouldBe mapOf("a" to "x")
			}

			"$name - object and array parameter values should be discarded" {
				mapper.readValue<Insurability>(
					"""{ "parameters": { "a": "x", "b": { "c": "d" }, "e": ["f"] } }""",
				).parameters shouldBe mapOf("a" to "x")
			}

			"$name - scalar parameter values should be kept as strings" {
				mapper.readValue<Insurability>(
					"""{ "parameters": { "a": "x", "b": 1, "c": 1.5, "d": true } }""",
				).parameters shouldBe mapOf("a" to "x", "b" to "1", "c" to "1.5", "d" to "true")
			}

			"$name - missing or null parameters should be empty" {
				mapper.readValue<Insurability>("""{ "insuranceId": "i" }""").parameters shouldBe emptyMap()
				mapper.readValue<Insurability>("""{ "parameters": null }""").parameters shouldBe emptyMap()
			}
		}
	})
