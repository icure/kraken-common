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
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Repository
import org.taktik.couchdb.annotation.View
import org.taktik.couchdb.dao.DesignDocumentProvider
import org.taktik.couchdb.entity.ComplexKey
import org.taktik.couchdb.id.IDGenerator
import org.taktik.couchdb.queryViewIncludeDocsNoValue
import org.taktik.icure.asyncdao.CouchDbDispatcher
import org.taktik.icure.asyncdao.FormTemplateDAO
import org.taktik.icure.asynclogic.datastore.IDatastoreInformation
import org.taktik.icure.cache.ConfiguredCacheProvider
import org.taktik.icure.cache.EntityCacheFactory
import org.taktik.icure.cache.getConfiguredCache
import org.taktik.icure.config.DaoConfig
import org.taktik.icure.entities.FormTemplate
import org.taktik.icure.utils.writeTo
import java.nio.ByteBuffer

@Repository("formTemplateDAO")
@Profile("app")
@View(name = "all", map = "function(doc) { if (doc.java_type == 'org.taktik.icure.entities.FormTemplate' && !doc.deleted) emit(null, doc._id)}")
internal class FormTemplateDAOImpl(
	@Qualifier("baseCouchDbDispatcher") couchDbDispatcher: CouchDbDispatcher,
	idGenerator: IDGenerator,
	entityCacheFactory: ConfiguredCacheProvider,
	designDocumentProvider: DesignDocumentProvider,
	daoConfig: DaoConfig
) : GenericDAOImpl<FormTemplate>(FormTemplate::class.java, couchDbDispatcher, idGenerator, entityCacheFactory.getConfiguredCache(), designDocumentProvider, daoConfig = daoConfig), FormTemplateDAO {

	@View(name = "by_userId_and_guid", map = "function(doc) { if (doc.java_type == 'org.taktik.icure.entities.FormTemplate' && !doc.deleted && doc.author) emit([doc.author,doc.guid], null )}")
	override fun listFormTemplatesByUserGuid(datastoreInformation: IDatastoreInformation, userId: String, guid: String?, loadLayout: Boolean) = flow {
		val client = couchDbDispatcher.getClient(datastoreInformation)

		val from = ComplexKey.of(userId, guid ?: "")
		val to = ComplexKey.of(userId, guid ?: "\ufff0")
		val formTemplates = client.queryViewIncludeDocsNoValue<Array<String>, FormTemplate>(createQuery(
			datastoreInformation,
			"by_userId_and_guid"
		).startKey(from).endKey(to).includeDocs(true)).map { it.doc }

		// invoke postLoad()
		emitAll(
			if (loadLayout) {
				formTemplates.map {
					postLoad(datastoreInformation, it)
				}
			} else formTemplates
		)
	}

	@View(name = "by_guid", map = "function(doc) { if (doc.java_type == 'org.taktik.icure.entities.FormTemplate' && !doc.deleted) emit(doc.guid, null )}")
	override fun listFormsByGuid(datastoreInformation: IDatastoreInformation, guid: String, loadLayout: Boolean) = flow {
		val client = couchDbDispatcher.getClient(datastoreInformation)

		val formTemplates = client.queryViewIncludeDocsNoValue<String, FormTemplate>(createQuery(
			datastoreInformation,
			"by_guid"
		).key(guid).includeDocs(true)).map { it.doc }

		emitAll(
			if (loadLayout) {
				formTemplates.map {
					postLoad(datastoreInformation, it)
				}
			} else formTemplates
		)
	}

	@View(name = "by_specialty_code_and_guid", map = "function(doc) { if (doc.java_type == 'org.taktik.icure.entities.FormTemplate' && !doc.deleted && doc.specialty) emit([doc.specialty.code,doc.guid], null )}")
	override fun listFormsBySpecialtyAndGuid(datastoreInformation: IDatastoreInformation, specialityCode: String, guid: String?, loadLayout: Boolean) = flow {
		val client = couchDbDispatcher.getClient(datastoreInformation)

		val formTemplates = if (guid != null) {
			val key = ComplexKey.of(specialityCode, guid)
			client.queryViewIncludeDocsNoValue<Array<String>, FormTemplate>(createQuery(
				datastoreInformation,
				"by_specialty_code_and_guid"
			).key(key).includeDocs(true)).map { it.doc }
		} else {
			val from = ComplexKey.of(specialityCode, null)
			val to = ComplexKey.of(specialityCode, ComplexKey.emptyObject())
			client.queryViewIncludeDocsNoValue<Array<String>, FormTemplate>(createQuery(
				datastoreInformation,
				"by_specialty_code_and_guid"
			).startKey(from).endKey(to).includeDocs(true)).map { it.doc }
		}

		emitAll(
			if (loadLayout) {
				formTemplates.map {
					postLoad(datastoreInformation, it)
				}
			} else formTemplates
		)
	}

	override suspend fun createFormTemplate(datastoreInformation: IDatastoreInformation, entity: FormTemplate): FormTemplate {
		super.save(datastoreInformation, true, entity)
		return entity
	}

	/**
	 * Note: we treat the attachments here instead of in logic because on the client side we don't treat this as an
	 * entity with attachments. The fact we store part of its content as a couchdb attachment is an implementation
	 * detail of the DAO.
	 */
	override suspend fun beforeSave(datastoreInformation: IDatastoreInformation, entity: FormTemplate) =
		super.beforeSave(datastoreInformation, entity).let { formTemplate ->
			if (formTemplate.templateLayout != null) {
				val newAttachmentId = DigestUtils.sha256Hex(formTemplate.templateLayout)

				if (formTemplate.templateLayoutAttachmentId != newAttachmentId) {
					formTemplate.copy(
						attachments = getExistingEntityAttachmentStubs(
							datastoreInformation,
							formTemplate,
							excludingAttachmentId = formTemplate.templateLayoutAttachmentId
						), // Couchdb will automatically remove any attachment that is not in the map of attachments
						templateLayoutAttachmentId = newAttachmentId,
						isAttachmentDirty = true
					)
				} else {
					formTemplate.copy(
						attachments = getExistingEntityAttachmentStubs(
							datastoreInformation,
							formTemplate,
						), // Preserve existing attachments
						isAttachmentDirty = false
					)
				}
			} else {
				formTemplate.copy(
					attachments = getExistingEntityAttachmentStubs(
						datastoreInformation,
						formTemplate,
						excludingAttachmentId = formTemplate.templateLayoutAttachmentId
					), // Couchdb will automatically remove any attachment that is not in the map of attachments
					templateLayoutAttachmentId = null,
					isAttachmentDirty = false
				)
			}
		}

	override suspend fun afterSave(
		datastoreInformation: IDatastoreInformation,
		savedEntity: FormTemplate,
		preSaveEntity: FormTemplate
	) =
		super.afterSave(datastoreInformation, savedEntity, preSaveEntity).let { afterSuperSave ->
			if (preSaveEntity.isAttachmentDirty && afterSuperSave.templateLayoutAttachmentId != null && afterSuperSave.rev != null && preSaveEntity.templateLayout != null) {
				createAttachment(
					datastoreInformation,
					afterSuperSave.id,
					afterSuperSave.templateLayoutAttachmentId!!,
					afterSuperSave.rev!!,
					"application/json",
					flowOf(ByteBuffer.wrap(preSaveEntity.templateLayout))
				).let {
					afterSuperSave.copy(
						rev = it,
						templateLayout = preSaveEntity.templateLayout,
						isAttachmentDirty = false
					)
				}
			} else afterSuperSave
		}

	override suspend fun postLoad(datastoreInformation: IDatastoreInformation, entity: FormTemplate) =
		super.postLoad(datastoreInformation, entity).let { formTemplate ->
			val formTemplateLayout = formTemplate.templateLayoutAttachmentId?.let { laId ->
				val attachmentFlow = getAttachment(datastoreInformation, formTemplate.id, laId, formTemplate.rev)
				ByteArrayOutputStream().use {
					attachmentFlow.writeTo(it)
					it.toByteArray()
				}
			}

			@Suppress("DEPRECATION")
			val formLayout = formTemplate.layoutAttachmentId?.takeIf { formTemplateLayout == null }?.let { laId ->
				val attachmentFlow = getAttachment(datastoreInformation, formTemplate.id, laId, formTemplate.rev)
				ByteArrayOutputStream().use {
					attachmentFlow.writeTo(it)
					it.toByteArray()
				}
			}

			if (formTemplateLayout != null || formLayout != null) {
				formTemplate.copy(templateLayout = formTemplateLayout, layout = formLayout)
			} else formTemplate
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

	companion object {
		val log: Logger = LoggerFactory.getLogger(FormTemplateDAOImpl::class.java)
	}
}
