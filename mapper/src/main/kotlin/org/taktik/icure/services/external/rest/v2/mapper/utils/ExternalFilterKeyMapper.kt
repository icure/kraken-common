package org.taktik.icure.services.external.rest.v2.mapper.utils

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.treeToValue
import org.springframework.stereotype.Service
import org.taktik.icure.entities.utils.ExternalFilterKey
import org.taktik.icure.services.external.rest.v2.dto.utils.ExternalFilterKeyDto

@Service
class ExternalFilterKeyV2Mapper(
	val objectMapper: ObjectMapper,
) {

	fun map(externalFilterKey: ExternalFilterKey): ExternalFilterKeyDto = when (externalFilterKey) {
		is ExternalFilterKey.ExternalFilterStringKey -> ExternalFilterKeyDto.ExternalFilterStringKeyDto(externalFilterKey.key)
		is ExternalFilterKey.ExternalFilterLongKey -> ExternalFilterKeyDto.ExternalFilterLongKeyDto(externalFilterKey.key)
		is ExternalFilterKey.ExternalFilterComplexKey -> ExternalFilterKeyDto.ExternalFilterComplexKeyDto(
			objectMapper.valueToTree(externalFilterKey.key),
		)
	}

	fun map(externalFilterKeyDto: ExternalFilterKeyDto): ExternalFilterKey = when (externalFilterKeyDto) {
		is ExternalFilterKeyDto.ExternalFilterStringKeyDto -> ExternalFilterKey.ExternalFilterStringKey(externalFilterKeyDto.key)
		is ExternalFilterKeyDto.ExternalFilterLongKeyDto -> ExternalFilterKey.ExternalFilterLongKey(externalFilterKeyDto.key)
		is ExternalFilterKeyDto.ExternalFilterComplexKeyDto -> ExternalFilterKey.ExternalFilterComplexKey(
			objectMapper.treeToValue(externalFilterKeyDto.key),
		)
	}
}
