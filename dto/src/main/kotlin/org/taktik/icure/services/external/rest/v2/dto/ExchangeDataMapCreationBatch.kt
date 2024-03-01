package org.taktik.icure.services.external.rest.v2.dto

import org.taktik.icure.services.external.rest.v2.dto.specializations.AccessControlKeyStringDto
import org.taktik.icure.services.external.rest.v2.dto.specializations.Base64StringDto
import org.taktik.icure.services.external.rest.v2.dto.specializations.KeypairFingerprintV2StringDto

data class ExchangeDataMapCreationBatch(
    /**
     * Each entry of this map can be used to create a new ExchangeDataMap. Each key is the hex-encoded access control
     * key while the value is another map that associated the encrypted ExchangeData id to the fingerprint
     * of the public key used to encrypt it.
     */
    val batch: Map<AccessControlKeyStringDto, Map<KeypairFingerprintV2StringDto, Base64StringDto>> = emptyMap()

)