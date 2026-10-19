package org.taktik.icure.properties

import java.net.URI

interface CouchDbProperties {
	val url: String
	val altUrls: String
	val preferredUrl: String?
	val username: String?
	val password: String?
	val maxConnections: Int?
	val maxIdleTimeMs: Long?
	val maxPendingAcquire: Int?

	fun knownServerUrls() = if (altUrls.isBlank()) listOf(url) else altUrls.split(";").let { if (it.contains(url)) it else listOf(url) + it }
	fun knownServerUris() = knownServerUrls().map { URI(it) }
	fun preferredServerUrlForNewlyRegisteredDatabase() = preferredUrl ?: knownServerUrls().last()
	fun primaryConnectionPoolConfig() = ConnectionPoolConfig(
		maxConnections = maxConnections,
		maxIdleTimeMs = maxIdleTimeMs,
		maxPendingAcquire = maxPendingAcquire,
	)

	data class ConnectionPoolConfig(
		val maxConnections: Int?,
		val maxIdleTimeMs: Long?,
		val maxPendingAcquire: Int?,
	)
}
