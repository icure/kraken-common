package org.taktik.icure.entities.utils

/**
 * A string which represents some data encoded using standard base-64 (with + and /, but possibly without trailing =).
 */
typealias Base64String = String

/**
 * A string which represents some data encoded in hexadecimal.
 */
typealias HexString = String

/**
 * A string which represents a sha256 hash encoded in hexadecimal.
 */
typealias Sha256HexString = String

/**
 * Fingerprint of an RSA key pair. This is the last 32 characters of the public key represented in hex-encoded spki format.
 */
typealias KeypairFingerprintString = String

/**
 * Fingerprint V2 of an RSA key pair. This is the last 32 characters of the public key represented in hex-encoded spki format,
 * minus the last 10 characters that are common for the SPKI format.
 */
typealias KeypairFingerprintV2String = String
