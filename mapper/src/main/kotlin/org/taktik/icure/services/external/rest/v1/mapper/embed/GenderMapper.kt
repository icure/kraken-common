package org.taktik.icure.services.external.rest.v1.mapper.embed

import org.springframework.stereotype.Service
import org.taktik.icure.entities.embed.Gender
import org.taktik.icure.services.external.rest.v1.dto.embed.GenderDto

@Service
class GenderMapper {

	fun map(gender: Gender): GenderDto = GenderDto.valueOf(gender.name)
	fun map(genderDto: GenderDto): Gender = Gender.valueOf(genderDto.name)
}
