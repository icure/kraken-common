/*
 * Copyright (c) 2020. Taktik SA, All rights reserved.
 */

package org.taktik.icure.services.external.rest.v1.mapper.embed

import org.mapstruct.InjectionStrategy
import org.mapstruct.Mapper
import org.taktik.icure.entities.embed.InvoicingCode
import org.taktik.icure.services.external.rest.v1.dto.embed.InvoicingCodeDto

@Mapper(componentModel = "spring", uses = [], injectionStrategy = InjectionStrategy.CONSTRUCTOR)
interface InvoicingCodeMapper {
	fun map(invoicingCodeDto: InvoicingCodeDto): InvoicingCode
	fun map(invoicingCode: InvoicingCode): InvoicingCodeDto
}
