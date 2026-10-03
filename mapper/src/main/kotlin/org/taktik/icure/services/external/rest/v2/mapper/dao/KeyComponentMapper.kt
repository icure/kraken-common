package org.taktik.icure.services.external.rest.v2.mapper.dao

import org.springframework.stereotype.Service
import org.taktik.icure.entities.dao.KeyComponent
import org.taktik.icure.services.external.rest.v2.dto.dao.KeyComponentDto

@Service
class KeyComponentV2Mapper {

	fun map(keyComponent: KeyComponent): KeyComponentDto = when (keyComponent) {
		is KeyComponent.Int -> KeyComponentDto.IntKeyComponentDto(keyComponent.value)
		is KeyComponent.Long -> KeyComponentDto.LongKeyComponentDto(keyComponent.value)
		is KeyComponent.Double -> KeyComponentDto.DoubleKeyComponentDto(keyComponent.value)
		is KeyComponent.Boolean -> KeyComponentDto.BooleanKeyComponentDto(keyComponent.value)
		is KeyComponent.String -> KeyComponentDto.StringKeyComponentDto(keyComponent.value)
	}

	fun map(keyComponentDto: KeyComponentDto): KeyComponent = when (keyComponentDto) {
		is KeyComponentDto.IntKeyComponentDto -> KeyComponent.Int(keyComponentDto.value)
		is KeyComponentDto.LongKeyComponentDto -> KeyComponent.Long(keyComponentDto.value)
		is KeyComponentDto.DoubleKeyComponentDto -> KeyComponent.Double(keyComponentDto.value)
		is KeyComponentDto.BooleanKeyComponentDto -> KeyComponent.Boolean(keyComponentDto.value)
		is KeyComponentDto.StringKeyComponentDto -> KeyComponent.String(keyComponentDto.value)
	}
}
