/*
 * Copyright (c) 2020. Taktik SA, All rights reserved.
 */

package org.taktik.icure.asynclogic

import kotlinx.coroutines.flow.Flow
import org.taktik.couchdb.DocIdentifier
import org.taktik.couchdb.entity.ComplexKey
import org.taktik.icure.asynclogic.base.EntityWithSecureDelegationsLogic
import org.taktik.icure.db.PaginationOffset
import org.taktik.icure.entities.ClassificationTemplate
import org.taktik.icure.entities.embed.Delegation
import org.taktik.icure.pagination.PaginationElement

interface ClassificationTemplateLogic : EntityPersister<ClassificationTemplate> {
	suspend fun createClassificationTemplate(classificationTemplate: ClassificationTemplate): ClassificationTemplate?

	suspend fun getClassificationTemplate(classificationTemplateId: String): ClassificationTemplate?
	fun getClassificationTemplates(ids: Collection<String>): Flow<ClassificationTemplate>

	/**
	 * Retrieves all the [ClassificationTemplate]s in a group in a format for pagination.
	 *
	 * @param paginationOffset a [PaginationOffset] of [ComplexKey] for pagination.
	 * @return a [Flow] of [PaginationElement] wrapping the [ClassificationTemplate]s.
	 */
	fun listClassificationTemplates(paginationOffset: PaginationOffset<String>): Flow<PaginationElement>
}
