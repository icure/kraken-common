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
package org.taktik.icure.services.external.rest.v2.dto

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonInclude
import io.swagger.v3.oas.annotations.media.Schema
import org.taktik.icure.SdkNonNullable
import org.taktik.icure.services.external.rest.v2.dto.base.CodeStubDto
import org.taktik.icure.services.external.rest.v2.dto.base.CryptoActorDto
import org.taktik.icure.services.external.rest.v2.dto.base.DataOwnerDto
import org.taktik.icure.services.external.rest.v2.dto.base.ICureDocumentDto
import org.taktik.icure.services.external.rest.v2.dto.base.IdentifierDto
import org.taktik.icure.services.external.rest.v2.dto.base.NamedDto
import org.taktik.icure.services.external.rest.v2.dto.base.StoredDocumentDto
import org.taktik.icure.services.external.rest.v2.dto.specializations.AesExchangeKeyEncryptionKeypairIdentifierDto
import org.taktik.icure.services.external.rest.v2.dto.specializations.AesExchangeKeyEntryKeyStringDto
import org.taktik.icure.services.external.rest.v2.dto.specializations.HexStringDto
import org.taktik.icure.services.external.rest.v2.dto.specializations.SpkiHexStringDto

@JsonIgnoreProperties(ignoreUnknown = true)
@Schema(
	description = """This entity is a root level object. It represents a device. It is serialized in JSON and saved in the underlying icure-device CouchDB database.""",
)
/**
 * Represents a device that sends medical data. This is a root-level entity stored in the icure-device CouchDB database.
 * A device can act as a data owner and crypto actor for secure data exchange.
 */
data class DeviceDto(
	/** the Id of the device. We encourage using either a v4 UUID or a HL7 Id. */
	override val id: String,
	/** the revision of the device in the database, used for conflict management / optimistic locking. */
	override val rev: String? = null,
	/** hard delete (unix epoch in ms) timestamp of the object. */
	override val deletionDate: Long? = null,
	/** The device's identifiers. Those identifiers are the ones identifying the device for the client. */
	val identifiers: List<IdentifierDto> = emptyList(),
	/** The timestamp (unix epoch in ms) of creation of the device. Enforced by the application server : will be filled automatically if missing. */
	override val created: Long? = null,
	/** the date (unix epoch in ms) of latest modification of the device. Enforced by the application server : will be filled automatically if missing. */
	override val modified: Long? = null,
	/** the id of the User that has created this device. Enforced by the application server : will be filled automatically if missing. */
	override val author: String? = null,
	/** the id of the HealthcareParty that is responsible for this device. Enforced by the application server : will be filled automatically if missing. */
	override val responsible: String? = null,
	/** tags that qualify the device as being member of a certain class. */
	override val tags: Set<CodeStubDto> = emptySet(),
	/** codes that identify or qualify this particular device. */
	override val codes: Set<CodeStubDto> = emptySet(),
	/** soft delete (unix epoch in ms) timestamp of the object. Unused for device. */
	@Deprecated("This field is deprecated for the use with Cardinal SDK")
	val endOfLife: Long? = null,
	/** the medical location where this device has been created. Not used for now. */
	@Deprecated("This field is deprecated for the use with Cardinal SDK")
	override val medicalLocationId: String? = null,
	/** A non-official id for the device. This one is not guaranteed to be unique in databases. */
	@Deprecated("This field is deprecated for the use with Cardinal SDK")
	val externalId: String? = null,
	/** Name of the device. */
	override val name: String? = null,
	/** Type of the device. Could be a smartphone, or a specific medical type sort, ... */
	val type: String? = null, // "persphysician" or "medicalHouse" or "perstechnician"
	/** Brand of the device (Samsung, Apple, Philips, ...) */
	val brand: String? = null,
	/** Model of the device (Galaxy S10, Kino.md, ...) */
	val model: String? = null,
	/** Serial number of the device */
	val serialNumber: String? = null,
	/** Id of parent of the user representing the device. */
	override val parentId: String? = null,
	/** A picture usually saved in JPEG format. */
	@Deprecated("This field is deprecated for the use with Cardinal SDK")
	val picture: ByteArray? = null,
	/** List of typed properties related to the device. Could be its version, specific device information, ... */
	override val properties: Set<PropertyStubDto> = emptySet(),
	/** When a device has access to the medical file for modification or has been given access to it (any time he/she acts as a Crypto Actor), the list of exchange keys with other healthcare parties. */
	override val hcPartyKeys: Map<String, List<HexStringDto>> = emptyMap(),
	/** Extra AES exchange keys, usually keys that were lost access to at some point. */
	override val aesExchangeKeys: Map<AesExchangeKeyEntryKeyStringDto, Map<String, Map<AesExchangeKeyEncryptionKeypairIdentifierDto, HexStringDto>>> = emptyMap(),
	/** Private keys encrypted with public keys for key transfer. */
	override val transferKeys: Map<AesExchangeKeyEncryptionKeypairIdentifierDto, Map<AesExchangeKeyEncryptionKeypairIdentifierDto, HexStringDto>> = emptyMap(),
	/** A set of shamir partitions for this device RSA private keys, encrypted with the public keys of the notaries (referred by their ids). Format is hcpId of key that has been partitioned : "threshold|partition in hex" */
	override val privateKeyShamirPartitions: Map<String, HexStringDto> = emptyMap(), // Format is hcpId of key that has been partitioned : "threshold|partition in hex"
	/** The public RSA key of this device */
	override val publicKey: SpkiHexStringDto? = null,
	/** The public keys of this actor that are generates using the OAEP Sha-256 standard */
	override val publicKeysForOaepWithSha256: Set<SpkiHexStringDto> = emptySet(),
	/** Properties specific to the crypto actor role of this device. */
	@SdkNonNullable @param:JsonInclude(JsonInclude.Include.NON_NULL) override val cryptoActorProperties: Set<PropertyStubDto>? = null,
) : StoredDocumentDto,
	ICureDocumentDto<String>,
	NamedDto,
	CryptoActorDto,
	DataOwnerDto {
	override fun withIdRev(
		id: String?,
		rev: String,
	) = if (id != null) this.copy(id = id, rev = rev) else this.copy(rev = rev)

	override fun withDeletionDate(deletionDate: Long?) = this.copy(deletionDate = deletionDate)
}
