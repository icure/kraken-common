package org.taktik.icure.serializers

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.core.json.JsonReadFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import com.fasterxml.jackson.databind.node.ObjectNode
import com.fasterxml.jackson.module.kotlin.KotlinFeature
import com.fasterxml.jackson.module.kotlin.KotlinModule
import com.fasterxml.jackson.module.kotlin.readValue
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import org.taktik.icure.entities.base.LinkQualification
import org.taktik.icure.entities.embed.Service
import java.util.UUID

private enum class TestEnum {
	@JsonProperty("thisHasNothingToDoWithEnumName")
	ENUM_VALUE_1,
	ENUM_VALUE_2,
}

private data class TestEntity(
	val id: String,
	@param:JsonDeserialize(using = TestDeserializer::class) val weirdMap: Map<TestEnum, Map<String, String>>,
)

private class TestDeserializer : EnumToMapOrListDeserializer<TestEnum>(TestEnum::class.java)

class EnumToMapOrListDeserializerTest :
	StringSpec({
		val mapper = ObjectMapper().registerModule( // TODO replace with domain mapper
			KotlinModule.Builder()
				.configure(KotlinFeature.NullIsSameAsDefault, true)
				.configure(KotlinFeature.NullToEmptyMap, true)
				.configure(KotlinFeature.NullToEmptyCollection, true)
				.build(),
		).apply {
			setSerializationInclusion(JsonInclude.Include.NON_NULL)
			configure(JsonReadFeature.ALLOW_UNESCAPED_CONTROL_CHARS.mappedFeature(), true)
		}

		"Deserialization of JSON with object values should preserve keys" {
			val json = """
            {
                "id": "entity1",
                "weirdMap": {
                    "thisHasNothingToDoWithEnumName": {
                        "key1": "value1",
                        "key2": "value2"
                    },
                    "ENUM_VALUE_2": {
                        "key3": "value3"
                    }
                }
            }
			""".trimIndent()
			val expected = TestEntity(
				id = "entity1",
				weirdMap = mapOf(
					TestEnum.ENUM_VALUE_1 to mapOf("key1" to "value1", "key2" to "value2"),
					TestEnum.ENUM_VALUE_2 to mapOf("key3" to "value3"),
				),
			)

			mapper.readValue<TestEntity>(json) shouldBe expected
		}

		"Deserialization of JSON with array values should convert to map" {
			val json = """
            {
                "id": "entity2",
                "weirdMap": {
                    "thisHasNothingToDoWithEnumName": [
                        "value1",
                        "value2"
                    ],
                    "ENUM_VALUE_2": [
                        "value3"
                    ]
                }
            }
			""".trimIndent()
			val expected = TestEntity(
				id = "entity2",
				weirdMap = mapOf(
					TestEnum.ENUM_VALUE_1 to mapOf("value1" to "value1", "value2" to "value2"),
					TestEnum.ENUM_VALUE_2 to mapOf("value3" to "value3"),
				),
			)

			mapper.readValue<TestEntity>(json) shouldBe expected
		}

		"Deserialization of Service with array for qualified links should convert to map. Serialization should keep the map" {
			val id = UUID.randomUUID().toString()
			val link1 = UUID.randomUUID().toString()
			val link2 = UUID.randomUUID().toString()
			val link3 = UUID.randomUUID().toString()
			val json = """
            {
                "_id": "$id",
                "qualifiedLinks": {
                    "${LinkQualification.parent}": [
                        "$link1",
                        "$link2"
                    ],
                    "${LinkQualification.child}": [
                        "$link3"
                    ]
                }
            }
			""".trimIndent()
			val expectedDeserialized = Service(
				id = id,
				qualifiedLinks = mapOf(
					LinkQualification.parent to mapOf(link1 to link1, link2 to link2),
					LinkQualification.child to mapOf(link3 to link3),
				),
			)
			val deserialized = mapper.readValue<Service>(json)
			deserialized shouldBe expectedDeserialized

			val serialized = mapper.valueToTree<ObjectNode>(deserialized)
			serialized["qualifiedLinks"][LinkQualification.parent.toString()].also {
				it.isObject shouldBe true
				it.properties().asSequence().associate { it.key to it.value.asText() } shouldBe mapOf(
					link1 to link1,
					link2 to link2,
				)
			}
			serialized["qualifiedLinks"][LinkQualification.child.toString()].also {
				it.isObject shouldBe true
				it.properties().asSequence().associate { it.key to it.value.asText() } shouldBe mapOf(
					link3 to link3,
				)
			}
		}

		"Deserialization of Service with Map should preserve keys" {
			val id = UUID.randomUUID().toString()
			val key1 = UUID.randomUUID().toString()
			val link1 = UUID.randomUUID().toString()
			val key2 = UUID.randomUUID().toString()
			val link2 = UUID.randomUUID().toString()
			val key3 = UUID.randomUUID().toString()
			val link3 = UUID.randomUUID().toString()
			val json = """
            {
                "_id": "$id",
                "qualifiedLinks": {
                    "${LinkQualification.parent}": {
                        "$key1": "$link1",
                        "$key2": "$link2"
                    },
                    "${LinkQualification.child}": {
                        "$key3": "$link3"
                    }
                }
            }
			""".trimIndent()
			val expected = Service(
				id = id,
				qualifiedLinks = mapOf(
					LinkQualification.parent to mapOf(key1 to link1, key2 to link2),
					LinkQualification.child to mapOf(key3 to link3),
				),
			)
			mapper.readValue<Service>(json) shouldBe expected
		}
	})
