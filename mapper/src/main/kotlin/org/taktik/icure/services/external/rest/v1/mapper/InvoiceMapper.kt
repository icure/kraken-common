/*
 * Copyright (c) 2020. Taktik SA, All rights reserved.
 */

package org.taktik.icure.services.external.rest.v1.mapper

import org.mapstruct.InjectionStrategy
import org.mapstruct.Mapper
import org.mapstruct.Mapping
import org.mapstruct.Mappings
import org.taktik.icure.entities.Invoice
import org.taktik.icure.services.external.rest.v1.dto.InvoiceDto
import org.taktik.icure.services.external.rest.v1.mapper.base.CodeStubMapper
import org.taktik.icure.services.external.rest.v1.mapper.base.IdentifierMapper
import org.taktik.icure.services.external.rest.v1.mapper.embed.DelegationMapper
import org.taktik.icure.services.external.rest.v1.mapper.embed.IdentityDocumentReaderMapper
import org.taktik.icure.services.external.rest.v1.mapper.embed.InvoicingCodeMapper
import org.taktik.icure.services.external.rest.v1.mapper.embed.PaymentMapper
import org.taktik.icure.services.external.rest.v1.mapper.embed.SecurityMetadataMapper

@Mapper(componentModel = "spring", uses = [InvoicingCodeMapper::class, IdentifierMapper::class, IdentityDocumentReaderMapper::class, CodeStubMapper::class, PaymentMapper::class, DelegationMapper::class, SecurityMetadataMapper::class], injectionStrategy = InjectionStrategy.CONSTRUCTOR)
interface InvoiceMapper {
	@Mappings(
		Mapping(target = "attachments", ignore = true),
		Mapping(target = "revHistory", ignore = true),
		Mapping(target = "conflicts", ignore = true),
		Mapping(target = "revisionsInfo", ignore = true)
	)
	fun map(invoiceDto: InvoiceDto): Invoice
	fun map(invoice: Invoice): InvoiceDto
}
