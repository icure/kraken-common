/*
 * Copyright (c) 2020. Taktik SA, All rights reserved.
 */

package org.taktik.icure.services.external.rest.v2.controllers.core

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.tags.Tag
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.reactive.asFlow
import kotlinx.coroutines.reactor.mono
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.context.annotation.Profile
import org.springframework.core.io.buffer.DataBuffer
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.MediaType.APPLICATION_JSON_VALUE
import org.springframework.http.codec.multipart.FilePart
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
import org.taktik.couchdb.DocIdentifier
import org.taktik.couchdb.entity.IdAndRev
import org.taktik.icure.asynclogic.objectstorage.DataAttachmentChange
import org.taktik.icure.asynclogic.objectstorage.DocumentDataAttachmentLoader
import org.taktik.icure.asynclogic.objectstorage.contentFlowOfNullable
import org.taktik.icure.asyncservice.DocumentService
import org.taktik.icure.cache.ReactorCacheInjector
import org.taktik.icure.entities.Document
import org.taktik.icure.entities.embed.DocumentType
import org.taktik.icure.exceptions.NotFoundRequestException
import org.taktik.icure.exceptions.objectstorage.ObjectStorageException
import org.taktik.icure.services.external.rest.v2.dto.DocumentDto
import org.taktik.icure.services.external.rest.v2.dto.ListOfIdsAndRevDto
import org.taktik.icure.services.external.rest.v2.dto.ListOfIdsDto
import org.taktik.icure.services.external.rest.v2.dto.couchdb.DocIdentifierDto
import org.taktik.icure.services.external.rest.v2.dto.filter.AbstractFilterDto
import org.taktik.icure.services.external.rest.v2.dto.requests.BulkShareOrUpdateMetadataParamsDto
import org.taktik.icure.services.external.rest.v2.dto.requests.EntityBulkShareResultDto
import org.taktik.icure.services.external.rest.v2.dto.requests.document.BulkAttachmentUpdateOptions
import org.taktik.icure.services.external.rest.v2.mapper.DocumentV2Mapper
import org.taktik.icure.services.external.rest.v2.mapper.IdWithRevV2Mapper
import org.taktik.icure.services.external.rest.v2.mapper.couchdb.DocIdentifierV2Mapper
import org.taktik.icure.services.external.rest.v2.mapper.filter.FilterV2Mapper
import org.taktik.icure.services.external.rest.v2.mapper.requests.DocumentBulkShareResultV2Mapper
import org.taktik.icure.services.external.rest.v2.mapper.requests.EntityShareOrMetadataUpdateRequestV2Mapper
import org.taktik.icure.utils.injectCachedReactorContext
import org.taktik.icure.utils.injectReactorContext
import org.taktik.icure.utils.orThrow
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

@RestController("documentControllerV2")
@Profile("app")
@RequestMapping("/rest/v2/document")
@Tag(name = "document")
class DocumentController(
	private val documentService: DocumentService,
	private val documentV2Mapper: DocumentV2Mapper,
	@Qualifier("documentDataAttachmentLoader") private val attachmentLoader: DocumentDataAttachmentLoader,
	private val bulkShareResultV2Mapper: DocumentBulkShareResultV2Mapper,
	private val filterV2Mapper: FilterV2Mapper,
	private val entityShareOrMetadataUpdateRequestV2Mapper: EntityShareOrMetadataUpdateRequestV2Mapper,
	private val docIdentifierV2Mapper: DocIdentifierV2Mapper,
	private val idWithRevV2Mapper: IdWithRevV2Mapper,
	private val reactorCacheInjector: ReactorCacheInjector
) {

	@Operation(summary = "Create a document", description = "Creates a document and returns an instance of created document afterward")
	@PostMapping
	fun createDocument(
		@RequestBody documentDto: DocumentDto,
		@RequestParam(required = false) strict: Boolean? = null
	): Mono<DocumentDto> = mono {
		val document = documentV2Mapper.map(documentDto)
		val createdDocument = documentService.createDocument(document, strict ?: true)
			?: throw ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Document creation failed")
		documentV2Mapper.map(createdDocument)
	}

	@Operation(summary = "Deletes multiple Documents")
	@PostMapping("/delete/batch")
	fun deleteDocuments(@RequestBody documentIds: ListOfIdsDto): Flux<DocIdentifierDto> =
		documentService.deleteDocuments(
			documentIds.ids.map { IdAndRev(it, null) }
		).map { docIdentifierV2Mapper.map(DocIdentifier(it.id, it.rev)) }.injectReactorContext()

	@Operation(summary = "Deletes a multiple Documents if they match the provided revs")
	@PostMapping("/delete/batch/withrev")
	fun deleteDocumentsWithRev(@RequestBody documentIds: ListOfIdsAndRevDto): Flux<DocIdentifierDto> =
		documentService.deleteDocuments(
			documentIds.ids.map(idWithRevV2Mapper::map)
		).map { docIdentifierV2Mapper.map(DocIdentifier(it.id, it.rev)) }.injectReactorContext()

	@Operation(summary = "Deletes an Document")
	@DeleteMapping("/{documentId}")
	fun deleteDocument(
		@PathVariable documentId: String,
		@RequestParam(required = false) rev: String? = null
	): Mono<DocIdentifierDto> = mono {
		documentService.deleteDocument(documentId, rev).let {
			docIdentifierV2Mapper.map(DocIdentifier(it.id, it.rev))
		}
	}

	@PostMapping("/undelete/{documentId}")
	fun undeleteDocument(
		@PathVariable documentId: String,
		@RequestParam(required=true) rev: String
	): Mono<DocumentDto> = mono {
		documentV2Mapper.map(documentService.undeleteDocument(documentId, rev))
	}

	@DeleteMapping("/purge/{documentId}")
	fun purgeDocument(
		@PathVariable documentId: String,
		@RequestParam(required=true) rev: String
	): Mono<DocIdentifierDto> = mono {
		documentService.purgeDocument(documentId, rev).let(docIdentifierV2Mapper::map)
	}

	@Operation(summary = "Load the main attachment of a document", responses = [ApiResponse(responseCode = "200", content = [Content(mediaType = MediaType.APPLICATION_OCTET_STREAM_VALUE, schema = Schema(type = "string", format = "binary"))])])
	@GetMapping("/{documentId}/attachment", produces = [MediaType.APPLICATION_OCTET_STREAM_VALUE])
	fun getMainAttachment(
		@PathVariable documentId: String,
		@RequestParam(required = false) fileName: String?,
		response: ServerHttpResponse
	) = response.writeWith(
		flow {
			val document = documentService.getDocument(documentId) ?: throw NotFoundRequestException("No document with id $documentId")
			val attachment = documentService.getMainAttachment(documentId)

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
		@Parameter(description = "Revision of the latest known version of the document. If it doesn't match the current revision the method will fail with CONFLICT.")
		@RequestParam(required = true)
		rev: String
	): Mono<DocumentDto> = reactorCacheInjector.monoWithCachedContext(10) {
		documentService.updateAttachments(
			documentId,
			rev,
			mainAttachmentChange = DataAttachmentChange.Delete
		).let { documentV2Mapper.map(checkNotNull(it) { "Failed to update attachment" }) }
	}

	@Operation(summary = "Creates or updates the main attachment of a document")
	@PutMapping("/{documentId}/attachment", consumes = [MediaType.APPLICATION_OCTET_STREAM_VALUE])
	fun setDocumentAttachment(
		@PathVariable
		documentId: String,
		@Parameter(description = "Revision of the latest known version of the document. If it doesn't match the current revision the method will fail with CONFLICT.")
		@RequestParam(required = true)
		rev: String,
		@RequestParam(required = false)
		@Parameter(description = "Utis for the attachment")
		utis: List<String>?,
		@Schema(type = "string", format = "binary")
		@RequestBody
		payload: Flow<DataBuffer>,
		@RequestHeader(name = HttpHeaders.CONTENT_LENGTH, required = false)
		lengthHeader: Long?,
		@RequestParam(required = false)
		encrypted: Boolean?
	): Mono<DocumentDto> = reactorCacheInjector.monoWithCachedContext(10) {
		val payloadSize = requireNotNull(lengthHeader?.takeIf { it > 0 }) {
			"The `Content-Length` header must contain the payload size"
		}
		documentService.updateAttachmentsWrappingExceptions(
			documentId,
			rev,
			mainAttachmentChange = DataAttachmentChange.CreateOrUpdate(payload, payloadSize, utis, encrypted ?: false)
		)?.let { documentV2Mapper.map(it) }
	}

	@Operation(summary = "Gets a document")
	@GetMapping("/{documentId}")
	fun getDocument(@PathVariable documentId: String) = mono {
		val document = documentService.getDocument(documentId)
			?: throw ResponseStatusException(HttpStatus.NOT_FOUND, "Document not found")
		documentV2Mapper.map(document)
	}

	@Operation(summary = "Gets a document")
	@GetMapping("/externaluuid/{externalUuid}")
	fun getDocumentByExternalUuid(@PathVariable externalUuid: String) = mono {
		val document = documentService.getDocumentsByExternalUuid(externalUuid).firstOrNull()
			?: throw ResponseStatusException(HttpStatus.NOT_FOUND, "Document not found")
		documentV2Mapper.map(document)
	}

	@Operation(summary = "Get all documents with externalUuid")
	@GetMapping("/externaluuid/{externalUuid}/all")
	fun getDocumentsByExternalUuid(@PathVariable externalUuid: String) = mono {
		documentService.getDocumentsByExternalUuid(externalUuid).map { documentV2Mapper.map(it) }
	}

	@Operation(summary = "Get one or more documents by their ids.")
	@PostMapping("/byIds")
	fun getDocuments(@RequestBody documentIds: ListOfIdsDto): Flux<DocumentDto> {
		require(documentIds.ids.isNotEmpty()) { "You must specify at least one id." }
		return documentService
			.getDocuments(documentIds.ids)
			.map(documentV2Mapper::map)
			.injectReactorContext()
	}

	@Operation(summary = "Updates a document")
	@PutMapping
	fun modifyDocument(@RequestBody documentDto: DocumentDto): Mono<DocumentDto> = mono {
		val prevDoc = documentService.getDocument(documentDto.id) ?: throw NotFoundRequestException("No document with id ${documentDto.id}")
		val newDocument = documentV2Mapper.map(documentDto)
		documentService.modifyDocument(newDocument, prevDoc, true).let { documentV2Mapper.map(it) }
	}

	@Operation(summary = "Updates a batch of documents", description = "Returns the modified documents.")
	@PutMapping("/batch")
	fun modifyDocuments(@RequestBody documentDtos: List<DocumentDto>): Flux<DocumentDto> = flow {
		documentService.modifyDocuments(documentDtos.map(documentV2Mapper::map)).collect { emit(documentV2Mapper.map(it)) }
	}.injectReactorContext()

	@Suppress("DEPRECATION")
	@Deprecated("This method is inefficient for high volumes of keys, use listDocumentIdsByDataOwnerPatientCreated instead")
	@Operation(summary = "List documents found by healthcare party and secret foreign keys.")
	@PostMapping("/byHcPartySecretForeignKeys")
	fun listDocumentsByHcPartyMessageForeignKeys(
		@RequestParam hcPartyId: String,
		@RequestParam(required = false) documentTypeCode: String?,
		@RequestBody secretMessageKeys: ListOfIdsDto,
	): Flux<DocumentDto> {
		val documentList = documentTypeCode?.let {
			if (DocumentType.fromName(documentTypeCode) == null) {
				throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid documentTypeCode.")
			}
			documentService.listDocumentsByDocumentTypeHCPartySecretMessageKeys(documentTypeCode, hcPartyId, secretMessageKeys.ids)
		} ?: documentService.listDocumentsByHCPartySecretMessageKeys(hcPartyId, secretMessageKeys.ids)
		return documentList.map { document -> documentV2Mapper.map(document) }.injectReactorContext()
	}

	@Operation(summary = "Find Document ids by data owner id, patient secret keys and creation date.")
	@PostMapping("/byDataOwnerPatientCreated", produces = [APPLICATION_JSON_VALUE])
	fun listDocumentIdsByDataOwnerPatientCreated(
		@RequestParam dataOwnerId: String,
		@RequestParam(required = false) startDate: Long?,
		@RequestParam(required = false) endDate: Long?,
		@RequestParam(required = false) descending: Boolean?,
		@RequestBody secretPatientKeys: ListOfIdsDto
	): Flux<String> {
		require(secretPatientKeys.ids.isNotEmpty()) {
			"You need to provide at least one secret patient key"
		}
		return documentService
			.listDocumentIdsByDataOwnerPatientCreated(
				dataOwnerId = dataOwnerId,
				secretForeignKeys = secretPatientKeys.ids.toSet(),
				startDate = startDate,
				endDate = endDate,
				descending = descending ?: false)
			.injectReactorContext()
	}

	@Operation(summary = "List documents with no delegation")
	@GetMapping("/woDelegation")
	fun findWithoutDelegation(@RequestParam(required = false) limit: Int?): Flux<DocumentDto> {
		val documentList = documentService.listDocumentsWithoutDelegation(limit ?: 100)
		return documentList.map { document -> documentV2Mapper.map(document) }.injectReactorContext()
	}

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
		lengthHeader: Long?,
		@RequestParam(required = false)
		encrypted: Boolean?
	): Mono<DocumentDto> = reactorCacheInjector.monoWithCachedContext(10) {
		val attachmentSize = lengthHeader ?: throw ResponseStatusException(
			HttpStatus.BAD_REQUEST,
			"Attachment size must be specified in the content-length header"
		)
		documentService.updateAttachmentsWrappingExceptions(
			documentId,
			rev,
			secondaryAttachmentsChanges = mapOf(
				key to DataAttachmentChange.CreateOrUpdate(
					payload,
					attachmentSize,
					utis,
					encrypted ?: false
				)
			)
		).let { documentV2Mapper.map(checkNotNull(it) { "Could not update document" }) }
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
			} ?: throw NotFoundRequestException("No document with id $documentId")
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
	): Mono<DocumentDto> = reactorCacheInjector.monoWithCachedContext(10) {
		documentService.updateAttachments(
			documentId,
			rev,
			secondaryAttachmentsChanges = mapOf(key to DataAttachmentChange.Delete)
		).let { documentV2Mapper.map(checkNotNull(it) { "Could not update document" }) }
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
	): Mono<DocumentDto> = reactorCacheInjector.monoWithCachedContext(10) {
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
		val mainAttachmentKey = Document.mainAttachmentKeyFromId(documentId)
		val mainAttachmentChange = attachmentsByKey[mainAttachmentKey]?.let {
			makeMultipartAttachmentUpdate("main attachment", it, options.updateAttachmentsMetadata[mainAttachmentKey])
		} ?: DataAttachmentChange.Delete.takeIf { mainAttachmentKey in options.deleteAttachments }
		val secondaryAttachmentsChanges = (options.deleteAttachments - mainAttachmentKey).associateWith { DataAttachmentChange.Delete } +
			(attachmentsByKey - mainAttachmentKey).mapValues { (key, value) ->
				makeMultipartAttachmentUpdate("secondary attachment $key", value, options.updateAttachmentsMetadata[key])
			}
		documentService.updateAttachmentsWrappingExceptions(documentId, rev, mainAttachmentChange, secondaryAttachmentsChanges)
			.let { documentV2Mapper.map(checkNotNull(it) { "Could not update document" }) }
	}

	private fun makeMultipartAttachmentUpdate(name: String, part: FilePart, metadata: BulkAttachmentUpdateOptions.AttachmentMetadata?) =
		DataAttachmentChange.CreateOrUpdate(
			part.content().asFlow(),
			part.headers().contentLength.takeIf { it > 0 } ?: throw ResponseStatusException(
				HttpStatus.BAD_REQUEST,
				"Missing size information for $name: you must provide the size of the attachment in bytes using a Content-Length part header."
			),
			metadata?.utis,
			metadata?.dataIsEncrypted ?: false
		)

	private fun checkRevision(rev: String, document: Document) {
		if (rev != document.rev) throw ResponseStatusException(
			HttpStatus.CONFLICT,
			"Obsolete document revision. The current revision is ${document.rev}"
		)
	}

	private suspend fun DocumentService.updateAttachmentsWrappingExceptions(
		documentId: String,
		documentRev: String,
		mainAttachmentChange: DataAttachmentChange? = null,
		secondaryAttachmentsChanges: Map<String, DataAttachmentChange> = emptyMap(),
	): Document? =
		try {
			updateAttachments(documentId, documentRev, mainAttachmentChange, secondaryAttachmentsChanges)
		} catch (e: ObjectStorageException) {
			throw ResponseStatusException(
				HttpStatus.SERVICE_UNAVAILABLE,
				"One or more attachments must be stored using the object storage service, but the service is currently unavailable."
			)
		}

	@Operation(description = "Shares one or more documents with one or more data owners.")
	@PutMapping("/bulkSharedMetadataUpdate")
	fun bulkShare(
		@RequestBody request: BulkShareOrUpdateMetadataParamsDto
	): Flux<EntityBulkShareResultDto<DocumentDto>> = flow {
		emitAll(documentService.bulkShareOrUpdateMetadata(
			entityShareOrMetadataUpdateRequestV2Mapper.map(request)
		).map { bulkShareResultV2Mapper.map(it) })
	}.injectCachedReactorContext(reactorCacheInjector, 50)

	@Operation(description = "Shares one or more documents with one or more data owners but does not return the updated entity.")
	@PutMapping("/bulkSharedMetadataUpdateMinimal")
	fun bulkShareMinimal(
		@RequestBody request: BulkShareOrUpdateMetadataParamsDto
	): Flux<EntityBulkShareResultDto<Nothing>> = flow {
		emitAll(documentService.bulkShareOrUpdateMetadata(
			entityShareOrMetadataUpdateRequestV2Mapper.map(request)
		).map { bulkShareResultV2Mapper.map(it).minimal() })
	}.injectCachedReactorContext(reactorCacheInjector, 50)

	@Operation(summary = "Get the ids of the Documents matching the provided filter.")
	@PostMapping("/match", produces = [APPLICATION_JSON_VALUE])
	fun matchDocumentsBy(
		@RequestBody filter: AbstractFilterDto<DocumentDto>
	) = documentService.matchDocumentsBy(
		filter = filterV2Mapper.tryMap(filter).orThrow()
	).injectReactorContext()
}
