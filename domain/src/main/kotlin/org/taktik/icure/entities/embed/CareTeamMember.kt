/*
 * Copyright (c) 2020. Taktik SA, All rights reserved.
 */

package org.taktik.icure.entities.embed

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonProperty
import org.taktik.couchdb.id.Identifiable
import org.taktik.icure.entities.base.CodeStub
import org.taktik.icure.mergers.annotations.Mergeable
import org.taktik.icure.validation.AutoFix
import org.taktik.icure.validation.NotBlank
import java.io.Serializable

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
@Mergeable(["id"])
data class CareTeamMember(
	/** The unique identifier of this care team member. */
	@field:NotBlank(autoFix = AutoFix.UUID) @param:JsonProperty("_id") override val id: String = "",
	/** The type of care team member (physician, specialist, or other). */
	val careTeamMemberType: CareTeamMemberType? = null,
	/** The identifier of the associated healthcare party. */
	val healthcarePartyId: String? = null,
	/** A code describing the quality or qualification of this care team member. */
	val quality: CodeStub? = null,
	/** The base64-encoded encrypted content of this care team member. */
	override val encryptedSelf: String? = null,
) : Encryptable,
	Serializable,
	Identifiable<String>
