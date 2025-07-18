/*
 * Copyright (c) 2020. Taktik SA, All rights reserved.
 */
package org.taktik.icure.asynclogic.impl

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Service
import org.taktik.icure.asyncdao.DocumentTemplateDAO
import org.taktik.icure.asynclogic.DocumentTemplateLogic
import org.taktik.icure.asynclogic.SessionInformationProvider
import org.taktik.icure.asynclogic.impl.filter.Filters
import org.taktik.icure.datastore.DatastoreInstanceProvider
import org.taktik.icure.db.PaginationOffset
import org.taktik.icure.entities.DocumentTemplate
import org.taktik.icure.pagination.PaginationElement
import org.taktik.icure.pagination.limitIncludingKey
import org.taktik.icure.pagination.toPaginatedFlow
import org.taktik.icure.validation.aspect.Fixer

@Service
@Profile("app")
class DocumentTemplateLogicImpl(
	private val documentTemplateDAO: DocumentTemplateDAO,
	private val sessionLogic: SessionInformationProvider,
	datastoreInstanceProvider: DatastoreInstanceProvider,
	fixer: Fixer,
	filters: Filters,
) : GenericLogicImpl<DocumentTemplate, DocumentTemplateDAO>(fixer, datastoreInstanceProvider, filters),
	DocumentTemplateLogic {
	override fun createEntities(entities: Collection<DocumentTemplate>): Flow<DocumentTemplate> = flow {
		emitAll(
			super.createEntities(
				entities.map { dt ->
					fix(dt, isCreate = true) { e ->
						e.owner?.let { e } ?: e.copy(owner = sessionLogic.getCurrentUserId())
					}
				},
			),
		)
	}

	override suspend fun createDocumentTemplate(entity: DocumentTemplate): DocumentTemplate = fix(entity, isCreate = true) { documentTemplate ->
		val datastoreInformation = getInstanceAndGroup()
		documentTemplateDAO.createDocumentTemplate(
			datastoreInformation,
			documentTemplate.owner?.let { documentTemplate } ?: documentTemplate.copy(owner = sessionLogic.getCurrentUserId()),
		)
	}

	override suspend fun getDocumentTemplate(documentTemplateId: String): DocumentTemplate? {
		val datastoreInformation = getInstanceAndGroup()
		return documentTemplateDAO.get(datastoreInformation, documentTemplateId)
	}

	override fun getDocumentTemplatesBySpecialty(
		specialityCode: String,
		loadAttachment: Boolean,
	): Flow<DocumentTemplate> = flow {
		val datastoreInformation = getInstanceAndGroup()
		emitAll(
			documentTemplateDAO.listDocumentTemplatesBySpecialtyAndGuid(
				datastoreInformation,
				specialityCode,
				null,
				loadAttachment,
			),
		)
	}

	override fun getDocumentTemplatesByDocumentType(
		documentTypeCode: String,
		loadAttachment: Boolean,
	): Flow<DocumentTemplate> = flow {
		val datastoreInformation = getInstanceAndGroup()
		emitAll(documentTemplateDAO.listDocumentsByTypeUserGuid(datastoreInformation, documentTypeCode, null, null, loadAttachment))
	}

	override fun getDocumentTemplatesByDocumentTypeAndUser(
		documentTypeCode: String,
		userId: String,
		loadAttachment: Boolean,
	): Flow<DocumentTemplate> = flow {
		val datastoreInformation = getInstanceAndGroup()
		emitAll(
			documentTemplateDAO.listDocumentsByTypeUserGuid(
				datastoreInformation,
				documentTypeCode,
				userId,
				null,
				loadAttachment,
			),
		)
	}

	override fun getDocumentTemplatesByUser(
		userId: String,
		loadAttachment: Boolean,
	): Flow<DocumentTemplate> = flow {
		val datastoreInformation = getInstanceAndGroup()
		emitAll(documentTemplateDAO.listDocumentTemplatesByUserGuid(datastoreInformation, userId, null, loadAttachment))
	}

	override suspend fun modifyDocumentTemplate(documentTemplate: DocumentTemplate) = fix(documentTemplate, isCreate = false) { fixedDocumentTemplate ->
		val datastoreInformation = getInstanceAndGroup()
		documentTemplateDAO.save(
			datastoreInformation,
			fixedDocumentTemplate.owner?.let { fixedDocumentTemplate } ?: fixedDocumentTemplate.copy(owner = sessionLogic.getCurrentUserId()),
		)
	}

	override fun getAllDocumentTemplates(paginationOffset: PaginationOffset<String>): Flow<PaginationElement> = flow {
		val datastoreInformation = getInstanceAndGroup()
		emitAll(
			documentTemplateDAO
				.getAllDocumentTemplates(datastoreInformation, paginationOffset.limitIncludingKey())
				.toPaginatedFlow<DocumentTemplate>(paginationOffset.limit),
		)
	}

	override fun getGenericDAO(): DocumentTemplateDAO = documentTemplateDAO
}
