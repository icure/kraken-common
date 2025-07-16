package org.taktik.icure.services.external.rest.v2.mapper.requests

import org.mapstruct.Mapping
import org.mapstruct.Named
import org.springframework.stereotype.Service
import org.taktik.icure.entities.Invoice
import org.taktik.icure.entities.requests.EntityBulkShareResult
import org.taktik.icure.services.external.rest.v2.dto.InvoiceDto
import org.taktik.icure.services.external.rest.v2.dto.requests.EntityBulkShareResultDto
import org.taktik.icure.services.external.rest.v2.mapper.InvoiceV2Mapper

//TODO tmp no support yet for generics

interface InvoiceBulkShareResultV2Mapper {
    @Mapping(source = "updatedEntity", target = "updatedEntity", qualifiedByName = ["invoiceToDto"])
    fun map(bulkShareResultDto: EntityBulkShareResultDto<InvoiceDto>): EntityBulkShareResult<Invoice>
    @Mapping(source = "updatedEntity", target = "updatedEntity", qualifiedByName = ["dtoToInvoice"])
    fun map(bulkShareResult: EntityBulkShareResult<Invoice>): EntityBulkShareResultDto<InvoiceDto>

    @Named("invoiceToDto")
    fun invoiceToDto(invoice: Invoice?): InvoiceDto?

    @Named("dtoToInvoice")
    fun dtoToInvoice(invoiceDto: InvoiceDto?): Invoice?
}

@Service
class InvoiceBulkShareResultV2MapperImpl(
    private val rejectedShareRequestV2Mapper: RejectedShareRequestV2Mapper,
    private val invoiceMapper: InvoiceV2Mapper
) : InvoiceBulkShareResultV2Mapper {
    override fun map(bulkShareResultDto: EntityBulkShareResultDto<InvoiceDto>):
            EntityBulkShareResult<Invoice> = EntityBulkShareResult(
        updatedEntity = bulkShareResultDto.updatedEntity?.let { invoiceMapper.map(it) },
        entityId = bulkShareResultDto.entityId,
        entityRev = bulkShareResultDto.entityRev,
        rejectedRequests = bulkShareResultDto.rejectedRequests.map { (k, v) ->
            k to this.rejectedShareRequestV2Mapper.map(v)
        }.toMap(),
    )

    override fun map(bulkShareResult: EntityBulkShareResult<Invoice>):
            EntityBulkShareResultDto<InvoiceDto> = EntityBulkShareResultDto(
        updatedEntity =
        bulkShareResult.updatedEntity?.let { invoiceMapper.map(it) },
        entityId = bulkShareResult.entityId,
        entityRev = bulkShareResult.entityRev,
        rejectedRequests = bulkShareResult.rejectedRequests.map { (k, v) ->
            k to this.rejectedShareRequestV2Mapper.map(v)
        }.toMap(),
    )

    override fun invoiceToDto(invoice: Invoice?): InvoiceDto? = invoice?.let { invoiceMapper.map(it) }
    override fun dtoToInvoice(invoiceDto: InvoiceDto?): Invoice? = invoiceDto?.let { invoiceMapper.map(it) }
}

