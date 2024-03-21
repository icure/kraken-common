/*
 * Copyright (c) 2020. Taktik SA, All rights reserved.
 */
package org.taktik.icure.services.external.rest.v1.dto

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonInclude
import org.taktik.icure.entities.embed.TypedValuesType
import org.taktik.icure.services.external.rest.v1.dto.base.StoredDocumentDto
import org.taktik.icure.services.external.rest.v1.dto.constants.PropertyTypeScopeDto

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
data class PropertyTypeDto(
    override val id: String,
    override val rev: String? = null,
    override val deletionDate: Long? = null,

    val identifier: String,
    val type: TypedValuesType? = null,
    val scope: PropertyTypeScopeDto? = null,
    val unique: Boolean = false,
    val editor: String? = null,
    val localized: Boolean = false
) : StoredDocumentDto {
	override fun withIdRev(id: String?, rev: String) = if (id != null) this.copy(id = id, rev = rev) else this.copy(rev = rev)
	override fun withDeletionDate(deletionDate: Long?) = this.copy(deletionDate = deletionDate)
}
