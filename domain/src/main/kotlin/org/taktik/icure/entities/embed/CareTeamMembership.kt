/*
 * Copyright (c) 2020. Taktik SA, All rights reserved.
 */

package org.taktik.icure.entities.embed

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonInclude
import java.io.Serializable

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
data class CareTeamMembership(
	/** The start date (unix epoch in ms) of this membership. */
	val startDate: Long? = null,
	/** The end date (unix epoch in ms) of this membership. */
	val endDate: Long? = null,
	/** The identifier of the care team member. */
	val careTeamMemberId: String? = null,
	/** The type of membership. */
	val membershipType: MembershipType? = null,
	/** The base64-encoded encrypted content of this membership. */
	override val encryptedSelf: String? = null,
) : Encryptable,
	Serializable
