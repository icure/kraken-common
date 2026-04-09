/*
 * Copyright (c) 2020. Taktik SA, All rights reserved.
 */
package org.taktik.icure.entities.embed

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonProperty
import org.taktik.couchdb.id.Identifiable
import org.taktik.icure.entities.base.Named
import org.taktik.icure.mergers.annotations.MergeStrategyMax
import org.taktik.icure.mergers.annotations.MergeStrategyMin
import org.taktik.icure.mergers.annotations.Mergeable
import org.taktik.icure.validation.AutoFix
import org.taktik.icure.validation.NotNull
import java.io.Serializable
import java.util.*

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
@Mergeable(["id"])
data class Episode(
	/** The unique identifier of this episode. */
	@param:JsonProperty("_id") override val id: String = UUID.randomUUID().toString(), // Autofix if needed when loading from db
	/** The name of the episode. */
	override val name: String? = null,
	/** A comment associated with the episode. */
	val comment: String? = null,
	/** The start date in YYYYMMDDHHMMSS format. Unknown components are set to 00. */
	@MergeStrategyMin
	@field:NotNull(autoFix = AutoFix.FUZZYNOW)
	val startDate: Long? = null, // YYYYMMDDHHMMSS if unknown, 00, ex:20010800000000. Note that to avoid all confusion: 2015/01/02 00:00:00 is encoded as 20140101235960.
	/** The end date in YYYYMMDDHHMMSS format. Unknown components are set to 00. */
	@MergeStrategyMax
	@field:NotNull(autoFix = AutoFix.FUZZYNOW)
	val endDate: Long? = null, // YYYYMMDDHHMMSS if unknown, 00, ex:20010800000000. Note that to avoid all confusion: 2015/01/02 00:00:00 is encoded as 20140101235960.
	/** The base64-encoded encrypted content of this episode. */
	override val encryptedSelf: String? = null,
) : Encryptable,
	Serializable,
	Identifiable<String>,
	Named
