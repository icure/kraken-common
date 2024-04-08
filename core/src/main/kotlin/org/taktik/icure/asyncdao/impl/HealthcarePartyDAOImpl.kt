/*
 * Copyright (c) 2020. Taktik SA, All rights reserved.
 */

package org.taktik.icure.asyncdao.impl

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.fold
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.withIndex
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Repository
import org.taktik.couchdb.ViewQueryResultEvent
import org.taktik.couchdb.annotation.View
import org.taktik.couchdb.dao.DesignDocumentProvider
import org.taktik.couchdb.entity.ComplexKey
import org.taktik.couchdb.id.IDGenerator
import org.taktik.couchdb.queryView
import org.taktik.couchdb.queryViewIncludeDocs
import org.taktik.icure.asyncdao.CouchDbDispatcher
import org.taktik.icure.asyncdao.HealthcarePartyDAO
import org.taktik.icure.asynclogic.datastore.IDatastoreInformation
import org.taktik.icure.cache.EntityCacheFactory
import org.taktik.icure.config.DaoConfig
import org.taktik.icure.db.PaginationOffset
import org.taktik.icure.db.sanitize
import org.taktik.icure.db.sanitizeString
import org.taktik.icure.entities.HealthcareParty
import org.taktik.icure.entities.embed.Identifier

@Repository("healthcarePartyDAO")
@Profile("app")
@View(name = "all", map = "function(doc) { if (doc.java_type == 'org.taktik.icure.entities.HealthcareParty' && !doc.deleted) emit( doc.lastName, doc._id )}")
internal class HealthcarePartyDAOImpl(
	@Qualifier("baseCouchDbDispatcher") couchDbDispatcher: CouchDbDispatcher,
	idGenerator: IDGenerator,
	entityCacheFactory: EntityCacheFactory,
	designDocumentProvider: DesignDocumentProvider,
	daoConfig: DaoConfig
) : GenericDAOImpl<HealthcareParty>(HealthcareParty::class.java, couchDbDispatcher, idGenerator, entityCacheFactory.localAndDistributedCache(HealthcareParty::class.java), designDocumentProvider, daoConfig = daoConfig), HealthcarePartyDAO {

	@View(name = "by_nihii", map = "function(doc) { if (doc.java_type == 'org.taktik.icure.entities.HealthcareParty' && !doc.deleted) emit(doc.nihii.substr(0,8), doc._id )}")
	override fun listHealthcarePartiesByNihii(datastoreInformation: IDatastoreInformation, nihii: String?) = flow {
		val client = couchDbDispatcher.getClient(datastoreInformation)

		emitAll(
			if (nihii == null) {
				flowOf()
			} else {
				val key = if (nihii.length > 8) nihii.substring(0, 8) else nihii
				client.queryViewIncludeDocs<String, String, HealthcareParty>(createQuery(
					datastoreInformation,
					"by_nihii"
				).key(key).includeDocs(true)).map { it.doc }
			}
		)
	}

	@View(name = "by_ssin", map = "function(doc) { if (doc.java_type == 'org.taktik.icure.entities.HealthcareParty' && !doc.deleted) emit(doc.ssin, doc._id )}")
	override fun listHealthcarePartiesBySsin(datastoreInformation: IDatastoreInformation, ssin: String) = flow {
		val client = couchDbDispatcher.getClient(datastoreInformation)

		emitAll(client.queryViewIncludeDocs<String, String, HealthcareParty>(createQuery(
			datastoreInformation,
			"by_ssin"
		).key(ssin).includeDocs(true)).map { it.doc })
	}

	@View(name = "by_speciality_postcode", map = "classpath:js/healthcareparty/By_speciality_postcode.js")
	override fun listHealthcarePartiesBySpecialityAndPostcode(datastoreInformation: IDatastoreInformation, type: String, spec: String, firstCode: String, lastCode: String, offset: PaginationOffset<ComplexKey>) = flow {
		val client = couchDbDispatcher.getClient(datastoreInformation)

		val viewQuery = pagedViewQuery(
			datastoreInformation,
			"by_speciality_postcode",
			ComplexKey.of(type, spec, firstCode),
			ComplexKey.of(type, spec, lastCode),
			offset,
			false
		)
		emitAll(client.queryView(viewQuery, ComplexKey::class.java, String::class.java, HealthcareParty::class.java))
	}

	@View(name = "allForPagination", map = "classpath:js/healthcareparty/All_for_pagination.js")
	override fun findHealthCareParties(datastoreInformation: IDatastoreInformation, pagination: PaginationOffset<String>, desc: Boolean?) = flow {
		val client = couchDbDispatcher.getClient(datastoreInformation)

		val viewQuery = pagedViewQuery(
			datastoreInformation,
			"allForPagination",
			if (pagination.startKey != null) pagination.startKey.toString() else if (desc != null && desc) "\ufff0" else "\u0000",
			if (desc != null && desc) "\u0000" else "\ufff0",
			pagination,
			desc
				?: false
		)

		emitAll(client.queryView(viewQuery, String::class.java, String::class.java, HealthcareParty::class.java))
	}

	@View(name = "by_name", map = "function(doc) { if (doc.java_type == 'org.taktik.icure.entities.HealthcareParty' && !doc.deleted) emit(doc.name, doc._id )}")
	override fun listHealthcarePartiesByName(datastoreInformation: IDatastoreInformation, name: String) = flow {
		val client = couchDbDispatcher.getClient(datastoreInformation)

		emitAll(client.queryViewIncludeDocs<String, String, HealthcareParty>(createQuery(
			datastoreInformation,
			"by_name"
		).key(name).includeDocs(true)).map { it.doc })
	}

	@View(name = "by_ssin_or_nihii", map = "classpath:js/healthcareparty/By_ssin_or_nihii.js")
	override fun findHealthcarePartiesBySsinOrNihii(datastoreInformation: IDatastoreInformation, searchValue: String?, offset: PaginationOffset<String>, desc: Boolean?) = flow {
		val client = couchDbDispatcher.getClient(datastoreInformation)

		val isDesc = desc != null && desc
		val from = if (isDesc) searchValue!! + "\ufff0" else searchValue
		val to = if (searchValue != null) searchValue.takeIf { isDesc } ?: (searchValue + "\ufff0") else "\ufff0"

		val viewQuery = pagedViewQuery(datastoreInformation, "by_ssin_or_nihii", from, to, offset, isDesc)

		emitAll(client.queryView(viewQuery, String::class.java, String::class.java, HealthcareParty::class.java))
	}

	@View(name = "by_hcParty_name", map = "classpath:js/healthcareparty/By_hcparty_name_map.js")
	override fun findHealthcarePartiesByHcPartyNameContainsFuzzy(datastoreInformation: IDatastoreInformation, searchString: String?, offset: PaginationOffset<String>, desc: Boolean?) = flow {
		val client = couchDbDispatcher.getClient(datastoreInformation)

		val r = if (searchString != null) sanitizeString(searchString) else null
		val isDesc = desc != null && desc
		val from = if (offset.startKey == null) if (isDesc) r!! + "\ufff0" else r else offset.startKey
		val to = if (r != null) if (isDesc) r else r + "\ufff0" else if (isDesc) null else "\ufff0"

		val viewQuery = pagedViewQuery(datastoreInformation, "by_hcParty_name", from, to, offset, isDesc)

		emitAll(client.queryView(viewQuery, String::class.java, String::class.java, HealthcareParty::class.java))
	}

	override fun listHealthcareParties(datastoreInformation: IDatastoreInformation, searchString: String, offset: Int, limit: Int) = flow {
		val client = couchDbDispatcher.getClient(datastoreInformation)
		// TODO test
		val r = sanitizeString(searchString)
		val from = ComplexKey.of(r)
		val to = ComplexKey.of(r + "\ufff0")

		emitAll(
			client.queryViewIncludeDocs<String, String, HealthcareParty>(createQuery(
				datastoreInformation,
				"by_hcParty_name"
			).startKey(from).endKey(to).includeDocs(true).limit(limit + offset)).map { it.doc }
				.withIndex().filter { it.index >= offset }.map { it.value }
		)
	}

	@Deprecated("A HCP may now have multiple AES Keys. Use getAesExchangeKeysForDelegate instead")
	@View(name = "by_hcparty_delegate_keys", map = "classpath:js/healthcareparty/By_hcparty_delegate_keys_map.js")
	override suspend fun getHcPartyKeysForDelegate(datastoreInformation: IDatastoreInformation, healthcarePartyId: String): Map<String, String> {
		val client = couchDbDispatcher.getClient(datastoreInformation)

		//Not transactional aware
		val result = client.queryView<String, List<String>>(createQuery(
			datastoreInformation,
			"by_hcparty_delegate_keys"
		).key(healthcarePartyId).includeDocs(false)).mapNotNull { it.value }

		val resultMap = HashMap<String, String>()
		result.collect {
			resultMap[it[0]] = it[1]
		}
		return resultMap
	}

	@View(name = "by_delegate_aes_exchange_keys", map = "classpath:js/healthcareparty/By_delegate_aes_exchange_keys_map.js")
	override suspend fun getAesExchangeKeysForDelegate(datastoreInformation: IDatastoreInformation, healthcarePartyId: String): Map<String, Map<String, Map<String, String>>> {
		val client = couchDbDispatcher.getClient(datastoreInformation)
		val result = client.queryView<String, List<String>>(
			createQuery(datastoreInformation, "by_delegate_aes_exchange_keys")
				.key(healthcarePartyId)
				.includeDocs(false)
		).map { it.key to it.value }

		return result.fold(emptyMap()) { acc, (key, value) ->
			if (key != null && value != null) {
				acc + (
					value[0] to (acc[value[0]] ?: emptyMap()).let {
						it + (
							value[1].let { v -> v.substring((v.length - 32).coerceAtLeast(0)) } to (
								it[value[1]]
									?: emptyMap()
								).let { dels ->
								dels + (value[2] to value[3])
							}
							)
					}
					)
			} else acc
		}
	}

	@View(name = "by_parent", map = "function(doc) { if (doc.java_type == 'org.taktik.icure.entities.HealthcareParty' && !doc.deleted && doc.parentId) emit(doc.parentId, doc._id)}")
	override fun listHealthcarePartiesByParentId(datastoreInformation: IDatastoreInformation, parentId: String) = flow {
		val client = couchDbDispatcher.getClient(datastoreInformation)

		emitAll(client.queryViewIncludeDocs<String, String, HealthcareParty>(createQuery(
			datastoreInformation,
			"by_parent"
		).key(parentId).includeDocs(true)).map { it.doc })
	}

	override fun findHealthcarePartiesByIds(datastoreInformation: IDatastoreInformation, hcpIds: Flow<String>): Flow<ViewQueryResultEvent> = flow {
		val client = couchDbDispatcher.getClient(datastoreInformation)
		emitAll(client.getForPagination(hcpIds, HealthcareParty::class.java))
	}

	@View(name = "by_identifiers", map = "classpath:js/healthcareparty/By_identifier.js")
	override fun listHealthcarePartyIdsByIdentifiers(datastoreInformation: IDatastoreInformation, hcpIdentifiers: List<Identifier>): Flow<String> = flow {
		val client = couchDbDispatcher.getClient(datastoreInformation)

		val queryView = createQuery(datastoreInformation, "by_identifiers")
			.keys(
				hcpIdentifiers.map {
					ComplexKey.of(it.system, it.value)
				}
			)

		emitAll(
			client.queryView<ComplexKey, String>(queryView)
				.mapNotNull {
					if (it.key != null && it.key!!.components.size >= 2) {
						it.id
					} else {
						null
					}
				}
		)
	}

	@View(name = "by_codes", map = "classpath:js/healthcareparty/By_codes.js")
	override fun listHealthcarePartyIdsByCode(datastoreInformation: IDatastoreInformation, codeType: String, codeCode: String?): Flow<String> = flow {
		val client = couchDbDispatcher.getClient(datastoreInformation)

		val from = ComplexKey.of(
			codeType,
			codeCode
		)
		val to = ComplexKey.of(
			codeType,
			codeCode ?: ComplexKey.emptyObject()
		)

		val viewQuery = createQuery(datastoreInformation, "by_codes")
			.startKey(from)
			.endKey(to)
			.includeDocs(false)

		emitAll(client.queryView<Array<String>, String>(viewQuery).mapNotNull { it.value })
	}

	@View(name = "by_tags", map = "classpath:js/healthcareparty/By_tags.js")
	override fun listHealthcarePartyIdsByTag(datastoreInformation: IDatastoreInformation, tagType: String, tagCode: String?): Flow<String> = flow {
		val client = couchDbDispatcher.getClient(datastoreInformation)

		val from = ComplexKey.of(
			tagType,
			tagCode
		)
		val to = ComplexKey.of(
			tagType,
			tagCode ?: ComplexKey.emptyObject()
		)

		val viewQuery = createQuery(datastoreInformation, "by_tags")
			.startKey(from)
			.endKey(to)
			.includeDocs(false)

		emitAll(client.queryView<Array<String>, String>(viewQuery).mapNotNull { it.value })
	}

    override fun listHealthcarePartyIdsByName(datastoreInformation: IDatastoreInformation, name: String, desc: Boolean) = flow {
		val client = couchDbDispatcher.getClient(datastoreInformation)

		val r = name.sanitize() ?: ""
		val from = if (desc) r + "\ufff0" else r
		val to = if (desc) r else r + "\ufff0"

		val viewQuery = createQuery(datastoreInformation, "by_hcParty_name")
			.startKey(from)
			.endKey(to)
			.includeDocs(false)
			.descending(desc)

		emitAll(client.queryView<String, String>(viewQuery).mapNotNull { it.id })
    }
}
