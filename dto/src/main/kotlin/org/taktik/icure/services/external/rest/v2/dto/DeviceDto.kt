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
import org.taktik.icure.entities.RawJson
import org.taktik.icure.services.external.rest.v2.dto.base.CodeStubDto
import org.taktik.icure.services.external.rest.v2.dto.base.CryptoActorDto
import org.taktik.icure.services.external.rest.v2.dto.base.DataOwnerDto
import org.taktik.icure.services.external.rest.v2.dto.base.ICureDocumentDto
import org.taktik.icure.services.external.rest.v2.dto.base.IdentifierDto
import org.taktik.icure.services.external.rest.v2.dto.base.NamedDto
import org.taktik.icure.services.external.rest.v2.dto.base.StoredDocumentDto
import org.taktik.icure.services.external.rest.v2.dto.base.ExtendableRootDto
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
	/** The Id of the device. We encourage using either a v4 UUID or a HL7 Id. */
	override val id: String,
	/** The revision of the device in the database, used for conflict management / optimistic locking. */
	override val rev: String? = null,
	/** Hard delete (unix epoch in ms) timestamp of the object. */
	override val deletionDate: Long? = null,
	/** The device's identifiers used for client-side identification. */
	val identifiers: List<IdentifierDto> = emptyList(),
	/** The timestamp (unix epoch in ms) of creation of this entity. */
	override val created: Long? = null,
	/** The timestamp (unix epoch in ms) of the latest modification of this entity. */
	override val modified: Long? = null,
	/** The id of the User that created this device. */
	override val author: String? = null,
	/** The id of the data owner that is responsible for this device. */
	override val responsible: String? = null,
	/** Tags that qualify the device as being member of a certain class. */
	override val tags: Set<CodeStubDto> = emptySet(),
	/** Codes that identify or qualify this particular device. */
	override val codes: Set<CodeStubDto> = emptySet(),
	/** Soft delete (unix epoch in ms) timestamp of the object. */
	@Deprecated("This field is deprecated for the use with Cardinal SDK")
	val endOfLife: Long? = null,
	/** The medical location where this entity was created. */
	@Deprecated("This field is deprecated for the use with Cardinal SDK")
	override val medicalLocationId: String? = null,
	/** A non-official external id for the device, not guaranteed to be unique. */
	@Deprecated("This field is deprecated for the use with Cardinal SDK")
	val externalId: String? = null,
	/** The name of the device. */
	override val name: String? = null,
	/** The type of the device (e.g., smartphone, medical device sort). */
	val type: String? = null, // "persphysician" or "medicalHouse" or "perstechnician"
	/** The brand of the device (e.g., Samsung, Apple, Philips). */
	val brand: String? = null,
	/** The model of the device (e.g., Galaxy S10). */
	val model: String? = null,
	/** The serial number of the device. */
	val serialNumber: String? = null,
	/** The id of the parent of the user representing the device. */
	override val parentId: String? = null,
	/** A picture of the device, usually in JPEG format. */
	@Deprecated("This field is deprecated for the use with Cardinal SDK")
	val picture: ByteArray? = null,
	/** Typed properties related to the device (e.g., version, specific device information). */
	override val properties: Set<PropertyStubDto> = emptySet(),
	/** The exchange keys with other healthcare parties, encrypted using public keys. */
	override val hcPartyKeys: Map<String, List<HexStringDto>> = emptyMap(),
	/** Extra AES exchange keys, usually keys that were lost access to at some point. */
	override val aesExchangeKeys: Map<AesExchangeKeyEntryKeyStringDto, Map<String, Map<AesExchangeKeyEncryptionKeypairIdentifierDto, HexStringDto>>> = emptyMap(),
	/** Private keys encrypted with public keys for key transfer. */
	override val transferKeys: Map<AesExchangeKeyEncryptionKeypairIdentifierDto, Map<AesExchangeKeyEncryptionKeypairIdentifierDto, HexStringDto>> = emptyMap(),
	/** Shamir partitions of this device's RSA private keys, encrypted with notary public keys. */
	override val privateKeyShamirPartitions: Map<String, HexStringDto> = emptyMap(), // Format is hcpId of key that has been partitioned : "threshold|partition in hex"
	/** The public RSA key of this device. */
	override val publicKey: SpkiHexStringDto? = null,
	/** The public keys of this device generated using OAEP with SHA-256. */
	override val publicKeysForOaepWithSha256: Set<SpkiHexStringDto> = emptySet(),
	/** Properties specific to the crypto actor role of this device. */
	@SdkNonNullable @param:JsonInclude(JsonInclude.Include.NON_NULL) override val cryptoActorProperties: Set<PropertyStubDto>? = null,
	override val extensions: RawJson.JsonObject? = null,
	override val extensionsVersion: Int? = null,
) : StoredDocumentDto,
	ICureDocumentDto<String>,
	NamedDto,
	CryptoActorDto,
	DataOwnerDto,
	ExtendableRootDto {
	override fun withIdRev(
		id: String?,
		rev: String,
	) = if (id != null) this.copy(id = id, rev = rev) else this.copy(rev = rev)

	override fun withDeletionDate(deletionDate: Long?) = this.copy(deletionDate = deletionDate)
}
