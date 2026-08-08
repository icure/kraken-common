package org.taktik.icure.config.filters

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import org.springframework.core.codec.Encoder
import org.taktik.icure.config.SharedWebFluxConfiguration
import org.taktik.icure.entities.utils.SemanticVersion
import org.taktik.icure.services.external.rest.v2.dto.DocumentDto
import org.taktik.icure.services.external.rest.v2.dto.GroupDto
import org.taktik.icure.services.external.rest.v2.dto.HealthElementDto
import org.taktik.icure.services.external.rest.v2.dto.InsuranceDto
import org.taktik.icure.services.external.rest.v2.dto.embed.DataAttachmentDto

/**
 * Exposes the protected mappers built by [SharedWebFluxConfiguration] so that tests can serialize DTOs with the
 * exact same [CardinalMappersProviderImpl] wiring used in production (mixins registered per SDK version, as
 * generated from the `@SerializationPolicy`/`@LegacyField`/`@ActiveField` annotations on the DTOs).
 */
private class TestCardinalConfig : SharedWebFluxConfiguration(CardinalMappersProviderImpl()) {
	val mappers get() = cardinalMappers
	val legacyMapper get() = legacyObjectMapper

	override fun getJackson2JsonEncoder(): Encoder<Any> = throw UnsupportedOperationException("not used in tests")
}

private val config = TestCardinalConfig()

/**
 * Picks the mapper that would be used to serialize a response for the given (nullable) requested cardinal
 * [version] and legacy-compatibility flag, mirroring [org.taktik.icure.spring.encoder.MultiMapperHttpMessageEncoder].
 * A null [version] means no version header was provided at all, in which case the legacy mapper is always used.
 */
private fun mapperFor(version: String?, includingLegacyFields: Boolean = false): ObjectMapper =
	version
		?.let { config.mappers.getForVersion(SemanticVersion(it), includingLegacyFields) }
		?: config.legacyMapper

private fun ObjectMapper.toTree(value: Any): JsonNode = this.valueToTree(value)

class CardinalVersionedSerializationTest : StringSpec({
	"a field annotated with SerializationPolicy(Since(Filtered) -> Since(ActiveField)) is filtered out only strictly before the ActiveField version, in both default and legacy-compatible mappers" {
		val document = DocumentDto(id = "doc", mainAttachmentStoredDataSize = 123L)

		// No version header at all: always the legacy mapper, field always included.
		mapperFor(version = null).toTree(document).has("mainAttachmentStoredDataSize") shouldBe true

		// Filtered in both regimes below 2.4.0...
		mapperFor("2.0.0", includingLegacyFields = false).toTree(document).has("mainAttachmentStoredDataSize") shouldBe false
		mapperFor("2.0.0", includingLegacyFields = true).toTree(document).has("mainAttachmentStoredDataSize") shouldBe false
		mapperFor("2.3.9", includingLegacyFields = false).toTree(document).has("mainAttachmentStoredDataSize") shouldBe false
		mapperFor("2.3.9", includingLegacyFields = true).toTree(document).has("mainAttachmentStoredDataSize") shouldBe false

		// ...and included again from 2.4.0 onwards, in both regimes.
		mapperFor("2.4.0", includingLegacyFields = false).toTree(document).has("mainAttachmentStoredDataSize") shouldBe true
		mapperFor("2.4.0", includingLegacyFields = true).toTree(document).has("mainAttachmentStoredDataSize") shouldBe true
		mapperFor("2.13.0", includingLegacyFields = false).toTree(document).has("mainAttachmentStoredDataSize") shouldBe true
	}

	"the same Filtered -> ActiveField transition applies to DataAttachmentDto.storedDataSize" {
		val attachment = DataAttachmentDto(storedDataSize = 456L)

		mapperFor(version = null).toTree(attachment).has("storedDataSize") shouldBe true
		mapperFor("2.0.0", includingLegacyFields = false).toTree(attachment).has("storedDataSize") shouldBe false
		mapperFor("2.0.0", includingLegacyFields = true).toTree(attachment).has("storedDataSize") shouldBe false
		mapperFor("2.4.0", includingLegacyFields = false).toTree(attachment).has("storedDataSize") shouldBe true
		mapperFor("2.4.0", includingLegacyFields = true).toTree(attachment).has("storedDataSize") shouldBe true
	}

	"GroupDto's cockpit-managed fields are filtered out only strictly before their ActiveField version (2.7.0)" {
		val group = GroupDto(id = "group", designDocSchemaVersions = setOf(1), defaultChildrenSchemaVersion = 5)

		mapperFor(version = null).toTree(group).let {
			it.has("designDocSchemaVersions") shouldBe true
			it.has("defaultChildrenSchemaVersion") shouldBe true
		}
		mapperFor("2.0.0", includingLegacyFields = false).toTree(group).let {
			it.has("designDocSchemaVersions") shouldBe false
			it.has("defaultChildrenSchemaVersion") shouldBe false
		}
		mapperFor("2.0.0", includingLegacyFields = true).toTree(group).let {
			it.has("designDocSchemaVersions") shouldBe false
			it.has("defaultChildrenSchemaVersion") shouldBe false
		}
		mapperFor("2.4.0", includingLegacyFields = false).toTree(group).has("designDocSchemaVersions") shouldBe false
		mapperFor("2.7.0", includingLegacyFields = false).toTree(group).let {
			it.has("designDocSchemaVersions") shouldBe true
			it.has("defaultChildrenSchemaVersion") shouldBe true
		}
		mapperFor("2.7.0", includingLegacyFields = true).toTree(group).let {
			it.has("designDocSchemaVersions") shouldBe true
			it.has("defaultChildrenSchemaVersion") shouldBe true
		}
	}

	"a field annotated with SerializationPolicy(Since(Filtered)) with no later ActiveField stays filtered for every version, in both regimes" {
		val insurance = InsuranceDto(
			id = "ins",
			privateInsurance = true,
			hospitalisationInsurance = true,
			ambulatoryInsurance = true,
		)
		fun JsonNode.hasAnyDeprecatedInsuranceField() =
			has("privateInsurance") || has("hospitalisationInsurance") || has("ambulatoryInsurance")

		mapperFor(version = null).toTree(insurance).hasAnyDeprecatedInsuranceField() shouldBe true

		// Not yet filtered before the Since("2.9.0") threshold.
		mapperFor("2.0.0", includingLegacyFields = false).toTree(insurance).hasAnyDeprecatedInsuranceField() shouldBe true
		mapperFor("2.7.0", includingLegacyFields = false).toTree(insurance).hasAnyDeprecatedInsuranceField() shouldBe true

		// Filtered from 2.9.0 onwards, regardless of the legacy-compatibility flag.
		mapperFor("2.9.0", includingLegacyFields = false).toTree(insurance).hasAnyDeprecatedInsuranceField() shouldBe false
		mapperFor("2.9.0", includingLegacyFields = true).toTree(insurance).hasAnyDeprecatedInsuranceField() shouldBe false
		mapperFor("2.13.0", includingLegacyFields = false).toTree(insurance).hasAnyDeprecatedInsuranceField() shouldBe false
		mapperFor("2.13.0", includingLegacyFields = true).toTree(insurance).hasAnyDeprecatedInsuranceField() shouldBe false
	}

	"a field annotated with @LegacyField is serialized only when no version is requested, or when legacy compatibility is explicitly requested" {
		val healthElement = HealthElementDto(id = "he", status = 7)

		// No version header: legacy mapper, field included.
		mapperFor(version = null).toTree(healthElement).has("status") shouldBe true

		// Any cardinal version without the legacy-compatibility flag: field excluded.
		mapperFor("2.0.0", includingLegacyFields = false).toTree(healthElement).has("status") shouldBe false
		mapperFor("2.13.0", includingLegacyFields = false).toTree(healthElement).has("status") shouldBe false

		// Same cardinal versions, but with the legacy-compatibility flag: field included again.
		mapperFor("2.0.0", includingLegacyFields = true).toTree(healthElement).has("status") shouldBe true
		mapperFor("2.13.0", includingLegacyFields = true).toTree(healthElement).has("status") shouldBe true
	}
})
