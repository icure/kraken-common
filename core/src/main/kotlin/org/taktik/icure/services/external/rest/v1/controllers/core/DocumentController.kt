/*
 * Copyright (c) 2020. Taktik SA, All rights reserved.
 */

package org.taktik.icure.services.external.rest.v1.controllers.core

import com.fasterxml.jackson.databind.ObjectMapper
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.tags.Tag
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.reactive.asFlow
import kotlinx.coroutines.reactor.mono
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.context.annotation.Profile
import org.springframework.core.io.buffer.DataBuffer
import org.springframework.core.io.buffer.DefaultDataBufferFactory
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.codec.multipart.FilePart
import org.springframework.http.codec.multipart.Part
import org.springframework.http.server.reactive.ServerHttpResponse
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RequestPart
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.server.ResponseStatusException
import org.taktik.couchdb.entity.IdAndRev
import org.taktik.icure.asynclogic.objectstorage.DataAttachmentChange
import org.taktik.icure.asynclogic.objectstorage.DocumentDataAttachmentLoader
import org.taktik.icure.asynclogic.objectstorage.contentFlowOfNullable
import org.taktik.icure.asyncservice.DocumentService
import org.taktik.icure.config.SharedPaginationConfig
import org.taktik.icure.domain.BatchUpdateDocumentInfo
import org.taktik.icure.entities.Document
import org.taktik.icure.entities.embed.DocumentType
import org.taktik.icure.exceptions.NotFoundRequestException
import org.taktik.icure.exceptions.objectstorage.ObjectStorageException
import org.taktik.icure.security.CryptoUtils
import org.taktik.icure.security.CryptoUtils.isValidAesKey
import org.taktik.icure.security.CryptoUtils.tryKeyFromHexString
import org.taktik.icure.services.external.rest.v1.dto.DocumentDto
import org.taktik.icure.services.external.rest.v1.dto.IcureStubDto
import org.taktik.icure.services.external.rest.v1.dto.ListOfIdsDto
import org.taktik.icure.services.external.rest.v1.dto.requests.document.BulkAttachmentUpdateOptions
import org.taktik.icure.services.external.rest.v1.mapper.DocumentMapper
import org.taktik.icure.services.external.rest.v1.mapper.StubMapper
import org.taktik.icure.services.external.rest.v1.mapper.embed.DelegationMapper
import org.taktik.icure.utils.injectReactorContext
import org.taktik.icure.utils.toByteArray
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

@RestController
@Profile("app")
@RequestMapping("/rest/v1/document")
@Tag(name = "document")
class DocumentController(
	private val documentService: DocumentService,
	private val documentMapper: DocumentMapper,
	private val delegationMapper: DelegationMapper,
	private val stubMapper: StubMapper,
	private val paginationConfig: SharedPaginationConfig,
	private val objectMapper: ObjectMapper,
	@Qualifier("documentDataAttachmentLoader") private val attachmentLoader: DocumentDataAttachmentLoader
) {

	@Operation(summary = "Create a document", description = "Creates a document and returns an instance of created document afterward")
	@PostMapping
	fun createDocument(
		@RequestBody documentDto: DocumentDto,
		@RequestParam(required = false) strict: Boolean = false
	): Mono<DocumentDto> = mono {
		val document = documentMapper.map(documentDto)
		val createdDocument = documentService.createDocument(document, strict)
			?: throw ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Document creation failed")
		documentMapper.map(createdDocument)
	}

	@Operation(summary = "Delete a document", description = "Deletes a batch of documents and returns the list of deleted document ids")
	@DeleteMapping("/{documentIds}")
	fun deleteDocument(@PathVariable documentIds: String) = flow {
		val documentIdsList = documentIds.split(',')
		emitAll(documentService.deleteDocuments(documentIdsList.map { IdAndRev(it, null) }))
	}.injectReactorContext()

	@Operation(summary = "Load a document's main attachment", responses = [ApiResponse(responseCode = "200", content = [Content(mediaType = MediaType.APPLICATION_OCTET_STREAM_VALUE, schema = Schema(type = "string", format = "binary"))])])
	@GetMapping("/{documentId}/attachment/{attachmentId}", produces = [MediaType.APPLICATION_OCTET_STREAM_VALUE])
	fun getDocumentAttachment(
		@PathVariable documentId: String,
		@PathVariable attachmentId: String,
		@RequestParam(required = false) enckeys: String?,
		@RequestParam(required = false) fileName: String?,
		response: ServerHttpResponse
	) = getDocumentAttachment(documentId, enckeys, fileName, response)

	@Operation(summary = "Load a document's main attachment", responses = [ApiResponse(responseCode = "200", content = [Content(mediaType = MediaType.APPLICATION_OCTET_STREAM_VALUE, schema = Schema(type = "string", format = "binary"))])])
	@GetMapping("/{documentId}/attachment", produces = [MediaType.APPLICATION_OCTET_STREAM_VALUE])
	fun getDocumentAttachment(
		@PathVariable documentId: String,
		@RequestParam(required = false) enckeys: String?,
		@RequestParam(required = false) fileName: String?,
		response: ServerHttpResponse
	) = response.writeWith(
		flow {
			val document = documentService.getDocument(documentId) ?: throw NotFoundRequestException("Document not found")
			val attachment =
				if (enckeys.isNullOrBlank()) {
					attachmentLoader.contentFlowOfNullable(document, Document::mainAttachment)
				} else {
					attachmentLoader.decryptMainAttachment(document, enckeys)?.let { flowOf(DefaultDataBufferFactory.sharedInstance.wrap(it)) }
				} ?: throw ResponseStatusException(HttpStatus.NOT_FOUND, "AttachmentDto not found")

			response.headers["Content-Type"] = document.mainAttachment?.mimeType ?: "application/octet-stream"
			response.headers["Content-Disposition"] = "attachment; filename=\"${fileName ?: document.name}\""

			emitAll(attachment)
		}.injectReactorContext()
	)

	@Operation(summary = "Delete a document's main attachment", description = "Deletes the main attachment of a document and returns the modified document instance afterward")
	@DeleteMapping("/{documentId}/attachment")
	fun deleteAttachment(
		@PathVariable
		documentId: String,
		@Parameter(description = "Revision of the latest known version of the document. If provided the method will fail with a CONFLICT status code if the current version does not have this revision")
		@RequestParam(required = false)
		rev: String?
	): Mono<DocumentDto> = mono {
		val document = documentService.getDocument(documentId) ?: throw NotFoundRequestException("Document not found")
		checkRevision(rev, document)
		if (document.mainAttachment != null) {
			documentService.updateAttachments(
				document,
				mainAttachmentChange = DataAttachmentChange.Delete
			).let { documentMapper.map(checkNotNull(it) { "Failed to update attachment" }) }
		} else documentMapper.map(document)
	}

	@Operation(summary = "Creates or modifies a document's attachment", description = "Creates a document's attachment and returns the modified document instance afterward")
	@PutMapping("/{documentId}/attachment", consumes = [MediaType.APPLICATION_OCTET_STREAM_VALUE])
	fun setDocumentAttachment(
		@PathVariable
		documentId: String,
		@RequestParam(required = false)
		enckeys: String?,
		@Parameter(description = "Revision of the latest known version of the document. If provided the method will fail with a CONFLICT status code if the current version does not have this revision")
		@RequestParam(required = false)
		rev: String?,
		@RequestParam(required = false)
		@Parameter(description = "Utis for the attachment")
		utis: List<String>?,
		@Schema(type = "string", format = "binary")
		@RequestBody
		payload: Flow<DataBuffer>,
		@RequestHeader(name = HttpHeaders.CONTENT_LENGTH, required = false)
		lengthHeader: Long?
	): Mono<DocumentDto> = doSetDocumentAttachment(documentId, enckeys, rev, utis, lengthHeader?.takeIf { it > 0 }, payload)

	@Operation(summary = "Create or modifies a document's attachment", description = "Creates a document attachment and returns the modified document instance afterward")
	@PutMapping("/attachment", consumes = [MediaType.APPLICATION_OCTET_STREAM_VALUE])
	fun setDocumentAttachmentBody(
		@RequestParam(required = true)
		documentId: String,
		@RequestParam(required = false)
		enckeys: String?,
		@Parameter(description = "Revision of the latest known version of the document. If provided the method will fail with a CONFLICT status code if the current version does not have this revision")
		@RequestParam(required = false)
		rev: String?,
		@RequestParam(required = false)
		@Parameter(description = "Utis for the attachment")
		utis: List<String>?,
		@Schema(type = "string", format = "binary")
		@RequestBody
		payload: Flow<DataBuffer>,
		@RequestHeader(name = HttpHeaders.CONTENT_LENGTH, required = false)
		lengthHeader: Long?
	): Mono<DocumentDto> = doSetDocumentAttachment(documentId, enckeys, rev, utis, lengthHeader?.takeIf { it > 0 }, payload)

	@Operation(summary = "Creates or modifies a document's attachment", description = "Creates a document attachment and returns the modified document instance afterward")
	@PutMapping("/{documentId}/attachment/multipart", consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
	fun setDocumentAttachmentMulti(
		@PathVariable
		documentId: String,
		@RequestParam(required = false)
		enckeys: String?,
		@Parameter(description = "Revision of the latest known version of the document. If provided the method will fail with a CONFLICT status code if the current version does not have this revision")
		@RequestParam(required = false)
		rev: String?,
		@RequestParam(required = false)
		@Parameter(description = "Utis for the attachment")
		utis: List<String>?,
		@RequestPart("attachment")
		payload: Part,
	): Mono<DocumentDto> = doSetDocumentAttachment(
		documentId,
		enckeys,
		rev,
		utis,
		payload.headers().contentLength.takeIf { it > 0 },
		payload.also {
			require(it.headers().contentType != null) {
				"attachment part must specify ${HttpHeaders.CONTENT_TYPE} header."
			}
		}.content().asFlow()
	)

	private fun doSetDocumentAttachment(
		documentId: String,
		enckeys: String?,
		rev: String?,
		utis: List<String>?,
		size: Long?,
		payload: Flow<DataBuffer>
	): Mono<DocumentDto> = mono {
		val validEncryptionKeys = enckeys
			?.takeIf { it.isNotEmpty() }
			?.split(',')
			?.mapNotNull { sfk -> sfk.tryKeyFromHexString()?.takeIf { it.isValidAesKey() } }
		if (enckeys != null && validEncryptionKeys.isNullOrEmpty()) throw ResponseStatusException(
			HttpStatus.BAD_REQUEST,
			"`enckeys` must contain at least a valid aes key"
		)
		val (newPayload, newSize, encrypted) =
			if (validEncryptionKeys?.isNotEmpty() == true) Triple(
				// Encryption should never fail if the key is valid
				CryptoUtils.encryptFlowAES(payload, validEncryptionKeys.first())
					.map { DefaultDataBufferFactory.sharedInstance.wrap(it) },
				size?.let { CryptoUtils.predictAESEncryptedSize(it) },
				true
			) else Triple(payload, size, false)
		val document = documentService.getDocument(documentId) ?: throw NotFoundRequestException("Document not found")
		checkRevision(rev, document)
		val mainAttachmentChange =
			if (newSize != null)
				DataAttachmentChange.CreateOrUpdate(newPayload, newSize, utis, encrypted)
			else
				newPayload.toByteArray(true).let { payloadBytes ->
					DataAttachmentChange.CreateOrUpdate(
						flowOf(DefaultDataBufferFactory.sharedInstance.wrap(payloadBytes)),
						payloadBytes.size.toLong(),
						utis,
						encrypted
					)
				}
		documentService.updateAttachmentsWrappingExceptions(
			document,
			mainAttachmentChange = mainAttachmentChange
		)?.let { documentMapper.map(it) }
	}

	@Operation(summary = "Get a document", description = "Returns the document corresponding to the identifier passed in the request")
	@GetMapping("/{documentId}")
	fun getDocument(@PathVariable documentId: String): Mono<DocumentDto> = mono {
		val document = documentService.getDocument(documentId) ?: throw NotFoundRequestException("Document not found")
		documentMapper.map(document)
	}

	@Operation(summary = "Get a document", description = "Returns the first document corresponding to the externalUuid passed in the request")
	@GetMapping("/externaluuid/{externalUuid}")
	fun getDocumentByExternalUuid(@PathVariable externalUuid: String) = mono {
		val document = documentService.getDocumentsByExternalUuid(externalUuid).sortedByDescending { it.version }.firstOrNull()
			?: throw ResponseStatusException(HttpStatus.NOT_FOUND, "Document not found")
		documentMapper.map(document)
	}

	@Operation(summary = "Get all documents with externalUuid", description = "Returns a list of document corresponding to the externalUuid passed in the request")
	@GetMapping("/externaluuid/{externalUuid}/all")
	fun getDocumentsByExternalUuid(@PathVariable externalUuid: String) = mono {
		documentService.getDocumentsByExternalUuid(externalUuid).map { documentMapper.map(it) }
	}

	@Operation(summary = "Get a batch of document", description = "Returns a list of document corresponding to the identifiers passed in the body")
	@PostMapping("/batch")
	fun getDocuments(@RequestBody documentIds: ListOfIdsDto): Flux<DocumentDto> =
		documentService.getDocuments(documentIds.ids).map { doc -> documentMapper.map(doc) }.injectReactorContext()


	@Operation(summary = "Update a document", description = "Updates the document and returns an instance of the modified document afterward")
	@PutMapping
	fun modifyDocument(@RequestBody documentDto: DocumentDto): Mono<DocumentDto> = mono {
		val prevDoc = kotlin.runCatching { documentService.getDocument(documentDto.id) }.getOrNull()
		val newDocument = documentMapper.map(documentDto)
		(
			if (prevDoc == null) {
				documentService.createDocument(newDocument.copy(rev = null), false)
			} else if (prevDoc.attachmentId != newDocument.attachmentId) {
				documentService.modifyDocument(newDocument,  prevDoc, false).let {
					documentService.updateAttachments(it, mainAttachmentChange = DataAttachmentChange.Delete)
				}
			} else {
				documentService.modifyDocument(newDocument, prevDoc, false)
			}
		)?.let {
			documentMapper.map(it)
		} ?: throw ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Document modification failed")
	}

	@Operation(summary = "Update a batch of documents", description = "Returns the modified documents.")
	@PutMapping("/batch")
	fun modifyDocuments(@RequestBody documentDtos: List<DocumentDto>): Flux<DocumentDto> = flow {
		val previousDocumentsById = documentService.getDocuments(documentDtos.map { it.id }).toList().associateBy { it.id }
		val allNewDocuments = documentDtos.map { documentMapper.map(it) }
		val newDocumentsById = allNewDocuments.associateBy { it.id }
		require(newDocumentsById.size == allNewDocuments.size) {
			"Provided documents can't have duplicate ids"
		}
		documentService.createOrModifyDocuments(
			allNewDocuments.map { newDoc ->
				val prevDoc = previousDocumentsById[newDoc.id]
				if (prevDoc == null) {
					BatchUpdateDocumentInfo(newDoc, null)
				} else {
					BatchUpdateDocumentInfo(
						if (newDoc.attachmentId == prevDoc.attachmentId)
							newDoc
						else
							newDoc.copy(attachmentId = prevDoc.attachmentId),
						prevDoc
					)
				}
			},
			false
		).map {
			val prev = previousDocumentsById[it.id]
			val curr = newDocumentsById.getValue(it.id)
			if (prev != null && prev.attachmentId != curr.attachmentId) {
				// No support for batch attachment update of different documents
				documentService.updateAttachments(it, mainAttachmentChange = DataAttachmentChange.Delete) ?: it
			} else it
		}.map {
			documentMapper.map(it)
		}.collect { emit(it) }
	}.injectReactorContext()

	@Operation(summary = "List documents found By Healthcare Party and secret foreign keys.", description = "Keys must be delimited by comma")
	@GetMapping("/byHcPartySecretForeignKeys")
	fun findDocumentsByHCPartyPatientForeignKeys(
		@RequestParam hcPartyId: String,
		@RequestParam secretFKeys: String
	): Flux<DocumentDto> {

		val secretMessageKeys = secretFKeys.split(',').map { it.trim() }
		val documentList = documentService.listDocumentsByHCPartySecretMessageKeys(hcPartyId, secretMessageKeys)
		return documentList.map { document -> documentMapper.map(document) }.injectReactorContext()
	}

	@Suppress("DEPRECATION")
	@Deprecated("This method cannot include results with secure delegations, use listDocumentIdsByDataOwnerPatientCreated instead")
	@Operation(summary = "List documents found By Healthcare Party and secret foreign keys.")
	@PostMapping("/byHcPartySecretForeignKeys")
	fun findDocumentsByHCPartyPatientForeignKeys(
		@RequestParam hcPartyId: String,
		@RequestBody secretMessageKeys: List<String>,
	): Flux<DocumentDto> {
		val documentList = documentService.listDocumentsByHCPartySecretMessageKeys(hcPartyId, secretMessageKeys)
		return documentList.map { document -> documentMapper.map(document) }.injectReactorContext()
	}

	@Operation(summary = "List documents found By type, By Healthcare Party and secret foreign keys.", description = "Keys must be delimited by comma")
	@GetMapping("/byTypeHcPartySecretForeignKeys")
	fun findByTypeHCPartyMessageSecretFKeys(
		@RequestParam documentTypeCode: String,
		@RequestParam hcPartyId: String,
		@RequestParam secretFKeys: String
	): Flux<DocumentDto> {
		if (DocumentType.fromName(documentTypeCode) == null) {
			throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid documentTypeCode.")
		}

		val secretMessageKeys = secretFKeys.split(',').map { it.trim() }
		val documentList = documentService.listDocumentsByDocumentTypeHCPartySecretMessageKeys(documentTypeCode, hcPartyId, secretMessageKeys)

		return documentList.map { document -> documentMapper.map(document) }.injectReactorContext()
	}

	@Operation(summary = "List documents with no delegation")
	@GetMapping("/woDelegation")
	fun findWithoutDelegation(@RequestParam(required = false) limit: Int?): Flux<DocumentDto> {
		val documentList = documentService.listDocumentsWithoutDelegation(limit ?: 100)
		return documentList.map { document -> documentMapper.map(document) }.injectReactorContext()
	}

	@Operation(summary = "Update delegations in a document.", description = "Keys must be delimited by comma")
	@PostMapping("/delegations")
	fun setDocumentsDelegations(@RequestBody stubs: List<IcureStubDto>) = flow {
		val stubsById = stubs.associateBy { it.id }
		val invoices = documentService.getDocuments(stubs.map { it.id }).map { document ->
			stubsById.getValue(document.id).let { stub ->
				document.copy(
					delegations = document.delegations.mapValues { (s, dels) -> stub.delegations[s]?.map { delegationMapper.map(it) }?.toSet() ?: dels } +
						stub.delegations.filterKeys { k -> !document.delegations.containsKey(k) }.mapValues { (_, value) -> value.map { delegationMapper.map(it) }.toSet() },
					encryptionKeys = document.encryptionKeys.mapValues { (s, dels) -> stub.encryptionKeys[s]?.map { delegationMapper.map(it) }?.toSet() ?: dels } +
						stub.encryptionKeys.filterKeys { k -> !document.encryptionKeys.containsKey(k) }.mapValues { (_, value) -> value.map { delegationMapper.map(it) }.toSet() },
					cryptedForeignKeys = document.cryptedForeignKeys.mapValues { (s, dels) -> stub.cryptedForeignKeys[s]?.map { delegationMapper.map(it) }?.toSet() ?: dels } +
						stub.cryptedForeignKeys.filterKeys { k -> !document.cryptedForeignKeys.containsKey(k) }.mapValues { (_, value) -> value.map { delegationMapper.map(it) }.toSet() },
				)
			}.let { newDocument -> BatchUpdateDocumentInfo(newDocument, document) }
		}
		emitAll(documentService.createOrModifyDocuments(invoices.toList(), true).map { stubMapper.mapToStub(it) })
	}.injectReactorContext()

	@Operation(summary = "Creates or modifies a secondary attachment for a document", description = "Creates a secondary attachment for a document and returns the modified document instance afterward")
	@PutMapping("/{documentId}/secondaryAttachments/{key}", consumes = [MediaType.APPLICATION_OCTET_STREAM_VALUE])
	fun setSecondaryAttachment(
		@PathVariable
		documentId: String,
		@Parameter(description = "Key of the secondary attachment to update")
		@PathVariable
		key: String,
		@Parameter(description = "Revision of the latest known version of the document. If the revision does not match the current version of the document the method will fail with CONFLICT status")
		@RequestParam(required = true)
		rev: String,
		@RequestParam(required = false)
		@Parameter(description = "Utis for the attachment")
		utis: List<String>?,
		@Schema(type = "string", format = "binary")
		@RequestBody
		payload: Flow<DataBuffer>,
		@RequestHeader(name = HttpHeaders.CONTENT_LENGTH, required = false)
		lengthHeader: Long?
	): Mono<DocumentDto> = mono {
		val attachmentSize = lengthHeader ?: throw ResponseStatusException(
			HttpStatus.BAD_REQUEST,
			"Attachment size must be specified in the content-length header"
		)
		documentService.updateAttachmentsWrappingExceptions(
			documentService.getDocument(documentId)?.also {
				checkRevision(rev, it)
				require(key != it.mainAttachmentKey) {
					"Secondary attachments can't use $key as key: this key is reserved for the main attachment."
				}
			} ?: throw NotFoundRequestException("Document not found"),
			secondaryAttachmentsChanges = mapOf(
				key to DataAttachmentChange.CreateOrUpdate(
					payload,
					attachmentSize,
					utis,
					false
				)
			)
		).let { documentMapper.map(checkNotNull(it) { "Could not update document" }) }
	}

	@Operation(summary = "Retrieve a secondary attachment of a document", description = "Get the secondary attachment with the provided key for a document")
	@GetMapping("/{documentId}/secondaryAttachments/{key}")
	fun getSecondaryAttachment(
		@PathVariable
		documentId: String,
		@Parameter(description = "Key of the secondary attachment to retrieve")
		@PathVariable
		key: String,
		@RequestParam(required = false)
		fileName: String?,
		response: ServerHttpResponse
	) = response.writeWith(
		flow {
			val document = documentService.getDocument(documentId)?.also {
				require(key != it.mainAttachmentKey) {
					"Secondary attachments can't use $key as key: this key is reserved for the main attachment."
				}
			} ?: throw NotFoundRequestException("Document not found")
			val attachment = attachmentLoader.contentFlowOfNullable(document, key) ?: throw ResponseStatusException(
				HttpStatus.NOT_FOUND,
				"No secondary attachment with key $key for document $documentId"
			)

			response.headers["Content-Type"] = document.mainAttachment?.mimeType ?: "application/octet-stream"
			response.headers["Content-Disposition"] = "attachment; filename=\"${fileName ?: document.name}\""

			emitAll(attachment)
		}.injectReactorContext()
	)

	@Operation(summary = "Deletes a secondary attachment of a document", description = "Delete the secondary attachment with the provided key for a document")
	@DeleteMapping("/{documentId}/secondaryAttachments/{key}")
	fun deleteSecondaryAttachment(
		@PathVariable
		documentId: String,
		@Parameter(description = "Key of the secondary attachment to retrieve")
		@PathVariable
		key: String,
		@Parameter(description = "Revision of the latest known version of the document. If the revision does not match the current version of the document the method will fail with CONFLICT status")
		@RequestParam(required = true)
		rev: String,
	): Mono<DocumentDto> = mono {
		documentService.updateAttachments(
			documentService.getDocument(documentId)?.also {
				checkRevision(rev, it)
				require(key != it.mainAttachmentKey) {
					"Secondary attachments can't use $key as key: this key is reserved for the main attachment."
				}
			} ?: throw NotFoundRequestException("Document not found"),
			secondaryAttachmentsChanges = mapOf(key to DataAttachmentChange.Delete)
		).let { documentMapper.map(checkNotNull(it) { "Could not update document" }) }
	}

	@Operation(
		summary = "Creates, modifies, or delete the attachments of a document",
		description = "Batch operation to modify multiple attachments of a document at once"
	)
	@PutMapping("/{documentId}/attachments", consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
	fun setDocumentAttachments(
		@Parameter(description = "Id of the document to update")
		@PathVariable
		documentId: String,
		@Parameter(description = "Revision of the latest known version of the document. If the revision does not match the current version of the document the method will fail with CONFLICT status")
		@RequestParam(required = true)
		rev: String,
		@Parameter(description = "Describes the operations to execute with this update.")
		@RequestPart("options", required = true)
		options: BulkAttachmentUpdateOptions,
		@Parameter(description = "New attachments (to create or update). The file name will be used as the attachment key. To update the main attachment use the document id")
		@RequestPart("attachments", required = false)
		attachments: Flux<FilePart>?
	): Mono<DocumentDto> = mono {
		val attachmentsByKey: Map<String, FilePart> = attachments?.asFlow()?.toList()?.let { partsList ->
			partsList.associateBy { it.filename() }.also { partsMap ->
				require(partsList.size == partsMap.size) {
					"Duplicate keys for new attachments ${partsList.groupingBy { it.filename() }.eachCount().filter { it.value > 1 }.keys}"
				}
			}
		} ?: emptyMap()
		require(attachmentsByKey.values.all { it.headers().contentType != null }) {
			"Each attachment part must specify a ${HttpHeaders.CONTENT_TYPE} header."
		}
		require(attachmentsByKey.keys.containsAll(options.updateAttachmentsMetadata.keys)) {
			"Missing attachments for metadata: ${options.updateAttachmentsMetadata.keys - attachmentsByKey.keys}"
		}
		require(attachmentsByKey.isNotEmpty() || options.deleteAttachments.isNotEmpty()) { "Nothing to do" }
		val document = documentService.getDocument(documentId) ?: throw NotFoundRequestException("Document not found")
		checkRevision(rev, document)
		val mainAttachmentChange = attachmentsByKey[document.mainAttachmentKey]?.let {
			makeMultipartAttachmentUpdate("main attachment", it, options.updateAttachmentsMetadata[document.mainAttachmentKey])
		} ?: DataAttachmentChange.Delete.takeIf { document.mainAttachmentKey in options.deleteAttachments }
		val secondaryAttachmentsChanges = (options.deleteAttachments - document.mainAttachmentKey).associateWith { DataAttachmentChange.Delete } +
			(attachmentsByKey - document.mainAttachmentKey).mapValues { (key, value) ->
				makeMultipartAttachmentUpdate("secondary attachment $key", value, options.updateAttachmentsMetadata[key])
			}
		documentService.updateAttachmentsWrappingExceptions(document, mainAttachmentChange, secondaryAttachmentsChanges)
			.let { documentMapper.map(checkNotNull(it) { "Could not update document" }) }
	}

	private fun makeMultipartAttachmentUpdate(name: String, part: FilePart, metadata: BulkAttachmentUpdateOptions.AttachmentMetadata?) =
		DataAttachmentChange.CreateOrUpdate(
			part.content().asFlow(),
			part.headers().contentLength.takeIf { it > 0 } ?: throw ResponseStatusException(
				HttpStatus.BAD_REQUEST,
				"Missing size information for $name: you must provide the size of the attachment in bytes using a Content-Length part header."
			),
			metadata?.utis,
			false
		)

	private fun checkRevision(rev: String?, document: Document) {
		if (rev != null && rev != document.rev) throw ResponseStatusException(
			HttpStatus.CONFLICT,
			"Obsolete document revision. The current revision is ${document.rev}"
		)
	}


	private suspend fun DocumentService.updateAttachmentsWrappingExceptions(
		currentDocument: Document,
		mainAttachmentChange: DataAttachmentChange? = null,
		secondaryAttachmentsChanges: Map<String, DataAttachmentChange> = emptyMap()
	): Document? =
		try {
			updateAttachments(currentDocument, mainAttachmentChange, secondaryAttachmentsChanges)
		} catch (e: ObjectStorageException) {
			throw ResponseStatusException(
				HttpStatus.SERVICE_UNAVAILABLE,
				"One or more attachments must be stored using the object storage service, but the service is currently unavailable."
			)
		}
}