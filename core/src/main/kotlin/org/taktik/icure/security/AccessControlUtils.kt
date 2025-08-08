package org.taktik.icure.security

import org.apache.commons.codec.digest.DigestUtils

fun hashAccessControlKey(key: ByteArray): String = DigestUtils.sha256Hex(key)
