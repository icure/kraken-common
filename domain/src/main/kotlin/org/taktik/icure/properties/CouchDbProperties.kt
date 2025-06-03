package org.taktik.icure.properties

import java.net.URI

interface CouchDbProperties {
    var url: String
    var altUrls: String
    var preferredUrl: String?
    var username: String?
    var password: String?
    var maxConnections: Int?
    var maxIdleTimeMs: Long?
    var maxPendingAcquire: Int?

    fun knownServerUrls() = if (altUrls.isBlank()) listOf(url) else altUrls.split(";").let { if (it.contains(url)) it else listOf(url) + it }
    fun knownServerUris() = knownServerUrls().map { URI(it) }
    fun preferredServerUrlForNewlyRegisteredDatabase() = preferredUrl ?: knownServerUrls().last()
}
