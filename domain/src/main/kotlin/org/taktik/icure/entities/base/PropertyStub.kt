/*
 * Copyright (c) 2020. Taktik SA, All rights reserved.
 */

package org.taktik.icure.entities.base

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonProperty
import org.taktik.icure.entities.embed.Encryptable
import org.taktik.icure.entities.embed.TypedValue
import org.taktik.icure.mergers.annotations.Mergeable
import java.io.Serializable

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
@Mergeable(["id"])
data class PropertyStub(
	/** The unique identifier of the property stub. */
	val id: String? = null,
	/** The type stub definition of this property. */
	val type: PropertyTypeStub? = null,
	/** The typed value held by this property. */
	val typedValue: TypedValue? = null,

	/** The soft-delete timestamp in epoch milliseconds. Deprecated: remove from list instead. */
	@Deprecated("Remove from list instead")
	@param:JsonProperty("deleted")
	val deletionDate: Long? = null,

	/** The encrypted content of this property, encoded as a Base64 string. */
	override val encryptedSelf: String? = null,
) : Serializable, Encryptable
