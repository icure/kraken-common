/*
 * Copyright (c) 2020. Taktik SA, All rights reserved.
 */

package org.taktik.icure.services.external.rest.v1.dto.base

import io.swagger.v3.oas.annotations.media.Schema
import org.taktik.icure.services.external.rest.v1.dto.embed.DelegationDto
import org.taktik.icure.services.external.rest.v1.dto.embed.SecurityMetadataDto

interface EncryptableDto: VersionableDto<String> {
	@get:Schema(description = "The secretForeignKeys are filled at the to many end of a one to many relationship (for example inside Contact for the Patient -> Contacts relationship). Used when we want to find all contacts for a specific patient. These keys are in clear. You can have several to partition the medical document space.")
	val secretForeignKeys: Set<String>

	@get:Schema(description = "The secretForeignKeys are filled at the to many end of a one to many relationship (for example inside Contact for the Patient -> Contacts relationship). Used when we want to find the patient for a specific contact. These keys are the encrypted id (using the hcParty key for the delegate) that can be found in clear inside the patient. ids encrypted using the hcParty keys.")
	val cryptedForeignKeys: Map<String, Set<DelegationDto>>

	@get:Schema(description = "When a document is created, the responsible generates a cryptographically random master key (never to be used for something else than referencing from other entities). He/she encrypts it using his own AES exchange key and stores it as a delegation. The responsible is thus always in the delegations as well")
	val delegations: Map<String, Set<DelegationDto>>

	@get:Schema(description = "When a document needs to be encrypted, the responsible generates a cryptographically random master key (different from the delegation key, never to appear in clear anywhere in the db. He/she encrypts it using his own AES exchange key and stores it as a delegation")
	val encryptionKeys: Map<String, Set<DelegationDto>>

	@get:Schema(description = "The base64 encoded data of this object, formatted as JSON and encrypted in AES using the random master key from encryptionKeys.")
	val encryptedSelf: String?

	@get:Schema(description = """Security metadata for the entity, contains metadata necessary for access control.
In [Encryptable] entities this is also used to store additional encrypted metadata on the entity, including encryption keys for the
[Encryptable.encryptedSelf] (replacing [Encryptable.encryptionKeys]), owning entity id (replacing [Encryptable.cryptedForeignKeys]),
and secret id (replacing the keys of [Encryptable.delegations]).""")
	val securityMetadata: SecurityMetadataDto?
}
