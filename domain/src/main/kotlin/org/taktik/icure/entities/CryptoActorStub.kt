package org.taktik.icure.entities

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonInclude
import org.taktik.couchdb.entity.Versionable
import org.taktik.icure.entities.base.CodeStub
import org.taktik.icure.entities.base.CryptoActor
import org.taktik.icure.entities.base.HasTags
import org.taktik.icure.entities.base.PropertyStub

/**
 * Holds only data specific for crypto actors without any additional information (from patient, hcparty, device).
 */
@JsonInclude(JsonInclude.Include.NON_EMPTY)
@JsonIgnoreProperties(ignoreUnknown = true)
data class CryptoActorStub(
    override val id: String,
    override val rev: String, // Stubs can't be created, but only updated or retrieved: rev is never null.
    override val hcPartyKeys: Map<String, List<String>> = emptyMap(),
    override val aesExchangeKeys: Map<String, Map<String, Map<String, String>>> = emptyMap(),
    override val transferKeys: Map<String, Map<String, String>> = emptyMap(),
    override val privateKeyShamirPartitions: Map<String, String> = emptyMap(),
    override val publicKey: String? = null,
    override val publicKeysForOaepWithSha256: Set<String> = emptySet(),
    override val revHistory: Map<String, String>? = null,
    override val parentId: String? = null,
    override val cryptoActorProperties: Set<PropertyStub>? = null,
) : Versionable<String>, CryptoActor {
    override fun withIdRev(id: String?, rev: String): CryptoActorStub =
        copy(id = id ?: this.id, rev = rev)
}
