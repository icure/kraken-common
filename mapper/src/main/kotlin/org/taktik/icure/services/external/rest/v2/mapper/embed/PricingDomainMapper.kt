package org.taktik.icure.services.external.rest.v2.mapper.embed

import org.springframework.stereotype.Service
import org.taktik.icure.entities.embed.PricingDomain
import org.taktik.icure.services.external.rest.v2.dto.embed.PricingDomainDto

@Service
class PricingDomainV2Mapper {

	fun map(pricingDomain: PricingDomain): PricingDomainDto = PricingDomainDto.valueOf(pricingDomain.name)
	fun map(pricingDomainDto: PricingDomainDto): PricingDomain = PricingDomain.valueOf(pricingDomainDto.name)
}