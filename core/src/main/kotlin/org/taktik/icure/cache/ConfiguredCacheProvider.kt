package org.taktik.icure.cache

import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Service
import org.taktik.icure.entities.base.StoredDocument
import org.taktik.icure.properties.IcureEntitiesCacheProperties

interface ConfiguredCacheProvider {
	fun <T : StoredDocument> getCacheConfiguredForClass(clazz: Class<T>): EntityCacheChainLink<T>?
}

inline fun <reified T : StoredDocument> ConfiguredCacheProvider.getConfiguredCache(): EntityCacheChainLink<T>? =
	getCacheConfiguredForClass(T::class.java)

@Service
@Profile("app")
class ConfiguredCacheProviderImpl(
	private val entityCacheFactory: EntityCacheFactory,
	private val entityCacheProperties: IcureEntitiesCacheProperties
) : ConfiguredCacheProvider {
	override fun <T : StoredDocument> getCacheConfiguredForClass(clazz: Class<T>): EntityCacheChainLink<T>? =
		when (entityCacheProperties.getConfigurationForClass(clazz).type) {
			IcureEntitiesCacheProperties.CacheType.NONE -> null
			IcureEntitiesCacheProperties.CacheType.REQUEST -> entityCacheFactory.localOnlyCache(clazz)
			IcureEntitiesCacheProperties.CacheType.GLOBAL -> entityCacheFactory.localAndDistributedCache(clazz)
		}
}