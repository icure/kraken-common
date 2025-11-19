package org.taktik.icure.serializers

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.core.json.JsonReadFeature
import com.fasterxml.jackson.databind.JsonMappingException
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import com.fasterxml.jackson.module.kotlin.KotlinFeature
import com.fasterxml.jackson.module.kotlin.KotlinModule
import com.fasterxml.jackson.module.kotlin.readValue
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.shouldBe
import org.taktik.icure.entities.base.ParticipantType
import org.taktik.icure.entities.embed.ContactParticipant

private data class TestParticipantEntity(
	val id: String,
	@param:JsonDeserialize(using = ParticipantListSerializer::class)
	val participants: Collection<ContactParticipant>,
)

class ParticipantListSerializerTest :
	StringSpec({
		val mapper = ObjectMapper().registerModule(
			KotlinModule.Builder()
				.configure(KotlinFeature.NullIsSameAsDefault, true)
				.configure(KotlinFeature.NullToEmptyMap, true)
				.configure(KotlinFeature.NullToEmptyCollection, true)
				.build(),
		).apply {
			setSerializationInclusion(JsonInclude.Include.NON_NULL)
			configure(JsonReadFeature.ALLOW_UNESCAPED_CONTROL_CHARS.mappedFeature(), true)
		}

		"Deserialization of JSON with array of ContactParticipant objects" {
			val json = """
            {
                "id": "entity1",
                "participants": [
                    {
                        "type": "admitter",
                        "hcpId": "hcp-001"
                    },
					{
                        "type": "attender",
                        "hcpId": "hcp-002"
                    },
                    {
                        "type": "consultant",
                        "hcpId": "hcp-003"
                    },
					{
                        "type": "admitter",
                        "hcpId": "hcp-004"
                    }
                ]
            }
			""".trimIndent()

			val result = mapper.readValue<TestParticipantEntity>(json)

			result.id shouldBe "entity1"
			result.participants.size shouldBe 4
			result.participants shouldContainExactlyInAnyOrder listOf(
				ContactParticipant(type = ParticipantType.Admitter, hcpId = "hcp-001"),
				ContactParticipant(type = ParticipantType.Attender, hcpId = "hcp-002"),
				ContactParticipant(type = ParticipantType.Consultant, hcpId = "hcp-003"),
				ContactParticipant(type = ParticipantType.Admitter, hcpId = "hcp-004"),
			)
		}

		"Deserialization of JSON with object (legacy Map format)" {
			val json = """
            {
                "id": "entity2",
                "participants": {
                    "admitter": "hcp-001",
                    "referrer": "hcp-004",
                    "discharger": "hcp-005"
                }
            }
			""".trimIndent()

			val result = mapper.readValue<TestParticipantEntity>(json)

			result.id shouldBe "entity2"
			result.participants.size shouldBe 3
			result.participants shouldContainExactlyInAnyOrder listOf(
				ContactParticipant(type = ParticipantType.Admitter, hcpId = "hcp-001"),
				ContactParticipant(type = ParticipantType.Referrer, hcpId = "hcp-004"),
				ContactParticipant(type = ParticipantType.Discharger, hcpId = "hcp-005"),
			)
		}

		"Deserialization of empty array" {
			val json = """
            {
                "id": "entity3",
                "participants": []
            }
			""".trimIndent()

			val result = mapper.readValue<TestParticipantEntity>(json)

			result.id shouldBe "entity3"
			result.participants.size shouldBe 0
		}

		"Deserialization of empty object" {
			val json = """
            {
                "id": "entity4",
                "participants": {}
            }
			""".trimIndent()

			val result = mapper.readValue<TestParticipantEntity>(json)

			result.id shouldBe "entity4"
			result.participants.size shouldBe 0
		}

		"Deserialization with all ParticipantType values in object format" {
			val json = """
            {
                "id": "entity5",
                "participants": {
					"admitter": "hcp-admitter",
					"attender": "hcp-attender",
					"callback": "hcp-callback",
					"consultant": "hcp-consultant",
					"discharger": "hcp-discharger",
					"escort": "hcp-escort",
					"referrer": "hcp-referrer",
					"secondaryPerformer": "hcp-secondaryPerformer",
					"primaryPerformer": "hcp-primaryPerformer",
					"participation": "hcp-participation",
					"translator": "hcp-translator",
					"emergency": "hcp-emergency",
					"location": "hcp-location",
					"recorder": "hcp-recorder"
                }
            }
			""".trimIndent()

			val result = mapper.readValue<TestParticipantEntity>(json)

			result.id shouldBe "entity5"
			result.participants.size shouldBe 14
			result.participants shouldContainExactlyInAnyOrder listOf(
				ContactParticipant(type = ParticipantType.Admitter, hcpId = "hcp-admitter"),
				ContactParticipant(type = ParticipantType.Attender, hcpId = "hcp-attender"),
				ContactParticipant(type = ParticipantType.CallbackContact, hcpId = "hcp-callback"),
				ContactParticipant(type = ParticipantType.Consultant, hcpId = "hcp-consultant"),
				ContactParticipant(type = ParticipantType.Discharger, hcpId = "hcp-discharger"),
				ContactParticipant(type = ParticipantType.Escort, hcpId = "hcp-escort"),
				ContactParticipant(type = ParticipantType.Referrer, hcpId = "hcp-referrer"),
				ContactParticipant(type = ParticipantType.SecondaryPerformer, hcpId = "hcp-secondaryPerformer"),
				ContactParticipant(type = ParticipantType.PrimaryPerformer, hcpId = "hcp-primaryPerformer"),
				ContactParticipant(type = ParticipantType.Participation, hcpId = "hcp-participation"),
				ContactParticipant(type = ParticipantType.Translator, hcpId = "hcp-translator"),
				ContactParticipant(type = ParticipantType.Emergency, hcpId = "hcp-emergency"),
				ContactParticipant(type = ParticipantType.Location, hcpId = "hcp-location"),
				ContactParticipant(type = ParticipantType.Recorder, hcpId = "hcp-recorder"),
			)
		}

		"Deserialization should fail with invalid JSON value type" {
			val json = """
            {
                "id": "entity6",
                "participants": "invalid-string-value"
            }
			""".trimIndent()

			shouldThrow<JsonMappingException> {
				mapper.readValue<TestParticipantEntity>(json)
			}
		}

		"Deserialization should fail with invalid JSON value type (number)" {
			val json = """
            {
                "id": "entity7",
                "participants": 12345
            }
			""".trimIndent()

			shouldThrow<JsonMappingException> {
				mapper.readValue<TestParticipantEntity>(json)
			}
		}
	})

