/*
 *  iCure Data Stack. Copyright (c) 2020 Taktik SA
 *
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU Affero General Public License as
 *     published by the Free Software Foundation, either version 3 of the
 *     License, or (at your option) any later version.
 *
 *     This program is distributed in the hope that it will be useful, but
 *     WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *     Affero General Public License for more details.
 *
 *     You should have received a copy of the GNU Affero General Public
 *     License along with this program.  If not, see
 *     <https://www.gnu.org/licenses/>.
 */
package org.taktik.icure.services.external.rest.v2.dto

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.databind.JsonNode
import org.taktik.icure.services.external.rest.v2.dto.base.StoredDocumentDto

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
/**
 * Represents a reusable template for creating entities. Entity templates store a JSON-based entity definition
 * that can be used as a starting point for creating new entities of a given type.
 */
data class EntityTemplateDto(
	/** The Id of the entity template. */
	override val id: String,
	/** The revision of the entity template in the database, used for conflict management / optimistic locking. */
	override val rev: String? = null,
	/** Hard delete (unix epoch in ms) timestamp of the object. */
	override val deletionDate: Long? = null,
	/** The id of the user who owns this entity template. */
	var userId: String? = null,
	/** A description of the entity template. */
	val descr: String? = null,
	/** A set of keywords for searching and categorizing the template. */
	val keywords: Set<String>? = null,
	/** The type of entity this template is for. */
	var entityType: String? = null,
	/** The sub-type of entity this template is for. */
	var subType: String? = null,
	/** Whether this is the default template for its entity type and sub-type. */
	var defaultTemplate: Boolean? = null,
	/** The JSON representation of the template entity content. */
	var entity: List<JsonNode> = emptyList(),
) : StoredDocumentDto {
	override fun withIdRev(
		id: String?,
		rev: String,
	) = if (id != null) this.copy(id = id, rev = rev) else this.copy(rev = rev)

	override fun withDeletionDate(deletionDate: Long?) = this.copy(deletionDate = deletionDate)
}
