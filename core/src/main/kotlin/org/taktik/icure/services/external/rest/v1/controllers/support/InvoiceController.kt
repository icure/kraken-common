/*
 * Copyright (c) 2020. Taktik SA, All rights reserved.
 */

package org.taktik.icure.services.external.rest.v1.controllers.support

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.tags.Tag
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.reactor.mono
import org.springframework.context.annotation.Profile
import org.springframework.http.HttpStatus
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
import org.taktik.icure.config.SharedPaginationConfig
import org.taktik.icure.db.PaginationOffset
import org.taktik.icure.entities.embed.InvoiceType
import org.taktik.icure.entities.embed.MediumType
import org.taktik.icure.pagination.PaginatedFlux
import org.taktik.icure.pagination.asPaginatedFlux
import org.taktik.icure.pagination.mapElements
import org.taktik.icure.services.external.rest.v1.dto.IcureStubDto
import org.taktik.icure.services.external.rest.v1.dto.InvoiceDto
import org.taktik.icure.services.external.rest.v1.dto.ListOfIdsDto
import org.taktik.icure.services.external.rest.v1.dto.data.LabelledOccurenceDto
import org.taktik.icure.services.external.rest.v1.dto.embed.DelegationDto
import org.taktik.icure.services.external.rest.v1.dto.embed.InvoicingCodeDto
import org.taktik.icure.services.external.rest.v1.dto.filter.chain.FilterChain
import org.taktik.icure.services.external.rest.v1.mapper.InvoiceMapper
import org.taktik.icure.services.external.rest.v1.mapper.StubMapper
import org.taktik.icure.services.external.rest.v1.mapper.embed.DelegationMapper
import org.taktik.icure.services.external.rest.v1.mapper.embed.InvoicingCodeMapper
import org.taktik.icure.services.external.rest.v1.mapper.filter.FilterChainMapper
import org.taktik.icure.utils.JsonString
import org.taktik.icure.utils.injectReactorContext
import org.taktik.icure.utils.orThrow
import reactor.core.publisher.Flux

@RestController
@Profile("app")
@RequestMapping("/rest/v1/invoice")
@Tag(name = "invoice")
class InvoiceController(
    private val invoiceService: InvoiceService,
    private val sessionLogic: SessionInformationProvider,
    private val uuidGenerator: UUIDGenerator,
    private val invoiceMapper: InvoiceMapper,
    private val filterChainMapper: FilterChainMapper,
    private val delegationMapper: DelegationMapper,
    private val invoicingCodeMapper: InvoicingCodeMapper,
    private val stubMapper: StubMapper,
    private val objectMapper: ObjectMapper,
	private val paginationConfig: SharedPaginationConfig
) {

	@Operation(summary = "Creates an invoice")
	@PostMapping
	fun createInvoice(@RequestBody invoiceDto: InvoiceDto) = mono {
		val invoice = invoiceService.createInvoice(invoiceMapper.map(invoiceDto))
			?: throw ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Invoice creation failed")
		invoiceMapper.map(invoice)
	}

	@Operation(summary = "Deletes an invoice")
	@DeleteMapping("/{invoiceId}")
	fun deleteInvoice(@PathVariable invoiceId: String) = mono {
		invoiceService.deleteInvoice(invoiceId, null)
	}

	@Operation(summary = "Gets an invoice")
	@GetMapping("/{invoiceId}")
	fun getInvoice(@PathVariable invoiceId: String) = mono {
		val invoice = invoiceService.getInvoice(invoiceId)
			?: throw ResponseStatusException(HttpStatus.NOT_FOUND, "Invoice fetching failed")
		invoiceMapper.map(invoice)
	}

	@Operation(summary = "Gets an invoice")
	@PostMapping("/byIds")
	fun getInvoices(@RequestBody invoiceIds: ListOfIdsDto) = invoiceService.getInvoices(invoiceIds.ids)
		.map { invoiceMapper.map(it) }
		.injectReactorContext()

	@Operation(summary = "Modifies an invoice")
	@PutMapping
	fun modifyInvoice(@RequestBody invoiceDto: InvoiceDto) = mono {
		val invoice = invoiceService.modifyInvoice(invoiceMapper.map(invoiceDto))
			?: throw ResponseStatusException(HttpStatus.NOT_FOUND, "Invoice modification failed")

		invoiceMapper.map(invoice)
	}

	@Operation(summary = "Modifies an invoice")
	@PostMapping("/reassign")
	fun reassignInvoice(@RequestBody invoiceDto: InvoiceDto) = mono {
		val invoice = invoiceMapper.map(invoiceDto).let { it.reassign(it.invoicingCodes, uuidGenerator) }

		invoiceMapper.map(invoice)
	}

	@Operation(summary = "Adds a delegation to a invoice")
	@PutMapping("/{invoiceId}/delegate")
	fun newInvoiceDelegations(@PathVariable invoiceId: String, @RequestBody ds: List<DelegationDto>) = mono {
		val invoice = invoiceService.addDelegations(invoiceId, ds.map { delegationMapper.map(it) })
		if (invoice?.delegations != null && invoice.delegations.isNotEmpty()) {
			invoiceMapper.map(invoice)
		} else {
			throw ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "New delegation for invoice failed")
		}
	}

	@Operation(summary = "Gets all invoices for author at date")
	@PostMapping("/mergeTo/{invoiceId}")
	fun mergeTo(@PathVariable invoiceId: String, @RequestBody ids: ListOfIdsDto) = mono {
		invoiceMapper.map(
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
					?: throw AccessDeniedException("Current user is not a healthcare party"),
				it,
				scheme,
				forcedValue
			)?.let(invoiceMapper::map)
		}
	}

	@Operation(summary = "Append codes to new or existing invoice")
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
			invoicingCodes.map { ic -> invoicingCodeMapper.map(ic) }, invoiceId, gracePeriod
		)
		emitAll(invoices.map { invoiceMapper.map(it) })
	}.injectReactorContext()

	@Operation(summary = "removeCodes for linked serviceId")
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

			return invoices.map { invoiceMapper.map(it) }.injectReactorContext()
		}
	}

	@Operation(summary = "Gets all invoices for author at date")
	@GetMapping("/byauthor/{hcPartyId}")
	fun findByAuthor(
		@PathVariable hcPartyId: String,
		@RequestParam(required = false) fromDate: Long?,
		@RequestParam(required = false) toDate: Long?,
		@Parameter(description = "The start key for pagination: a JSON representation of an array containing all the necessary " + "components to form the Complex Key's startKey")
		@RequestParam(required = false) startKey: JsonString?,
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
			.mapElements(invoiceMapper::map)
			.asPaginatedFlux()
	}

	@Suppress("DEPRECATION")
	@Deprecated("This method cannot include results with secure delegations, use listInvoiceIdsByDataOwnerPatientInvoiceDate instead")
	@Operation(summary = "List invoices found by healthcare party and secret foreign patient keys.", description = "Keys have to delimited by comma")
	@GetMapping("/byHcPartySecretForeignKeys")
	fun findInvoicesByHCPartyPatientForeignKeys(@RequestParam hcPartyId: String, @RequestParam secretFKeys: String): Flux<InvoiceDto> {
		val secretPatientKeys = secretFKeys.split(',').map { it.trim() }.toSet()
		val elementList = invoiceService.listInvoicesByHcPartyAndPatientSfks(hcPartyId, secretPatientKeys)

		return elementList.map { element -> invoiceMapper.map(element) }.injectReactorContext()
	}

	@Suppress("DEPRECATION")
	@Deprecated("This method cannot include results with secure delegations, use listInvoiceIdsByDataOwnerPatientInvoiceDate instead")
	@Operation(summary = "List invoices found by healthcare party and secret foreign patient keys.")
	@PostMapping("/byHcPartySecretForeignKeys")
	fun findInvoicesByHCPartyPatientForeignKeys(@RequestParam hcPartyId: String, @RequestBody secretPatientKeys: List<String>): Flux<InvoiceDto> {
		val elementList = invoiceService.listInvoicesByHcPartyAndPatientSfks(hcPartyId, secretPatientKeys.toSet())

		return elementList.map { element -> invoiceMapper.map(element) }.injectReactorContext()
	}

	@Suppress("DEPRECATION")
	@Deprecated("This method cannot include results with secure delegations, use findInvoicesDelegationsStubsByIds instead")
	@Operation(summary = "List invoice stubs found by healthcare party and secret foreign keys.", description = "Keys must be delimited by comma")
	@GetMapping("/byHcPartySecretForeignKeys/delegations")
	fun findInvoicesDelegationsStubsByHCPartyPatientForeignKeys(
		@RequestParam hcPartyId: String,
		@RequestParam secretFKeys: String
	): Flux<IcureStubDto> {
		val secretPatientKeys = secretFKeys.split(',').map { it.trim() }.toSet()
		return invoiceService.listInvoicesByHcPartyAndPatientSfks(hcPartyId, secretPatientKeys).map { invoice -> stubMapper.mapToStub(invoice) }.injectReactorContext()
	}

	@Suppress("DEPRECATION")
	@Deprecated("This method cannot include results with secure delegations, use findInvoicesDelegationsStubsByIds instead")
	@Operation(summary = "List invoice stubs found by healthcare party and secret foreign keys.")
	@PostMapping("/byHcPartySecretForeignKeys/delegations")
	fun findInvoicesDelegationsStubsByHCPartyPatientForeignKeys(
		@RequestParam hcPartyId: String,
		@RequestBody secretPatientKeys: List<String>,
	): Flux<IcureStubDto> {
		return invoiceService.listInvoicesByHcPartyAndPatientSfks(hcPartyId, secretPatientKeys.toSet()).map { invoice -> stubMapper.mapToStub(invoice) }.injectReactorContext()
	}

	@Operation(summary = "List invoice stubs found by ids.")
	@PostMapping("/delegationsByIds")
	fun findInvoicesDelegationsStubsByIds(
		@RequestBody invoiceIds: ListOfIdsDto,
	): Flux<IcureStubDto> =invoiceService
		.getInvoices(invoiceIds.ids)
		.map { invoice -> stubMapper.mapToStub(invoice) }
		.injectReactorContext()

	@Operation(summary = "List invoices by groupId")
	@GetMapping("/byHcPartyGroupId/{hcPartyId}/{groupId}")
	fun listByHcPartyGroupId(@PathVariable hcPartyId: String, @PathVariable groupId: String): Flux<InvoiceDto> {
		val invoices = invoiceService.listInvoicesByHcPartyAndGroupId(hcPartyId, groupId)
		return invoices.map { el -> invoiceMapper.map(el) }.injectReactorContext()
	}

	@Operation(summary = "List invoices by type, sent or unsent")
	@GetMapping("/byHcParty/{hcPartyId}/mediumType/{sentMediumType}/invoiceType/{invoiceType}/sent/{sent}")
	fun listByHcPartySentMediumTypeInvoiceTypeSentDate(
		@PathVariable hcPartyId: String,
		@PathVariable sentMediumType: MediumType,
		@PathVariable invoiceType: InvoiceType,
		@PathVariable sent: Boolean,
		@RequestParam(required = false) from: Long?,
		@RequestParam(required = false) to: Long?
	): Flux<InvoiceDto> {
		val invoices = invoiceService.listInvoicesByHcPartySentMediumTypeInvoiceTypeSentDate(hcPartyId, sentMediumType, invoiceType, sent, from, to)
		return invoices.map { el -> invoiceMapper.map(el) }.injectReactorContext()
	}

	@Operation(summary = "Update delegations in healthElements.")
	@PostMapping("/delegations")
	fun setInvoicesDelegations(@RequestBody stubs: List<IcureStubDto>) = flow {
		val invoices = invoiceService.getInvoices(stubs.map { it.id }).map { invoice ->
			stubs.find { s -> s.id == invoice.id }?.let { stub ->
				invoice.copy(
					delegations = invoice.delegations.mapValues { (s, dels) -> stub.delegations[s]?.map { delegationMapper.map(it) }?.toSet() ?: dels } +
						stub.delegations.filterKeys { k -> !invoice.delegations.containsKey(k) }.mapValues { (_, value) -> value.map { delegationMapper.map(it) }.toSet() },
					encryptionKeys = invoice.encryptionKeys.mapValues { (s, dels) -> stub.encryptionKeys[s]?.map { delegationMapper.map(it) }?.toSet() ?: dels } +
						stub.encryptionKeys.filterKeys { k -> !invoice.encryptionKeys.containsKey(k) }.mapValues { (_, value) -> value.map { delegationMapper.map(it) }.toSet() },
					cryptedForeignKeys = invoice.cryptedForeignKeys.mapValues { (s, dels) -> stub.cryptedForeignKeys[s]?.map { delegationMapper.map(it) }?.toSet() ?: dels } +
						stub.cryptedForeignKeys.filterKeys { k -> !invoice.cryptedForeignKeys.containsKey(k) }.mapValues { (_, value) -> value.map { delegationMapper.map(it) }.toSet() },
				)
			} ?: invoice
		}.toList()
		emitAll(invoiceService.modifyInvoices(invoices).map { stubMapper.mapToStub(it) })
	}.injectReactorContext()

	@Operation(summary = "Gets all invoices by Contact ids")
	@PostMapping("/byCtcts")
	fun listByContactIds(@RequestBody contactIds: ListOfIdsDto) = flow {
		emitAll(
			invoiceService.listInvoicesByHcPartyContacts(sessionLogic.getCurrentSessionContext().getHealthcarePartyId()!!, HashSet(contactIds.ids))
				.map { invoiceMapper.map(it) }
		)
	}.injectReactorContext()

	@Operation(summary = "Gets all invoices by recipient ids")
	@GetMapping("/to/{recipientIds}")
	fun listByRecipientsIds(@PathVariable recipientIds: String) = flow {
		emitAll(
			invoiceService.listInvoicesByHcPartyAndRecipientIds(sessionLogic.getCurrentSessionContext().getHealthcarePartyId()!!, recipientIds.split(',').toSet())
				.map { invoiceMapper.map(it) }
		)
	}.injectReactorContext()

	@Operation(summary = "Gets all invoices for author at date")
	@GetMapping("/toInsurances")
	fun listToInsurances(@RequestParam(required = false) userIds: String?): Flux<InvoiceDto> =
		invoiceService.getInvoicesForUsersAndInsuranceIds(userIds?.split(','))
			.map(invoiceMapper::map)
			.injectReactorContext()

	@Operation(summary = "Gets all invoices for author at date")
	@GetMapping("/toInsurances/unsent")
	fun listToInsurancesUnsent(@RequestParam(required = false) userIds: String?): Flux<InvoiceDto> =
		invoiceService.getUnsentInvoicesForUsersAndInsuranceIds(userIds?.split(','))
			.map(invoiceMapper::map)
			.injectReactorContext()

	@Operation(summary = "Gets all invoices for author at date")
	@GetMapping("/toPatients")
	fun listToPatients(@RequestParam(required = false) hcPartyId: String?): Flux<InvoiceDto> = flow {
		emitAll(
			invoiceService.listInvoicesByHcPartyAndRecipientIds(
				hcPartyId
					?: sessionLogic.getCurrentSessionContext().getHealthcarePartyId()!!,
				setOf<String?>(null)
			).map { invoiceMapper.map(it) }
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
			).map { invoiceMapper.map(it) }
		)
	}.injectReactorContext()

	@Operation(summary = "Gets all invoices for author at date")
	@GetMapping("/byIds/{invoiceIds}")
	fun listByIds(@PathVariable invoiceIds: String): Flux<InvoiceDto> {
		return invoiceService.getInvoices(invoiceIds.split(',')).map { invoiceMapper.map(it) }.injectReactorContext()
	}

	@Operation(summary = "Get all invoices by author, by sending mode, by status and by date")
	@GetMapping("/byHcpartySendingModeStatusDate/{hcPartyId}")
	fun listByHcpartySendingModeStatusDate(
		@PathVariable hcPartyId: String,
		@RequestParam(required = false) sendingMode: String?,
		@RequestParam(required = false) status: String?,
		@RequestParam(required = false) from: Long?,
		@RequestParam(required = false) to: Long?
	): Flux<InvoiceDto> {
		return invoiceService.listInvoicesByHcPartySendingModeStatus(hcPartyId, sendingMode, status, from, to).map { invoiceMapper.map(it) }.injectReactorContext()
	}

	@Operation(summary = "Gets all invoices for author at date")
	@GetMapping("/byServiceIds/{serviceIds}")
	fun listByServiceIds(@PathVariable serviceIds: String): Flux<InvoiceDto> {
		return invoiceService.listInvoicesByServiceIds(serviceIds.split(',').toSet()).map { invoiceMapper.map(it) }.injectReactorContext()
	}

	@Operation(summary = "Gets all invoices per status")
	@PostMapping("/allHcpsByStatus/{status}")
	fun listAllHcpsByStatus(
		@PathVariable status: String,
		@RequestParam(required = false) from: Long?,
		@RequestParam(required = false) to: Long?,
		@RequestBody hcpIds: ListOfIdsDto
	): Flux<InvoiceDto> {
		return invoiceService.listInvoicesHcpsByStatus(status, from, to, hcpIds.ids).map { invoiceMapper.map(it) }.injectReactorContext()
	}

	@Operation(summary = "Get the list of all used tarifications frequencies in invoices")
	@GetMapping("/codes/{minOccurences}")
	fun getTarificationsCodesOccurences(@PathVariable minOccurences: Long) = mono {
		invoiceService.getTarificationsCodesOccurrences(sessionLogic.getCurrentSessionContext().getHealthcarePartyId()!!, minOccurences).map { LabelledOccurenceDto(it.label, it.occurence) }
	}

	@Operation(summary = "Filter invoices for the current user (HcParty)", description = "Returns a list of invoices along with next start keys and Document ID. If the nextStartKey is Null it means that this is the last page.")
	@PostMapping("/filter")
	fun filterInvoicesBy(@RequestBody filterChain: FilterChain<InvoiceDto>): Flux<InvoiceDto> {
		val invoices = invoiceService.filter(filterChainMapper.tryMap(filterChain).orThrow())
		return invoices.map { element -> invoiceMapper.map(element) }.injectReactorContext()
	}

	@Operation(summary = "Modify a batch of invoices", description = "Returns the modified invoices.")
	@PutMapping("/batch")
	fun modifyInvoices(@RequestBody invoiceDtos: List<InvoiceDto>): Flux<InvoiceDto> =
		invoiceService.modifyInvoices(
			invoiceDtos.map(invoiceMapper::map)
		).map(invoiceMapper::map).injectReactorContext()


	@Operation(summary = "Create a batch of invoices", description = "Returns the created invoices.")
	@PostMapping("/batch")
	fun createInvoices(@RequestBody invoiceDtos: List<InvoiceDto>): Flux<InvoiceDto> =
		invoiceService.createInvoices(
			invoiceDtos.map(invoiceMapper::map)
		).map(invoiceMapper::map).injectReactorContext()
}