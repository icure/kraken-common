package org.taktik.icure.domain

import java.io.Serializable

data class IndexingInfo(
    val statuses: Map<String, Int>?
): Serializable
