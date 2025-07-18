/*
 * Copyright (c) 2020. Taktik SA, All rights reserved.
 */
package org.taktik.icure.services.external.rest.v1.dto.embed

/**
 * Created by aduchate on 06/07/13, 10:09
 */

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonInclude
import io.swagger.v3.oas.annotations.media.Schema
import org.taktik.icure.services.external.rest.v1.dto.base.CodeStubDto
import org.taktik.icure.services.external.rest.v1.dto.base.ICureDocumentDto

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
@Schema(
	description = """This entity represents a sub-contact. It is serialized in JSON and saved in the underlying icure-contact CouchDB database.""",
)
data class SubContactDto(
	@get:Schema(description = "The Id of the sub-contact. We encourage using either a v4 UUID or a HL7 Id.") override val id: String? = null,
	override val created: Long? = null,
	override val modified: Long? = null,
	override val author: String? = null,
	override val responsible: String? = null,
	override val medicalLocationId: String? = null,
	override val tags: Set<CodeStubDto> = emptySet(),
	override val codes: Set<CodeStubDto> = emptySet(),
	override val endOfLife: Long? = null,
	@get:Schema(description = "Description of the sub-contact") val descr: String? = null,
	@get:Schema(description = "Protocol based on which the sub-contact was used for linking services to structuring elements") val protocol: String? = null,
	val status: Int? = null, // To be refactored
	@get:Schema(
		description = "Id of the form used in the sub-contact. Several sub-contacts with the same form ID can coexist as long as they are in different contacts or they relate to a different planOfActionID",
	) val formId: String? = null, // form or subform unique ID. Several subcontacts with the same form ID can coexist as long as they are in different contacts or they relate to a different planOfActionID
	@get:Schema(description = "Id of the plan of action (healthcare approach) that is linked by the sub-contact to a service.") val planOfActionId: String? = null,
	@get:Schema(description = "Id of the healthcare element that is linked by the sub-contact to a service") val healthElementId: String? = null,
	val classificationId: String? = null,
	@get:Schema(
		description = "List of all services provided to the patient under a given contact which is linked by this sub-contact to other structuring elements.",
	) val services: List<ServiceLinkDto> = emptyList(),
	override val encryptedSelf: String? = null,
) : EncryptableDto,
	ICureDocumentDto<String?>
