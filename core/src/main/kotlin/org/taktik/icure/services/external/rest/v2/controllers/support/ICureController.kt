/*
 * Copyright (c) 2020. Taktik SA, All rights reserved.
 */

package org.taktik.icure.services.external.rest.v2.controllers.support

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.reactor.mono
import org.springframework.context.annotation.Profile
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import org.taktik.couchdb.entity.IdAndRev
import org.taktik.icure.annotations.permissions.AccessControl
import org.taktik.icure.asyncservice.ContactService
import org.taktik.icure.asyncservice.DocumentService
import org.taktik.icure.asyncservice.FormService
import org.taktik.icure.asyncservice.HealthElementService
import org.taktik.icure.asyncservice.ICureSharedService
import org.taktik.icure.asyncservice.InvoiceService
import org.taktik.icure.asyncservice.MessageService
import org.taktik.icure.asyncservice.PatientService
import org.taktik.icure.services.external.rest.v2.dto.IdWithRevDto
import org.taktik.icure.services.external.rest.v2.mapper.IdWithRevV2Mapper
import org.taktik.icure.services.external.rest.v2.mapper.IndexingInfoV2Mapper
import org.taktik.icure.services.external.rest.v2.mapper.ReplicationInfoV2Mapper
import org.taktik.icure.utils.injectReactorContext
import reactor.core.publisher.Flux

@RestController("iCureControllerV2")
@Profile("app")
@RequestMapping("/rest/v2/icure")
@Tag(name = "icure")
class ICureController(
	private val iCureService: ICureSharedService,
	private val patientService: PatientService,
	private val contactService: ContactService,
	private val messageService: MessageService,
	private val invoiceService: InvoiceService,
	private val documentService: DocumentService,
	private val healthElementService: HealthElementService,
	private val formService: FormService,
	private val idWithRevMapper: IdWithRevV2Mapper,
	private val indexingInfoV2Mapper: IndexingInfoV2Mapper,
	private val replicationInfoV2Mapper: ReplicationInfoV2Mapper,
) {

	private val idAndRevToIdWithRevDto = { idWithRev: IdAndRev -> idWithRevMapper.map(idWithRev) }

	@Operation(summary = "Get version")
	@GetMapping("/v", produces = [MediaType.TEXT_PLAIN_VALUE])
	fun getVersion(): String = iCureService.getVersion()

	@GetMapping("/ok", produces = [MediaType.TEXT_PLAIN_VALUE])
	fun isReady() = "true"

	@Operation(summary = "Get process info")
	@GetMapping("/p", produces = [MediaType.TEXT_PLAIN_VALUE])
	fun getProcessInfo() = mono { iCureService.getProcessInfo() }

	@Operation(summary = "Get index info")
	@GetMapping("/i")
	fun getIndexingInfo() = mono {
		indexingInfoV2Mapper.map(iCureService.getIndexingStatus())
	}

	@Operation(summary = "Get replication info")
	@GetMapping("/r")
	fun getReplicationInfo() = mono {
		iCureService.getReplicationInfo().let(replicationInfoV2Mapper::map)
	}

	@Operation(summary = "Force update design doc")
	@PostMapping("/dd/{entityName}")
	fun updateDesignDoc(@PathVariable entityName: String, @RequestParam(required = false) warmup: Boolean? = null) = mono {
		iCureService.updateDesignDocForCurrentUser(entityName, warmup ?: false)
		true
	}

	@AccessControl("CanAccessAsHcp")
	@Operation(summary = "Resolve patients conflicts")
	@PostMapping("/conflicts/patient")
	fun resolvePatientsConflicts(@RequestParam(required = false) limit: Int? = null): Flux<IdWithRevDto> = patientService.solveConflicts(limit).map(idAndRevToIdWithRevDto).injectReactorContext()

	@AccessControl("CanAccessAsHcp")
	@Operation(summary = "Resolve contacts conflicts")
	@PostMapping("/conflicts/contact")
	fun resolveContactsConflicts(@RequestParam(required = false) limit: Int? = null): Flux<IdWithRevDto> = contactService.solveConflicts(limit).map(idAndRevToIdWithRevDto).injectReactorContext()

	@AccessControl("CanAccessAsHcp OR CanAccessAsAdmin")
	@Operation(summary = "resolve forms conflicts")
	@PostMapping("/conflicts/form")
	fun resolveFormsConflicts(@RequestParam(required = false) limit: Int? = null): Flux<IdWithRevDto> = formService.solveConflicts(limit).map(idAndRevToIdWithRevDto).injectReactorContext()

	@AccessControl("CanAccessAsHcp")
	@Operation(summary = "resolve healthcare elements conflicts")
	@PostMapping("/conflicts/healthelement")
	fun resolveHealthElementsConflicts(@RequestParam(required = false) limit: Int? = null): Flux<IdWithRevDto> = healthElementService.solveConflicts(limit).map(idAndRevToIdWithRevDto).injectReactorContext()

	@AccessControl("CanAccessAsHcp OR CanAccessAsAdmin")
	@Operation(summary = "resolve invoices conflicts")
	@PostMapping("/conflicts/invoice")
	fun resolveInvoicesConflicts(@RequestParam(required = false) limit: Int? = null): Flux<IdWithRevDto> = invoiceService.solveConflicts(limit).map(idAndRevToIdWithRevDto).injectReactorContext()

	@AccessControl("CanAccessAsHcp OR CanAccessAsAdmin")
	@Operation(summary = "resolve messages conflicts")
	@PostMapping("/conflicts/message")
	fun resolveMessagesConflicts(@RequestParam(required = false) limit: Int? = null): Flux<IdWithRevDto> = messageService.solveConflicts(limit).map(idAndRevToIdWithRevDto).injectReactorContext()

	@AccessControl("CanAccessAsHcp")
	@Operation(summary = "resolve documents conflicts")
	@PostMapping("/conflicts/document")
	fun resolveDocumentsConflicts(@RequestParam(required = false) ids: String?, @RequestParam(required = false) limit: Int? = null): Flux<IdWithRevDto> = documentService.solveConflicts(limit, ids?.split(",")).map(idAndRevToIdWithRevDto).injectReactorContext()
}
