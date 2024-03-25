package org.taktik.icure.services.external.rest.v2.dto.specializations

/**
 * A string that represents the keypair used for the encryption of an aes exchange key entry. This should usually be
 * a fingerprint v1, but due to bugs in older iCure version it may also be a public key in hex-encoded spki format, or
 * due to corruption of some healthcare parties public key it may also be an empty string, to represent the fact that
 * the key used for the encryption is unknown.
 */
typealias AesExchangeKeyEncryptionKeypairIdentifierDto = String

typealias Base64StringDto = String

typealias HexStringDto = String

typealias Sha256HexStringDto = String

typealias AccessControlSecretDto = String

typealias AccessControlKeyHexStringDto = String

typealias SecureDelegationKeyStringDto = String

typealias SpkiHexStringDto = String

typealias KeypairFingerprintV1StringDto = String

typealias KeypairFingerprintV2StringDto = String