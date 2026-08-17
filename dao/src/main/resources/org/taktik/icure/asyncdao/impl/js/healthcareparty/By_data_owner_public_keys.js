map = function (doc) {
    if (doc.java_type == 'org.taktik.icure.entities.HealthcareParty' && !doc.deleted) {
        // A single row per healthcare party, holding all of its public keys as { publicKey: algorithmCode }.
        // The algorithm is a numeric code rather than a name to keep the index small: 0 is RSA-OAEP with sha1,
        // 1 is RSA-OAEP with sha256, mapped back by RsaEncryptionAlgorithm.fromViewCode, which must be kept in
        // sync with this. Must also stay in sync with CryptoActor.publicKeysWithAlgorithm(), the same mapping
        // applied to an already loaded crypto actor.
        var pubkeys = {};
        var hasKeys = false;
        // publicKey is normally also one of the aesExchangeKeys entries: the map holds it once either way.
        if (doc.publicKey) {
            pubkeys[doc.publicKey] = 0;
            hasKeys = true;
        }
        if (doc.aesExchangeKeys) {
            for (var publicKey in doc.aesExchangeKeys) {
                pubkeys[publicKey] = 0;
                hasKeys = true;
            }
        }
        // Written last, so that a key declared for both algorithms - which should not happen, a keypair is
        // generated for one scheme - is reported as the sha256 one, the explicit declaration of the two.
        if (doc.publicKeysForOaepWithSha256) {
            for (var i = 0; i < doc.publicKeysForOaepWithSha256.length; i++) {
                pubkeys[doc.publicKeysForOaepWithSha256[i]] = 1;
                hasKeys = true;
            }
        }
        // A healthcare party without any public key produces no row at all, rather than an empty map: a database
        // may hold far more healthcare parties that never act as data owners than ones that do, and indexing them
        // would size this view by the whole collection. Emitting anyway would let a caller tell "no such
        // healthcare party" from "no keys" in one query; that is deliberately given up, see ADR 0008 decision 4.
        if (hasKeys) {
            emit(doc._id, { pubkeys: pubkeys });
        }
    }
}
