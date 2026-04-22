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
import org.taktik.icure.entities.RawJson
import org.taktik.icure.services.external.rest.v2.dto.base.ExtendableDto
import org.taktik.icure.services.external.rest.v2.dto.base.IdentifiableDto
import org.taktik.icure.services.external.rest.v2.dto.base.NamedDto
import org.taktik.icure.services.external.rest.v2.dto.specializations.Base64StringDto
import java.io.Serializable

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
/**
 * Represents a medical episode, which is a time-bounded grouping of healthcare elements related to a specific concern.
 */
data class EpisodeDto(
	/** The unique identifier of this episode. */
	override val id: String,
	/** The name of the episode. */
	override val name: String? = null,
	/** A comment associated with the episode. */
	val comment: String? = null,
	/** The start date in YYYYMMDDHHMMSS format. Unknown components are set to 00. */
	var startDate: Long? = null, // YYYYMMDDHHMMSS if unknown, 00, ex:20010800000000. Note that to avoid all confusion: 2015/01/02 00:00:00 is encoded as 20140101235960.
	/** The end date in YYYYMMDDHHMMSS format. Unknown components are set to 00. */
	var endDate: Long? = null, // YYYYMMDDHHMMSS if unknown, 00, ex:20010800000000. Note that to avoid all confusion: 2015/01/02 00:00:00 is encoded as 20140101235960.
	/** The base64-encoded encrypted content of this episode. */
	override val encryptedSelf: Base64StringDto? = null,
	override val extensions: RawJson.JsonObject? = null,
) : EncryptableDto,
	Serializable,
	IdentifiableDto<String>,
	NamedDto,
	ExtendableDto
