/*
 * Copyright (c) 2020. Taktik SA, All rights reserved.
 */

package org.taktik.icure.services.external.rest.v2.controllers.support

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.tags.Tag
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.reactor.mono
import org.springframework.context.annotation.Profile
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.security.access.AccessDeniedException
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.server.ResponseStatusException
import org.taktik.couchdb.entity.ComplexKey
import org.taktik.couchdb.id.UUIDGenerator
import org.taktik.icure.asynclogic.SessionInformationProvider
import org.taktik.icure.asyncservice.InvoiceService
import org.taktik.icure.cache.ReactorCacheInjector
import org.taktik.icure.config.SharedPaginationConfig
import org.taktik.icure.db.PaginationOffset
import org.taktik.icure.entities.embed.InvoiceType
import org.taktik.icure.entities.embed.MediumType
import org.taktik.icure.exceptions.NotFoundRequestException
import org.taktik.icure.pagination.PaginatedFlux
import org.taktik.icure.pagination.asPaginatedFlux
import org.taktik.icure.pagination.mapElements
import org.taktik.icure.services.external.rest.v2.dto.IcureStubDto
import org.taktik.icure.services.external.rest.v2.dto.InvoiceDto
import org.taktik.icure.services.external.rest.v2.dto.ListOfIdsDto
import org.taktik.icure.services.external.rest.v2.dto.data.LabelledOccurenceDto
import org.taktik.icure.services.external.rest.v2.dto.embed.InvoiceTypeDto
import org.taktik.icure.services.external.rest.v2.dto.embed.InvoicingCodeDto
import org.taktik.icure.services.external.rest.v2.dto.embed.MediumTypeDto
import org.taktik.icure.services.external.rest.v2.dto.filter.chain.FilterChain
import org.taktik.icure.services.external.rest.v2.dto.requests.BulkShareOrUpdateMetadataParamsDto
import org.taktik.icure.services.external.rest.v2.dto.requests.EntityBulkShareResultDto
import org.taktik.icure.services.external.rest.v2.mapper.InvoiceV2Mapper
import org.taktik.icure.services.external.rest.v2.mapper.StubV2Mapper
import org.taktik.icure.services.external.rest.v2.mapper.embed.InvoicingCodeV2Mapper
import org.taktik.icure.services.external.rest.v2.mapper.filter.FilterChainV2Mapper
import org.taktik.icure.services.external.rest.v2.mapper.requests.EntityShareOrMetadataUpdateRequestV2Mapper
import org.taktik.icure.services.external.rest.v2.mapper.requests.InvoiceBulkShareResultV2Mapper
import org.taktik.icure.utils.FuzzyValues
import org.taktik.icure.utils.JsonString
import org.taktik.icure.utils.injectCachedReactorContext
import org.taktik.icure.utils.injectReactorContext
import org.taktik.icure.utils.orThrow
import reactor.core.publisher.Flux

@RestController("invoiceControllerV2")
@Profile("app")
@RequestMapping("/rest/v2/invoice")
@Tag(name = "invoice")
class InvoiceController(
	private val invoiceService: InvoiceService,
	private val sessionLogic: SessionInformationProvider,
	private val uuidGenerator: UUIDGenerator,
	private val invoiceV2Mapper: InvoiceV2Mapper,
	private val filterChainV2Mapper: FilterChainV2Mapper,
	private val invoicingCodeV2Mapper: InvoicingCodeV2Mapper,
	private val stubV2Mapper: StubV2Mapper,
	private val objectMapper: ObjectMapper,
	private val bulkShareResultV2Mapper: InvoiceBulkShareResultV2Mapper,
	private val entityShareOrMetadataUpdateRequestV2Mapper: EntityShareOrMetadataUpdateRequestV2Mapper,
	private val reactorCacheInjector: ReactorCacheInjector,
	private val paginationConfig: SharedPaginationConfig
) {

	@Operation(summary = "Creates an invoice")
	@PostMapping
	fun createInvoice(@RequestBody invoiceDto: InvoiceDto) = mono {
		val invoice = invoiceService.createInvoice(invoiceV2Mapper.map(invoiceDto))
			?: throw ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Invoice creation failed")
		invoiceV2Mapper.map(invoice)
	}

	@Operation(summary = "Deletes an invoice")
	@DeleteMapping("/{invoiceId}")
	fun deleteInvoice(@PathVariable invoiceId: String) = mono {
		invoiceService.deleteInvoice(invoiceId) ?: throw NotFoundRequestException("Insurance not found")
	}

	@Operation(summary = "Gets an invoice")
	@GetMapping("/{invoiceId}")
	fun getInvoice(@PathVariable invoiceId: String) = mono {
		val invoice = invoiceService.getInvoice(invoiceId)
			?: throw ResponseStatusException(HttpStatus.NOT_FOUND, "Invoice fetching failed")
		invoiceV2Mapper.map(invoice)
	}

	@Operation(summary = "Gets an invoice")
	@PostMapping("/byIds")
	fun getInvoices(@RequestBody invoiceIds: ListOfIdsDto): Flux<InvoiceDto> {
		require(invoiceIds.ids.isNotEmpty()) { "You must specify at least one id" }
		return invoiceService.getInvoices(invoiceIds.ids)
			.map { invoiceV2Mapper.map(it) }
			.injectReactorContext()
	}

	@Operation(summary = "Modifies an invoice")
	@PutMapping
	fun modifyInvoice(@RequestBody invoiceDto: InvoiceDto) = mono {
		val invoice = invoiceService.modifyInvoice(invoiceV2Mapper.map(invoiceDto))
			?: throw ResponseStatusException(HttpStatus.NOT_FOUND, "Invoice modification failed")

		invoiceV2Mapper.map(invoice)
	}

	@Operation(summary = "Modifies an invoice")
	@PostMapping("/reassign")
	fun reassignInvoice(@RequestBody invoiceDto: InvoiceDto) = mono {
		val invoice = invoiceV2Mapper.map(invoiceDto).let { it.reassign(it.invoicingCodes, uuidGenerator) }

		invoiceV2Mapper.map(invoice)
	}

	@Operation(summary = "Gets all invoices for author at date")
	@PostMapping("/mergeTo/{invoiceId}")
	fun mergeTo(@PathVariable invoiceId: String, @RequestBody ids: ListOfIdsDto) = mono {
		invoiceV2Mapper.map(
			invoiceService.mergeInvoices(
				sessionLogic.getCurrentSessionContext().getHealthcarePartyId()!!, 
				ids.ids,
				invoiceService.getInvoice(invoiceId)
			) ?: throw ResponseStatusException(HttpStatus.NOT_FOUND, "Invoice $invoiceId not found")
		)
	}

	@Operation(summary = "Gets all invoices for author at date")
	@PostMapping("/validate/{invoiceId}")
	fun validate(@PathVariable invoiceId: String, @RequestParam scheme: String, @RequestParam forcedValue: String) = mono {
		invoiceService.getInvoice(invoiceId)?.let {
			invoiceService.validateInvoice(
				sessionLogic.getCurrentSessionContext().getHealthcarePartyId()
					?: throw AccessDeniedException("Current user is not a HCP"),
				it,
				scheme,
				forcedValue)?.let(invoiceV2Mapper::map)
		}
	}

	@Operation(summary = "Gets all invoices for author at date")
	@PostMapping("/byauthor/{userId}/append/{type}/{sentMediumType}")
	fun appendCodes(
		@PathVariable userId: String,
		@PathVariable type: String,
		@PathVariable sentMediumType: String,
		@RequestParam secretFKeys: String,
		@RequestParam(required = false) insuranceId: String?,
		@RequestParam(required = false) invoiceId: String?,
		@RequestParam(required = false) gracePeriod: Int?,
		@RequestBody invoicingCodes: List<InvoicingCodeDto>
	): Flux<InvoiceDto> = flow {
		val secretPatientKeys = secretFKeys.split(',').map { it.trim() }.toSet()
		val invoices = invoiceService.appendCodes(
			sessionLogic.getCurrentSessionContext().getHealthcarePartyId()!!, userId, insuranceId, secretPatientKeys, InvoiceType.valueOf(type), MediumType.valueOf(sentMediumType),
			invoicingCodes.map { ic -> invoicingCodeV2Mapper.map(ic) }, invoiceId, gracePeriod
		)
		emitAll(invoices.map { invoiceV2Mapper.map(it) })
	}.injectReactorContext()

	@Operation(summary = "Remove an invoice of an user")
	@PostMapping("/byauthor/{userId}/service/{serviceId}")
	fun removeCodes(
		@PathVariable userId: String,
		@PathVariable serviceId: String,
		@RequestParam secretFKeys: String,
		@RequestBody tarificationIds: List<String>
	): Flux<InvoiceDto> {
		if (tarificationIds.isEmpty()) {
			throw ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Cannot modify invoice: tarificationIds is empty")
		} else {
			val secretPatientKeys = secretFKeys.split(',').map { it.trim() }.toSet()

			val invoices = invoiceService.removeCodes(userId, secretPatientKeys, serviceId, tarificationIds)

			return invoices.map { invoiceV2Mapper.map(it) }.injectReactorContext()
		}
	}

	@Operation(summary = "Gets all invoices for author at date")
	@GetMapping("/byauthor/{hcPartyId}")
	fun findInvoicesByAuthor(
		@PathVariable hcPartyId: String,
		@RequestParam(required = false) fromDate: Long?,
		@RequestParam(required = false) toDate: Long?,
		@Parameter(description = "The start key for pagination: a JSON representation of an array containing all the necessary " + "components to form the Complex Key's startKey") @RequestParam("startKey", required = false) startKey: JsonString?,
		@Parameter(description = "A patient document ID") @RequestParam(required = false) startDocumentId: String?,
		@Parameter(description = "Number of rows") @RequestParam(required = false) limit: Int?
	): PaginatedFlux<InvoiceDto> {
		val startKeyElements = startKey?.let { objectMapper.readValue<ComplexKey>(it)}
		val paginationOffset = PaginationOffset(
			startKeyElements,
			startDocumentId,
			0,
			limit ?: paginationConfig.defaultLimit
		)
		return invoiceService
			.findInvoicesByAuthor(hcPartyId, fromDate, toDate, paginationOffset)
			.mapElements(invoiceV2Mapper::map)
			.asPaginatedFlux()
	}

	@Operation(summary = "List invoices found By Healthcare Party and secret foreign patient keys.", description = "Keys have to delimited by coma")
	@GetMapping("/byHcPartySecretForeignKeys")
	fun listInvoicesByHCPartyAndPatientForeignKeys(@RequestParam hcPartyId: String, @RequestParam secretFKeys: String): Flux<InvoiceDto> {
		val secretPatientKeys = secretFKeys.split(',').map { it.trim() }.toSet()
		val elementList = invoiceService.listInvoicesByHcPartyAndPatientSfks(hcPartyId, secretPatientKeys)

		return elementList.map { element -> invoiceV2Mapper.map(element) }.injectReactorContext()
	}

	@Operation(summary = "List invoices found By Healthcare Party and secret foreign patient keys.", description = "Keys have to delimited by coma")
	@PostMapping("/byHcPartySecretForeignKeys")
	fun findInvoicesByHCPartyPatientForeignKeys(@RequestParam hcPartyId: String, @RequestBody secretPatientKeys: List<String>): Flux<InvoiceDto> {
		val elementList = invoiceService.listInvoicesByHcPartyAndPatientSfks(hcPartyId, secretPatientKeys.toSet())

		return elementList.map { element -> invoiceV2Mapper.map(element) }.injectReactorContext()
	}

	@Operation(summary = "List helement stubs found By Healthcare Party and secret foreign keys.", description = "Keys must be delimited by coma")
	@GetMapping("/byHcPartySecretForeignKeys/delegations")
	fun listInvoicesDelegationsStubsByHCPartyAndPatientForeignKeys(
		@RequestParam hcPartyId: String,
		@RequestParam secretFKeys: String
	): Flux<IcureStubDto> {
		val secretPatientKeys = secretFKeys.split(',').map { it.trim() }.toSet()
		return invoiceService.listInvoicesByHcPartyAndPatientSfks(hcPartyId, secretPatientKeys).map { invoice -> stubV2Mapper.mapToStub(invoice) }.injectReactorContext()
	}

	@Operation(summary = "Find Invoices ids by data owner id, patient secret keys and invoice date")
	@PostMapping("/byDataOwnerPatientInvoiceDate", produces = [MediaType.APPLICATION_JSON_VALUE])
	fun listInvoiceIdsByDataOwnerPatientInvoiceDate(
		@RequestParam dataOwnerId: String,
		@RequestParam(required = false) startDate: Long?,
		@RequestParam(required = false) endDate: Long?,
		@RequestParam(required = false) descending: Boolean?,
		@RequestBody secretPatientKeys: ListOfIdsDto
	): Flux<String> {
		require(secretPatientKeys.ids.isNotEmpty()) {
			"You need to provide at least one secret patient key"
		}
		return invoiceService
			.listInvoiceIdsByDataOwnerPatientInvoiceDate(
				dataOwnerId = dataOwnerId,
				secretForeignKeys = secretPatientKeys.ids.toSet(),
				startDate = startDate?.let { FuzzyValues.getFuzzyDateTime(it) },
				endDate = endDate?.let { FuzzyValues.getFuzzyDateTime(it) },
				descending = descending ?: false)
			.injectReactorContext()
	}

	@Operation(summary = "List helement stubs found By Healthcare Party and secret foreign keys.")
	@PostMapping("/byHcPartySecretForeignKeys/delegations")
	fun findInvoicesDelegationsStubsByHCPartyPatientForeignKeys(
		@RequestParam hcPartyId: String,
		@RequestBody secretPatientKeys: List<String>,
	): Flux<IcureStubDto> {
		return invoiceService.listInvoicesByHcPartyAndPatientSfks(hcPartyId, secretPatientKeys.toSet()).map { invoice -> stubV2Mapper.mapToStub(invoice) }.injectReactorContext()
	}

	@Operation(summary = "List invoices by groupId", description = "Keys have to delimited by coma")
	@GetMapping("/byHcPartyGroupId/{hcPartyId}/{groupId}")
	fun listInvoicesByHcPartyAndGroupId(@PathVariable hcPartyId: String, @PathVariable groupId: String): Flux<InvoiceDto> {
		val invoices = invoiceService.listInvoicesByHcPartyAndGroupId(hcPartyId, groupId)
		return invoices.map { el -> invoiceV2Mapper.map(el) }.injectReactorContext()
	}

	@Operation(summary = "List invoices by type, sent or unsent", description = "Keys have to delimited by coma")
	@GetMapping("/byHcParty/{hcPartyId}/mediumType/{sentMediumType}/invoiceType/{invoiceType}/sent/{sent}")
	fun listInvoicesByHcPartySentMediumTypeInvoiceTypeSentDate(
		@PathVariable hcPartyId: String,
		@PathVariable sentMediumType: MediumTypeDto,
		@PathVariable invoiceType: InvoiceTypeDto,
		@PathVariable sent: Boolean,
		@RequestParam(required = false) from: Long?,
		@RequestParam(required = false) to: Long?
	): Flux<InvoiceDto> {
		val invoices = invoiceService.listInvoicesByHcPartySentMediumTypeInvoiceTypeSentDate(
			hcPartyId,
			MediumType.valueOf(sentMediumType.name),
			InvoiceType.valueOf(invoiceType.name),
			sent,
			from,
			to
		)
		return invoices.map { el -> invoiceV2Mapper.map(el) }.injectReactorContext()
	}

	@Operation(summary = "Gets all invoices for author at date")
	@PostMapping("/byContacts")
	fun listInvoicesByContactIds(@RequestBody contactIds: ListOfIdsDto) = flow {
		emitAll(
			invoiceService.listInvoicesByHcPartyContacts(sessionLogic.getCurrentSessionContext().getHealthcarePartyId()!!, HashSet(contactIds.ids))
				.map { invoiceV2Mapper.map(it) }
		)
	}.injectReactorContext()

	@Operation(summary = "Gets all invoices for author at date")
	@GetMapping("/to/{recipientIds}")
	fun listInvoicesByRecipientsIds(@PathVariable recipientIds: String) = flow {
		emitAll(
			invoiceService.listInvoicesByHcPartyAndRecipientIds(sessionLogic.getCurrentSessionContext().getHealthcarePartyId()!!, recipientIds.split(',').toSet())
				.map { invoiceV2Mapper.map(it) }
		)
	}.injectReactorContext()

	@Operation(summary = "Gets all invoices for author at date")
	@GetMapping("/toInsurances")
	fun listToInsurances(@RequestParam(required = false) userIds: String?): Flux<InvoiceDto> =
		invoiceService.getInvoicesForUsersAndInsuranceIds(userIds?.split(','))
			.map(invoiceV2Mapper::map)
			.injectReactorContext()

	@Operation(summary = "Gets all invoices for author at date")
	@GetMapping("/toInsurances/unsent")
	fun listToInsurancesUnsent(@RequestParam(required = false) userIds: String?): Flux<InvoiceDto> =
		invoiceService.getUnsentInvoicesForUsersAndInsuranceIds(userIds?.split(','))
			.map(invoiceV2Mapper::map)
			.injectReactorContext()

	@Operation(summary = "Gets all invoices for author at date")
	@GetMapping("/toPatients")
	fun listToPatients(@RequestParam(required = false) hcPartyId: String?): Flux<InvoiceDto> = flow {
		emitAll(
			invoiceService.listInvoicesByHcPartyAndRecipientIds(
				hcPartyId
					?: sessionLogic.getCurrentSessionContext().getHealthcarePartyId()!!,
				setOf<String?>(null)
			).map { invoiceV2Mapper.map(it) }
		)
	}.injectReactorContext()

	@Operation(summary = "Gets all invoices for author at date")
	@GetMapping("/toPatients/unsent")
	fun listToPatientsUnsent(@RequestParam(required = false) hcPartyId: String?): Flux<InvoiceDto> = flow {
		emitAll(
			invoiceService.listInvoicesByHcPartyAndRecipientIdsUnsent(
				hcPartyId
					?: sessionLogic.getCurrentSessionContext().getHealthcarePartyId()!!,
				setOf<String?>(null)
			).map { invoiceV2Mapper.map(it) }
		)
	}.injectReactorContext()

	@Operation(summary = "Gets all invoices for author at date")
	@GetMapping("/byIds/{invoiceIds}")
	fun listInvoicesByIds(@PathVariable invoiceIds: String): Flux<InvoiceDto> {
		return invoiceService.getInvoices(invoiceIds.split(',')).map { invoiceV2Mapper.map(it) }.injectReactorContext()
	}

	@Operation(summary = "Get all invoices by author, by sending mode, by status and by date")
	@GetMapping("/byHcpartySendingModeStatusDate/{hcPartyId}")
	fun listInvoicesByHcpartySendingModeStatusDate(
		@PathVariable hcPartyId: String,
		@RequestParam(required = false) sendingMode: String?,
		@RequestParam(required = false) status: String?,
		@RequestParam(required = false) from: Long?,
		@RequestParam(required = false) to: Long?
	): Flux<InvoiceDto> {
		return invoiceService.listInvoicesByHcPartySendingModeStatus(hcPartyId, sendingMode, status, from, to).map { invoiceV2Mapper.map(it) }.injectReactorContext()
	}

	@Operation(summary = "Gets all invoices for author at date")
	@GetMapping("/byServiceIds/{serviceIds}")
	fun listInvoicesByServiceIds(@PathVariable serviceIds: String): Flux<InvoiceDto> {
		return invoiceService.listInvoicesByServiceIds(serviceIds.split(',').toSet()).map { invoiceV2Mapper.map(it) }.injectReactorContext()
	}

	@Operation(summary = "Gets all invoices per status")
	@PostMapping("/allHcpsByStatus/{status}")
	fun listAllHcpsByStatus(
		@PathVariable status: String,
		@RequestParam(required = false) from: Long?,
		@RequestParam(required = false) to: Long?,
		@RequestBody hcpIds: ListOfIdsDto
	): Flux<InvoiceDto> {
		return invoiceService.listInvoicesHcpsByStatus(status, from, to, hcpIds.ids).map { invoiceV2Mapper.map(it) }.injectReactorContext()
	}

	@Operation(summary = "Get the list of all used tarifications frequencies in invoices")
	@GetMapping("/codes/{minOccurrences}")
	fun getTarificationsCodesOccurrences(@PathVariable minOccurrences: Long) = mono {
		invoiceService.getTarificationsCodesOccurrences(sessionLogic.getCurrentSessionContext().getHealthcarePartyId()!!, minOccurrences).map { LabelledOccurenceDto(it.label, it.occurence) }
	}

	@Operation(summary = "Filter invoices for the current user (HcParty)", description = "Returns a list of invoices along with next start keys and Document ID. If the nextStartKey is Null it means that this is the last page.")
	@PostMapping("/filter")
	fun filterInvoicesBy(@RequestBody filterChain: FilterChain<InvoiceDto>): Flux<InvoiceDto> {
		val invoices = invoiceService.filter(filterChainV2Mapper.tryMap(filterChain).orThrow())
		return invoices.map { element -> invoiceV2Mapper.map(element) }.injectReactorContext()
	}

	@Operation(summary = "Modify a batch of invoices", description = "Returns the modified invoices.")
	@PutMapping("/batch")
	fun modifyInvoices(@RequestBody invoiceDtos: List<InvoiceDto>): Flux<InvoiceDto> =
		invoiceService.modifyInvoices(
			invoiceDtos.map(invoiceV2Mapper::map)
		).map(invoiceV2Mapper::map).injectReactorContext()

	@Operation(summary = "Create a batch of invoices", description = "Returns the created invoices.")
	@PostMapping("/batch")
	fun createInvoices(@RequestBody invoiceDtos: List<InvoiceDto>): Flux<InvoiceDto> =
		invoiceService.createInvoices(
			invoiceDtos.map(invoiceV2Mapper::map)
		).map(invoiceV2Mapper::map).injectReactorContext()

	@Operation(description = "Shares one or more patients with one or more data owners")
	@PutMapping("/bulkSharedMetadataUpdate")
	fun bulkShare(
		@RequestBody request: BulkShareOrUpdateMetadataParamsDto
	): Flux<EntityBulkShareResultDto<InvoiceDto>> = flow {
		emitAll(invoiceService.bulkShareOrUpdateMetadata(
			entityShareOrMetadataUpdateRequestV2Mapper.map(request)
		).map { bulkShareResultV2Mapper.map(it) })
	}.injectCachedReactorContext(reactorCacheInjector, 50)

	@Operation(description = "Shares one or more contacts with one or more data owners but does not return the updated entity.")
	@PutMapping("/bulkSharedMetadataUpdateMinimal")
	fun bulkShareMinimal(
		@RequestBody request: BulkShareOrUpdateMetadataParamsDto
	): Flux<EntityBulkShareResultDto<InvoiceDto>> = flow {
		emitAll(invoiceService.bulkShareOrUpdateMetadata(
			entityShareOrMetadataUpdateRequestV2Mapper.map(request)
		).map { bulkShareResultV2Mapper.map(it).copy(updatedEntity = null) })
	}.injectCachedReactorContext(reactorCacheInjector, 50)
}
