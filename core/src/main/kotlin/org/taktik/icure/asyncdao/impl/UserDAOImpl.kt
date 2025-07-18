/*
 * Copyright (c) 2020. Taktik SA, All rights reserved.
 */

package org.taktik.icure.asyncdao.impl

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.onCompletion
import org.springframework.beans.factory.annotation.Qualifier
import org.taktik.couchdb.TotalCount
import org.taktik.couchdb.ViewQueryResultEvent
import org.taktik.couchdb.ViewRowWithDoc
import org.taktik.couchdb.annotation.View
import org.taktik.couchdb.dao.DesignDocumentProvider
import org.taktik.couchdb.exception.DocumentNotFoundException
import org.taktik.couchdb.id.IDGenerator
import org.taktik.couchdb.queryView
import org.taktik.couchdb.queryViewIncludeDocsNoValue
import org.taktik.couchdb.queryViewNoValue
import org.taktik.icure.asyncdao.CouchDbDispatcher
import org.taktik.icure.asyncdao.MAURICE_PARTITION
import org.taktik.icure.asyncdao.UserDAO
import org.taktik.icure.datastore.IDatastoreInformation
import org.taktik.icure.cache.ConfiguredCacheProvider
import org.taktik.icure.cache.getConfiguredCache
import org.taktik.icure.config.DaoConfig
import org.taktik.icure.db.PaginationOffset
import org.taktik.icure.entities.User

// Differences between lite and cloud version: instantiated as a bean in the respective DAOConfig
@View(name = "all", map = "function(doc) { if (doc.java_type == 'org.taktik.icure.entities.User' && !doc.deleted) emit( null, doc._rev )}")
open class UserDAOImpl(
	@Qualifier("baseCouchDbDispatcher") couchDbDispatcher: CouchDbDispatcher,
	idGenerator: IDGenerator,
	entityCacheFactory: ConfiguredCacheProvider,
	designDocumentProvider: DesignDocumentProvider,
	daoConfig: DaoConfig
) : GenericDAOImpl<User>(User::class.java, couchDbDispatcher, idGenerator, entityCacheFactory.getConfiguredCache(), designDocumentProvider, daoConfig = daoConfig), UserDAO {

	@View(name = "by_username", map = "function(doc) {  if (doc.java_type == 'org.taktik.icure.entities.User' && !doc.deleted) {emit(doc.login, null)}}")
	override fun listUsersByUsername(datastoreInformation: IDatastoreInformation, username: String) = flow {
		val client = couchDbDispatcher.getClient(datastoreInformation)

		emitAll(client.queryViewIncludeDocsNoValue<String, User>(createQuery(datastoreInformation, "by_username").includeDocs(true).key(username)).mapNotNull { it.doc })
	}

	override fun findUsedUsernames(
		datastoreInformation: IDatastoreInformation,
		usernames: Collection<String>
	): Flow<String> = flow {
		val client = couchDbDispatcher.getClient(datastoreInformation)

		emitAll(client.queryViewNoValue<String>(createQuery(datastoreInformation, "by_username").includeDocs(false).keys(usernames)).mapNotNull { it.key })
	}

	@View(name = "by_email", map = "function(doc) {  if (doc.java_type == 'org.taktik.icure.entities.User' && !doc.deleted) {emit(doc.email, null)}}")
	override fun listUsersByEmail(datastoreInformation: IDatastoreInformation, searchString: String) = flow {
		val client = couchDbDispatcher.getClient(datastoreInformation)

		emitAll(client.queryViewIncludeDocsNoValue<String, User>(createQuery(datastoreInformation, "by_email").includeDocs(true).key(searchString)).mapNotNull { it.doc })
	}

	override fun findUsedEmails(datastoreInformation: IDatastoreInformation, emails: Collection<String>): Flow<String> = flow {
		val client = couchDbDispatcher.getClient(datastoreInformation)

		emitAll(client.queryViewNoValue<String>(createQuery(datastoreInformation, "by_email").includeDocs(false).keys(emails)).mapNotNull { it.key })
	}

	private fun normalizePhone(phone: String): String =
		phone.trim().let { if (it.startsWith("+")) "+${it.substring(1).replace(Regex("[^0-9]"), "")}" else it.replace(Regex("[^0-9]"), "") }

	@View(name = "by_phone", map = "classpath:js/user/By_phone.js")
	override fun listUsersByPhone(datastoreInformation: IDatastoreInformation, phone: String): Flow<User> = flow {
		val client = couchDbDispatcher.getClient(datastoreInformation)
		emitAll(client.queryViewIncludeDocsNoValue<String, User>(createQuery(datastoreInformation, "by_phone").includeDocs(true).key(normalizePhone(phone))).mapNotNull { it.doc })
	}

	override fun findUsedPhones(datastoreInformation: IDatastoreInformation, phones: Collection<String>): Flow<String> = flow {
		val client = couchDbDispatcher.getClient(datastoreInformation)
		emitAll(client.queryViewNoValue<String>(createQuery(datastoreInformation, "by_phone").includeDocs(false).key(phones.map { normalizePhone(it) })).mapNotNull { it.key })
	}

	/**
	 * startKey in pagination is the email of the patient.
	 */
	@View(name = "allForPagination", map = "map = function (doc) { if (doc.java_type == 'org.taktik.icure.entities.User' && !doc.deleted) { emit(doc.login, null); }};")
	override fun findUsers(datastoreInformation: IDatastoreInformation, pagination: PaginationOffset<String>, skipPatients: Boolean): Flow<ViewQueryResultEvent> = findUsers(datastoreInformation, pagination, skipPatients, 1f, 0, false)

	private fun findUsers(datastoreInformation: IDatastoreInformation, pagination: PaginationOffset<String>, skipPatients: Boolean, extensionFactor: Float, prevTotalCount: Int, isContinuation: Boolean): Flow<ViewQueryResultEvent> = flow {
		val client = couchDbDispatcher.getClient(datastoreInformation)

		var seenElements = 0
		var sentElements = 0
		var totalCount = 0
		var latestResult: ViewRowWithDoc<*, *, *>? = null
		var skipped = false
		val extendedLimit = (pagination.limit * extensionFactor).toInt()

		val viewQuery = pagedViewQuery(
			datastoreInformation, "allForPagination", null, "\ufff0",
			pagination.copy(limit = extendedLimit),
			false
		)
		emitAll(client.queryView(viewQuery, String::class.java, Nothing::class.java, User::class.java).let { flw ->
			if (!skipPatients) flw else flw.filter {
				when (it) {
					is ViewRowWithDoc<*, *, *> -> {
						latestResult = it
						seenElements++
						if (skipped || !isContinuation) {
							if (((it.doc as User).patientId == null || (it.doc as User).healthcarePartyId != null) && sentElements < pagination.limit) {
								sentElements++
								true
							} else false
						} else {
							skipped = true
							false
						}
					}

					is TotalCount -> {
						totalCount = it.total
						false
					}

					else -> true
				}
			}.onCompletion {
				if ((seenElements >= extendedLimit) && (sentElements < seenElements)) {
					emitAll(
						findUsers(
							datastoreInformation,
							pagination.copy(startKey = latestResult?.key as? String, startDocumentId = latestResult?.id, limit = pagination.limit - sentElements),
							true,
							(if (seenElements == 0) extensionFactor * 2 else (seenElements.toFloat() / sentElements)).coerceAtMost(100f),
							totalCount + prevTotalCount,
							true
						)
					)
				} else {
					emit(TotalCount(totalCount + prevTotalCount))
				}
			}
		})
	}

	@View(name = "by_hcp_id", map = "classpath:js/user/By_hcp_id.js")
	override fun listUsersByHcpId(datastoreInformation: IDatastoreInformation, hcPartyId: String) = flow {
		val client = couchDbDispatcher.getClient(datastoreInformation)

		emitAll(client.queryViewIncludeDocsNoValue<String, User>(createQuery(datastoreInformation, "by_hcp_id").key(hcPartyId).includeDocs(true)).map { it.doc })
	}

	override fun listUserIdsByHcpId(datastoreInformation: IDatastoreInformation, hcPartyId: String): Flow<String> = flow {
		val client = couchDbDispatcher.getClient(datastoreInformation)

		emitAll(client.queryView<String, String>(
			createQuery(datastoreInformation, "by_hcp_id")
				.key(hcPartyId)
				.includeDocs(false)
		).map { it.id })

	}

	@View(name = "by_patient_id", map = "classpath:js/user/by_patient_id.js")
	override fun listUsersByPatientId(datastoreInformation: IDatastoreInformation, patientId: String): Flow<User> = flow {
		val client = couchDbDispatcher.getClient(datastoreInformation)

		emitAll(client.queryViewIncludeDocsNoValue<String, User>(createQuery(datastoreInformation, "by_patient_id").key(patientId).includeDocs(true)).map { it.doc })
	}

	override fun listUserIdsByPatientId(datastoreInformation: IDatastoreInformation, patientId: String): Flow<String> = flow {
		val client = couchDbDispatcher.getClient(datastoreInformation)

		emitAll(client.queryView<String, String>(
			createQuery(datastoreInformation, "by_patient_id")
				.key(patientId)
				.includeDocs(false)
		).map { it.id })

	}

	@View(name = "by_name_email_phone", map = "classpath:js/user/By_name_email_phone.js")
	override fun listUserIdsByNameEmailPhone(datastoreInformation: IDatastoreInformation, searchString: String): Flow<String> = flow {
		val client = couchDbDispatcher.getClient(datastoreInformation)
		emitAll(client.queryView<String, Int>(createQuery(datastoreInformation, "by_name_email_phone").startKey(searchString).endKey("$searchString\ufff0").includeDocs(false)).map { it.id })
	}

	override fun findUsersByNameEmailPhone(datastoreInformation: IDatastoreInformation, searchString: String, pagination: PaginationOffset<String>) = flow {
		val client = couchDbDispatcher.getClient(datastoreInformation)

		val viewQuery = pagedViewQuery(
			datastoreInformation,
			"by_name_email_phone",
			searchString,
			"$searchString\ufff0",
			pagination,
			false
		)
		emitAll(client.queryView(viewQuery, String::class.java, Nothing::class.java, User::class.java))
	}

	override suspend fun getUserOnUserDb(datastoreInformation: IDatastoreInformation, userId: String, bypassCache: Boolean): User {
		val client = couchDbDispatcher.getClient(datastoreInformation)
		val value = if (bypassCache) null
		else cacheChain?.getEntity(datastoreInformation.getFullIdFor(userId))

		return value
			?: (client.get(userId, User::class.java)?.also {
				cacheChain?.putInCache(datastoreInformation.getFullIdFor(userId), it)
			} ?: throw DocumentNotFoundException(userId))
	}

	override suspend fun findUserOnUserDb(datastoreInformation: IDatastoreInformation, userId: String, bypassCache: Boolean): User? {
		val client = couchDbDispatcher.getClient(datastoreInformation)
		val value = if (bypassCache) null
			else cacheChain?.getEntity(datastoreInformation.getFullIdFor(userId))

		return value
			?: client.get(userId, User::class.java)?.also {
				cacheChain?.putInCache(datastoreInformation.getFullIdFor(userId), it)
			}
	}

	override fun getUsersOnDb(datastoreInformation: IDatastoreInformation) = flow {
		val client = couchDbDispatcher.getClient(datastoreInformation)

		emitAll(client.queryViewIncludeDocsNoValue<String, User>(createQuery(datastoreInformation, "all").includeDocs(true)).map { it.doc })
	}

	override fun findUsersByIds(datastoreInformation: IDatastoreInformation, userIds: Flow<String>): Flow<ViewQueryResultEvent> = flow {
		val client = couchDbDispatcher.getClient(datastoreInformation)
		emitAll(client.getForPagination(userIds, User::class.java))
	}

	@View(name = "conflicts", map = "function(doc) { if (doc.java_type == 'org.taktik.icure.entities.User' && !doc.deleted && doc._conflicts) emit(doc._id )}", secondaryPartition = MAURICE_PARTITION)
	override fun listConflicts(datastoreInformation: IDatastoreInformation) = flow {
		val client = couchDbDispatcher.getClient(datastoreInformation)

		val viewQuery = createQuery(datastoreInformation, "conflicts", MAURICE_PARTITION).includeDocs(true)
		emitAll(client.queryViewIncludeDocsNoValue<String, User>(viewQuery).map { it.doc })
	}

}
