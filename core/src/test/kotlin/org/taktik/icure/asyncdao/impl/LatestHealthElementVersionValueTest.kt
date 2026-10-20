package org.taktik.icure.asyncdao.impl

import com.fasterxml.jackson.databind.ObjectMapper
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe

class LatestHealthElementVersionValueTest : StringSpec({

	val objectMapper = ObjectMapper()
	fun parse(json: String): LatestHealthElementVersionValue = objectMapper.readValue(json, LatestHealthElementVersionValue::class.java)

	// Values as returned by `by_health_element_id_latest?group=true` with each reduce function (CouchDB 3.5.1)

	"Reads the latest document id from the value of the custom reduce" {
		parse("""[1755606048834,"11df4a19-2a1f-42b4-a367-fdf4a2352453"]""").documentId shouldBe "11df4a19-2a1f-42b4-a367-fdf4a2352453"
		parse("""[null,"b3"]""").documentId shouldBe "b3"
	}

	"Reads the latest document id from the value of the built-in _top_1 reducer" {
		parse("""[[1755606048834,"11df4a19-2a1f-42b4-a367-fdf4a2352453"]]""").documentId shouldBe "11df4a19-2a1f-42b4-a367-fdf4a2352453"
		parse("""[[null,"b3"]]""").documentId shouldBe "b3"
	}

	"Rejects values in an unexpected shape" {
		listOf(
			"""[1755606048834]""",
			"""[1755606048834,"a1","extra"]""",
			"""[1755606048834,42]""",
			"""[[1755606048834,"a1"],[1755606048833,"a2"]]""",
			"""[["a1"]]""",
		).forEach {
			shouldThrow<IllegalStateException> { parse(it) }
		}
	}
})
