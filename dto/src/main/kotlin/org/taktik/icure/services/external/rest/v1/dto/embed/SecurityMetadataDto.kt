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
    val secureDelegations: Map<Sha256HexString, SecureDelegationDto>,
    @get:Schema(description = """Holds aliases for secure delegation keys that apply to this entity: `a -> b` means that anyone with key `a` has access to the
secure delegation in `secureDelegations['b']`.
This map is useful in cases when it is not possible to know for certain if the delegate of a new secure delegation will be able
to produce the access control key we are planning to use. For example the access control key may be produced by a combination of
access control secret and secret foreign key of the entity: what happens if the entity has multiple secret foreign keys? The
delegate may have access to only one of them but not all, so if we chose an unlucky secret foreign key in the creation of the
access control key the delegate will never be able to access the entity. This field allows to essentially create an access control
key and corresponding secure delegation key for each secret foreign key without having to replicate the actual secure delegation.""")
    val keysEquivalences: Map<Sha256HexString, Sha256HexString> = emptyMap()
)

