package org.taktik.icure.entities

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.KotlinFeature
import com.fasterxml.jackson.module.kotlin.KotlinModule
import com.fasterxml.jackson.module.kotlin.readValue
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import org.taktik.icure.entities.base.CryptoActor
import java.util.UUID

class CryptoActorDeserializationTest :
	StringSpec({
		val mapper = ObjectMapper().registerModule(
			KotlinModule.Builder()
				.configure(KotlinFeature.NullIsSameAsDefault, true)
				.configure(KotlinFeature.NullToEmptyCollection, true)
				.configure(KotlinFeature.NullToEmptyMap, true)
				.build(),
		)

		val readers: Map<String, (String) -> CryptoActor> = mapOf(
			"Patient" to { json -> mapper.readValue<Patient>(json) },
			"HealthcareParty" to { json -> mapper.readValue<HealthcareParty>(json) },
			"Device" to { json -> mapper.readValue<Device>(json) },
			"CryptoActorStub" to { json -> mapper.readValue<CryptoActorStub>(json) },
		)

		// Couch entities use `_id` and `_rev`, CryptoActorStub uses `id` and `rev`
		fun idAndRev() = UUID.randomUUID().toString().let { id ->
			""""_id": "$id", "_rev": "1-a", "id": "$id", "rev": "1-a""""
		}

		fun cryptoActorJson(aesExchangeKeys: String) = """{ ${idAndRev()}, "aesExchangeKeys": $aesExchangeKeys }"""

		readers.forEach { (name, read) ->
			"$name - null aes exchange key values should be discarded" {
				read(
					cryptoActorJson(
						"""
						{
							"03913fcd-e9ea-483a-8b02-773b68458668": {
								"0be7c82b3098e1250de5a90203010001": {
									"AesExchangeKeyEncryptionKeypairIdentifier(s=)": null,
									"0be7c82b3098e1250de5a90203010001": "aa"
								}
							}
						}
						""".trimIndent(),
					),
				).aesExchangeKeys shouldBe mapOf(
					"03913fcd-e9ea-483a-8b02-773b68458668" to mapOf(
						"0be7c82b3098e1250de5a90203010001" to mapOf("0be7c82b3098e1250de5a90203010001" to "aa"),
					),
				)
			}

			"$name - aes exchange key entries with a value of the wrong shape should be discarded" {
				read(
					cryptoActorJson(
						"""
						{
							"pk1": {
								"d1": { "fp1": "aa", "fp2": { "x": "y" }, "fp3": ["z"] },
								"d2": null,
								"d3": "not a map"
							},
							"pk2": null,
							"pk3": ["x"]
						}
						""".trimIndent(),
					),
				).aesExchangeKeys shouldBe mapOf("pk1" to mapOf("d1" to mapOf("fp1" to "aa")))
			}

			"$name - missing or null aes exchange keys should be empty" {
				read("""{ ${idAndRev()} }""").aesExchangeKeys shouldBe emptyMap()
				read(cryptoActorJson("null")).aesExchangeKeys shouldBe emptyMap()
			}
		}
	})
