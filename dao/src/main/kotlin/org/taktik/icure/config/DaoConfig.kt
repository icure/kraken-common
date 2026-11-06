package org.taktik.icure.config

interface DaoConfig {
	val useDataOwnerPartition: Boolean
	val useObsoleteViews: Boolean
	val queryProviderCompatibilityMode: Boolean
	val viewQueryChunkSize: Int get() = 1000
	val viewQueryMaxConcurrency: Int get() = 4
}
