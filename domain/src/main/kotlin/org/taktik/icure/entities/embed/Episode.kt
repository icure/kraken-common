/*
 * Copyright (c) 2020. Taktik SA, All rights reserved.
 */
package org.taktik.icure.entities.embed

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonProperty
import org.taktik.couchdb.id.Identifiable
import org.taktik.icure.entities.base.Named
import org.taktik.icure.utils.DynamicInitializer
import org.taktik.icure.utils.invoke
import org.taktik.icure.validation.AutoFix
import org.taktik.icure.validation.NotNull
import java.io.Serializable
import java.util.UUID

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
data class Episode(
	@param:JsonProperty("_id") override val id: String = UUID.randomUUID().toString(), // Autofix if needed when loading from db
	override val name: String? = null,
	val comment: String? = null,
	@field:NotNull(autoFix = AutoFix.FUZZYNOW) var startDate: Long? = null, // YYYYMMDDHHMMSS if unknown, 00, ex:20010800000000. Note that to avoid all confusion: 2015/01/02 00:00:00 is encoded as 20140101235960.
	@field:NotNull(autoFix = AutoFix.FUZZYNOW) var endDate: Long? = null, // YYYYMMDDHHMMSS if unknown, 00, ex:20010800000000. Note that to avoid all confusion: 2015/01/02 00:00:00 is encoded as 20140101235960.
	override val encryptedSelf: String? = null,
) : Encryptable,
	Serializable,
	Identifiable<String>,
	Named {
	companion object : DynamicInitializer<Episode>

	fun merge(other: Episode) = Episode(args = this.solveConflictsWith(other))
	fun solveConflictsWith(other: Episode) = super.solveConflictsWith(other) +
		mapOf(
			"id" to (this.id),
			"name" to (this.name ?: other.name),
			"comment" to (this.comment ?: other.comment),
			"startDate" to (startDate?.coerceAtMost(other.startDate ?: Long.MAX_VALUE) ?: other.startDate),
			"endDate" to (endDate?.coerceAtLeast(other.endDate ?: 0L) ?: other.endDate),
		)
}
