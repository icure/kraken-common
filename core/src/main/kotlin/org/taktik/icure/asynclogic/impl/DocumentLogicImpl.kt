/*
 * Copyright (c) 2020. Taktik SA, All rights reserved.
 */
package org.taktik.icure.asynclogic.impl

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.singleOrNull
import kotlinx.coroutines.flow.toList
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.core.io.buffer.DataBuffer
import org.taktik.couchdb.DocIdentifier
import org.taktik.couchdb.exception.DocumentNotFoundException
import org.taktik.icure.asyncdao.DocumentDAO
import org.taktik.icure.asyncdao.results.filterSuccessfulUpdates
import org.taktik.icure.asynclogic.ConflictResolutionLogic
import org.taktik.icure.asynclogic.DocumentLogic
import org.taktik.icure.asynclogic.ExchangeDataMapLogic
import org.taktik.icure.asynclogic.SessionInformationProvider
import org.taktik.icure.asynclogic.base.impl.EntityWithEncryptionMetadataLogic
import org.taktik.icure.asynclogic.impl.filter.Filters
import org.taktik.icure.asynclogic.objectstorage.DataAttachmentChange
import org.taktik.icure.asynclogic.objectstorage.DocumentDataAttachmentLoader
import org.taktik.icure.asynclogic.objectstorage.DocumentDataAttachmentModificationLogic
import org.taktik.icure.asynclogic.objectstorage.contentFlowOfNullable
import org.taktik.icure.datastore.DatastoreInstanceProvider
import org.taktik.icure.datastore.IDatastoreInformation
import org.taktik.icure.domain.BatchUpdateDocumentInfo
import org.taktik.icure.entities.Document
import org.taktik.icure.entities.embed.SecurityMetadata
import org.taktik.icure.exceptions.NotFoundRequestException
import org.taktik.icure.mergers.generated.DocumentMerger
import org.taktik.icure.validation.aspect.Fixer
import java.nio.ByteBuffer

open class DocumentLogicImpl(
	private val documentDAO: DocumentDAO,
	sessionLogic: SessionInformationProvider,
	datastoreInstanceProvider: DatastoreInstanceProvider,
	exchangeDataMapLogic: ExchangeDataMapLogic,
	private val attachmentModificationLogic: DocumentDataAttachmentModificationLogic,
	@param:Qualifier("documentDataAttachmentLoader") private val attachmentLoader: DocumentDataAttachmentLoader,
	fixer: Fixer,
	filters: Filters,
	documentMerger: DocumentMerger
) : EntityWithEncryptionMetadataLogic<Document, DocumentDAO>(fixer, sessionLogic, datastoreInstanceProvider, exchangeDataMapLogic, filters),
	ConflictResolutionLogic by ConflictResolutionLogicImpl(documentDAO, documentMerger, datastoreInstanceProvider),
	DocumentLogic {
	override suspend fun createDocument(
		document: Document,
		strict: Boolean,
	) = fix(document, isCreate = true) { fixedDocument ->
		val datastoreInformation = getInstanceAndGroup()
		documentDAO.save(datastoreInformation, checkNewDocument(fixedDocument, strict))
	}

	override fun createDocuments(documents: List<Document>): Flow<Document> = flow {
		val datastoreInformation = getInstanceAndGroup()
		emitAll(
			documentDAO.saveBulk(
				datastoreInformation,
				documents.map {
					checkNewDocument(fix(it, isCreate = true), strict = true)
				}
			).filterSuccessfulUpdates()
		)
	}

	override suspend fun getDocument(documentId: String): Document? = getEntity(documentId)

	override suspend fun getDocumentsByExternalUuid(documentId: String): List<Document> {
		val datastoreInformation = getInstanceAndGroup()
		return documentDAO.listDocumentsByExternalUuid(datastoreInformation, documentId)
	}

	override fun getDocuments(documentIds: Collection<String>): Flow<Document> = getEntities(documentIds)

	override suspend fun getMainAttachment(documentId: String): Flow<DataBuffer> {
		val document = getDocument(documentId) ?: throw NotFoundRequestException("No document with id $documentId")
		return getMainAttachment(document)
	}

	override suspend fun getMainAttachment(document: Document): Flow<DataBuffer> = attachmentLoader.contentFlowOfNullable(document, Document::mainAttachment)
		?: throw DocumentNotFoundException("Main attachment not found")

	override fun getAttachment(
		documentId: String,
		attachmentId: String,
	): Flow<ByteBuffer> = flow {
		val datastoreInformation = getInstanceAndGroup()
		emitAll(
			getDocument(documentId)?.let {
				documentDAO.getAttachment(datastoreInformation, documentId, attachmentId)
			} ?: emptyFlow(),
		)
	}

	override fun createEntities(entities: Flow<Document>): Flow<Document> = flow {
		emitAll(createEntities(entities.toList()))
	}

	override fun createEntities(entities: Collection<Document>): Flow<Document> = flow {
		val datastoreInformation = getInstanceAndGroup()

		emitAll(
			documentDAO
				.saveBulk(
					datastoreInformation,
					entities.onEach {
						checkValidityForCreation(it)
					}.mapNotNull {
						runCatching {
							checkNewDocument(fix(it, isCreate = true), true)
						}.getOrNull()
					},
				).filterSuccessfulUpdates(),
		)
	}

	override fun modifyEntities(entities: Flow<Document>): Flow<Document> = flow {
		emitAll(modifyEntities(entities.toList()))
	}

	override fun modifyEntities(entities: Collection<Document>): Flow<Document> = flow {
		val datastoreInformation = getInstanceAndGroup()
		entities.onEach { checkValidityForModification(it) }
		emitAll(
			doModifyDocuments(
				entities.toList(),
				datastoreInformation,
			) { fix(it, isCreate = false) }
		)
	}

	protected fun ensureValidAttachmentChanges(
		updatedDocument: Document,
		baseline: Document,
		strict: Boolean,
	): Document = updatedDocument.copy(attachments = baseline.attachments).let {
		attachmentModificationLogic.ensureValidAttachmentChanges(
			baseline,
			it,
			if (strict) emptySet() else setOf(updatedDocument.mainAttachmentKey),
		)
	}

	override suspend fun modifyDocument(
		updatedDocument: Document,
		strict: Boolean,
	): Document = fix(updatedDocument, isCreate = false) { newDoc ->
		val datastoreInformation = getInstanceAndGroup()
		checkValidityForModification(newDoc)
		val baseline = requireNotNull(documentDAO.get(datastoreInformation, newDoc.id)) {
			"Attempting to modify a non-existing document ${newDoc.id}."
		}
		require(newDoc.rev == baseline.rev) { "Updated document has an older revision ${newDoc.rev} -> ${baseline.rev}" }
		val validatedEntityForAttachments = ensureValidAttachmentChanges(updatedDocument = newDoc, baseline, strict)
		checkValidEntityChange(validatedEntityForAttachments, currentEntity = baseline)
		documentDAO.save(datastoreInformation, validatedEntityForAttachments)
	}

	protected fun doModifyDocuments(
		documents: List<Document>,
		datastoreInformation: IDatastoreInformation,
		doFix: suspend (document: Document) -> Document
	): Flow<Document>  = flow {
		val originalDocumentsById = documentDAO.getEntities(datastoreInformation, documents.map { it.id }).toList().associateBy { it.id }
		val modifiedDocumentPairedWithOriginal = documents.mapNotNull { mDoc -> originalDocumentsById[mDoc.id]?.let { mDoc to it } }

		emitAll(
			modifiedDocumentPairedWithOriginal
				.mapNotNull { (newDoc, prevDoc) ->
					runCatching {
						doFix(newDoc).copy(attachments = prevDoc.attachments).let {
							attachmentModificationLogic.ensureValidAttachmentChanges(
								prevDoc,
								it,
								emptySet(),
							)
						}
					}.getOrNull()
				}.let {
					super.filterValidEntityChanges(datastoreInformation, it)
				}.let {
					documentDAO.saveBulk(datastoreInformation, it.toList())
				}.map {
					it.entityOrThrow()
				},
		)
	}

	override fun createOrModifyDocuments(
		documents: List<BatchUpdateDocumentInfo>,
		strict: Boolean,
	): Flow<Document> = flow {
		val datastoreInformation = getInstanceAndGroup()

		val (documentToCreate, documentsToUpdate) = documents.partition { it.isNewDocument }

		val fixedDocumentsToCreate =
			documentToCreate.mapNotNull {
				runCatching {
					checkNewDocument(fix(it.newDocument, isCreate = true), strict)
				}.getOrNull()
			}

		val fixedDocumentsToUpdate =
			documentsToUpdate
				.mapNotNull { (newDoc, prevDoc) ->
					runCatching {
						fix(newDoc, isCreate = false).copy(attachments = prevDoc!!.attachments).let {
							attachmentModificationLogic.ensureValidAttachmentChanges(
								prevDoc,
								it,
								if (strict) emptySet() else setOf(newDoc.mainAttachmentKey),
							)
						}
					}.getOrNull()
				}.let {
					super.filterValidEntityChanges(datastoreInformation, it)
				}.toList()

		emitAll(documentDAO.create(datastoreInformation, fixedDocumentsToCreate))
		emitAll(documentDAO.saveBulk(datastoreInformation, fixedDocumentsToUpdate).filterSuccessfulUpdates())
	}

	override suspend fun updateAttachments(
		documentId: String,
		documentRev: String?,
		mainAttachmentChange: DataAttachmentChange?,
		secondaryAttachmentsChanges: Map<String, DataAttachmentChange>,
	): Document? {
		require(!secondaryAttachmentsChanges.containsKey(Document.mainAttachmentKeyFromId(documentId))) {
			"Secondary attachments cannot use the main attachment key"
		}
		return attachmentModificationLogic.updateAttachments(
			documentId,
			documentRev,
			mainAttachmentChange
				?.let {
					if (it is DataAttachmentChange.CreateOrUpdate && it.utis == null) {
						val currentDocument = documentDAO.get(getInstanceAndGroup(), documentId)
						if (currentDocument?.mainAttachment == null) {
							// Capture cases where the document has no attachment id set (main attachment is null) but specifies some utis
							it.copy(utis = listOfNotNull(currentDocument?.mainUti) + currentDocument?.otherUtis.orEmpty())
						} else {
							it
						}
					} else {
						it
					}
				}?.let {
					secondaryAttachmentsChanges + (Document.mainAttachmentKeyFromId(documentId) to it)
				} ?: secondaryAttachmentsChanges,
		)
	}

	override fun listDocumentsByDocumentTypeHCPartySecretMessageKeys(
		documentTypeCode: String,
		hcPartyId: String,
		secretForeignKeys: List<String>,
	): Flow<Document> = flow {
		val datastoreInformation = getInstanceAndGroup()
		emitAll(
			documentDAO.listDocumentsByDocumentTypeHcPartySecretMessageKeys(
				datastoreInformation,
				documentTypeCode,
				getAllSearchKeysIfCurrentDataOwner(hcPartyId),
				secretForeignKeys,
			),
		)
	}

	@Suppress("DEPRECATION")
	@Deprecated("This method is inefficient for high volumes of keys, use listDocumentIdsByDataOwnerPatientCreated instead")
	override fun listDocumentsByHCPartySecretMessageKeys(
		hcPartyId: String,
		secretForeignKeys: List<String>,
	): Flow<Document> = flow {
		val datastoreInformation = getInstanceAndGroup()
		emitAll(
			documentDAO.listDocumentsByHcPartyAndSecretMessageKeys(
				datastoreInformation,
				getAllSearchKeysIfCurrentDataOwner(hcPartyId),
				secretForeignKeys,
			),
		)
	}

	override fun listDocumentIdsByDataOwnerPatientCreated(
		dataOwnerId: String,
		secretForeignKeys: Set<String>,
		startDate: Long?,
		endDate: Long?,
		descending: Boolean,
	): Flow<String> = flow {
		val datastoreInformation = getInstanceAndGroup()
		emitAll(
			documentDAO.listDocumentIdsByDataOwnerPatientCreated(
				datastoreInformation,
				getAllSearchKeysIfCurrentDataOwner(dataOwnerId),
				secretForeignKeys,
				startDate,
				endDate,
				descending,
			),
		)
	}

	override fun listDocumentsWithoutDelegation(limit: Int): Flow<Document> = flow {
		val datastoreInformation = getInstanceAndGroup()
		emitAll(documentDAO.listDocumentsWithNoDelegations(datastoreInformation, limit))
	}

	override fun entityWithUpdatedSecurityMetadata(
		entity: Document,
		updatedMetadata: SecurityMetadata,
	): Document = entity.copy(securityMetadata = updatedMetadata)

	override fun getGenericDAO() = documentDAO

	protected fun checkNewDocument(
		document: Document,
		strict: Boolean,
	): Document {
		checkValidityForCreation(document)
		require(document.secondaryAttachments.isEmpty()) {
			"New document can't provide any secondary attachments information."
		}
		require(document.deletedAttachments.isEmpty()) {
			"New document can't specify deleted attachments."
		}
		if (strict) {
			require(document.mainAttachment == null && document.mainUti == null && document.otherUtis.isEmpty()) {
				"New document can't specify any main attachment information"
			}
		}
		require(document.objectStoreReference == null) {
			"New document can't specify a value for the main attachment object store id."
		}
		return if (document.attachmentId != null) document.copy(attachmentId = null) else document
	}

	override suspend fun purgeEntity(
		id: String,
		rev: String,
	): DocIdentifier {
		val entity = getEntityWithExpectedRev(id, rev)
		return checkNotNull(
			getGenericDAO()
				.purge(
					getInstanceAndGroup(),
					listOf(entity),
				).singleOrNull(),
		) {
			"Too many update result from purge"
		}.entityOrThrow().also {
			attachmentModificationLogic.cleanupPurgedEntityAttachments(entity)
		}
	}
}
