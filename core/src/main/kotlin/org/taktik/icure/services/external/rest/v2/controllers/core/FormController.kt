/*
 * Copyright (c) 2020. Taktik SA, All rights reserved.
 */

package org.taktik.icure.services.external.rest.v2.controllers.core

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.reactive.asFlow
import kotlinx.coroutines.reactor.mono
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Profile
import org.springframework.core.io.buffer.DataBuffer
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
import org.taktik.icure.asynclogic.SessionInformationProvider
import org.taktik.icure.asyncservice.FormService
import org.taktik.icure.asyncservice.FormTemplateService
import org.taktik.icure.cache.ReactorCacheInjector
import org.taktik.icure.exceptions.MissingRequirementsException
import org.taktik.icure.services.external.rest.v2.dto.FormDto
import org.taktik.icure.services.external.rest.v2.dto.FormTemplateDto
import org.taktik.icure.services.external.rest.v2.dto.IcureStubDto
import org.taktik.icure.services.external.rest.v2.dto.ListOfIdsDto
import org.taktik.icure.services.external.rest.v2.dto.couchdb.DocIdentifierDto
import org.taktik.icure.services.external.rest.v2.dto.requests.BulkShareOrUpdateMetadataParamsDto
import org.taktik.icure.services.external.rest.v2.dto.requests.EntityBulkShareResultDto
import org.taktik.icure.services.external.rest.v2.mapper.FormTemplateV2Mapper
import org.taktik.icure.services.external.rest.v2.mapper.FormV2Mapper
import org.taktik.icure.services.external.rest.v2.mapper.RawFormTemplateV2Mapper
import org.taktik.icure.services.external.rest.v2.mapper.StubV2Mapper
import org.taktik.icure.services.external.rest.v2.mapper.couchdb.DocIdentifierV2Mapper
import org.taktik.icure.services.external.rest.v2.mapper.requests.EntityShareOrMetadataUpdateRequestV2Mapper
import org.taktik.icure.services.external.rest.v2.mapper.requests.FormBulkShareResultV2Mapper
import org.taktik.icure.utils.FuzzyValues
import org.taktik.icure.utils.injectCachedReactorContext
import org.taktik.icure.utils.injectReactorContext
import org.taktik.icure.utils.toByteArray
import reactor.core.publisher.Flux

@RestController("formControllerV2")
@Profile("app")
@RequestMapping("/rest/v2/form")
@Tag(name = "form")
class FormController(
	private val formTemplateService: FormTemplateService,
	private val formService: FormService,
	private val sessionLogic: SessionInformationProvider,
	private val formV2Mapper: FormV2Mapper,
	private val formTemplateV2Mapper: FormTemplateV2Mapper,
	private val rawFormTemplateV2Mapper: RawFormTemplateV2Mapper,
	private val stubV2Mapper: StubV2Mapper,
	private val bulkShareResultV2Mapper: FormBulkShareResultV2Mapper,
	private val entityShareOrMetadataUpdateRequestV2Mapper: EntityShareOrMetadataUpdateRequestV2Mapper,
	private val docIdentifierV2Mapper: DocIdentifierV2Mapper,
	private val reactorCacheInjector: ReactorCacheInjector
) {
	private val logger: Logger = LoggerFactory.getLogger(javaClass)

	@Operation(summary = "Gets a form")
	@GetMapping("/{formId}")
	fun getForm(@PathVariable formId: String) = mono {
		val form = formService.getForm(formId)
			?: throw ResponseStatusException(HttpStatus.NOT_FOUND, "Form fetching failed")
		formV2Mapper.map(form)
	}

	@Operation(summary = "Get a list of forms by ids", description = "Keys must be delimited by coma")
	@PostMapping("/byIds")
	fun getForms(@RequestBody formIds: ListOfIdsDto): Flux<FormDto> {
		require(formIds.ids.isNotEmpty()) { "You must specify at least one id." }
		return formService.getForms(formIds.ids).map(formV2Mapper::map).injectReactorContext()
	}

	@Operation(summary = "Gets the most recent form with the given logicalUuid")
	@GetMapping("/logicalUuid/{logicalUuid}")
	fun getFormByLogicalUuid(@PathVariable logicalUuid: String) = mono {
		val form = formService.getAllByLogicalUuid(logicalUuid)
			.sortedByDescending { it.created }
			.firstOrNull()
			?: throw ResponseStatusException(HttpStatus.NOT_FOUND, "Form not found")
		formV2Mapper.map(form)
	}

	@Operation(summary = "Gets all forms with given logicalUuid")
	@GetMapping("/all/logicalUuid/{logicalUuid}")
	fun getFormsByLogicalUuid(@PathVariable logicalUuid: String) = flow {
		formService.getAllByLogicalUuid(logicalUuid)
			.map { form -> formV2Mapper.map(form) }
			.forEach { emit(it) }
	}.injectReactorContext()

	@Operation(summary = "Gets all forms by uniqueId")
	@GetMapping("/all/uniqueId/{uniqueId}")
	fun getFormsByUniqueId(@PathVariable uniqueId: String) = flow {
		formService.getAllByUniqueId(uniqueId)
			.map { form -> formV2Mapper.map(form) }
			.forEach { emit(it) }
	}.injectReactorContext()

	@Operation(summary = "Gets the most recent form with the given uniqueId")
	@GetMapping("/uniqueId/{uniqueId}")
	fun getFormByUniqueId(@PathVariable uniqueId: String) = mono {
		val form = formService.getAllByUniqueId(uniqueId)
			.sortedByDescending { it.created }
			.firstOrNull()
			?: throw ResponseStatusException(HttpStatus.NOT_FOUND, "Form not found")
		formV2Mapper.map(form)
	}

	@Operation(summary = "Get a list of forms by parents ids", description = "Keys must be delimited by coma")
	@GetMapping("/childrenOf/{formId}/{hcPartyId}")
	fun getChildrenForms(
		@PathVariable formId: String,
		@PathVariable hcPartyId: String
	): Flux<FormDto> {
		val forms = formService.listByHcPartyAndParentId(hcPartyId, formId)
		return forms.map { formV2Mapper.map(it) }.injectReactorContext()
	}

	@Operation(summary = "Create a form with the current user", description = "Returns an instance of created form.")
	@PostMapping
	fun createForm(@RequestBody ft: FormDto) = mono {
		val form = try {
			formService.createForm(formV2Mapper.map(ft))
				?: throw ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Form creation failed")
		} catch (e: MissingRequirementsException) {
			logger.warn(e.message, e)
			throw ResponseStatusException(HttpStatus.BAD_REQUEST, e.message)
		}
		formV2Mapper.map(form)
	}

	@Operation(summary = "Modify a form", description = "Returns the modified form.")
	@PutMapping
	fun modifyForm(@RequestBody formDto: FormDto) = mono {
		val modifiedForm = formService.modifyForm(formV2Mapper.map(formDto))
			?: throw ResponseStatusException(HttpStatus.NOT_FOUND, "Form modification failed")
		formV2Mapper.map(modifiedForm)
	}

	@Operation(summary = "Deletes a batch of forms", description = "Response is a set containing the ID's of deleted forms.")
	@PostMapping("/delete/batch")
	fun deleteForms(@RequestBody formIds: ListOfIdsDto): Flux<DocIdentifierDto> =
		formIds.ids.takeIf { it.isNotEmpty() }?.let { ids ->
			formService.deleteForms(HashSet(ids))
				.map(docIdentifierV2Mapper::map)
				.injectReactorContext()
		} ?: throw ResponseStatusException(HttpStatus.BAD_REQUEST, "A required query parameter was not specified for this request.").also { logger.error(it.message) }

	@Operation(summary = "Deletes a  form", description = "Deletes a single form returning its identifier")
	@DeleteMapping("/{formId}")
	fun deleteForm(@PathVariable formId: String) = mono {
		formService.deleteForm(formId)
			.let(docIdentifierV2Mapper::map)
	}

	@Operation(summary = "Modify a batch of forms", description = "Returns the modified forms.")
	@PutMapping("/batch")
	fun modifyForms(@RequestBody formDtos: List<FormDto>): Flux<FormDto> =
		formService.modifyForms(formDtos.map { formV2Mapper.map(it) }).map { formV2Mapper.map(it) }.injectReactorContext()


	@Operation(summary = "Create a batch of forms", description = "Returns the created forms.")
	@PostMapping("/batch")
	fun createForms(@RequestBody formDtos: List<FormDto>): Flux<FormDto> =
		formService.createForms(formDtos.map { formV2Mapper.map(it) }).map { formV2Mapper.map(it) }.injectReactorContext()

	@Operation(summary = "List forms found By Healthcare Party and secret foreign keys.", description = "Keys must be delimited by coma")
	@GetMapping("/byHcPartySecretForeignKeys")
	fun listFormsByHCPartyAndPatientForeignKeys(
		@RequestParam hcPartyId: String,
		@RequestParam secretFKeys: String,
		@RequestParam(required = false) healthElementId: String?,
		@RequestParam(required = false) planOfActionId: String?,
		@RequestParam(required = false) formTemplateId: String?
	): Flux<FormDto> {
		val secretPatientKeys = secretFKeys.split(',').map { it.trim() }
		val formsList = formService.listFormsByHCPartyAndPatient(hcPartyId, secretPatientKeys, healthElementId, planOfActionId, formTemplateId)
		return formsList.map { contact -> formV2Mapper.map(contact) }.injectReactorContext()
	}

	@Operation(summary = "List forms found By Healthcare Party and secret foreign keys.", description = "Keys must be delimited by coma")
	@PostMapping("/byHcPartySecretForeignKeys")
	fun findFormsByHCPartyPatientForeignKeys(
		@RequestParam hcPartyId: String,
		@RequestBody secretPatientKeys: List<String>,
		@RequestParam(required = false) healthElementId: String?,
		@RequestParam(required = false) planOfActionId: String?,
		@RequestParam(required = false) formTemplateId: String?
	): Flux<FormDto> {
		val formsList = formService.listFormsByHCPartyAndPatient(hcPartyId, secretPatientKeys, healthElementId, planOfActionId, formTemplateId)
		return formsList.map { contact -> formV2Mapper.map(contact) }.injectReactorContext()
	}

	@Operation(summary = "Find Forms ids by data owner id, patient secret keys and opening date")
	@PostMapping("/byDataOwnerPatientOpeningDate", produces = [MediaType.APPLICATION_JSON_VALUE])
	fun listFormIdsByDataOwnerPatientOpeningDate(
		@RequestParam dataOwnerId: String,
		@RequestParam(required = false) startDate: Long?,
		@RequestParam(required = false) endDate: Long?,
		@RequestParam(required = false) descending: Boolean?,
		@RequestBody secretPatientKeys: ListOfIdsDto
	): Flux<String> {
		require(secretPatientKeys.ids.isNotEmpty()) {
			"You need to provide at least one secret patient key"
		}
		return formService
			.listFormIdsByDataOwnerPatientOpeningDate(
				dataOwnerId = dataOwnerId,
				secretForeignKeys = secretPatientKeys.ids.toSet(),
				startDate = startDate?.let { FuzzyValues.getFuzzyDateTime(it) },
				endDate = endDate?.let { FuzzyValues.getFuzzyDateTime(it) },
				descending = descending ?: false)
			.injectReactorContext()
	}

	@Operation(summary = "List form stubs found By Healthcare Party and secret foreign keys.", description = "Keys must be delimited by coma")
	@GetMapping("/byHcPartySecretForeignKeys/delegations")
	fun listFormsDelegationsStubsByHCPartyAndPatientForeignKeys(
		@RequestParam hcPartyId: String,
		@RequestParam secretFKeys: String
	): Flux<IcureStubDto> {
		val secretPatientKeys = secretFKeys.split(',').map { it.trim() }
		return formService.listFormsByHCPartyAndPatient(hcPartyId, secretPatientKeys, null, null, null).map { form -> stubV2Mapper.mapToStub(form) }.injectReactorContext()
	}

	@Operation(summary = "List form stubs found By Healthcare Party and secret foreign keys.", description = "Keys must be delimited by coma")
	@PostMapping("/byHcPartySecretForeignKeys/delegations")
	fun listFormsDelegationsStubsByHCPartyAndPatientForeignKeys(
		@RequestParam hcPartyId: String,
		@RequestBody secretPatientKeys: List<String>,
	): Flux<IcureStubDto> {
		return formService.listFormsByHCPartyAndPatient(hcPartyId, secretPatientKeys, null, null, null).map { form -> stubV2Mapper.mapToStub(form) }.injectReactorContext()
	}

	@Operation(summary = "Gets a form template by guid")
	@GetMapping("/template/{formTemplateId}")
	fun getFormTemplate(@PathVariable formTemplateId: String, @RequestParam(required = false) raw: Boolean?) = mono {
		val formTemplate = formTemplateService.getFormTemplate(formTemplateId)
			?: throw ResponseStatusException(HttpStatus.NOT_FOUND, "FormTemplate fetching failed")
		if (raw == true) rawFormTemplateV2Mapper.map(formTemplate) else formTemplateV2Mapper.map(formTemplate)
	}

	@Operation(summary = "Gets a form template")
	@GetMapping("/template/{specialityCode}/guid/{formTemplateGuid}")
	fun getFormTemplatesByGuid(@PathVariable formTemplateGuid: String, @PathVariable specialityCode: String, @RequestParam(required = false) raw: Boolean?): Flux<FormTemplateDto> = flow {
		emitAll(
			formTemplateService.getFormTemplatesByGuid(sessionLogic.getCurrentUserId(), specialityCode, formTemplateGuid)
				.map { if (raw == true) rawFormTemplateV2Mapper.map(it) else formTemplateV2Mapper.map(it) }
		)
	}.injectReactorContext()

	@Operation(summary = "Gets all form templates")
	@GetMapping("/template/bySpecialty/{specialityCode}")
	fun listFormTemplatesBySpeciality(@PathVariable specialityCode: String, @RequestParam(required = false) loadLayout: Boolean?, @RequestParam(required = false) raw: Boolean?): Flux<FormTemplateDto> {
		val formTemplates = formTemplateService.getFormTemplatesBySpecialty(specialityCode, loadLayout ?: true)
		return formTemplates.map { if (raw == true) rawFormTemplateV2Mapper.map(it) else formTemplateV2Mapper.map(it) }.injectReactorContext()
	}

	@Operation(summary = "Gets all form templates for current user")
	@GetMapping("/template")
	fun getFormTemplates(@RequestParam(required = false) loadLayout: Boolean?, @RequestParam(required = false) raw: Boolean?): Flux<FormTemplateDto> = flow {
		val formTemplates = try {
			formTemplateService.getFormTemplatesByUser(sessionLogic.getCurrentUserId(), loadLayout ?: true)
		} catch (e: Exception) {
			logger.warn(e.message, e)
			throw ResponseStatusException(HttpStatus.BAD_REQUEST, e.message)
		}
		emitAll(
			formTemplates.map { if (raw == true) rawFormTemplateV2Mapper.map(it) else formTemplateV2Mapper.map(it) }
		)
	}.injectReactorContext()

	@Operation(summary = "Create a form template with the current user", description = "Returns an instance of created form template.")
	@PostMapping("/template")
	fun createFormTemplate(@RequestBody ft: FormTemplateDto) = mono {
		val formTemplate = formTemplateService.createFormTemplate(formTemplateV2Mapper.map(ft))
		formTemplateV2Mapper.map(formTemplate)
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
		val template = formTemplateV2Mapper.map(ft).copy(id = formTemplateId)
		val formTemplate = formTemplateService.modifyFormTemplate(template)
			?: throw ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Form modification failed")
		formTemplateV2Mapper.map(formTemplate)
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

	@Operation(summary = "Update a form template's layout")
	@PutMapping("/template/{formTemplateId}/attachment", consumes = [MediaType.APPLICATION_OCTET_STREAM_VALUE])
	fun setTemplateAttachment(
		@PathVariable formTemplateId: String,
		@RequestBody payload: Flow<DataBuffer>,
	) = mono {
		val formTemplate = formTemplateService.getFormTemplate(formTemplateId)
			?: throw ResponseStatusException(HttpStatus.NOT_FOUND, "FormTemplate with id $formTemplateId not found")
		formTemplateService.modifyFormTemplate(formTemplate.copy(templateLayout = payload.toByteArray(true)))?.rev
			?: throw ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Form Template modification failed")
	}

	@Operation(description = "Shares one or more forms with one or more data owners")
	@PutMapping("/bulkSharedMetadataUpdate")
	fun bulkShare(
		@RequestBody request: BulkShareOrUpdateMetadataParamsDto
	): Flux<EntityBulkShareResultDto<FormDto>> = flow {
		emitAll(formService.bulkShareOrUpdateMetadata(
			entityShareOrMetadataUpdateRequestV2Mapper.map(request)
		).map { bulkShareResultV2Mapper.map(it) })
	}.injectCachedReactorContext(reactorCacheInjector, 50)

	@Operation(description = "Shares one or more forms with one or more data owners but does not return the updated entity.")
	@PutMapping("/bulkSharedMetadataUpdateMinimal")
	fun bulkShareMinimal(
		@RequestBody request: BulkShareOrUpdateMetadataParamsDto
	): Flux<EntityBulkShareResultDto<FormDto>> = flow {
		emitAll(formService.bulkShareOrUpdateMetadata(
			entityShareOrMetadataUpdateRequestV2Mapper.map(request)
		).map { bulkShareResultV2Mapper.map(it).copy(updatedEntity = null) })
	}.injectCachedReactorContext(reactorCacheInjector, 50)
}
