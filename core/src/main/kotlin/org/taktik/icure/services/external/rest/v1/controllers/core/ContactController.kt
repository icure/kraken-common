/*
 * Copyright (c) 2020. Taktik SA, All rights reserved.
 */

package org.taktik.icure.services.external.rest.v1.controllers.core

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.tags.Tag
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.reactor.mono
import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Profile
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType.APPLICATION_JSON_VALUE
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
import org.taktik.couchdb.entity.IdAndRev
import org.taktik.couchdb.exception.DocumentNotFoundException
import org.taktik.icure.asynclogic.SessionInformationProvider
import org.taktik.icure.asyncservice.ContactService
import org.taktik.icure.config.SharedPaginationConfig
import org.taktik.icure.db.PaginationOffset
import org.taktik.icure.exceptions.MissingRequirementsException
import org.taktik.icure.pagination.PaginatedFlux
import org.taktik.icure.pagination.asPaginatedFlux
import org.taktik.icure.pagination.mapElements
import org.taktik.icure.services.external.rest.v1.dto.ContactDto
import org.taktik.icure.services.external.rest.v1.dto.IcureStubDto
import org.taktik.icure.services.external.rest.v1.dto.ListOfIdsDto
import org.taktik.icure.services.external.rest.v1.dto.PaginatedDocumentKeyIdPair
import org.taktik.icure.services.external.rest.v1.dto.couchdb.DocIdentifierDto
import org.taktik.icure.services.external.rest.v1.dto.data.LabelledOccurenceDto
import org.taktik.icure.services.external.rest.v1.dto.embed.ContentDto
import org.taktik.icure.services.external.rest.v1.dto.embed.DelegationDto
import org.taktik.icure.services.external.rest.v1.dto.embed.ServiceDto
import org.taktik.icure.services.external.rest.v1.dto.filter.AbstractFilterDto
import org.taktik.icure.services.external.rest.v1.dto.filter.chain.FilterChain
import org.taktik.icure.services.external.rest.v1.mapper.ContactMapper
import org.taktik.icure.services.external.rest.v1.mapper.StubMapper
import org.taktik.icure.services.external.rest.v1.mapper.couchdb.DocIdentifierMapper
import org.taktik.icure.services.external.rest.v1.mapper.embed.DelegationMapper
import org.taktik.icure.services.external.rest.v1.mapper.embed.ServiceMapper
import org.taktik.icure.services.external.rest.v1.mapper.filter.FilterChainMapper
import org.taktik.icure.services.external.rest.v1.mapper.filter.FilterMapper
import org.taktik.icure.services.external.rest.v1.utils.paginatedList
import org.taktik.icure.utils.FuzzyValues
import org.taktik.icure.utils.injectReactorContext
import org.taktik.icure.utils.orThrow
import org.taktik.icure.utils.warn
import reactor.core.publisher.Flux
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit
import java.util.*

@RestController
@Profile("app")
@RequestMapping("/rest/v1/contact")
@Tag(name = "contact")
class ContactController(
	private val contactService: ContactService,
	private val sessionLogic: SessionInformationProvider,
	private val contactMapper: ContactMapper,
	private val serviceMapper: ServiceMapper,
	private val delegationMapper: DelegationMapper,
	private val filterChainMapper: FilterChainMapper,
	private val filterMapper: FilterMapper,
	private val stubMapper: StubMapper,
	private val docIdentifierMapper: DocIdentifierMapper,
	private val paginationConfig: SharedPaginationConfig
) {

	companion object {
		private val log = LoggerFactory.getLogger(this::class.java)
	}

	@Operation(summary = "Get an empty content")
	@GetMapping("/service/content/empty")
	fun getEmptyContent() = ContentDto()

	@Operation(summary = "Create a contact with the current user", description = "Creates a contact with the current user and returns an instance of created contact afterward.")
	@PostMapping
	fun createContact(@RequestBody c: ContactDto) = mono {
		val contact = try {
			// handling services' indexes
			contactService.createContact(contactMapper.map(handleServiceIndexes(c)))
				?: throw ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Contact creation failed")
		} catch (e: MissingRequirementsException) {
			log.warn(e) { e.message }
			throw ResponseStatusException(HttpStatus.BAD_REQUEST, e.message)
		}
		contactMapper.map(contact)
	}

	protected fun handleServiceIndexes(c: ContactDto) = if (c.services.any { it.index == null }) {
		val maxIndex = c.services.maxByOrNull { it.index ?: 0 }?.index ?: 0
		c.copy(
			services = c.services.mapIndexed { idx, it ->
				if (it.index == null) {
					it.copy(
						index = idx + maxIndex
					)
				} else it
			}.toSet()
		)
	} else c

	@Operation(summary = "Get a contact", description = "Gets a contact based on its id")
	@GetMapping("/{contactId}")
	fun getContact(@PathVariable contactId: String) = mono {
		val contact = contactService.getContact(contactId)
			?: throw ResponseStatusException(HttpStatus.NOT_FOUND, "Getting Contact failed. Possible reasons: no such contact exists, or server error. Please try again or read the server log.")
		contactMapper.map(contact)
	}

	@Operation(summary = "Get contacts by batch", description = "Get a list of contact by ids/keys.")
	@PostMapping("/byIds")
	fun getContacts(@RequestBody contactIds: ListOfIdsDto): Flux<ContactDto> =
		contactService.getContacts(contactIds.ids).map { c -> contactMapper.map(c) }.injectReactorContext()

	@Operation(summary = "Get the list of all used codes frequencies in services")
	@GetMapping("/service/codes/{codeType}/{minOccurrences}")
	fun getServiceCodesOccurrences(
		@PathVariable codeType: String,
		@PathVariable minOccurrences: Long,
	) = mono {
		contactService.getServiceCodesOccurences(
			sessionLogic.getCurrentSessionContext().getHealthcarePartyId() ?: throw AccessDeniedException("Current user is not an HCP"),
			codeType,
			minOccurrences).map { LabelledOccurenceDto(it.label, it.occurence) }
	}

	@Operation(summary = "Get a list of contacts found by Healthcare Party and form's id.")
	@GetMapping("/byHcPartyFormId")
	fun findByHCPartyFormId(
		@RequestParam hcPartyId: String,
		@RequestParam formId: String
	): Flux<ContactDto> {
		val contactList = contactService.listContactsByHcPartyAndFormId(hcPartyId, formId)
		return contactList.map { contact -> contactMapper.map(contact) }.injectReactorContext()
	}

	@Operation(summary = "List contacts found By Healthcare Party and service Id.")
	@GetMapping("/byHcPartyServiceId")
	fun findByHCPartyServiceId(@RequestParam hcPartyId: String, @RequestParam serviceId: String): Flux<ContactDto> =
		contactService
			.listContactsByHcPartyServiceId(hcPartyId, serviceId)
			.map(contactMapper::map)
			.injectReactorContext()

	@Operation(summary = "List contacts found By externalId.")
	@PostMapping("/byExternalId")
	fun findContactsByExternalId(@RequestParam externalId: String): Flux<ContactDto> =
		contactService
			.listContactsByExternalId(externalId)
			.map(contactMapper::map)
			.injectReactorContext()

	@Operation(summary = "Get a list of contacts found by Healthcare Party and form's ids.")
	@PostMapping("/byHcPartyFormIds")
	fun findByHCPartyFormIds(@RequestParam hcPartyId: String, @RequestBody formIds: ListOfIdsDto): Flux<ContactDto> {
		if (formIds.ids.isEmpty()) {
			throw ResponseStatusException(HttpStatus.BAD_REQUEST, "A required query parameter was not specified for this request.")
		}
		val contactList = contactService.listContactsByHcPartyAndFormIds(hcPartyId, formIds.ids)

		return contactList.map { contact -> contactMapper.map(contact) }.injectReactorContext()
	}

	@Suppress("DEPRECATION")
	@Deprecated("This method cannot include results with secure delegations, use listContactIdsByDataOwnerPatientOpeningDate instead")
	@Operation(summary = "Get a list of contacts found by Healthcare Party and Patient foreign keys.")
	@PostMapping("/byHcPartyPatientForeignKeys")
	fun findContactsByHCPartyPatientForeignKeys(@RequestParam hcPartyId: String, @RequestBody patientForeignKeys: ListOfIdsDto): Flux<ContactDto> {
		if (patientForeignKeys.ids.isEmpty()) {
			throw ResponseStatusException(HttpStatus.BAD_REQUEST, "A required query parameter was not specified for this request.")
		}
		val contactList = contactService.listContactsByHCPartyAndPatient(hcPartyId, patientForeignKeys.ids)

		return contactList.map { contact -> contactMapper.map(contact) }.injectReactorContext()
	}

	@Suppress("DEPRECATION")
	@Deprecated("This method cannot include results with secure delegations, use listContactIdsByDataOwnerPatientOpeningDate instead")
	@Operation(summary = "Get a list of contacts found by healthcare party and secret foreign keys.", description = "Keys must be delimited by comma")
	@GetMapping("/byHcPartySecretForeignKeys")
	fun findByHCPartyPatientSecretFKeys(
		@RequestParam hcPartyId: String,
		@RequestParam secretFKeys: String,
		@RequestParam(required = false) planOfActionsIds: String?,
		@RequestParam(required = false) skipClosedContacts: Boolean?,
	): Flux<ContactDto> {
		val secretPatientKeys = secretFKeys.split(',').map { it.trim() }
		val contactList = contactService.listContactsByHCPartyAndPatient(hcPartyId, secretPatientKeys)

		return if (planOfActionsIds != null) {
			val poaids = planOfActionsIds.split(',')
			contactList.filter { c -> (skipClosedContacts == null || !skipClosedContacts || c.closingDate == null) && !Collections.disjoint(c.subContacts.map { it.planOfActionId }, poaids) }.map { contact -> contactMapper.map(contact) }.injectReactorContext()
		} else {
			contactList.filter { c -> skipClosedContacts == null || !skipClosedContacts || c.closingDate == null }.map { contact -> contactMapper.map(contact) }.injectReactorContext()
		}
	}

	@Suppress("DEPRECATION")
	@Deprecated("This method cannot include results with secure delegations, use findContactsDelegationsStubsByIds instead")
	@Operation(summary = "List contacts found by healthcare party and secret foreign keys.")
	@GetMapping("/byHcPartySecretForeignKeys/delegations")
	fun findContactsDelegationsStubsByHCPartyPatientForeignKeys(
		@RequestParam hcPartyId: String,
		@RequestParam secretFKeys: String,
	): Flux<IcureStubDto> {
		val secretPatientKeys = secretFKeys.split(',').map { it.trim() }
		return contactService.listContactsByHCPartyAndPatient(hcPartyId, secretPatientKeys).map { contact -> stubMapper.mapToStub(contact) }.injectReactorContext()
	}

	@Suppress("DEPRECATION")
	@Deprecated("This method cannot include results with secure delegations, use listContactIdsByDataOwnerPatientOpeningDate instead")
	@Operation(summary = "Get a list of contacts found by healthcare party and secret foreign keys.", description = "Keys must be delimited by comma.")
	@PostMapping("/byHcPartySecretForeignKeys")
	fun findByHCPartyPatientSecretFKeys(
		@RequestParam hcPartyId: String,
		@RequestBody secretPatientKeys: List<String>,
		@RequestParam(required = false) planOfActionsIds: String?,
		@RequestParam(required = false) skipClosedContacts: Boolean?,
	): Flux<ContactDto> {
		val contactList = contactService.listContactsByHCPartyAndPatient(hcPartyId, secretPatientKeys)

		return if (planOfActionsIds != null) {
			val poaids = planOfActionsIds.split(',')
			contactList.filter { c -> (skipClosedContacts == null || !skipClosedContacts || c.closingDate == null) && !Collections.disjoint(c.subContacts.map { it.planOfActionId }, poaids) }.map { contact -> contactMapper.map(contact) }.injectReactorContext()
		} else {
			contactList.filter { c -> skipClosedContacts == null || !skipClosedContacts || c.closingDate == null }.map { contact -> contactMapper.map(contact) }.injectReactorContext()
		}
	}

	@Suppress("DEPRECATION")
	@Deprecated("This method cannot include results with secure delegations, use findContactsDelegationsStubsByIds instead")
	@Operation(summary = "List contacts found By Healthcare Party and secret foreign keys.")
	@PostMapping("/byHcPartySecretForeignKeys/delegations")
	fun findContactsDelegationsStubsByHCPartyPatientForeignKeys(
		@RequestParam hcPartyId: String,
		@RequestBody secretPatientKeys: List<String>,
	): Flux<IcureStubDto> {
		return contactService.listContactsByHCPartyAndPatient(hcPartyId, secretPatientKeys).map { contact -> stubMapper.mapToStub(contact) }.injectReactorContext()
	}

	@Operation(summary = "List contact stubs by ids.")
	@PostMapping("/delegations")
	fun findContactsDelegationsStubsByIds(
		@RequestBody contactIds: ListOfIdsDto,
	): Flux<IcureStubDto> = contactService
		.getContacts(contactIds.ids)
		.map { contact -> stubMapper.mapToStub(contact) }
		.injectReactorContext()


	@Operation(summary = "Update delegations in healthElements.", description = "Keys must be delimited by coma")
	@PostMapping("/delegations")
	fun setContactsDelegations(@RequestBody stubs: List<IcureStubDto>) = flow {
		val contacts = contactService.getContacts(stubs.map { it.id }).map { contact ->
			stubs.find { s -> s.id == contact.id }?.let { stub ->
				contact.copy(
					delegations = contact.delegations.mapValues { (s, dels) -> stub.delegations[s]?.map { delegationMapper.map(it) }?.toSet() ?: dels } +
						stub.delegations.filterKeys { k -> !contact.delegations.containsKey(k) }.mapValues { (_, value) -> value.map { delegationMapper.map(it) }.toSet() },
					encryptionKeys = contact.encryptionKeys.mapValues { (s, dels) -> stub.encryptionKeys[s]?.map { delegationMapper.map(it) }?.toSet() ?: dels } +
						stub.encryptionKeys.filterKeys { k -> !contact.encryptionKeys.containsKey(k) }.mapValues { (_, value) -> value.map { delegationMapper.map(it) }.toSet() },
					cryptedForeignKeys = contact.cryptedForeignKeys.mapValues { (s, dels) -> stub.cryptedForeignKeys[s]?.map { delegationMapper.map(it) }?.toSet() ?: dels } +
						stub.cryptedForeignKeys.filterKeys { k -> !contact.cryptedForeignKeys.containsKey(k) }.mapValues { (_, value) -> value.map { delegationMapper.map(it) }.toSet() },
				)
			} ?: contact
		}
		emitAll(contactService.modifyContacts(contacts.toList()).map { contactMapper.map(it) })
	}.injectReactorContext()

	@Suppress("DEPRECATION")
	@Deprecated("This method cannot include results with secure delegations, use listContactIdsByDataOwnerPatientOpeningDate instead")
	@Operation(summary = "Close contacts for healthcare party and secret foreign keys.", description = "Keys must be delimited by comma")
	@PutMapping("/byHcPartySecretForeignKeys/close")
	fun closeForHCPartyPatientForeignKeys(
		@RequestParam hcPartyId: String,
		@RequestParam secretFKeys: String,
	): Flux<ContactDto> {
		val secretPatientKeys = secretFKeys.split(',').map { it.trim() }
		val contactFlow = contactService.listContactsByHCPartyAndPatient(hcPartyId, secretPatientKeys)

		val savedOrFailed = contactFlow.mapNotNull { c ->
			if (c.closingDate == null) {
				contactService.modifyContact(c.copy(closingDate = FuzzyValues.getFuzzyDateTime(LocalDateTime.now(), ChronoUnit.SECONDS)))
			} else {
				null
			}
		}

		return savedOrFailed.map { contact -> contactMapper.map(contact) }.injectReactorContext()
	}

	@Operation(summary = "Delete contacts.", description = "Response is a set containing the ID's of deleted contacts.")
	@DeleteMapping("/{contactIds}")
	fun deleteContacts(@PathVariable contactIds: String): Flux<DocIdentifierDto> {
		if (contactIds.isBlank()) {
			throw ResponseStatusException(HttpStatus.BAD_REQUEST, "A required query parameter was not specified for this request.")
		}
		// TODO versioning?
		return contactService.deleteContacts(contactIds.split(',').toSet().map { IdAndRev(it, null) })
			.map(docIdentifierMapper::map)
			.injectReactorContext()
	}

	@Operation(summary = "Modify a contact", description = "Returns the modified contact.")
	@PutMapping
	fun modifyContact(@RequestBody contactDto: ContactDto) = mono {
		handleServiceIndexes(contactDto)

		contactService.modifyContact(contactMapper.map(contactDto))?.let {
			contactMapper.map(it)
		} ?: throw DocumentNotFoundException("Contact modification failed.")
	}

	@Operation(summary = "Modify a batch of contacts", description = "Returns the modified contacts.")
	@PutMapping("/batch")
	fun modifyContacts(@RequestBody contactDtos: List<ContactDto>): Flux<ContactDto> = flow {
		val contacts = contactService.modifyContacts(contactDtos.map { c -> handleServiceIndexes(c) }.map { f -> contactMapper.map(f) })
		emitAll(contacts.map { f -> contactMapper.map(f) })
	}.injectReactorContext()

	@Operation(summary = "Create a batch of contacts", description = "Returns the modified contacts.")
	@PostMapping("/batch")
	fun createContacts(@RequestBody contactDtos: List<ContactDto>): Flux<ContactDto> = flow {
		val contacts = contactService.createContacts(contactDtos.map { c -> handleServiceIndexes(c) }.map { f -> contactMapper.map(f) })
		emitAll(contacts.map { f -> contactMapper.map(f) })
	}.injectReactorContext()

	@Operation(summary = "Delegates a contact to a healthcare party", description = "It delegates a contact to a healthcare party (By current healthcare party). Returns the contact with new delegations.")
	@PostMapping("/{contactId}/delegate")
	fun newContactDelegations(@PathVariable contactId: String, @RequestBody d: DelegationDto) = mono {
		contactService.addDelegation(contactId, delegationMapper.map(d))
		val contactWithDelegation = contactService.getContact(contactId)

		// TODO: write more function to add complete access to all contacts of a patient to a HcParty, or a contact or a subset of contacts
		// TODO: kind of adding new secretForeignKeys and cryptedForeignKeys

		val succeed = contactWithDelegation?.delegations != null && contactWithDelegation.delegations.isNotEmpty()
		if (succeed) {
			contactWithDelegation?.let { contactMapper.map(it) }
		} else {
			throw ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Delegation creation for Contact failed.")
		}
	}

	@Operation(summary = "List contacts for the current user (HcParty) or the given hcparty in the filter ", description = "Returns a list of contacts along with next start keys and Document ID. If the nextStartKey is Null it means that this is the last page.")
	@PostMapping("/filter")
	fun filterContactsBy(
		@Parameter(description = "A Contact document ID") @RequestParam(required = false) startDocumentId: String?,
		@Parameter(description = "Number of rows") @RequestParam(required = false) limit: Int?,
		@RequestBody filterChain: FilterChain<ContactDto>,
	) = mono {

		val realLimit = limit ?: paginationConfig.defaultLimit

		val paginationOffset = PaginationOffset(null, startDocumentId, null, realLimit + 1)

		val contacts = contactService.filterContacts(paginationOffset, filterChainMapper.tryMap(filterChain).orThrow())

		contacts.paginatedList(contactMapper::map, realLimit)
	}

	@Operation(summary = "Get the ids of the Contacts matching the provided filter.")
	@PostMapping("/match", produces = [APPLICATION_JSON_VALUE])
	fun matchContactsBy(
		@RequestBody filter: AbstractFilterDto<ContactDto>
	) =
		contactService.matchContactsBy(
			filter = filterMapper.tryMap(filter).orThrow()
		).injectReactorContext()

	@Operation(summary = "Get a service by id")
	@GetMapping("/service/{serviceId}")
	fun getService(
		@Parameter(description = "The id of the service to retrieve") @PathVariable serviceId: String
	) = mono {
		contactService.getService(serviceId)?.let { serviceMapper.map(it) }
			?: throw DocumentNotFoundException("Service with id $serviceId not found.")
	}

	@Operation(summary = "List services for the current user (HcParty) or the given hcparty in the filter ", description = "Returns a list of contacts along with next start keys and Document ID. If the nextStartKey is Null it means that this is the last page.")
	@PostMapping("/service/filter")
	fun filterServicesBy(
		@Parameter(description = "A Contact document ID") @RequestParam(required = false) startDocumentId: String?,
		@Parameter(description = "Number of rows") @RequestParam(required = false) limit: Int?,
		@RequestBody filterChain: FilterChain<ServiceDto>,
	) = mono {

		val realLimit = limit ?: paginationConfig.defaultLimit

		val paginationOffset = PaginationOffset(null, startDocumentId, null, realLimit + 1)

		val services: List<ServiceDto> = contactService.filterServices(paginationOffset, filterChainMapper.tryMap(filterChain).orThrow()).map { serviceMapper.map(it) }.toList()

		val totalSize = services.size

		if (services.size <= realLimit) {
			org.taktik.icure.services.external.rest.v1.dto.PaginatedList(services.size, totalSize, services, null)
		} else {
			val nextKeyPair = services.lastOrNull()?.let { PaginatedDocumentKeyIdPair(null, it.id) }
			val rows = services.subList(0, services.size - 1)
			org.taktik.icure.services.external.rest.v1.dto.PaginatedList(realLimit, totalSize, rows, nextKeyPair)
		}
	}

	@Operation(summary = "Get the ids of the Services matching the provided filter.")
	@PostMapping("/service/match", produces = [APPLICATION_JSON_VALUE])
	fun matchServicesBy(
		@RequestBody filter: AbstractFilterDto<ServiceDto>
	) = contactService.matchServicesBy(
			filter = filterMapper.tryMap(filter).orThrow()
		).injectReactorContext()

	@Operation(summary = "List services with provided ids ", description = "Returns a list of services")
	@PostMapping("/service/byIds")
	fun listServices(@RequestBody ids: ListOfIdsDto) = contactService.getServices(ids.ids).map { svc -> serviceMapper.map(svc) }.injectReactorContext()

	@Operation(summary = "List services linked to provided ids ", description = "Returns a list of services")
	@PostMapping("/service/linkedTo")
	fun listServicesLinkedTo(
		@Parameter(description = "The type of the link") @RequestParam(required = false) linkType: String?,
		@RequestBody ids: ListOfIdsDto,
	) = contactService.getServicesLinkedTo(ids.ids, linkType).map { svc -> serviceMapper.map(svc) }.injectReactorContext()

	@Operation(summary = "List services by related association id", description = "Returns a list of services")
	@GetMapping("/service/associationId")
	fun listServicesByAssociationId(
		@RequestParam associationId: String,
	) = contactService.listServicesByAssociationId(associationId).map { svc -> serviceMapper.map(svc) }.injectReactorContext()

	@Operation(summary = "List services linked to a health element", description = "Returns the list of services linked to the provided health element id")
	@GetMapping("/service/healthElementId/{healthElementId}")
	fun listServicesByHealthElementId(
		@PathVariable healthElementId: String,
		@Parameter(description = "hcPartyId", required = true) @RequestParam hcPartyId: String
	) = contactService.listServicesByHcPartyAndHealthElementIds(hcPartyId, listOf(healthElementId)).map { svc -> serviceMapper.map(svc) }.injectReactorContext()

	@Operation(summary = "List contacts by opening date parties with(out) pagination", description = "Returns a list of contacts.")
	@GetMapping("/byOpeningDate")
	fun listContactsByOpeningDate(
		@Parameter(description = "The contact openingDate", required = true) @RequestParam startKey: Long,
		@Parameter(description = "The contact max openingDate", required = true) @RequestParam endKey: Long,
		@Parameter(description = "hcPartyId", required = true) @RequestParam hcpartyid: String,
		@Parameter(description = "A contact party document ID") @RequestParam(required = false) startDocumentId: String?,
		@Parameter(description = "Number of rows") @RequestParam(required = false) limit: Int?,
	): PaginatedFlux<ContactDto> {
		val paginationOffset = PaginationOffset<ComplexKey>(null, startDocumentId, null, limit ?: paginationConfig.defaultLimit)
		return contactService
			.listContactsByOpeningDate(hcpartyid, startKey, endKey, paginationOffset)
			.mapElements(contactMapper::map)
			.asPaginatedFlux()
	}

}