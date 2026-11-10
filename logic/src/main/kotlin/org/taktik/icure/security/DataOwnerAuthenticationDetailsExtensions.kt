package org.taktik.icure.security

import org.apache.commons.codec.digest.DigestUtils

val DataOwnerAuthenticationDetails.accessControlKeysHashes: Set<String> get() =
	accessControlKeys.map { DigestUtils.sha256Hex(it) }.toSet()
