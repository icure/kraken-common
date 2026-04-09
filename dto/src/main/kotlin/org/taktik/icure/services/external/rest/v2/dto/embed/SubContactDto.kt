/*
 *  iCure Data Stack. Copyright (c) 2020 Taktik SA
 *
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU Affero General Public License as
 *     published by the Free Software Foundation, either version 3 of the
 *     License, or (at your option) any later version.
 *
 *     This program is distributed in the hope that it will be useful, but
 *     WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *     Affero General Public License for more details.
 *
 *     You should have received a copy of the GNU Affero General Public
 *     License along with this program.  If not, see
 *     <https://www.gnu.org/licenses/>.
 */
package org.taktik.icure.services.external.rest.v2.dto.embed

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonInclude
import io.swagger.v3.oas.annotations.media.Schema
import org.taktik.icure.services.external.rest.v2.dto.base.CodeStubDto
import org.taktik.icure.services.external.rest.v2.dto.base.ICureDocumentDto
import org.taktik.icure.services.external.rest.v2.dto.specializations.Base64StringDto

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
@Schema(
	description = """This entity represents a sub-contact. It is serialized in JSON and saved in the underlying icure-contact CouchDB database.""",
)
data class SubContactDto(
	/** The Id of the sub-contact. We encourage using either a v4 UUID or a HL7 Id. */
	@param:Schema(description = "The Id of the sub-contact. We encourage using either a v4 UUID or a HL7 Id.") override val id: String? = null,
	/** The timestamp (unix epoch in ms) of creation of the sub-contact, will be filled automatically if missing. Not enforced by the application server. */
	override val created: Long? = null,
	/** The date (unix epoch in ms) of the latest modification of the sub-contact, will be filled automatically if missing. Not enforced by the application server. */
	override val modified: Long? = null,
	/** The id of the User that has created this sub-contact, will be filled automatically if missing. Not enforced by the application server. */
	override val author: String? = null,
	/** The id of the HealthcareParty that is responsible for this sub-contact, will be filled automatically if missing. Not enforced by the application server. */
	override val responsible: String? = null,
	/** The id of the medical location where the sub-contact was created. */
	@Deprecated("This field is deprecated for the use with Cardinal SDK")
	override val medicalLocationId: String? = null,
	/** Tags that qualify the sub-contact as being member of a certain class. */
	override val tags: Set<CodeStubDto> = emptySet(),
	/** Codes that identify or qualify this particular sub-contact. */
	override val codes: Set<CodeStubDto> = emptySet(),
	/** Soft delete (unix epoch in ms) timestamp of the object. */
	@Deprecated("This field is deprecated for the use with Cardinal SDK")
	val endOfLife: Long? = null,
	/** Description of the sub-contact */
	@param:Schema(description = "Description of the sub-contact") val descr: String? = null,
	/** Protocol based on which the sub-contact was used for linking services to structuring elements */
	@param:Schema(description = "Protocol based on which the sub-contact was used for linking services to structuring elements") val protocol: String? = null,
	@Deprecated("This field is deprecated for the use with Cardinal SDK")
	val status: Int? = null, // To be refactored
	/** Id of the form used in the sub-contact. Several sub-contacts with the same form ID can coexist as long as they are in different contacts or they relate to a different planOfActionID */
	@param:Schema(description = "Id of the form used in the sub-contact. Several sub-contacts with the same form ID can coexist as long as they are in different contacts or they relate to a different planOfActionID")
	val formId: String? = null, // form or subform unique ID. Several subcontacts with the same form ID can coexist as long as they are in different contacts or they relate to a different planOfActionID
	/** Id of the plan of action (healthcare approach) that is linked by the sub-contact to a service. */
	@param:Schema(description = "Id of the plan of action (healthcare approach) that is linked by the sub-contact to a service.")
	val planOfActionId: String? = null,
	/** Id of the healthcare element that is linked by the sub-contact to a service */
	@param:Schema(description = "Id of the healthcare element that is linked by the sub-contact to a service")
	val healthElementId: String? = null,
	@Deprecated("This field is deprecated for the use with Cardinal SDK")
	val classificationId: String? = null,
	/** List of all services provided to the patient under a given contact which is linked by this sub-contact to other structuring elements. */
	@param:Schema(description = "List of all services provided to the patient under a given contact which is linked by this sub-contact to other structuring elements.")
	val services: List<ServiceLinkDto> = emptyList(),
	/** The encrypted fields of this sub-contact. */
	override val encryptedSelf: Base64StringDto? = null,
) : EncryptableDto,
	ICureDocumentDto<String?>
