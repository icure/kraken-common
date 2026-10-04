package org.taktik.icure.entities.designdoc

import com.fasterxml.jackson.module.kotlin.readValue
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import org.taktik.icure.serialization.IcureDomainObjectMapper

class DesignDocSchemaSerializationTest :
	StringSpec({
		val mapper = IcureDomainObjectMapper.new()

		"DesignDocSchema should be preserved through a serialization round-trip" {
			val schema = DesignDocSchema(
				rev = "1-abc",
				applicationGroupId = "app-group",
				version = 3,
				committed = true,
				viewsByEntity = mapOf(
					"Patient" to mapOf("by_custom_key" to 1, "by_data_owner_custom" to 0),
					"Contact" to mapOf("by_date" to 2),
				),
				viewsNotByDataOwner = mapOf(
					"Patient" to setOf("by_custom_key"),
					"Contact" to setOf("by_date"),
				),
				customViewsVersion = 7,
				deletionDate = 1234L,
			)
			mapper.readValue<DesignDocSchema>(mapper.writeValueAsString(schema)) shouldBe schema
		}

		"viewsNotByDataOwner should be read from its serialized property name" {
			val json = mapper.writeValueAsString(
				DesignDocSchema(
					rev = null,
					applicationGroupId = "app-group",
					version = 0,
					viewsByEntity = mapOf("Patient" to mapOf("by_custom_key" to 0)),
					viewsNotByDataOwner = mapOf("Patient" to setOf("by_custom_key")),
				),
			)
			mapper.readTree(json).has("viewsNotByDataOwner") shouldBe true
			mapper.readValue<DesignDocSchema>(json).viewsNotByDataOwner shouldBe mapOf("Patient" to setOf("by_custom_key"))
		}
	})
