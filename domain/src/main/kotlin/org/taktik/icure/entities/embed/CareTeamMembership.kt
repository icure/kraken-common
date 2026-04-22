/*
 * Copyright (c) 2020. Taktik SA, All rights reserved.
 */

package org.taktik.icure.entities.embed

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonInclude
import org.taktik.icure.entities.RawJson
import org.taktik.icure.entities.base.Extendable
import java.io.Serializable

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
data class CareTeamMembership(
	val startDate: Long? = null,
	val endDate: Long? = null,
	val careTeamMemberId: String? = null,
	val membershipType: MembershipType? = null,
	override val encryptedSelf: String? = null,
	override val extensions: RawJson.JsonObject? = null,
) : Encryptable,
	Serializable,
	Extendable
