/*
 * Copyright (c) 2020. Taktik SA, All rights reserved.
 */

package org.taktik.icure.services.external.rest.v1.dto.embed

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonInclude
import org.taktik.icure.services.external.rest.v1.dto.base.CodeStubDto
import org.taktik.icure.services.external.rest.v1.dto.base.HasCodesDto
import org.taktik.icure.services.external.rest.v1.dto.base.HasTagsDto
import org.taktik.icure.services.external.rest.v1.dto.base.IdentifiableDto
import java.io.Serializable

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
data class CareTeamMemberDto(
	override val id: String,
	override val tags: Set<CodeStubDto> = emptySet(),
	override val codes: Set<CodeStubDto> = emptySet(),
	val careTeamMemberType: CareTeamMemberTypeDto? = null,
	val healthcarePartyId: String? = null,
	val quality: CodeStubDto? = null,
	override val encryptedSelf: String? = null,
) : EncryptableDto,
	Serializable,
	HasTagsDto,
	HasCodesDto,
	IdentifiableDto<String>
