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
import io.swagger.v3.oas.annotations.media.Schema
import org.taktik.icure.services.external.rest.v2.dto.base.StoredDocumentDto
import org.taktik.icure.services.external.rest.v2.dto.embed.FrontEndMigrationStatusDto
import com.fasterxml.jackson.annotation.JsonFilter
import org.taktik.icure.dto.annotations.filtering.ActiveField

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
/**
 * Represents a front-end migration task. A front-end migration tracks the progress of data migration operations
 * initiated from the front-end application.
 */
@JsonFilter("org.taktik.icure.services.external.rest.v2.dto.FrontEndMigrationDto")
data class FrontEndMigrationDto(
	/** The unique identifier of the front-end migration. */
	override val id: String,
	/** The revision of the front-end migration in the database, used for conflict management / optimistic locking. */
	override val rev: String? = null,
	/** Hard delete (unix epoch in ms) timestamp of the object. */
	override val deletionDate: Long? = null,
	/** The name of the migration. */
	@ActiveField val name: String? = null,
	/** The start date (unix epoch in ms) of the migration. */
	@ActiveField val startDate: Long? = null,
	/** The end date (unix epoch in ms) of the migration. */
	@ActiveField val endDate: Long? = null,
	/** The current status of the migration. */
	@ActiveField val status: FrontEndMigrationStatusDto? = null,
	/** Logs produced during the migration process. */
	@ActiveField val logs: String? = null,
	/** The id of the user that initiated the migration. */
	@ActiveField val userId: String? = null,
	/** The start key used for pagination during migration. */
	@ActiveField val startKey: String? = null,
	/** The start key document id used for pagination during migration. */
	@ActiveField val startKeyDocId: String? = null,
	/** The number of items processed during the migration. */
	@ActiveField val processCount: Long? = null,
	@param:Schema(
		description = "Extra properties for the fem. Those properties are typed (see class Property)",
	/** Extra properties for the front-end migration. Those properties are typed (see class Property). */
	) @ActiveField val properties: Set<PropertyStubDto> =
		emptySet(),
) : StoredDocumentDto {
	override fun withIdRev(
		id: String?,
		rev: String,
	) = if (id != null) this.copy(id = id, rev = rev) else this.copy(rev = rev)

	override fun withDeletionDate(deletionDate: Long?) = this.copy(deletionDate = deletionDate)
}
