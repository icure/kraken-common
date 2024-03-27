/*
 * Copyright (c) 2020. Taktik SA, All rights reserved.
 */

package org.taktik.icure.entities.base

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonProperty
import org.taktik.icure.entities.embed.Encrypted
import org.taktik.icure.entities.embed.TypedValue
import java.io.Serializable

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
data class PropertyStub(
	val id: String? = null,
	val type: PropertyTypeStub? = null,
	val typedValue: TypedValue? = null,
	@Deprecated("Remove from list instead") @JsonProperty("deleted") val deletionDate: Long? = null,
	override val encryptedSelf: String? = null
) : Serializable, Encrypted
