/*
 * Copyright (c) 2020. Taktik SA, All rights reserved.
 */

package org.taktik.icure.services.external.rest.v2.controllers.support

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.tags.Tag
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.reactor.mono
import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Profile
import org.springframework.core.io.buffer.DefaultDataBufferFactory
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.server.reactive.ServerHttpResponse
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.server.ResponseStatusException
import org.taktik.couchdb.DocIdentifier
import org.taktik.couchdb.entity.IdAndRev
import org.taktik.icure.asynclogic.SessionInformationProvider
import org.taktik.icure.asyncservice.DocumentTemplateService
import org.taktik.icure.config.SharedPaginationConfig
import org.taktik.icure.db.PaginationOffset
import org.taktik.icure.entities.embed.DocumentType
import org.taktik.icure.pagination.PaginatedFlux
import org.taktik.icure.pagination.asPaginatedFlux
import org.taktik.icure.pagination.mapElements
import org.taktik.icure.services.external.rest.v2.dto.DocumentTemplateDto
import org.taktik.icure.services.external.rest.v2.dto.ListOfIdsDto
import org.taktik.icure.services.external.rest.v2.dto.couchdb.DocIdentifierDto
import org.taktik.icure.services.external.rest.v2.dto.data.ByteArrayDto
import org.taktik.icure.services.external.rest.v2.mapper.DocumentTemplateV2Mapper
import org.taktik.icure.services.external.rest.v2.mapper.couchdb.DocIdentifierV2Mapper
import org.taktik.icure.utils.injectReactorContext
import reactor.core.publisher.Flux

@RestController("documentTemplateControllerV2")
@Profile("app")
@RequestMapping("/rest/v2/doctemplate")
@Tag(name = "documentTemplate")
class DocumentTemplateController(
	private val documentTemplateService: DocumentTemplateService,
	private val sessionLogic: SessionInformationProvider,
	private val documentTemplateV2Mapper: DocumentTemplateV2Mapper,
	private val docIdentifierV2Mapper: DocIdentifierV2Mapper,
	private val paginationConfig: SharedPaginationConfig,
) {
	private val logger = LoggerFactory.getLogger(javaClass)

	@Operation(summary = "Gets a document template")
	@GetMapping("/{documentTemplateId}")
	fun getDocumentTemplate(
		@PathVariable documentTemplateId: String,
	) = mono {
		val documentTemplate =
			documentTemplateService.getDocumentTemplate(documentTemplateId)
				?: throw ResponseStatusException(HttpStatus.NOT_FOUND, "DocumentTemplate fetching failed")
		documentTemplateV2Mapper.map(documentTemplate)
	}

	@Operation(summary = "Deletes document templates")
	@PostMapping("/delete/batch")
	fun deleteDocumentTemplates(
		@RequestBody documentTemplateIds: ListOfIdsDto,
	): Flux<DocIdentifierDto> = documentTemplateIds.ids.takeIf { it.isNotEmpty() }?.let { ids ->
		documentTemplateService
			.deleteDocumentTemplates(ids.toSet().map { IdAndRev(it, null) })
			.map { docIdentifierV2Mapper.map(DocIdentifier(it.id, it.rev)) }
			.injectReactorContext()
	}
		?: throw ResponseStatusException(HttpStatus.BAD_REQUEST, "A required query parameter was not specified for this request.").also {
			logger.error(it.message)
		}

	@Operation(summary = "Gets all document templates")
	@GetMapping("/bySpecialty/{specialityCode}")
	fun findDocumentTemplatesBySpeciality(
		@PathVariable specialityCode: String,
		@RequestParam(required = false) loadAttachment: Boolean? = null,
	): Flux<DocumentTemplateDto> {
		val documentTemplates = documentTemplateService.getDocumentTemplatesBySpecialty(specialityCode, loadAttachment ?: true)
		return documentTemplates.map { ft -> documentTemplateV2Mapper.map(ft) }.injectReactorContext()
	}

	@Operation(summary = "Gets all document templates by Type")
	@GetMapping("/byDocumentType/{documentTypeCode}")
	fun listDocumentTemplatesByDocumentType(
		@PathVariable documentTypeCode: String,
		@RequestParam(required = false) loadAttachment: Boolean? = null,
	): Flux<DocumentTemplateDto> {
		DocumentType.fromName(documentTypeCode)
			?: throw ResponseStatusException(
				HttpStatus.BAD_REQUEST,
				"Cannot retrieve document templates: provided Document Type Code doesn't exists",
			)
		val documentTemplates = documentTemplateService.getDocumentTemplatesByDocumentType(documentTypeCode, loadAttachment ?: true)
		return documentTemplates.map { ft -> documentTemplateV2Mapper.map(ft) }.injectReactorContext()
	}

	@Operation(summary = "Gets all document templates by Type For currentUser")
	@GetMapping("/byDocumentTypeForCurrentUser/{documentTypeCode}")
	fun listDocumentTemplatesByDocumentTypeForCurrentUser(
		@PathVariable documentTypeCode: String,
		@RequestParam(required = false) loadAttachment: Boolean? = null,
	): Flux<DocumentTemplateDto> = flow {
		DocumentType.fromName(documentTypeCode)
			?: throw ResponseStatusException(
				HttpStatus.BAD_REQUEST,
				"Cannot retrieve document templates: provided Document Type Code doesn't exists",
			)
		emitAll(
			documentTemplateService
				.getDocumentTemplatesByDocumentTypeAndUser(
					documentTypeCode,
					sessionLogic.getCurrentUserId(),
					loadAttachment ?: true,
				).map { ft -> documentTemplateV2Mapper.map(ft) },
		)
	}.injectReactorContext()

	@Operation(summary = "Gets all document templates for current user")
	@GetMapping
	fun listDocumentTemplates(
		@RequestParam(required = false) loadAttachment: Boolean? = null,
	): Flux<DocumentTemplateDto> = flow {
		emitAll(
			documentTemplateService
				.getDocumentTemplatesByUser(sessionLogic.getCurrentUserId(), loadAttachment ?: true)
				.map { ft -> documentTemplateV2Mapper.map(ft) },
		)
	}.injectReactorContext()

	@Operation(summary = "Gets all document templates for all users with pagination")
	@GetMapping("/find/all")
	fun findAllDocumentTemplates(
		@Parameter(description = "The startKey for pagination") @RequestParam(required = false) startKey: String?,
		@Parameter(description = "A DocumentTemplate document ID") @RequestParam(required = false) startDocumentId: String?,
		@Parameter(description = "Number of rows") @RequestParam(required = false) limit: Int?,
	): PaginatedFlux<DocumentTemplateDto> {
		val offset = PaginationOffset(startKey, startDocumentId, null, limit ?: paginationConfig.defaultLimit)
		return documentTemplateService
			.getAllDocumentTemplates(offset)
			.mapElements(documentTemplateV2Mapper::map)
			.asPaginatedFlux()
	}

	@Operation(summary = "Create a document template with the current user", description = "Returns an instance of created document template.")
	@PostMapping
	fun createDocumentTemplate(
		@RequestBody ft: DocumentTemplateDto,
	) = mono {
		val documentTemplate = documentTemplateService.createDocumentTemplate(documentTemplateV2Mapper.map(ft))
		documentTemplateV2Mapper.map(documentTemplate)
	}

	@Operation(summary = "Modify a document template with the current user", description = "Returns an instance of created document template.")
	@PutMapping("/{documentTemplateId}")
	fun modifyDocumentTemplate(
		@PathVariable documentTemplateId: String,
		@RequestBody ft: DocumentTemplateDto,
	) = mono {
		val template = documentTemplateV2Mapper.map(ft).copy(id = documentTemplateId)
		val documentTemplate =
			documentTemplateService.modifyDocumentTemplate(template)
				?: throw ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Document Template update failed")

		documentTemplateV2Mapper.map(documentTemplate)
	}

	@Operation(
		summary = "Download a the document template attachment",
		responses = [
			ApiResponse(
				responseCode = "200",
				content = [Content(mediaType = MediaType.APPLICATION_OCTET_STREAM_VALUE, schema = Schema(type = "string", format = "binary"))],
			),
		],
	)
	@GetMapping("/{documentTemplateId}/attachment/{attachmentId}", produces = [MediaType.APPLICATION_OCTET_STREAM_VALUE])
	fun getDocumentTemplateAttachment(
		@PathVariable documentTemplateId: String,
		@PathVariable attachmentId: String,
		response: ServerHttpResponse,
	) = mono {
		val document =
			documentTemplateService.getDocumentTemplate(documentTemplateId)
				?: throw ResponseStatusException(HttpStatus.NOT_FOUND, "Document not found")
		document.attachment ?: throw ResponseStatusException(HttpStatus.NOT_FOUND, "AttachmentDto not found")
	}

	@Operation(
		summary = "Download a the document template attachment",
		responses = [
			ApiResponse(
				responseCode = "200",
				content = [Content(mediaType = MediaType.APPLICATION_OCTET_STREAM_VALUE, schema = Schema(type = "string", format = "binary"))],
			),
		],
	)
	@GetMapping("/{documentTemplateId}/attachmentText/{attachmentId}", produces = [MediaType.APPLICATION_OCTET_STREAM_VALUE])
	fun getAttachmentText(
		@PathVariable documentTemplateId: String,
		@PathVariable attachmentId: String,
		response: ServerHttpResponse,
	) = response.writeWith(
		flow {
			val document =
				documentTemplateService.getDocumentTemplate(documentTemplateId)
					?: throw ResponseStatusException(HttpStatus.NOT_FOUND, "Document not found")
			document.attachment?.let {
				emit(DefaultDataBufferFactory().wrap(it))
			} ?: throw ResponseStatusException(HttpStatus.NOT_FOUND, "AttachmentDto not found")
		}.injectReactorContext(),
	)

	@Operation(summary = "Creates a document's attachment")
	@PutMapping("/{documentTemplateId}/attachment", consumes = [MediaType.APPLICATION_OCTET_STREAM_VALUE])
	fun setDocumentTemplateAttachment(
		@PathVariable documentTemplateId: String,
		@RequestBody payload: ByteArray,
	) = mono {
		val documentTemplate =
			documentTemplateService.getDocumentTemplate(documentTemplateId)
				?: throw ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Document modification failed")
		documentTemplateService.modifyDocumentTemplate(documentTemplate.copy(attachment = payload))?.let { documentTemplateV2Mapper.map(it) }
	}

	@Operation(summary = "Creates a document's attachment")
	@PutMapping("/{documentTemplateId}/attachmentJson", consumes = [MediaType.APPLICATION_OCTET_STREAM_VALUE])
	fun setDocumentTemplateAttachmentJson(
		@PathVariable documentTemplateId: String,
		@RequestBody payload: ByteArrayDto,
	) = mono {
		val documentTemplate =
			documentTemplateService.getDocumentTemplate(documentTemplateId)
				?: throw ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Document modification failed")
		documentTemplateService.modifyDocumentTemplate(documentTemplate.copy(attachment = payload.data))?.let { documentTemplateV2Mapper.map(it) }
	}
}
