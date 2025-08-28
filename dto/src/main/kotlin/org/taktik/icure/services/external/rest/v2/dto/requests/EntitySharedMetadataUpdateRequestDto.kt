package org.taktik.icure.services.external.rest.v2.dto.requests

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonInclude
import io.swagger.v3.oas.annotations.media.Schema
import org.taktik.icure.services.external.rest.v2.dto.specializations.Base64StringDto
import org.taktik.icure.services.external.rest.v2.dto.specializations.SecureDelegationKeyStringDto

/**
 * Parameters for the update of shared metadata. Currently only changes to secret ids, encryption keys and owning entity
 * ids are allowed. In the future we are going to allow also changes to the permissions of users with access to the
 * shared metadata.
 */
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
@JsonIgnoreProperties(ignoreUnknown = false)
data class EntitySharedMetadataUpdateRequestDto(
	/**
	 * Access control hash of the metadata to update.
	 */
	@param:Schema(required = true)
	val metadataAccessControlHash: SecureDelegationKeyStringDto,
	/**
	 * Updates for secret ids: the key is an encrypted secret id and the value is if an entry with that encrypted secret
	 * id should be created or deleted.
	 */
	val secretIds: Map<Base64StringDto, EntryUpdateTypeDto> = emptyMap(),
	/**
	 * Updates for encryption keys: a key in the map is an encrypted encryption key and the value is if an entry with
	 * that encrypted encryption key should be created or deleted.
	 */
	val encryptionKeys: Map<Base64StringDto, EntryUpdateTypeDto> = emptyMap(),
	/**
	 * Updates for owning entity ids: the key is the encrypted id of an owning entity and the value is if an entry with
	 * that encrypted owning entity id should be created or deleted.
	 */
	val owningEntityIds: Map<Base64StringDto, EntryUpdateTypeDto> = emptyMap(),
)

/**
 * Specifies if an entry should be created anew or deleted
 */
enum class EntryUpdateTypeDto {
	CREATE,
	DELETE,
}
