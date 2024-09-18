/*
 * Copyright (c) 2020. Taktik SA, All rights reserved.
 */

package org.taktik.icure.services.external.rest.v2.controllers.core

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
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
import org.taktik.icure.cache.ReactorCacheInjector
import org.taktik.icure.config.SharedPaginationConfig
import org.taktik.icure.db.PaginationOffset
import org.taktik.icure.exceptions.MissingRequirementsException
import org.taktik.icure.pagination.PaginatedFlux
import org.taktik.icure.pagination.asPaginatedFlux
import org.taktik.icure.pagination.mapElements
import org.taktik.icure.services.external.rest.v2.dto.ContactDto
import org.taktik.icure.services.external.rest.v2.dto.IcureStubDto
import org.taktik.icure.services.external.rest.v2.dto.ListOfIdsAndRevDto
import org.taktik.icure.services.external.rest.v2.dto.ListOfIdsDto
import org.taktik.icure.services.external.rest.v2.dto.PaginatedDocumentKeyIdPair
import org.taktik.icure.services.external.rest.v2.dto.PaginatedList
import org.taktik.icure.services.external.rest.v2.dto.couchdb.DocIdentifierDto
import org.taktik.icure.services.external.rest.v2.dto.data.LabelledOccurenceDto
import org.taktik.icure.services.external.rest.v2.dto.embed.ContentDto
import org.taktik.icure.services.external.rest.v2.dto.embed.ServiceDto
import org.taktik.icure.services.external.rest.v2.dto.filter.AbstractFilterDto
import org.taktik.icure.services.external.rest.v2.dto.filter.chain.FilterChain
import org.taktik.icure.services.external.rest.v2.dto.requests.BulkShareOrUpdateMetadataParamsDto
import org.taktik.icure.services.external.rest.v2.dto.requests.EntityBulkShareResultDto
import org.taktik.icure.services.external.rest.v2.mapper.ContactV2Mapper
import org.taktik.icure.services.external.rest.v2.mapper.IdWithRevV2Mapper
import org.taktik.icure.services.external.rest.v2.mapper.StubV2Mapper
import org.taktik.icure.services.external.rest.v2.mapper.couchdb.DocIdentifierV2Mapper
import org.taktik.icure.services.external.rest.v2.mapper.embed.ServiceV2Mapper
import org.taktik.icure.services.external.rest.v2.mapper.filter.FilterChainV2Mapper
import org.taktik.icure.services.external.rest.v2.mapper.filter.FilterV2Mapper
import org.taktik.icure.services.external.rest.v2.mapper.requests.ContactBulkShareResultV2Mapper
import org.taktik.icure.services.external.rest.v2.mapper.requests.EntityShareOrMetadataUpdateRequestV2Mapper
import org.taktik.icure.services.external.rest.v2.utils.paginatedList
import org.taktik.icure.utils.FuzzyValues
import org.taktik.icure.utils.JsonString
import org.taktik.icure.utils.injectCachedReactorContext
import org.taktik.icure.utils.injectReactorContext
import org.taktik.icure.utils.orThrow
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit
import java.util.*

@RestController("contactControllerV2")
@Profile("app")
@RequestMapping("/rest/v2/contact")
@Tag(name = "contact")
class ContactController(
	private val contactService: ContactService,
	private val sessionLogic: SessionInformationProvider,
	private val contactV2Mapper: ContactV2Mapper,
	private val serviceV2Mapper: ServiceV2Mapper,
	private val filterChainV2Mapper: FilterChainV2Mapper,
	private val filterV2Mapper: FilterV2Mapper,
	private val stubV2Mapper: StubV2Mapper,
	private val docIdentifierV2Mapper: DocIdentifierV2Mapper,
	private val bulkShareResultV2Mapper: ContactBulkShareResultV2Mapper,
	private val entityShareOrMetadataUpdateRequestV2Mapper: EntityShareOrMetadataUpdateRequestV2Mapper,
	private val reactorCacheInjector: ReactorCacheInjector,
	private val paginationConfig: SharedPaginationConfig,
	private val idWithRevV2Mapper: IdWithRevV2Mapper,
	private val objectMapper: ObjectMapper
) {

	companion object {
		private val logger = LoggerFactory.getLogger(this::class.java)
	}

	@Operation(summary = "Get an empty content")
	@GetMapping("/service/content/empty")
	fun getEmptyContent() = ContentDto()

	@Operation(summary = "Create a contact with the current user", description = "Returns an instance of created contact.")
	@PostMapping
	fun createContact(@RequestBody c: ContactDto) = mono {
		val contact = try {
			// handling services' indexes
			contactService.createContact(contactV2Mapper.map(handleServiceIndexes(c)))
				?: throw ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Contact creation failed")
		} catch (e: MissingRequirementsException) {
			logger.warn(e.message, e)
			throw ResponseStatusException(HttpStatus.BAD_REQUEST, e.message)
		}
		contactV2Mapper.map(contact)
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

	@Operation(summary = "Get a contact")
	@GetMapping("/{contactId}")
	fun getContact(@PathVariable contactId: String) = mono {
		val contact = contactService.getContact(contactId)
			?: throw ResponseStatusException(HttpStatus.NOT_FOUND, "Getting Contact failed. Possible reasons: no such contact exists, or server error. Please try again or read the server logger.")
		contactV2Mapper.map(contact)
	}

	@Operation(summary = "Get contacts")
	@PostMapping("/byIds")
	fun getContacts(@RequestBody contactIds: ListOfIdsDto): Flux<ContactDto> {
		require(contactIds.ids.isNotEmpty()) {
			"You must specify at least one id."
		}
		return contactService
			.getContacts(contactIds.ids)
			.map(contactV2Mapper::map)
			.injectReactorContext()
	}

	@Operation(summary = "Get the list of all used codes frequencies in services")
	@GetMapping("/service/codes/{codeType}/{minOccurrences}")
	fun getServiceCodesOccurrences(
		@PathVariable codeType: String,
		@PathVariable minOccurrences: Long
	) = mono {
		contactService.getServiceCodesOccurences(
			sessionLogic.getCurrentSessionContext().getHealthcarePartyId()
					?: throw AccessDeniedException("Current user is not a healthcare party"),
			codeType,
			minOccurrences).map { LabelledOccurenceDto(it.label, it.occurence) }
	}

	@Operation(summary = "List contacts found By Healthcare Party and service Id.")
	@GetMapping("/byHcPartyServiceId")
	fun listContactByHCPartyServiceId(@RequestParam hcPartyId: String, @RequestParam serviceId: String) =
		contactService
			.listContactsByHcPartyServiceId(hcPartyId, serviceId)
			.map(contactV2Mapper::map)
			.injectReactorContext()

	@Operation(summary = "List contacts found By externalId.")
	@PostMapping("/byExternalId")
	fun listContactsByExternalId(@RequestParam externalId: String): Flux<ContactDto> =
		contactService
			.listContactsByExternalId(externalId)
			.map(contactV2Mapper::map)
			.injectReactorContext()

	@Operation(summary = "List contacts found By Healthcare Party and form Id.")
	@GetMapping("/byHcPartyFormId")
	fun listContactsByHCPartyAndFormId(@RequestParam hcPartyId: String, @RequestParam formId: String): Flux<ContactDto> {
		val contactList = contactService.listContactsByHcPartyAndFormId(hcPartyId, formId)
		return contactList.map { contact -> contactV2Mapper.map(contact) }.injectReactorContext()
	}

	@Operation(summary = "List contacts found By Healthcare Party and form Id.")
	@PostMapping("/byHcPartyFormIds")
	fun listContactsByHCPartyAndFormIds(@RequestParam hcPartyId: String, @RequestBody formIds: ListOfIdsDto): Flux<ContactDto> {
		if (formIds.ids.isEmpty()) {
			throw ResponseStatusException(HttpStatus.BAD_REQUEST, "A required query parameter was not specified for this request.")
		}
		val contactList = contactService.listContactsByHcPartyAndFormIds(hcPartyId, formIds.ids)

		return contactList.map { contact -> contactV2Mapper.map(contact) }.injectReactorContext()
	}

	@Operation(summary = "List contacts found By Healthcare Party and Patient foreign keys.")
	@PostMapping("/byHcPartyPatientForeignKeys")
	fun listContactsByHCPartyAndPatientForeignKeys(@RequestParam hcPartyId: String, @RequestBody patientForeignKeys: ListOfIdsDto): Flux<ContactDto> {
		if (patientForeignKeys.ids.isEmpty()) {
			throw ResponseStatusException(HttpStatus.BAD_REQUEST, "A required query parameter was not specified for this request.")
		}
		val contactList = contactService.listContactsByHCPartyAndPatient(hcPartyId, patientForeignKeys.ids)

		return contactList.map { contact -> contactV2Mapper.map(contact) }.injectReactorContext()
	}

	@Operation(summary = "Find Contact ids by data owner id, patient secret keys and opening date.")
	@PostMapping("/byDataOwnerPatientOpeningDate", produces = [APPLICATION_JSON_VALUE])
	fun listContactIdsByDataOwnerPatientOpeningDate(
		@RequestParam dataOwnerId: String,
		@RequestParam(required = false) startDate: Long?,
		@RequestParam(required = false) endDate: Long?,
		@RequestParam(required = false) descending: Boolean?,
		@RequestBody secretPatientKeys: ListOfIdsDto
	): Flux<String> {
		require(secretPatientKeys.ids.isNotEmpty()) {
			"You need to provide at least one secret patient key"
		}
		return contactService
			.listContactIdsByDataOwnerPatientOpeningDate(
				dataOwnerId = dataOwnerId,
				secretForeignKeys = secretPatientKeys.ids.toSet(),
				startDate = startDate?.let { FuzzyValues.getFuzzyDateTime(it) },
				endDate = endDate?.let { FuzzyValues.getFuzzyDateTime(it) },
				descending = descending ?: false)
			.injectReactorContext()
	}

	@Operation(summary = "List contacts found By Healthcare Party and secret foreign keys.", description = "Keys must be delimited by coma")
	@GetMapping("/byHcPartySecretForeignKeys")
	fun listContactsByHCPartyAndPatientSecretFKeys(
		@RequestParam hcPartyId: String,
		@RequestParam secretFKeys: String,
		@RequestParam(required = false) planOfActionsIds: String?,
		@RequestParam(required = false) skipClosedContacts: Boolean?
	): Flux<ContactDto> {
		val secretPatientKeys = secretFKeys.split(',').map { it.trim() }
		val contactList = contactService.listContactsByHCPartyAndPatient(hcPartyId, secretPatientKeys)

		return if (planOfActionsIds != null) {
			val poaids = planOfActionsIds.split(',')
			contactList.filter { c -> (skipClosedContacts == null || !skipClosedContacts || c.closingDate == null) && !Collections.disjoint(c.subContacts.map { it.planOfActionId }, poaids) }.map { contact -> contactV2Mapper.map(contact) }.injectReactorContext()
		} else {
			contactList.filter { c -> skipClosedContacts == null || !skipClosedContacts || c.closingDate == null }.map { contact -> contactV2Mapper.map(contact) }.injectReactorContext()
		}
	}

	@Operation(summary = "List contacts found By Healthcare Party and secret foreign keys.", description = "Keys must be delimited by coma")
	@PostMapping("/byHcPartySecretForeignKeys")
	fun listContactsByHCPartyAndPatientSecretFKeys(
		@RequestParam hcPartyId: String,
		@RequestBody secretPatientKeys: List<String>,
		@RequestParam(required = false) planOfActionsIds: String?,
		@RequestParam(required = false) skipClosedContacts: Boolean?
	): Flux<ContactDto> {
		val contactList = contactService.listContactsByHCPartyAndPatient(hcPartyId, secretPatientKeys)

		return if (planOfActionsIds != null) {
			val poaids = planOfActionsIds.split(',')
			contactList.filter { c -> (skipClosedContacts == null || !skipClosedContacts || c.closingDate == null) && !Collections.disjoint(c.subContacts.map { it.planOfActionId }, poaids) }.map { contact -> contactV2Mapper.map(contact) }.injectReactorContext()
		} else {
			contactList.filter { c -> skipClosedContacts == null || !skipClosedContacts || c.closingDate == null }.map { contact -> contactV2Mapper.map(contact) }.injectReactorContext()
		}
	}

	@Operation(summary = "List contacts found By Healthcare Party and secret foreign keys.", description = "Keys must be delimited by coma")
	@GetMapping("/byHcPartySecretForeignKeys/delegations")
	fun listContactsDelegationsStubsByHCPartyAndPatientForeignKeys(
		@RequestParam hcPartyId: String,
		@RequestParam secretFKeys: String
	): Flux<IcureStubDto> {
		val secretPatientKeys = secretFKeys.split(',').map { it.trim() }
		return contactService.listContactsByHCPartyAndPatient(hcPartyId, secretPatientKeys).map { contact -> stubV2Mapper.mapToStub(contact) }.injectReactorContext()
	}

	@Operation(summary = "List contacts found By Healthcare Party and secret foreign keys.", description = "Keys must be delimited by coma")
	@PostMapping("/byHcPartySecretForeignKeys/delegations")
	fun findContactsDelegationsStubsByHCPartyPatientForeignKeys(
		@RequestParam hcPartyId: String,
		@RequestBody secretPatientKeys: List<String>,
	): Flux<IcureStubDto> {
		return contactService.listContactsByHCPartyAndPatient(hcPartyId, secretPatientKeys).map { contact -> stubV2Mapper.mapToStub(contact) }.injectReactorContext()
	}

	@Operation(summary = "Close contacts for Healthcare Party and secret foreign keys.", description = "Keys must be delimited by coma")
	@PutMapping("/byHcPartySecretForeignKeys/close")
	fun closeForHCPartyPatientForeignKeys(
		@RequestParam hcPartyId: String,
		@RequestParam secretFKeys: String
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

		return savedOrFailed.map { contact -> contactV2Mapper.map(contact) }.injectReactorContext()
	}

	@Operation(summary = "Deletes multiple Contacts")
	@PostMapping("/delete/batch")
	fun deleteContacts(@RequestBody contactIds: ListOfIdsDto): Flux<DocIdentifierDto> =
		contactService.deleteContacts(
			contactIds.ids.map { IdAndRev(it, null) }
		).map(docIdentifierV2Mapper::map).injectReactorContext()

	@Operation(summary = "Deletes a multiple Contacts if they match the provided revs")
	@PostMapping("/delete/batch/withrev")
	fun deleteContactsWithRev(@RequestBody contactIds: ListOfIdsAndRevDto): Flux<DocIdentifierDto> =
		contactService.deleteContacts(
			contactIds.ids.map(idWithRevV2Mapper::map)
		).map(docIdentifierV2Mapper::map).injectReactorContext()

	@Operation(summary = "Deletes an Contact")
	@DeleteMapping("/{contactId}")
	fun deleteContact(
		@PathVariable contactId: String,
		@Parameter(required = false) rev: String? = null
	): Mono<DocIdentifierDto> = mono {
		contactService.deleteContact(contactId, rev).let(docIdentifierV2Mapper::map)
	}

	@PostMapping("/undelete/{contactId}")
	fun undeleteContact(
		@PathVariable contactId: String,
		@Parameter(required=true) rev: String
	): Mono<ContactDto> = mono {
		contactV2Mapper.map(contactService.undeleteContact(contactId, rev))
	}

	@DeleteMapping("/purge/{contactId}")
	fun purgeContact(
		@PathVariable contactId: String,
		@Parameter(required=true) rev: String
	): Mono<DocIdentifierDto> = mono {
		contactService.purgeContact(contactId, rev).let(docIdentifierV2Mapper::map)
	}
	
	@Operation(summary = "Modify a contact", description = "Returns the modified contact.")
	@PutMapping
	fun modifyContact(@RequestBody contactDto: ContactDto) = mono {
		handleServiceIndexes(contactDto)

		contactService.modifyContact(contactV2Mapper.map(contactDto))?.let {
			contactV2Mapper.map(it)
		} ?: throw DocumentNotFoundException("Contact modification failed.")
	}

	@Operation(summary = "Modify a batch of contacts", description = "Returns the modified contacts.")
	@PutMapping("/batch")
	fun modifyContacts(@RequestBody contactDtos: List<ContactDto>): Flux<ContactDto> {
		val contacts = contactService.modifyContacts(contactDtos.map { c -> handleServiceIndexes(c) }.map { f -> contactV2Mapper.map(f) })
		return contacts.map { f -> contactV2Mapper.map(f) }.injectReactorContext()
	}

	@Operation(summary = "Create a batch of contacts", description = "Returns the modified contacts.")
	@PostMapping("/batch")
	fun createContacts(@RequestBody contactDtos: List<ContactDto>): Flux<ContactDto> {
		val contacts = contactService.createContacts(contactDtos.map { c -> handleServiceIndexes(c) }.map { f -> contactV2Mapper.map(f) })
		return contacts.map { f -> contactV2Mapper.map(f) }.injectReactorContext()
	}

	@Operation(summary = "List contacts for the current user (HcParty) or the given hcparty in the filter ", description = "Returns a list of contacts along with next start keys and Document ID. If the nextStartKey is Null it means that this is the last page.")
	@PostMapping("/filter")
	fun filterContactsBy(
		@Parameter(description = "A Contact document ID") @RequestParam(required = false) startDocumentId: String?,
		@Parameter(description = "Number of rows") @RequestParam(required = false) limit: Int?,
		@RequestBody filterChain: FilterChain<ContactDto>
	) = mono {

		val realLimit = limit ?: paginationConfig.defaultLimit

		val paginationOffset = PaginationOffset(null, startDocumentId, null, realLimit + 1)

		val contacts = contactService.filterContacts(paginationOffset, filterChainV2Mapper.tryMap(filterChain).orThrow())

		contacts.paginatedList(contactV2Mapper::map, realLimit, objectMapper = objectMapper)
	}

	@Operation(summary = "Get the ids of the Contacts matching the provided filter.")
	@PostMapping("/match", produces = [APPLICATION_JSON_VALUE])
	fun matchContactsBy(
		@RequestBody filter: AbstractFilterDto<ContactDto>,
		@RequestParam(required = false) deduplicate: Boolean? = null
	) = contactService.matchContactsBy(
		filter = filterV2Mapper.tryMap(filter).orThrow()
	).injectReactorContext()

	@Operation(summary = "Get a service by id")
	@GetMapping("/service/{serviceId}")
	fun getService(
		@Parameter(description = "The id of the service to retrieve") @PathVariable serviceId: String
	) = mono {
		contactService.getService(serviceId)?.let { serviceV2Mapper.map(it) }
			?: throw DocumentNotFoundException("Service with id $serviceId not found.")
	}

	@Operation(summary = "List services for the current user (HcParty) or the given hcparty in the filter ", description = "Returns a list of contacts along with next start keys and Document ID. If the nextStartKey is Null it means that this is the last page.")
	@PostMapping("/service/filter")
	fun filterServicesBy(
		@Parameter(description = "A Contact document ID") @RequestParam(required = false) startDocumentId: String?,
		@Parameter(description = "Number of rows") @RequestParam(required = false) limit: Int?,
		@RequestBody filterChain: FilterChain<ServiceDto>
	) = mono {

		val realLimit = limit ?: paginationConfig.defaultLimit

		val paginationOffset = PaginationOffset(null, startDocumentId, null, realLimit + 1)
		val mappedFilterChain = filterChainV2Mapper.tryMap(filterChain).orThrow()
		val services: List<ServiceDto> = mappedFilterChain.applyTo(
			contactService.filterServices(paginationOffset, mappedFilterChain), sessionLogic.getSearchKeyMatcher()
		).map { serviceV2Mapper.map(it) }.toList()

		if (services.size <= realLimit) {
			PaginatedList(services, null)
		} else {
			val nextKeyPair = services.lastOrNull()?.let { PaginatedDocumentKeyIdPair(null, it.id) }
			val rows = services.subList(0, services.size - 1)
			PaginatedList(rows, nextKeyPair)
		}
	}

	@Operation(summary = "Get the ids of the Services matching the provided filter.")
	@PostMapping("/service/match")
	fun matchServicesBy(
		@RequestBody filter: AbstractFilterDto<ServiceDto>
	) = contactService.matchServicesBy(
		filter = filterV2Mapper.tryMap(filter).orThrow()
	).injectReactorContext()

	@Operation(summary = "List services with provided ids ", description = "Returns a list of services")
	@PostMapping("/service")
	fun getServices(@RequestBody ids: ListOfIdsDto) = contactService.getServices(ids.ids).map { svc -> serviceV2Mapper.map(svc) }.injectReactorContext()

	@Operation(summary = "List services linked to provided ids ", description = "Returns a list of services")
	@PostMapping("/service/linkedTo")
	fun getServicesLinkedTo(
		@Parameter(description = "The type of the link") @RequestParam(required = false) linkType: String?,
		@RequestBody ids: ListOfIdsDto
	) = contactService.getServicesLinkedTo(ids.ids, linkType).map { svc -> serviceV2Mapper.map(svc) }.injectReactorContext()

	@Operation(summary = "List services by related association id", description = "Returns a list of services")
	@GetMapping("/service/associationId")
	fun listServicesByAssociationId(
		@RequestParam associationId: String,
	) = contactService.listServicesByAssociationId(associationId).map { svc -> serviceV2Mapper.map(svc) }.injectReactorContext()

	@Operation(summary = "List services linked to a health element", description = "Returns the list of services linked to the provided health element id")
	@GetMapping("/service/healthElementId/{healthElementId}")
	fun listServicesByHealthElementId(
		@PathVariable healthElementId: String,
		@Parameter(description = "hcPartyId", required = true) @RequestParam hcPartyId: String
	) = contactService.listServicesByHcPartyAndHealthElementIds(hcPartyId, listOf(healthElementId))
		.map { svc -> serviceV2Mapper.map(svc) }
		.injectReactorContext()

	@Operation(summary = "List contacts by opening date parties with(out) pagination", description = "Returns a list of contacts.")
	@GetMapping("/byOpeningDate")
	fun findContactsByOpeningDate(
		@Parameter(description = "The contact openingDate", required = true) @RequestParam startDate: Long,
		@Parameter(description = "The contact max openingDate", required = true) @RequestParam endDate: Long,
		@Parameter(description = "hcPartyId", required = true) @RequestParam hcPartyId: String,
		@Parameter(description = "The start key for pagination") @RequestParam(required = false) startKey: JsonString?,
		@Parameter(description = "A contact party document ID") @RequestParam(required = false) startDocumentId: String?,
		@Parameter(description = "Number of rows") @RequestParam(required = false) limit: Int?
	): PaginatedFlux<ContactDto> {
		val key = startKey?.let { objectMapper.readValue<ComplexKey>(it) }
		val paginationOffset = PaginationOffset(key, startDocumentId, null, limit ?: paginationConfig.defaultLimit)
		return contactService
			.listContactsByOpeningDate(hcPartyId, startDate, endDate, paginationOffset)
			.mapElements(contactV2Mapper::map)
			.asPaginatedFlux()
	}

	@Operation(description = "Shares one or more contacts with one or more data owners")
	@PutMapping("/bulkSharedMetadataUpdate")
	fun bulkShare(
		@RequestBody request: BulkShareOrUpdateMetadataParamsDto
	): Flux<EntityBulkShareResultDto<ContactDto>> = flow {
		emitAll(contactService.bulkShareOrUpdateMetadata(
			entityShareOrMetadataUpdateRequestV2Mapper.map(request)
		).map { bulkShareResultV2Mapper.map(it) })
	}.injectCachedReactorContext(reactorCacheInjector, 50)

	@Operation(description = "Shares one or more contacts with one or more data owners but does not return the updated entity.")
	@PutMapping("/bulkSharedMetadataUpdateMinimal")
	fun bulkShareMinimal(
		@RequestBody request: BulkShareOrUpdateMetadataParamsDto
	): Flux<EntityBulkShareResultDto<Nothing>> = flow {
		emitAll(contactService.bulkShareOrUpdateMetadata(
			entityShareOrMetadataUpdateRequestV2Mapper.map(request)
		).map { bulkShareResultV2Mapper.map(it).minimal() })
	}.injectCachedReactorContext(reactorCacheInjector, 50)
}
