package org.taktik.icure.dao

import com.fasterxml.jackson.databind.ObjectMapper
import io.icure.asyncjacksonhttpclient.net.web.WebClient
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.taktik.icure.asyncdao.CouchDbDispatcher
import org.taktik.icure.security.CouchDbCredentialsProvider

interface CouchDbDispatcherProvider {

	@OptIn(ExperimentalCoroutinesApi::class)
	fun getDispatcher(
		httpClient: WebClient,
		objectMapper: ObjectMapper,
		prefix: String,
		dbFamily: String,
		couchDbCredentialsProvider: CouchDbCredentialsProvider,
		createdReplicasIfNotExists: Int
	): CouchDbDispatcher

}