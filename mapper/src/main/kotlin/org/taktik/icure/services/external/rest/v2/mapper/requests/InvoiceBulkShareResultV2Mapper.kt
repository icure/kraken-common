package org.taktik.icure.services.external.rest.v2.mapper.requests

import org.mapstruct.Mapping
import org.mapstruct.Named
import org.springframework.stereotype.Service
import org.taktik.icure.entities.Invoice
import org.taktik.icure.entities.requests.EntityBulkShareResult
import org.taktik.icure.services.external.rest.v2.dto.InvoiceDto
import org.taktik.icure.services.external.rest.v2.dto.requests.EntityBulkShareResultDto
import org.taktik.icure.services.external.rest.v2.mapper.InvoiceV2Mapper

// TODO tmp no support yet for generics

interface InvoiceBulkShareResultV2Mapper {
	@Mapping(source = "updatedEntity", target = "updatedEntity", qualifiedByName = ["dtoToInvoice"])
	fun map(bulkShareResult: EntityBulkShareResult<Invoice>): EntityBulkShareResultDto<InvoiceDto>

	@Named("invoiceToDto")
	fun invoiceToDto(invoice: Invoice?): InvoiceDto?
}

@Service
class InvoiceBulkShareResultV2MapperImpl(
	private val rejectedShareRequestV2Mapper: RejectedShareRequestV2Mapper,
	private val invoiceMapper: InvoiceV2Mapper,
) : InvoiceBulkShareResultV2Mapper {
	override fun map(bulkShareResult: EntityBulkShareResult<Invoice>): EntityBulkShareResultDto<InvoiceDto> = EntityBulkShareResultDto(
		updatedEntity =
		bulkShareResult.updatedEntity?.let { invoiceMapper.map(it) },
		entityId = bulkShareResult.entityId,
		entityRev = bulkShareResult.entityRev,
		rejectedRequests = bulkShareResult.rejectedRequests.map { (k, v) ->
			k to this.rejectedShareRequestV2Mapper.map(v)
		}.toMap(),
	)

	override fun invoiceToDto(invoice: Invoice?): InvoiceDto? = invoice?.let { invoiceMapper.map(it) }
}
