/*
 * Copyright (c) 2020. Taktik SA, All rights reserved.
 */

package org.taktik.icure.asyncdao.impl

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import org.apache.commons.codec.digest.DigestUtils
import org.apache.commons.io.output.ByteArrayOutputStream
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Repository
import org.taktik.commons.uti.UTI
import org.taktik.couchdb.ViewQueryResultEvent
import org.taktik.couchdb.annotation.View
import org.taktik.couchdb.dao.DesignDocumentProvider
import org.taktik.couchdb.entity.ComplexKey
import org.taktik.couchdb.id.IDGenerator
import org.taktik.couchdb.queryViewIncludeDocsNoValue
import org.taktik.icure.asyncdao.CouchDbDispatcher
import org.taktik.icure.asyncdao.DocumentTemplateDAO
import org.taktik.icure.asynclogic.datastore.IDatastoreInformation
import org.taktik.icure.cache.EntityCacheFactory
import org.taktik.icure.db.PaginationOffset
import org.taktik.icure.entities.DocumentTemplate
import org.taktik.icure.utils.writeTo
import java.nio.ByteBuffer

@Repository("documentTemplateDAO")
@Profile("app")
@View(name = "all", map = "function(doc) { if (doc.java_type == 'org.taktik.icure.entities.DocumentTemplate' && !doc.deleted) emit(doc._id, null )}")
class DocumentTemplateDAOImpl(
	@Qualifier("baseCouchDbDispatcher") couchDbDispatcher: CouchDbDispatcher,
	idGenerator: IDGenerator,
	entityCacheFactory: EntityCacheFactory,
	designDocumentProvider: DesignDocumentProvider
) : GenericDAOImpl<DocumentTemplate>(DocumentTemplate::class.java, couchDbDispatcher, idGenerator, entityCacheFactory.localOnlyCache(DocumentTemplate::class.java), designDocumentProvider), DocumentTemplateDAO {

	override fun getAllDocumentTemplates(
		datastoreInformation: IDatastoreInformation,
		paginationOffset: PaginationOffset<String>
	): Flow<ViewQueryResultEvent> = flow {
		val client = couchDbDispatcher.getClient(datastoreInformation)

		val viewQuery = pagedViewQuery(
			datastoreInformation, "all", null, null, paginationOffset, false
		)
		emitAll(client.queryView(viewQuery, String::class.java, String::class.java, DocumentTemplate::class.java))
	}

	@View(name = "by_userId_and_guid", map = "function(doc) { if (doc.java_type == 'org.taktik.icure.entities.DocumentTemplate' && !doc.deleted && doc.owner) emit([doc.owner,doc.guid], null )}")
	override fun listDocumentTemplatesByUserGuid(datastoreInformation: IDatastoreInformation, userId: String, guid: String?) = flow {
		val client = couchDbDispatcher.getClient(datastoreInformation)

		val from = ComplexKey.of(userId, "")
		val to = ComplexKey.of(userId, "\ufff0")
		val viewQuery = createQuery(datastoreInformation, "by_userId_and_guid").startKey(from).endKey(to).includeDocs(true)
		val documentTemplates = client.queryViewIncludeDocsNoValue<Array<String>, DocumentTemplate>(viewQuery).map { it.doc }

		// invoke postLoad()
		emitAll(
			documentTemplates.map {
				postLoad(datastoreInformation, it)
			}
		)
	}

	@View(name = "by_specialty_code_and_guid", map = "function(doc) { if (doc.java_type == 'org.taktik.icure.entities.DocumentTemplate' && !doc.deleted && doc.specialty) emit([doc.specialty.code,doc.guid], null )}")
	override fun listDocumentTemplatesBySpecialtyAndGuid(datastoreInformation: IDatastoreInformation, healthcarePartyId: String, guid: String?) = flow {
		val client = couchDbDispatcher.getClient(datastoreInformation)

		val documentTemplates = if (guid != null) {
			val key = ComplexKey.of(healthcarePartyId, guid)
			val viewQuery = createQuery(datastoreInformation, "by_specialty_code_and_guid").key(key).includeDocs(true)
			client.queryViewIncludeDocsNoValue<Array<String>, DocumentTemplate>(viewQuery).map { it.doc }
		} else {
			val from = ComplexKey.of(healthcarePartyId, "")
			val to = ComplexKey.of(healthcarePartyId, "\ufff0")
			val viewQuery = createQuery(datastoreInformation, "by_specialty_code_and_guid").startKey(from).endKey(to).includeDocs(true)
			client.queryViewIncludeDocsNoValue<Array<String>, DocumentTemplate>(viewQuery).map { it.doc }
		}

		// invoke postLoad()
		emitAll(
			documentTemplates.map {
				postLoad(datastoreInformation, it)
			}
		)
	}

	@View(name = "by_document_type_code_and_user_id_and_guid", map = "function(doc) { if (doc.java_type == 'org.taktik.icure.entities.DocumentTemplate' && !doc.deleted && doc.documentType ) emit([doc.documentType,doc.owner,doc.guid], null )}")
	override fun listDocumentsByTypeUserGuid(datastoreInformation: IDatastoreInformation, documentTypeCode: String, userId: String?, guid: String?) = flow {
		val client = couchDbDispatcher.getClient(datastoreInformation)

		val viewQuery = if (userId != null && guid != null) {
			val key = ComplexKey.of(documentTypeCode, userId, guid)
			createQuery(datastoreInformation, "by_document_type_code_and_user_id_and_guid").key(key).includeDocs(true)
		} else if (userId != null) {
			val from = ComplexKey.of(documentTypeCode, userId, "")
			val to = ComplexKey.of(documentTypeCode, userId, "\ufff0")
			createQuery(datastoreInformation, "by_document_type_code_and_user_id_and_guid").startKey(from).endKey(to).includeDocs(true)
		} else {
			val from = ComplexKey.of(documentTypeCode, "", "")
			val to = ComplexKey.of(documentTypeCode, "\ufff0", "\ufff0")
			createQuery(datastoreInformation, "by_document_type_code_and_user_id_and_guid").startKey(from).endKey(to).includeDocs(true)
		}
		val documentTemplates = client.queryViewIncludeDocsNoValue<Array<String>, DocumentTemplate>(viewQuery).map { it.doc }

		// invoke postLoad()
		emitAll(
			documentTemplates.map {
				postLoad(datastoreInformation, it)
			}
		)
	}

	override fun evictFromCache(entity: DocumentTemplate) {
		evictFromCache(entity)
	}

	override suspend fun createDocumentTemplate(datastoreInformation: IDatastoreInformation, entity: DocumentTemplate): DocumentTemplate {
		super.save(datastoreInformation, true, entity)
		return entity
	}

	/**
	 * Note: we treat the attachments here instead of in logic because on the client side we don't treat this as an
	 * entity with attachments. The fact we store part of its content as a couchdb attachment is an implementation
	 * detail of the DAO.
	 */
	override suspend fun beforeSave(datastoreInformation: IDatastoreInformation, entity: DocumentTemplate) =
		super.beforeSave(datastoreInformation, entity).let { documentTemplate ->
			/*
			 * Note: if the user manually changes the attachment id this does not delete existing attachments. This is
			 * wasteful but safer than only keeping the most recent attachment, because in case for some reason we have
			 * multiple attachments on the entity at least we don't delete them.
			 */
			if (documentTemplate.attachment != null) {
				val newAttachmentId = DigestUtils.sha256Hex(documentTemplate.attachment)

				if (documentTemplate.attachmentId != newAttachmentId) {
					documentTemplate.copy(
						attachments = getExistingEntityAttachmentStubs(
							datastoreInformation,
							documentTemplate,
							excludingAttachmentId = documentTemplate.attachmentId
						), // Couchdb will automatically remove any attachment that is not in the map of attachments
						attachmentId = newAttachmentId,
						isAttachmentDirty = true
					)
				} else {
					documentTemplate.copy(
						attachments = getExistingEntityAttachmentStubs(
							datastoreInformation,
							documentTemplate,
						), // Preserve existing attachments
						isAttachmentDirty = false
					)
				}

			} else {
				documentTemplate.copy(
					attachments = getExistingEntityAttachmentStubs(
						datastoreInformation,
						documentTemplate,
						excludingAttachmentId = documentTemplate.attachmentId
					), // Couchdb will automatically remove any attachment that is not in the map of attachments
					attachmentId = null,
					isAttachmentDirty = false
				)
			}
		}

	override suspend fun afterSave(
		datastoreInformation: IDatastoreInformation,
		savedEntity: DocumentTemplate,
		preSaveEntity: DocumentTemplate
	): DocumentTemplate =
		super.afterSave(datastoreInformation, savedEntity, preSaveEntity).let { afterSuperSave ->
			if (preSaveEntity.isAttachmentDirty && savedEntity.attachmentId != null && savedEntity.rev != null && preSaveEntity.attachment != null) {
				val uti = UTI.get(afterSuperSave.mainUti)
				var mimeType = "application/xml"
				if (uti != null && uti.mimeTypes != null && uti.mimeTypes.size > 0) {
					mimeType = uti.mimeTypes[0]
				}
				createAttachment(
					datastoreInformation,
					afterSuperSave.id,
					afterSuperSave.attachmentId!!,
					afterSuperSave.rev!!,
					mimeType,
					flowOf(ByteBuffer.wrap(preSaveEntity.attachment))
				).let {
					afterSuperSave.copy(
						rev = it,
						attachment = preSaveEntity.attachment,
						isAttachmentDirty = false
					)
				}
			} else afterSuperSave
		}

	override suspend fun postLoad(datastoreInformation: IDatastoreInformation, entity: DocumentTemplate) =
		super.postLoad(datastoreInformation, entity).let { documentTemplate ->
			if (documentTemplate.attachmentId != null) {
				try {
					val attachmentFlow = getAttachment(datastoreInformation, documentTemplate.id,
						documentTemplate.attachmentId!!, documentTemplate.rev)
					documentTemplate.copy(
						attachment = ByteArrayOutputStream().use {
							attachmentFlow.writeTo(it)
							it.toByteArray()
						}
					)
				} catch (e: Exception) {
					documentTemplate //Could not load
				}
			} else documentTemplate
		}

	override fun getAttachment(datastoreInformation: IDatastoreInformation, documentId: String, attachmentId: String, rev: String?): Flow<ByteBuffer> = flow {
		val client = couchDbDispatcher.getClient(datastoreInformation)
		emitAll(client.getAttachment(documentId, attachmentId, rev))
	}

	override suspend fun createAttachment(datastoreInformation: IDatastoreInformation, documentId: String, attachmentId: String, rev: String, contentType: String, data: Flow<ByteBuffer>): String {
		val client = couchDbDispatcher.getClient(datastoreInformation)
		return client.createAttachment(documentId, attachmentId, rev, contentType, data)
	}

	override suspend fun deleteAttachment(datastoreInformation: IDatastoreInformation, documentId: String, rev: String, attachmentId: String): String {
		val client = couchDbDispatcher.getClient(datastoreInformation)
		return client.deleteAttachment(documentId, attachmentId, rev)
	}

}
