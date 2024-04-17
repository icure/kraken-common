package org.taktik.icure.services.external.rest.v1.mapper.embed

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import org.taktik.icure.entities.embed.Measure
import org.taktik.icure.entities.embed.ReferenceRange
import org.taktik.icure.services.external.rest.v1.dto.embed.MeasureDto
import org.taktik.icure.services.external.rest.v1.mapper.base.CodeStubMapperImpl

class MeasureMapperTest : StringSpec({

	"If a Measure has no max or min but at least one reference range, then max and min are autofilled" {
		val mapper = MeasureMapperImpl(CodeStubMapperImpl())

		val value = 10.0
		val min = 1.0
		val max = 42.0

		val measure = Measure(
			value = value,
			referenceRanges = listOf(
				ReferenceRange(
					low = min,
					high = max
				)
			)
		)

		val expectedDto = MeasureDto(value = value, min = min, max = max)
		mapper.map(measure) shouldBe expectedDto
	}

	"If a Measure has no max or min but and no reference ranges, then max and min are null" {
		val mapper = MeasureMapperImpl(CodeStubMapperImpl())

		val value = 10.0
		val measure = Measure(value = value)

		val expectedDto = MeasureDto(value = value)
		mapper.map(measure) shouldBe expectedDto
	}

	"The mapper can still map max and min" {
		val mapper = MeasureMapperImpl(CodeStubMapperImpl())

		val value = 10.0
		val min = 1.0
		val max = 42.0

		val measure = Measure(
			value = value,
			max = max,
			min = min
		)

		val expectedDto = MeasureDto(value = value, min = min, max = max)
		mapper.map(measure) shouldBe expectedDto
	}

})