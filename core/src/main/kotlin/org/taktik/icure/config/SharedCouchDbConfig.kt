/*
 * Copyright (c) 2020. Taktik SA, All rights reserved.
 */

package org.taktik.icure.config

import org.springframework.beans.factory.ObjectProvider
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.context.annotation.Bean
import org.springframework.http.client.ReactorResourceFactory
import org.taktik.icure.asyncdao.CouchDbDispatcher
import org.taktik.icure.asyncdao.impl.EntityInfoDAOImpl
import org.taktik.icure.properties.CouchDbProperties
import reactor.netty.resources.ConnectionProvider
import java.time.Duration

open class SharedCouchDbConfig(
	protected open val couchDbProperties: CouchDbProperties,
) {

	protected fun newConnectionProvider(
		name: String,
		config: CouchDbProperties.ConnectionPoolConfig?,
		poolMeterRegistrar: ObjectProvider<ConnectionProvider.MeterRegistrar>?
	): ConnectionProvider = ConnectionProvider
		.builder(name)
		.let { builder -> config?.maxConnections?.let { maxConn -> builder.maxConnections(maxConn) } ?: builder }
		.maxIdleTime(Duration.ofMillis(config?.maxIdleTimeMs ?: 10_000))
		.pendingAcquireMaxCount(config?.maxPendingAcquire ?: -1)
		.let { builder -> poolMeterRegistrar?.ifAvailable?.let { registrar -> builder.metrics(true) { registrar } } ?: builder }
		.build()

	/**
	 * @param poolMeterRegistrar if a [ConnectionProvider.MeterRegistrar] bean exists, it receives the live metrics of
	 * every pool created by this provider (one pool per remote CouchDB address).
	 */
	@Bean
	fun largePoolConnectionProvider(
		poolMeterRegistrar: ObjectProvider<ConnectionProvider.MeterRegistrar>,
	): ConnectionProvider = newConnectionProvider(
		name = "LARGE_POOL",
		config = couchDbProperties.primaryConnectionPoolConfig(),
		poolMeterRegistrar = poolMeterRegistrar
	)

	@Bean
	fun reactorClientResourceFactory(
		@Qualifier("largePoolConnectionProvider") connectionProvider: ConnectionProvider
	) = ReactorResourceFactory().apply {
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
