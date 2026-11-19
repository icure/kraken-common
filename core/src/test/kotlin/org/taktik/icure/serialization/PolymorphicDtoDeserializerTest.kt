package org.taktik.icure.serialization

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import org.taktik.icure.services.external.rest.v2.dto.embed.form.template.Field
import org.taktik.icure.services.external.rest.v2.dto.embed.form.template.TextField
import org.taktik.icure.services.external.rest.v2.dto.filter.AbstractFilterDto
import org.taktik.icure.services.external.rest.v2.dto.filter.patient.PatientByIdsFilter
import org.taktik.icure.services.external.rest.v2.dto.filter.predicate.AndPredicate
import org.taktik.icure.services.external.rest.v2.dto.filter.predicate.Predicate
import org.taktik.icure.services.external.rest.v2.handlers.JacksonFilterDeserializer
import org.taktik.icure.services.external.rest.v1.dto.filter.AbstractFilterDto as AbstractFilterDtoV1
import org.taktik.icure.services.external.rest.v1.dto.filter.patient.PatientByIdsFilter as PatientByIdsFilterV1

/**
 * The polymorphic DTO deserializers resolve their subclasses with a classpath scan. That scan must happen once, and
 * the deserializers must be cachable, otherwise Jackson re-creates them (and re-scans the classpath) on every request.
 */
class PolymorphicDtoDeserializerTest : StringSpec({
	val mapper = ObjectMapper().registerKotlinModule()

	"v2 filter is deserialized using the \$type discriminator" {
		val filter = mapper.readValue<AbstractFilterDto<*>>("""{"${'$'}type":"PatientByIdsFilter","ids":["a","b"]}""")
		filter.shouldBeInstanceOf<PatientByIdsFilter>().ids shouldBe setOf("a", "b")
	}

	"v2 filter is deserialized using the alternative type/filterType format" {
		val filter = mapper.readValue<AbstractFilterDto<*>>("""{"type":"PatientByIdsFilter","filterType":"x","ids":["a"]}""")
		filter.shouldBeInstanceOf<PatientByIdsFilter>().ids shouldBe setOf("a")
	}

	"v1 filter is deserialized using the \$type discriminator" {
		val filter = mapper.readValue<AbstractFilterDtoV1<*>>("""{"${'$'}type":"PatientByIdsFilter","ids":["a"]}""")
		filter.shouldBeInstanceOf<PatientByIdsFilterV1>().ids shouldBe setOf("a")
	}

	"v2 predicate and field are deserialized" {
		mapper.readValue<Predicate>("""{"${'$'}type":"AndPredicate","predicates":[]}""").shouldBeInstanceOf<AndPredicate>()
		mapper.readValue<Field>("""{"type":"textfield","field":"f"}""").shouldBeInstanceOf<TextField>().field shouldBe "f"
	}

	"polymorphic deserializers are cachable" {
		JacksonFilterDeserializer().isCachable shouldBe true
		org.taktik.icure.services.external.rest.v1.handlers.JacksonFilterDeserializer().isCachable shouldBe true
	}
})
