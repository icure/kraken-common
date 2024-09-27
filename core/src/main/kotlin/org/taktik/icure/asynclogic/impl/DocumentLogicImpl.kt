/*
 * Copyright (c) 2020. Taktik SA, All rights reserved.
 */
package org.taktik.icure.asynclogic.impl

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.single
import kotlinx.coroutines.flow.singleOrNull
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.core.io.buffer.DataBuffer
import org.taktik.couchdb.DocIdentifier
import org.taktik.couchdb.entity.IdAndRev
import org.taktik.couchdb.entity.Option
import org.taktik.couchdb.exception.DocumentNotFoundException
import org.taktik.icure.asyncdao.DocumentDAO
import org.taktik.icure.asyncdao.results.filterSuccessfulUpdates
import org.taktik.icure.asynclogic.DocumentLogic
import org.taktik.icure.asynclogic.ExchangeDataMapLogic
import org.taktik.icure.asynclogic.SessionInformationProvider
import org.taktik.icure.asynclogic.base.impl.EntityWithEncryptionMetadataLogic
import org.taktik.icure.asynclogic.datastore.DatastoreInstanceProvider
import org.taktik.icure.asynclogic.datastore.IDatastoreInformation
import org.taktik.icure.asynclogic.impl.filter.Filters
import org.taktik.icure.asynclogic.objectstorage.DataAttachmentChange
import org.taktik.icure.asynclogic.objectstorage.DocumentDataAttachmentLoader
import org.taktik.icure.asynclogic.objectstorage.DocumentDataAttachmentModificationLogic
import org.taktik.icure.asynclogic.objectstorage.contentFlowOfNullable
import org.taktik.icure.domain.BatchUpdateDocumentInfo
import org.taktik.icure.entities.Document
import org.taktik.icure.entities.embed.SecurityMetadata
import org.taktik.icure.exceptions.NotFoundRequestException
import org.taktik.icure.validation.aspect.Fixer
import java.nio.ByteBuffer

open class DocumentLogicImpl(
    private val documentDAO: DocumentDAO,
    sessionLogic: SessionInformationProvider,
    datastoreInstanceProvider: DatastoreInstanceProvider,
    exchangeDataMapLogic: ExchangeDataMapLogic,
    private val attachmentModificationLogic: DocumentDataAttachmentModificationLogic,
    @Qualifier("documentDataAttachmentLoader") private val attachmentLoader: DocumentDataAttachmentLoader,
    fixer: Fixer,
    filters: Filters
) : EntityWithEncryptionMetadataLogic<Document, DocumentDAO>(fixer, sessionLogic, datastoreInstanceProvider, exchangeDataMapLogic, filters), DocumentLogic {

	override suspend fun createDocument(document: Document, strict: Boolean) = fix(document) { fixedDocument ->
		if(fixedDocument.rev != null) throw IllegalArgumentException("A new entity should not have a rev")
		val datastoreInformation = getInstanceAndGroup()
		documentDAO.save(datastoreInformation, checkNewDocument(fixedDocument, strict))
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

	override suspend fun getMainAttachment(document: Document): Flow<DataBuffer> =
		attachmentLoader.contentFlowOfNullable(document, Document::mainAttachment)
			?: throw DocumentNotFoundException("Main attachment not found")

	override fun getAttachment(documentId: String, attachmentId: String): Flow<ByteBuffer> = flow {
		val datastoreInformation = getInstanceAndGroup()
		emitAll(getDocument(documentId)?.let {
			documentDAO.getAttachment(datastoreInformation, documentId, attachmentId)
		} ?: emptyFlow())
	}

	override fun createEntities(entities: Flow<Document>): Flow<Document> = flow {
		emitAll(createEntities(entities.toList()))
	}

	override fun createEntities(entities: Collection<Document>): Flow<Document> = flow {
		val datastoreInformation = getInstanceAndGroup()

		emitAll(
			documentDAO.saveBulk(
				datastoreInformation,
				entities.filter { it.rev === null }.mapNotNull {
					kotlin.runCatching {
						checkNewDocument(fix(it), true)
					}.getOrNull()
				}
			).filterSuccessfulUpdates()
		)
	}

	override fun modifyEntities(entities: Flow<Document>): Flow<Document> = flow {
		emitAll(modifyEntities(entities.toList()))
	}

	override fun modifyEntities(entities: Collection<Document>): Flow<Document> = flow {
		val datastoreInformation = getInstanceAndGroup()

		val originalDocumentsById = documentDAO.getEntities(datastoreInformation, entities.map { it.id }).toList().associateBy { it.id }
		val modifiedDocumentPairedWithOriginal = entities.mapNotNull { mDoc -> originalDocumentsById[mDoc.id]?.let { mDoc to it } }

		emitAll(
			modifiedDocumentPairedWithOriginal.mapNotNull { (newDoc, prevDoc) ->
				runCatching {
					fix(newDoc).copy(attachments = prevDoc.attachments).let {
						attachmentModificationLogic.ensureValidAttachmentChanges(
							prevDoc, it, emptySet()
						)
					}
				}.getOrNull()
			}.let {
				super.filterValidEntityChanges(it)
			}.let {
				documentDAO.saveBulk(datastoreInformation, it.toList())
			}.map {
				it.entityOrThrow()
			}
		)
	}

	private fun ensureValidAttachmentChanges(updatedDocument: Document, baseline: Document, strict: Boolean): Document =
		updatedDocument.copy(attachments = baseline.attachments).let {
			attachmentModificationLogic.ensureValidAttachmentChanges(
				baseline, it, if (strict) emptySet() else setOf(updatedDocument.mainAttachmentKey)
			)
		}

	override suspend fun modifyDocument(updatedDocument: Document, strict: Boolean): Document? = fix(updatedDocument) { newDoc ->
		val datastoreInformation = getInstanceAndGroup()
		val baseline = requireNotNull(documentDAO.get(datastoreInformation, newDoc.id)) {
			"Attempting to modify a non-existing document ${newDoc.id}."
		}
		require(newDoc.rev == baseline.rev) { "Updated document has an older revision ${newDoc.rev} -> ${baseline.rev}" }
		val validatedEntityForAttachments = ensureValidAttachmentChanges(updatedDocument, baseline, strict)
		checkValidEntityChange(validatedEntityForAttachments, baseline)
		documentDAO.save(datastoreInformation, validatedEntityForAttachments)
	}

	override fun createOrModifyDocuments(documents: List<BatchUpdateDocumentInfo>, strict: Boolean): Flow<Document> = flow {
		val datastoreInformation = getInstanceAndGroup()

		val (documentToCreate, documentsToUpdate) = documents.partition { it.isNewDocument }

		val fixedDocumentsToCreate = documentToCreate.mapNotNull {
			kotlin.runCatching {
				checkNewDocument(fix(it.newDocument), strict)
			}.getOrNull()
		}

		val fixedDocumentsToUpdate = documentsToUpdate.mapNotNull { (newDoc, prevDoc) ->
			runCatching {
				fix(newDoc).copy(attachments = prevDoc!!.attachments).let {
					attachmentModificationLogic.ensureValidAttachmentChanges(
						prevDoc, it, if (strict) emptySet() else setOf(newDoc.mainAttachmentKey)
					)
				}
			}.getOrNull()
		}.let {
			super.filterValidEntityChanges(it)
		}.toList()

		emitAll(documentDAO.create(datastoreInformation, fixedDocumentsToCreate))
		emitAll(documentDAO.saveBulk(datastoreInformation, fixedDocumentsToUpdate).filterSuccessfulUpdates())
	}

	override suspend fun updateAttachments(
		currentDocument: Document, mainAttachmentChange: DataAttachmentChange?, secondaryAttachmentsChanges: Map<String, DataAttachmentChange>
	): Document? {
		return attachmentModificationLogic.updateAttachments(currentDocument, mainAttachmentChange?.let {
			if (it is DataAttachmentChange.CreateOrUpdate && it.utis == null && currentDocument.mainAttachment == null) {
				// Capture cases where the document has no attachment id set (main attachment is null) but specifies some utis
				it.copy(utis = listOfNotNull(currentDocument.mainUti) + currentDocument.otherUtis)
			} else it
		}?.let {
			secondaryAttachmentsChanges + (currentDocument.mainAttachmentKey to it)
		} ?: secondaryAttachmentsChanges)
	}

	override fun listDocumentsByDocumentTypeHCPartySecretMessageKeys(
		documentTypeCode: String,
		hcPartyId: String,
		secretForeignKeys: List<String>,
	): Flow<Document> = flow {
		val datastoreInformation = getInstanceAndGroup()
		emitAll(
			documentDAO.listDocumentsByDocumentTypeHcPartySecretMessageKeys(
				datastoreInformation, documentTypeCode, getAllSearchKeysIfCurrentDataOwner(hcPartyId), secretForeignKeys
			)
		)
	}

	override fun listDocumentsByHCPartySecretMessageKeys(
		hcPartyId: String,
		secretForeignKeys: List<String>,
	): Flow<Document> = flow {
		val datastoreInformation = getInstanceAndGroup()
		emitAll(
			documentDAO.listDocumentsByHcPartyAndSecretMessageKeys(
				datastoreInformation, getAllSearchKeysIfCurrentDataOwner(hcPartyId), secretForeignKeys
			)
		)
	}

	override fun listDocumentIdsByDataOwnerPatientCreated(
		dataOwnerId: String,
		secretForeignKeys: Set<String>,
		startDate: Long?,
		endDate: Long?,
		descending: Boolean
	): Flow<String> = flow {
		val datastoreInformation = getInstanceAndGroup()
		emitAll(
			documentDAO.listDocumentIdsByDataOwnerPatientCreated(
				datastoreInformation,
				getAllSearchKeysIfCurrentDataOwner(dataOwnerId),
				secretForeignKeys,
				startDate,
				endDate,
				descending
			)
		)
	}

	override fun listDocumentsWithoutDelegation(limit: Int): Flow<Document> = flow {
		val datastoreInformation = getInstanceAndGroup()
		emitAll(documentDAO.listDocumentsWithNoDelegations(datastoreInformation, limit))
	}

	override fun solveConflicts(limit: Int?, ids: List<String>?) = flow { emitAll(doSolveConflicts(
		ids,
		limit,
		getInstanceAndGroup()
	)) }

	protected fun doSolveConflicts(
		ids: List<String>?,
		limit: Int?,
		datastoreInformation: IDatastoreInformation,
	) =  flow {
		val flow = ids?.asFlow()?.mapNotNull { documentDAO.get(datastoreInformation, it, Option.CONFLICTS) }
			?: documentDAO.listConflicts(datastoreInformation)
				.mapNotNull { documentDAO.get(datastoreInformation, it.id, Option.CONFLICTS) }
		(limit?.let { flow.take(it) } ?: flow)
			.mapNotNull { document ->
				document.conflicts?.mapNotNull { conflictingRevision ->
					documentDAO.get(
						datastoreInformation, document.id, conflictingRevision
					)
				}?.fold(document to emptyList<Document>()) { (kept, toBePurged), conflict ->
					kept.merge(conflict) to toBePurged + conflict
				}?.let { (mergedDocument, toBePurged) ->
					documentDAO.save(datastoreInformation, mergedDocument).also {
						toBePurged.forEach {
							if (it.rev != null && it.rev != mergedDocument.rev) {
								documentDAO.purge(datastoreInformation, listOf(it)).single()
							}
						}
					}
				}
			}
			.collect { emit(IdAndRev(it.id, it.rev)) }
	}
	override fun entityWithUpdatedSecurityMetadata(entity: Document, updatedMetadata: SecurityMetadata): Document {
		return entity.copy(securityMetadata = updatedMetadata)
	}

	override fun getGenericDAO() = documentDAO

	private fun checkNewDocument(document: Document, strict: Boolean): Document {
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

	override suspend fun purgeEntity(id: String, rev: String): DocIdentifier {
		val entity = getEntityWithExpectedRev(id, rev)
		return checkNotNull(getGenericDAO().purge(
			getInstanceAndGroup(),
			listOf(entity)
		).singleOrNull()) {
			"Too many update result from purge"
		}.entityOrThrow().also {
			attachmentModificationLogic.cleanupPurgedEntityAttachments(entity)
		}
	}
}
