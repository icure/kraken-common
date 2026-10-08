/*
 * Copyright (c) 2020. Taktik SA, All rights reserved.
 */

package org.taktik.icure.config

import org.springframework.beans.factory.ObjectProvider
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile
import org.springframework.http.client.reactive.ReactorResourceFactory
import org.taktik.icure.asyncdao.CouchDbDispatcher
import org.taktik.icure.asyncdao.impl.EntityInfoDAOImpl
import org.taktik.icure.properties.CouchDbProperties
import reactor.netty.resources.ConnectionProvider
import java.time.Duration

@Configuration
@Profile("app")
class SharedCouchDbConfig(
	protected val couchDbProperties: CouchDbProperties,
) {
	/**
	 * @param poolMeterRegistrar if a [ConnectionProvider.MeterRegistrar] bean exists, it receives the live metrics of
	 * every pool created by this provider (one pool per remote CouchDB address).
	 */
	@Bean
	fun connectionProvider(
		poolMeterRegistrar: ObjectProvider<ConnectionProvider.MeterRegistrar>,
	): ConnectionProvider = ConnectionProvider
		.builder("LARGE_POOL")
		.let { builder -> couchDbProperties.maxConnections?.let { maxConn -> builder.maxConnections(maxConn) } ?: builder }
		.maxIdleTime(Duration.ofMillis(couchDbProperties.maxIdleTimeMs ?: 10_000))
		.pendingAcquireMaxCount(couchDbProperties.maxPendingAcquire ?: -1)
		.let { builder -> poolMeterRegistrar.ifAvailable?.let { registrar -> builder.metrics(true) { registrar } } ?: builder }
		.build()

	@Bean
	fun reactorClientResourceFactory(connectionProvider: ConnectionProvider) = ReactorResourceFactory().apply {
		isUseGlobalResources = false
		this.connectionProvider = connectionProvider
	}

	@Bean
	fun baseEntityInfoDao(
		@Qualifier("baseCouchDbDispatcher") baseCouchDbDispatcher: CouchDbDispatcher,
	) = EntityInfoDAOImpl(baseCouchDbDispatcher)

	@Bean
	fun patientEntityInfoDao(
		@Qualifier("patientCouchDbDispatcher") patientCouchDbDispatcher: CouchDbDispatcher,
	) = EntityInfoDAOImpl(patientCouchDbDispatcher)
}
