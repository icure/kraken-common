/*
 * Copyright (c) 2020. Taktik SA, All rights reserved.
 */

package org.taktik.icure.services.external.rest.v1.controllers.core

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.reactive.asFlow
import kotlinx.coroutines.reactor.mono
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Profile
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.codec.multipart.Part
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RequestPart
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.server.ResponseStatusException
import org.taktik.couchdb.entity.IdAndRev

import org.taktik.icure.asynclogic.SessionInformationProvider
import org.taktik.icure.asyncservice.FormService
import org.taktik.icure.asyncservice.FormTemplateService
import org.taktik.icure.exceptions.MissingRequirementsException
import org.taktik.icure.services.external.rest.v1.dto.FormDto
import org.taktik.icure.services.external.rest.v1.dto.FormTemplateDto
import org.taktik.icure.services.external.rest.v1.dto.IcureStubDto
import org.taktik.icure.services.external.rest.v1.dto.ListOfIdsDto
import org.taktik.icure.services.external.rest.v1.dto.couchdb.DocIdentifierDto
import org.taktik.icure.services.external.rest.v1.dto.embed.DelegationDto
import org.taktik.icure.services.external.rest.v1.mapper.FormMapper
import org.taktik.icure.services.external.rest.v1.mapper.FormTemplateMapper
import org.taktik.icure.services.external.rest.v1.mapper.RawFormTemplateMapper
import org.taktik.icure.services.external.rest.v1.mapper.StubMapper
import org.taktik.icure.services.external.rest.v1.mapper.couchdb.DocIdentifierMapper
import org.taktik.icure.services.external.rest.v1.mapper.embed.DelegationMapper
import org.taktik.icure.utils.injectReactorContext
import org.taktik.icure.utils.toByteArray
import org.taktik.icure.utils.warn
import reactor.core.publisher.Flux

@RestController
@Profile("app")
@RequestMapping("/rest/v1/form")
@Tag(name = "form")
class FormController(
	private val formTemplateService: FormTemplateService,
	private val formService: FormService,
	private val sessionLogic: SessionInformationProvider,
	private val formMapper: FormMapper,
	private val formTemplateMapper: FormTemplateMapper,
	private val rawFormTemplateMapper: RawFormTemplateMapper,
	private val delegationMapper: DelegationMapper,
	private val stubMapper: StubMapper,
	private val docIdentifierMapper: DocIdentifierMapper,
) {
	private val log: Logger = LoggerFactory.getLogger(javaClass)

	@Operation(summary = "Gets a form")
	@GetMapping("/{formId}")
	fun getForm(@PathVariable formId: String) = mono {
		val form = formService.getForm(formId)
			?: throw ResponseStatusException(HttpStatus.NOT_FOUND, "Form fetching failed")
		formMapper.map(form)
	}

	@Operation(summary = "Get a list of forms by ids", description = "Keys must be delimited by coma")
	@PostMapping("/byIds")
	fun getForms(@RequestBody formIds: ListOfIdsDto): Flux<FormDto> {
		val forms = formService.getForms(formIds.ids)
		return forms.map { formMapper.map(it) }.injectReactorContext()
	}

	@Operation(summary = "Gets the most recent form with the given logicalUuid")
	@GetMapping("/logicalUuid/{logicalUuid}")
	fun getFormByLogicalUuid(@PathVariable logicalUuid: String) = mono {
		val form = formService.listFormsByLogicalUuid(logicalUuid, true)
			.firstOrNull()
			?: throw ResponseStatusException(HttpStatus.NOT_FOUND, "Form not found")
		formMapper.map(form)
	}

	@Operation(summary = "Gets all forms with given logicalUuid")
	@GetMapping("/all/logicalUuid/{logicalUuid}")
	fun getFormsByLogicalUuid(@PathVariable logicalUuid: String) =
		formService.listFormsByLogicalUuid(logicalUuid, true)
			.map { form -> formMapper.map(form) }
			.injectReactorContext()

	@Operation(summary = "Gets all forms by uniqueId")
	@GetMapping("/all/uniqueId/{uniqueId}")
	fun getFormsByUniqueId(@PathVariable uniqueId: String) =
		formService.listFormsByUniqueId(uniqueId, true)
			.map { form -> formMapper.map(form) }
			.injectReactorContext()

	@Operation(summary = "Gets the most recent form with the given uniqueId")
	@GetMapping("/uniqueId/{uniqueId}")
	fun getFormByUniqueId(@PathVariable uniqueId: String) = mono {
		val form = formService.listFormsByUniqueId(uniqueId, true)
			.firstOrNull()
			?: throw ResponseStatusException(HttpStatus.NOT_FOUND, "Form not found")
		formMapper.map(form)
	}

	@Operation(summary = "Get a list of forms by ids", description = "Keys must be delimited by coma")
	@GetMapping("/childrenOf/{formId}/{hcPartyId}")
	fun getChildrenForms(
		@PathVariable formId: String,
		@PathVariable hcPartyId: String
	): Flux<FormDto> {
		val forms = formService.listByHcPartyAndParentId(hcPartyId, formId)
		return forms.map { formMapper.map(it) }.injectReactorContext()
	}

	@Operation(summary = "Create a form with the current user", description = "Returns an instance of created form.")
	@PostMapping
	fun createForm(@RequestBody ft: FormDto) = mono {
		val form = try {
			formService.createForm(formMapper.map(ft))
				?: throw ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Form creation failed")
		} catch (e: MissingRequirementsException) {
			log.warn(e) { e.message }
			throw ResponseStatusException(HttpStatus.BAD_REQUEST, e.message)
		}
		formMapper.map(form)
	}

	@Operation(summary = "Delegates a form to a healthcare party", description = "It delegates a form to a healthcare party. Returns the form with the new delegations.")
	@PostMapping("/delegate/{formId}")
	fun newFormDelegations(
		@PathVariable formId: String,
		@RequestBody ds: List<DelegationDto>
	) = mono {
		formService.addDelegations(formId, ds.map { d -> delegationMapper.map(d) })
		val formWithDelegation = formService.getForm(formId)

		if (formWithDelegation != null && formWithDelegation.delegations.isNotEmpty()) {
			formMapper.map(formWithDelegation)
		} else {
			throw ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Delegation creation for Form failed")
		}
	}

	@Operation(summary = "Modify a form", description = "Returns the modified form.")
	@PutMapping
	fun modifyForm(@RequestBody formDto: FormDto) = mono {
		val modifiedForm = formService.modifyForm(formMapper.map(formDto))
			?: throw ResponseStatusException(HttpStatus.NOT_FOUND, "Form not found")
		formMapper.map(modifiedForm)
	}

	@Operation(summary = "Delete forms.", description = "Response is a set containing the ID's of deleted forms.")
	@DeleteMapping("/{formIds}")
	fun deleteForms(@PathVariable formIds: String): Flux<DocIdentifierDto> {
		if (formIds.isEmpty()) {
			throw ResponseStatusException(HttpStatus.BAD_REQUEST, "formIds was empty")
		}
		return formService.deleteForms(formIds.split(',').toSet().map { IdAndRev(it, null) })
			.map(docIdentifierMapper::map)
			.injectReactorContext()
	}

	@Operation(summary = "Modify a batch of forms", description = "Returns the modified forms.")
	@PutMapping("/batch")
	fun modifyForms(@RequestBody formDtos: List<FormDto>): Flux<FormDto> =
		formService.modifyForms(formDtos.map { formMapper.map(it) }).map { formMapper.map(it) }.injectReactorContext()

	@Operation(summary = "Create a batch of forms", description = "Returns the created forms.")
	@PostMapping("/batch")
	fun createForms(@RequestBody formDtos: List<FormDto>): Flux<FormDto> =
		formService.createForms(formDtos.map { formMapper.map(it) }).map { formMapper.map(it) }.injectReactorContext()

	@Suppress("DEPRECATION")
	@Deprecated("This method cannot include results with secure delegations, use listFormIdsByDataOwnerPatientOpeningDate instead")
	@Operation(summary = "List forms found By Healthcare Party and secret foreign keys.", description = "Keys must be delimited by coma")
	@GetMapping("/byHcPartySecretForeignKeys")
	fun findFormsByHCPartyPatientForeignKeys(
		@RequestParam hcPartyId: String,
		@RequestParam secretFKeys: String,
		@RequestParam(required = false) healthElementId: String?,
		@RequestParam(required = false) planOfActionId: String?,
		@RequestParam(required = false) formTemplateId: String?
	): Flux<FormDto> {
		val secretPatientKeys = secretFKeys.split(',').map { it.trim() }
		val formsList = formService.listFormsByHCPartyAndPatient(hcPartyId, secretPatientKeys, healthElementId, planOfActionId, formTemplateId)
		return formsList.map { contact -> formMapper.map(contact) }.injectReactorContext()
	}

	@Suppress("DEPRECATION")
	@Deprecated("This method cannot include results with secure delegations, use listFormIdsByDataOwnerPatientOpeningDate instead")
	@Operation(summary = "List forms found By Healthcare Party and secret foreign keys.", description = "Keys must be delimited by comma")
	@PostMapping("/byHcPartySecretForeignKeys")
	fun findFormsByHCPartyPatientForeignKeys(
		@RequestParam hcPartyId: String,
		@RequestBody secretPatientKeys: List<String>,
		@RequestParam(required = false) healthElementId: String?,
		@RequestParam(required = false) planOfActionId: String?,
		@RequestParam(required = false) formTemplateId: String?
	): Flux<FormDto> {
		val formsList = formService.listFormsByHCPartyAndPatient(hcPartyId, secretPatientKeys, healthElementId, planOfActionId, formTemplateId)
		return formsList.map { contact -> formMapper.map(contact) }.injectReactorContext()
	}

	@Suppress("DEPRECATION")
	@Deprecated("This method cannot include results with secure delegations, use findFormsDelegationsStubsByIds instead")
	@Operation(summary = "List form stubs found by healthcare party and secret foreign keys.")
	@GetMapping("/byHcPartySecretForeignKeys/delegations")
	fun findFormsDelegationsStubsByHCPartyPatientForeignKeys(
		@RequestParam hcPartyId: String,
		@RequestParam secretFKeys: String
	): Flux<IcureStubDto> {
		val secretPatientKeys = secretFKeys.split(',').map { it.trim() }
		return formService.listFormsByHCPartyAndPatient(hcPartyId, secretPatientKeys, null, null, null).map { form -> stubMapper.mapToStub(form) }.injectReactorContext()
	}

	@Suppress("DEPRECATION")
	@Deprecated("This method cannot include results with secure delegations, use findFormsDelegationsStubsByIds instead")
	@Operation(summary = "List form stubs found by healthcare party and secret foreign keys.")
	@PostMapping("/byHcPartySecretForeignKeys/delegations")
	fun findFormsDelegationsStubsByHCPartyPatientForeignKeys(
		@RequestParam hcPartyId: String,
		@RequestBody secretPatientKeys: List<String>,
	): Flux<IcureStubDto> {
		return formService.listFormsByHCPartyAndPatient(hcPartyId, secretPatientKeys, null, null, null).map { form -> stubMapper.mapToStub(form) }.injectReactorContext()
	}

	@Operation(summary = "List form stubs found by healthcare party and secret foreign keys.")
	@PostMapping("/delegationsByIds")
	fun findFormsDelegationsStubsByIds(
		@RequestBody formIds: ListOfIdsDto,
	): Flux<IcureStubDto> = formService
		.getForms(formIds.ids)
		.map { form -> stubMapper.mapToStub(form) }
		.injectReactorContext()

	@Operation(summary = "Update delegations in form.")
	@PostMapping("/delegations")
	fun setFormsDelegations(@RequestBody stubs: List<IcureStubDto>) = flow {
		val forms = formService.getForms(stubs.map { it.id }).map { form ->
			stubs.find { s -> s.id == form.id }?.let { stub ->
				form.copy(
					delegations = form.delegations.mapValues { (s, dels) -> stub.delegations[s]?.map { delegationMapper.map(it) }?.toSet() ?: dels } +
						stub.delegations.filterKeys { k -> !form.delegations.containsKey(k) }.mapValues { (_, value) -> value.map { delegationMapper.map(it) }.toSet() },
					encryptionKeys = form.encryptionKeys.mapValues { (s, dels) -> stub.encryptionKeys[s]?.map { delegationMapper.map(it) }?.toSet() ?: dels } +
						stub.encryptionKeys.filterKeys { k -> !form.encryptionKeys.containsKey(k) }.mapValues { (_, value) -> value.map { delegationMapper.map(it) }.toSet() },
					cryptedForeignKeys = form.cryptedForeignKeys.mapValues { (s, dels) -> stub.cryptedForeignKeys[s]?.map { delegationMapper.map(it) }?.toSet() ?: dels } +
						stub.cryptedForeignKeys.filterKeys { k -> !form.cryptedForeignKeys.containsKey(k) }.mapValues { (_, value) -> value.map { delegationMapper.map(it) }.toSet() },
				)
			} ?: form
		}
		emitAll(formService.modifyForms(forms.toList()).map { stubMapper.mapToStub(it) })
	}.injectReactorContext()

	@Operation(summary = "Gets a form template by guid")
	@GetMapping("/template/{formTemplateId}")
	fun getFormTemplate(@PathVariable formTemplateId: String, @RequestParam(required = false) raw: Boolean?) = mono {
		val formTemplate = formTemplateService.getFormTemplate(formTemplateId)
			?: throw ResponseStatusException(HttpStatus.NOT_FOUND, "FormTemplate fetching failed")
		if (raw == true) rawFormTemplateMapper.map(formTemplate) else formTemplateMapper.map(formTemplate)
	}

	@Operation(summary = "Gets a form template")
	@GetMapping("/template/{specialityCode}/guid/{formTemplateGuid}")
	@Deprecated("This method has unintuitive behaviour, read FormTemplateService.getFormTemplatesByGuid doc for more info")
	fun getFormTemplatesByGuid(@PathVariable formTemplateGuid: String, @PathVariable specialityCode: String, @RequestParam(required = false) raw: Boolean?): Flux<FormTemplateDto> = flow {
		emitAll(
			formTemplateService.getFormTemplatesByGuid(sessionLogic.getCurrentUserId(), specialityCode, formTemplateGuid)
				.map { if (raw == true) rawFormTemplateMapper.map(it) else formTemplateMapper.map(it) }
		)
	}.injectReactorContext()

	@Operation(summary = "Gets all form templates")
	@GetMapping("/template/bySpecialty/{specialityCode}")
	fun findFormTemplatesBySpeciality(@PathVariable specialityCode: String, @RequestParam(required = false) loadLayout: Boolean?, @RequestParam(required = false) raw: Boolean?): Flux<FormTemplateDto> {
		val formTemplates = formTemplateService.getFormTemplatesBySpecialty(specialityCode, loadLayout ?: true)
		return formTemplates.map { if (raw == true) rawFormTemplateMapper.map(it) else formTemplateMapper.map(it) }.injectReactorContext()
	}

	@Operation(summary = "Gets all form templates for current user")
	@GetMapping("/template")
	fun findFormTemplates(@RequestParam(required = false) loadLayout: Boolean?, @RequestParam(required = false) raw: Boolean?): Flux<FormTemplateDto> = flow {
		val formTemplates = formTemplateService.getFormTemplatesByUser(sessionLogic.getCurrentUserId(), loadLayout ?: true)
		emitAll(
			formTemplates.map { if (raw == true) rawFormTemplateMapper.map(it) else formTemplateMapper.map(it) }
		)
	}.injectReactorContext()

	@Operation(summary = "Create a form template with the current user", description = "Returns an instance of created form template.")
	@PostMapping("/template")
	fun createFormTemplate(@RequestBody ft: FormTemplateDto) = mono {
		val formTemplate = formTemplateService.createFormTemplate(formTemplateMapper.map(ft))
		formTemplateMapper.map(formTemplate)
	}

	@Operation(summary = "Delete a form template")
	@DeleteMapping("/template/{formTemplateId}")
	fun deleteFormTemplate(@PathVariable formTemplateId: String) = mono {
		formTemplateService.deleteFormTemplates(setOf(formTemplateId)).firstOrNull()
			?: throw ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Form deletion failed")
	}

	@Operation(summary = "Modify a form template with the current user", description = "Returns an instance of created form template.")
	@PutMapping("/template/{formTemplateId}", consumes = [MediaType.APPLICATION_JSON_VALUE])
	fun updateFormTemplate(@PathVariable formTemplateId: String, @RequestBody ft: FormTemplateDto) = mono {
		val template = formTemplateMapper.map(ft).copy(id = formTemplateId)
		val formTemplate = formTemplateService.modifyFormTemplate(template)
			?: throw ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Form modification failed")
		formTemplateMapper.map(formTemplate)
	}

	@Operation(summary = "Update a form template's layout")
	@PutMapping("/template/{formTemplateId}/attachment/multipart", consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
	fun setTemplateAttachmentMulti(
		@PathVariable formTemplateId: String,
		@RequestPart("attachment") payload: Part
	) = mono {
		val formTemplate = formTemplateService.getFormTemplate(formTemplateId)
			?: throw ResponseStatusException(HttpStatus.NOT_FOUND, "FormTemplate with id $formTemplateId not found")
		formTemplateService.modifyFormTemplate(formTemplate.copy(templateLayout = payload.also {
			require(it.headers().contentType != null) {
				"attachment part must specify ${HttpHeaders.CONTENT_TYPE} header."
			}
		}.content().asFlow().toByteArray(true)))?.rev ?: throw ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Form Template modification failed")
	}
}
