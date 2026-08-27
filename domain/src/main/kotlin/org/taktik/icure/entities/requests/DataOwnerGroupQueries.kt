package org.taktik.icure.entities.requests

import org.taktik.icure.entities.base.CryptoActor
import org.taktik.icure.entities.base.DataOwnerGroupLinkType
import org.taktik.icure.entities.utils.HexString

/**
 * A data owner that declares a direct link to one of the queried data owner groups, through the legacy
 * [CryptoActor.parentId] or a [CryptoActor.dataOwnerGroups] entry.
 *
 * Group membership is transitive, but this is **not**: only data owners declaring a link to a queried group
 * are reported, never the data owners linked to *them*. Following the chain is up to the caller, which has to
 * decide whether it wants to, based on [groupLinkType].
 */
data class LinkedDataOwner(
	val dataOwnerId: String,
	/**
	 * The [CryptoActor.groupLinkType] of this data owner, that is the type any link pointing at *it* has, or
	 * null if it is the default for the type of this data owner (see
	 * [org.taktik.icure.entities.base.effectiveGroupLinkType]). Null is not a fourth type: a caller that needs
	 * a concrete type resolves it through the data owner type of the query.
	 */
	val groupLinkType: DataOwnerGroupLinkType? = null,
)

/**
 * The public keys of a data owner, each with the encryption algorithm it must be used with.
 */
data class DataOwnerPublicKeys(
	val dataOwnerId: String,
	val publicKeys: List<PublicKeyInfo>,
)

/**
 * A public key of a data owner and the encryption algorithm it must be used with. A key appears at most once in
 * a [DataOwnerPublicKeys]: a keypair is generated for one scheme, so a key declared for both is reported with
 * the stronger one (see [publicKeysWithAlgorithm]).
 */
data class PublicKeyInfo(
	/**
	 * The public key, in hex-encoded spki format.
	 */
	val publicKey: HexString,
	val algorithm: RsaEncryptionAlgorithm,
)

/**
 * An algorithm a public key of a data owner may be used with.
 *
 * This is an enum rather than a "uses sha256" boolean so that a third scheme can be introduced without a
 * breaking change to the wire format.
 */
enum class RsaEncryptionAlgorithm {
	/**
	 * RSA-OAEP with sha1. The keys of [CryptoActor.aesExchangeKeys] and [CryptoActor.publicKey] use this
	 * algorithm; they are considered legacy starting from v8 of the SDK.
	 */
	OaepWithSha1,

	/**
	 * RSA-OAEP with sha256. The keys in [CryptoActor.publicKeysForOaepWithSha256] use this algorithm.
	 */
	OaepWithSha256,
	;

	companion object {
		/**
		 * The algorithm a numeric code emitted by the `by_data_owner_public_keys` couchdb view of the healthcare
		 * party dao stands for. The view indexes a code rather than a name to keep the index small, so **this
		 * mapping is part of the stored index**: a code may never be reused for a different algorithm, and a new
		 * algorithm takes a new code (which also means a reader that predates it has to fail rather than guess,
		 * hence the exception below rather than a null).
		 * @throws IllegalStateException if [code] is not one this version knows about.
		 */
		fun fromViewCode(code: Int): RsaEncryptionAlgorithm = when (code) {
			0 -> OaepWithSha1
			1 -> OaepWithSha256
			else -> throw IllegalStateException("Unknown public key algorithm code $code in the by_data_owner_public_keys view")
		}
	}
}

/**
 * All the public keys of this crypto actor, each with the algorithm it must be used with: [CryptoActor.publicKey]
 * and the keys of [CryptoActor.aesExchangeKeys] use [RsaEncryptionAlgorithm.OaepWithSha1], the keys of
 * [CryptoActor.publicKeysForOaepWithSha256] use [RsaEncryptionAlgorithm.OaepWithSha256].
 *
 * A key is reported **once**, even when it is declared several times — as both [CryptoActor.publicKey] and an
 * [CryptoActor.aesExchangeKeys] entry, which is the normal case, or for both algorithms, which should not happen
 * since a keypair is generated for one scheme. In that last case it is reported as
 * [RsaEncryptionAlgorithm.OaepWithSha256], the explicit declaration of the two.
 *
 * This is the reference definition of the mapping between a crypto actor's stored keys and their algorithms: the
 * `by_data_owner_public_keys` couchdb view of the healthcare party dao computes the same thing, down to which
 * declaration wins, and must be kept in sync with it.
 */
fun CryptoActor.publicKeysWithAlgorithm(): List<PublicKeyInfo> = buildMap {
	publicKey?.let { put(it, RsaEncryptionAlgorithm.OaepWithSha1) }
	aesExchangeKeys.keys.forEach { put(it, RsaEncryptionAlgorithm.OaepWithSha1) }
	// Put last, so that a key declared for both algorithms is reported as the sha256 one.
	publicKeysForOaepWithSha256.forEach { put(it, RsaEncryptionAlgorithm.OaepWithSha256) }
}.map { (publicKey, algorithm) -> PublicKeyInfo(publicKey, algorithm) }
