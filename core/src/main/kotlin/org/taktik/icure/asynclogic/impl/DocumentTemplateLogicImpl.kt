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
import org.taktik.icure.asynclogic.datastore.DatastoreInstanceProvider
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
    fixer: Fixer
) : GenericLogicImpl<DocumentTemplate, DocumentTemplateDAO>(fixer, datastoreInstanceProvider), DocumentTemplateLogic {

	override fun createEntities(entities: Collection<DocumentTemplate>): Flow<DocumentTemplate> =
		flow {
			emitAll(
				super.createEntities(
					entities.map { dt ->
						fix(dt) { e ->
							e.owner?.let { e } ?: e.copy(owner = sessionLogic.getCurrentUserId())
						}
					}
				)
			)
		}

	override suspend fun createDocumentTemplate(entity: DocumentTemplate): DocumentTemplate {
		return fix(entity) { documentTemplate ->
			val datastoreInformation = getInstanceAndGroup()
			documentTemplateDAO.createDocumentTemplate(datastoreInformation, documentTemplate.owner?.let { documentTemplate } ?: documentTemplate.copy(owner = sessionLogic.getCurrentUserId()))
		}
	}

	override suspend fun getDocumentTemplate(documentTemplateId: String): DocumentTemplate? {
		val datastoreInformation = getInstanceAndGroup()
		return documentTemplateDAO.get(datastoreInformation, documentTemplateId)
	}

	override fun getDocumentTemplatesBySpecialty(specialityCode: String): Flow<DocumentTemplate> =
		flow {
			val datastoreInformation = getInstanceAndGroup()
			emitAll(documentTemplateDAO.listDocumentTemplatesBySpecialtyAndGuid(datastoreInformation, specialityCode, null))
		}

	override fun getDocumentTemplatesByDocumentType(documentTypeCode: String): Flow<DocumentTemplate>  =
		flow {
			val datastoreInformation = getInstanceAndGroup()
			emitAll(documentTemplateDAO.listDocumentsByTypeUserGuid(datastoreInformation, documentTypeCode, null, null))
		}

	override fun getDocumentTemplatesByDocumentTypeAndUser(documentTypeCode: String, userId: String): Flow<DocumentTemplate> =
		flow {
			val datastoreInformation = getInstanceAndGroup()
			emitAll(documentTemplateDAO.listDocumentsByTypeUserGuid(datastoreInformation, documentTypeCode, userId, null))
		}

	override fun getDocumentTemplatesByUser(userId: String): Flow<DocumentTemplate>  =
		flow {
			val datastoreInformation = getInstanceAndGroup()
			emitAll(documentTemplateDAO.listDocumentTemplatesByUserGuid(datastoreInformation, userId, null))
		}

	override suspend fun modifyDocumentTemplate(documentTemplate: DocumentTemplate) =
		fix(documentTemplate) { fixedDocumentTemplate ->
			val datastoreInformation = getInstanceAndGroup()
			documentTemplateDAO.save(datastoreInformation, fixedDocumentTemplate.owner?.let { fixedDocumentTemplate } ?: fixedDocumentTemplate.copy(owner = sessionLogic.getCurrentUserId()))
		}

	override fun getAllDocumentTemplates(paginationOffset: PaginationOffset<String>): Flow<PaginationElement> = flow {
		val datastoreInformation = getInstanceAndGroup()
		emitAll(documentTemplateDAO
			.getAllDocumentTemplates(datastoreInformation, paginationOffset.limitIncludingKey())
			.toPaginatedFlow<DocumentTemplate>(paginationOffset.limit)
		)
	}

	override fun getGenericDAO(): DocumentTemplateDAO {
		return documentTemplateDAO
	}
}
