package org.taktik.icure.services.external.rest.v2.mapper.conflicts

import org.taktik.icure.entities.AccessLog
import org.taktik.icure.entities.Agenda
import org.taktik.icure.entities.CalendarItem
import org.taktik.icure.entities.CalendarItemType
import org.taktik.icure.entities.Contact
import org.taktik.icure.entities.Device
import org.taktik.icure.entities.Document
import org.taktik.icure.entities.Form
import org.taktik.icure.entities.HealthElement
import org.taktik.icure.entities.HealthcareParty
import org.taktik.icure.entities.Insurance
import org.taktik.icure.entities.Invoice
import org.taktik.icure.entities.Message
import org.taktik.icure.entities.Patient
import org.taktik.icure.entities.Place
import org.taktik.icure.entities.Receipt
import org.taktik.icure.entities.User
import org.taktik.icure.entities.base.Code
import org.taktik.icure.entities.base.StoredDocument
import org.taktik.icure.entities.conflicts.ConflictResolutionResult
import org.taktik.icure.services.external.rest.v2.dto.base.StoredDocumentDto
import org.taktik.icure.services.external.rest.v2.dto.conflicts.ConflictResolutionResultDto
import org.taktik.icure.services.external.rest.v2.mapper.AccessLogV2Mapper
import org.taktik.icure.services.external.rest.v2.mapper.AgendaV2Mapper
import org.taktik.icure.services.external.rest.v2.mapper.CalendarItemTypeV2Mapper
import org.taktik.icure.services.external.rest.v2.mapper.CalendarItemV2Mapper
import org.taktik.icure.services.external.rest.v2.mapper.ContactV2Mapper
import org.taktik.icure.services.external.rest.v2.mapper.DeviceV2Mapper
import org.taktik.icure.services.external.rest.v2.mapper.DocumentV2Mapper
import org.taktik.icure.services.external.rest.v2.mapper.FormV2Mapper
import org.taktik.icure.services.external.rest.v2.mapper.HealthElementV2Mapper
import org.taktik.icure.services.external.rest.v2.mapper.HealthcarePartyV2Mapper
import org.taktik.icure.services.external.rest.v2.mapper.InsuranceV2Mapper
import org.taktik.icure.services.external.rest.v2.mapper.InvoiceV2Mapper
import org.taktik.icure.services.external.rest.v2.mapper.MessageV2Mapper
import org.taktik.icure.services.external.rest.v2.mapper.PatientV2Mapper
import org.taktik.icure.services.external.rest.v2.mapper.PlaceV2Mapper
import org.taktik.icure.services.external.rest.v2.mapper.ReceiptV2Mapper
import org.taktik.icure.services.external.rest.v2.mapper.UnsecureUserV2Mapper
import org.taktik.icure.services.external.rest.v2.mapper.base.CodeV2Mapper

open class ConflictResolutionV2Mapper(
	private val accessLogV2Mapper: AccessLogV2Mapper,
	private val agendaV2Mapper: AgendaV2Mapper,
	private val calendarItemV2Mapper: CalendarItemV2Mapper,
	private val calendarItemTypeV2Mapper: CalendarItemTypeV2Mapper,
	private val codeV2Mapper: CodeV2Mapper,
	private val contactV2Mapper: ContactV2Mapper,
	private val deviceV2Mapper: DeviceV2Mapper,
	private val documentV2Mapper: DocumentV2Mapper,
	private val formV2Mapper: FormV2Mapper,
	private val healthcarePartyV2Mapper: HealthcarePartyV2Mapper,
	private val healthElementV2Mapper: HealthElementV2Mapper,
	private val insuranceV2Mapper: InsuranceV2Mapper,
	private val invoiceV2Mapper: InvoiceV2Mapper,
	private val messageV2Mapper: MessageV2Mapper,
	private val patientV2Mapper: PatientV2Mapper,
	private val placeV2Mapper: PlaceV2Mapper,
	private val receiptV2Mapper: ReceiptV2Mapper,
	private val userV2Mapper: UnsecureUserV2Mapper
) {

	@Suppress("UNCHECKED_CAST")
	protected open suspend fun <I : StoredDocument, O : StoredDocumentDto> mapDocument(entity: I): O =
		when (entity) {
			is AccessLog -> accessLogV2Mapper.map(entity)
			is Agenda -> agendaV2Mapper.map(entity)
			is CalendarItem -> calendarItemV2Mapper.map(entity)
			is CalendarItemType -> calendarItemTypeV2Mapper.map(entity)
			is Code -> codeV2Mapper.map(entity)
			is Contact -> contactV2Mapper.map(entity)
			is Device -> deviceV2Mapper.map(entity)
			is Document -> documentV2Mapper.map(entity)
			is Form -> formV2Mapper.map(entity)
			is HealthcareParty -> healthcarePartyV2Mapper.map(entity)
			is HealthElement -> healthElementV2Mapper.map(entity)
			is Insurance -> insuranceV2Mapper.map(entity)
			is Invoice -> invoiceV2Mapper.map(entity)
			is Message -> messageV2Mapper.map(entity)
			is Patient -> patientV2Mapper.map(entity)
			is Place -> placeV2Mapper.map(entity)
			is Receipt -> receiptV2Mapper.map(entity)
			is User -> userV2Mapper.map(entity)
			else -> throw IllegalStateException("Invalid entity type: ${entity.javaClass.name}")
		} as O

	suspend fun <I : StoredDocument, O : StoredDocumentDto> map(
		conflictResolutionResult: ConflictResolutionResult<I>
	): ConflictResolutionResultDto<O> =
		ConflictResolutionResultDto(
			document = mapDocument(conflictResolutionResult.document),
			remainingConflicts = conflictResolutionResult.remainingConflicts
		)

}