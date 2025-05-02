package org.taktik.icure.services.external.rest.v1.dto.embed

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonInclude
import io.swagger.v3.oas.annotations.media.Schema
import org.taktik.icure.entities.utils.Sha256HexString

@JsonInclude(JsonInclude.Include.NON_EMPTY)
@JsonIgnoreProperties(ignoreUnknown = true)
@Schema(description = """Holds information for user-based access control and encryption of entities.""")
data class SecurityMetadataDto(
    @get:Schema(description = """This maps the hex-encoded sha256 hash of a key created by the client using a certain [ExchangeData.accessControlSecret] to the
[SecureDelegation] for the corresponding delegate-delegator pair. This hash is used by the server to perform access control for
anonymous data owners (see [DataOwnerAuthenticationDetails]) and in some cases also by the sdks to quickly find the appropriate
exchange key needed for the decryption of the content of the corresponding [SecureDelegation].
Note that it is also possible for a secure delegation in this map to have no entry for secretId, encryptionKey or owningEntityId.
This could happen in situations where a user should have access only to the unencrypted content of an entity.""")
    val secureDelegations: Map<Sha256HexString, SecureDelegationDto>
)

