/*
 * Copyright (c) 2020. Taktik SA, All rights reserved.
 */

package org.taktik.icure.entities.embed

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonProperty
import org.taktik.couchdb.id.Identifiable
import org.taktik.icure.entities.base.CodeStub
import org.taktik.icure.utils.DynamicInitializer
import org.taktik.icure.utils.invoke
import org.taktik.icure.validation.AutoFix
import org.taktik.icure.validation.NotBlank
import java.io.Serializable

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
data class CareTeamMember(
	@field:NotBlank(autoFix = AutoFix.UUID) @JsonProperty("_id") override val id: String = "",
	val careTeamMemberType: CareTeamMemberType? = null,
	val healthcarePartyId: String? = null,
	val quality: CodeStub? = null,
	override val encryptedSelf: String? = null,
) : Encryptable,
	Serializable,
	Identifiable<String> {
	companion object : DynamicInitializer<CareTeamMember>

	fun merge(other: CareTeamMember) = CareTeamMember(args = this.solveConflictsWith(other))
	fun solveConflictsWith(other: CareTeamMember) = super.solveConflictsWith(other) +
		mapOf(
			"id" to (this.id),
			"careTeamMemberType" to (this.careTeamMemberType ?: other.careTeamMemberType),
			"healthcarePartyId" to (this.healthcarePartyId ?: other.healthcarePartyId),
			"quality" to (this.quality ?: other.quality),
		)
}
