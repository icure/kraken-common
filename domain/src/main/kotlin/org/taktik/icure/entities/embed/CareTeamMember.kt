/*
 * Copyright (c) 2020. Taktik SA, All rights reserved.
 */

package org.taktik.icure.entities.embed

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonProperty
import org.taktik.couchdb.id.Identifiable
import org.taktik.icure.entities.base.CodeStub
import org.taktik.icure.entities.base.HasCodes
import org.taktik.icure.entities.base.HasTags
import org.taktik.icure.mergers.annotations.Mergeable
import org.taktik.icure.validation.AutoFix
import org.taktik.icure.validation.NotBlank
import org.taktik.icure.validation.ValidCode
import java.io.Serializable

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
@Mergeable(["id"])
data class CareTeamMember(
	@field:NotBlank(autoFix = AutoFix.UUID) @param:JsonProperty("_id") override val id: String = "",
	@field:ValidCode(autoFix = AutoFix.NORMALIZECODE) override val tags: Set<CodeStub> = emptySet(),
	@field:ValidCode(autoFix = AutoFix.NORMALIZECODE) override val codes: Set<CodeStub> = emptySet(),
	val careTeamMemberType: CareTeamMemberType? = null,
	val healthcarePartyId: String? = null,
	val quality: CodeStub? = null,
	override val encryptedSelf: String? = null,
) : Encryptable,
	Serializable,
	HasTags,
	HasCodes,
	Identifiable<String>
