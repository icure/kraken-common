/*
 * Copyright (c) 2020. Taktik SA, All rights reserved.
 */

package org.taktik.icure.asyncdao.impl

import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Repository
import org.taktik.couchdb.ViewQueryResultEvent
import org.taktik.couchdb.annotation.View
import org.taktik.couchdb.dao.DesignDocumentProvider
import org.taktik.couchdb.entity.ComplexKey
import org.taktik.couchdb.id.IDGenerator
import org.taktik.couchdb.queryViewIncludeDocs
import org.taktik.icure.asyncdao.CouchDbDispatcher
import org.taktik.icure.asyncdao.TarificationDAO
import org.taktik.icure.asynclogic.datastore.IDatastoreInformation
import org.taktik.icure.cache.EntityCacheFactory
import org.taktik.icure.db.PaginationOffset
import org.taktik.icure.db.sanitizeString
import org.taktik.icure.entities.Tarification

@Repository("tarificationDAO")
@Profile("app")
@View(name = "all", map = "function(doc) { if (doc.java_type == 'org.taktik.icure.entities.Tarification' && !doc.deleted) emit( null, doc._id )}")
class TarificationDAOImpl(
	@Qualifier("baseCouchDbDispatcher") couchDbDispatcher: CouchDbDispatcher,
	idGenerator: IDGenerator,
	entityCacheFactory: EntityCacheFactory,
	designDocumentProvider: DesignDocumentProvider
) : GenericDAOImpl<Tarification>(Tarification::class.java, couchDbDispatcher, idGenerator, entityCacheFactory.localOnlyCache(Tarification::class.java), designDocumentProvider), TarificationDAO {

	@View(name = "by_type_code_version", map = "classpath:js/tarif/By_type_code_version.js", reduce = "_count")
	override fun listTarificationsBy(datastoreInformation: IDatastoreInformation, type: String?, code: String?, version: String?) = flow {
		val client = couchDbDispatcher.getClient(datastoreInformation)
		emitAll(
			client.queryViewIncludeDocs<ComplexKey, String, Tarification>(
				createQuery(datastoreInformation, "by_type_code_version")
					.includeDocs(true)
					.reduce(false)
					.startKey(
						ComplexKey.of(
							type ?: "\u0000",
							code ?: "\u0000",
							version ?: "\u0000"
						)
					)
					.endKey(
						ComplexKey.of(
							type ?: ComplexKey.emptyObject(),
							code ?: ComplexKey.emptyObject(),
							version ?: ComplexKey.emptyObject()
						)
					)
			).map { it.doc }
		)
	}

	@View(name = "by_region_type_code_version", map = "classpath:js/tarif/By_region_type_code_version.js", reduce = "_count")
	override fun listTarificationsBy(datastoreInformation: IDatastoreInformation, region: String?, type: String?, code: String?, version: String?) = flow {
		val client = couchDbDispatcher.getClient(datastoreInformation)
		emitAll(
			client.queryViewIncludeDocs<Array<String>, String, Tarification>(
				createQuery(datastoreInformation, "by_region_type_code_version")
					.includeDocs(true)
					.reduce(false)
					.startKey(
						ComplexKey.of(
							region ?: "\u0000",
							type ?: "\u0000",
							code ?: "\u0000",
							version ?: "\u0000"
						)
					)
					.endKey(
						ComplexKey.of(
							region ?: ComplexKey.emptyObject(),
							type ?: ComplexKey.emptyObject(),
							code ?: ComplexKey.emptyObject(),
							version ?: ComplexKey.emptyObject()
						)
					)
			).map { it.doc }
		)
	}

	override fun findTarificationsBy(
		datastoreInformation: IDatastoreInformation,
		region: String?,
		type: String?,
		code: String?,
		version: String?,
		pagination: PaginationOffset<List<String?>>
	) = flow<ViewQueryResultEvent> {
		val client = couchDbDispatcher.getClient(datastoreInformation)
		val from = ComplexKey.of(
			region ?: "\u0000",
			type ?: "\u0000",
			code ?: "\u0000",
			version ?: "\u0000"
		)
		val to = ComplexKey.of(
			region?.let { it + "" } ?: ComplexKey.emptyObject(),
			type?.let { it + "" } ?: ComplexKey.emptyObject(),
			code?.let { it + "" } ?: ComplexKey.emptyObject(),
			version?.let { it + "" } ?: ComplexKey.emptyObject()
		)
		val viewQuery = pagedViewQuery(
			datastoreInformation,
			"by_region_type_code_version",
			from,
			to,
			pagination.toPaginationOffset { ComplexKey.of(*it.toTypedArray()) },
			false
		)
		emitAll(client.queryView(viewQuery, ComplexKey::class.java, String::class.java, Tarification::class.java))
	}

	@View(name = "by_language_label", map = "classpath:js/tarif/By_language_label.js")
	override fun findTarificationsByLabel(datastoreInformation: IDatastoreInformation, region: String?, language: String?, label: String?, pagination: PaginationOffset<List<String?>>) = flow {
		val client = couchDbDispatcher.getClient(datastoreInformation)

		val label = label?.let { sanitizeString(it) }

		val startKey = pagination.startKey?.toMutableList()
		startKey?.takeIf { it.size > 2 }?.get(2)?.let { startKey[2] = sanitizeString(it) }
		val from = ComplexKey.of(
			region ?: "\u0000",
			language ?: "\u0000",
			label ?: "\u0000"
		)

		val to = ComplexKey.of(
			if (region == null) ComplexKey.emptyObject() else if (language == null) region + "\ufff0" else region,
			if (language == null) ComplexKey.emptyObject() else if (label == null) language + "\ufff0" else language,
			if (label == null) ComplexKey.emptyObject() else label + "\ufff0"
		)
		val viewQuery = pagedViewQuery(
			datastoreInformation,
			"by_language_label",
			from,
			to,
			pagination.toPaginationOffset { ComplexKey.of(*it.toTypedArray()) },
			false
		)
		emitAll(client.queryView(viewQuery, Array<String>::class.java, Integer::class.java, Tarification::class.java))
	}

	@View(name = "by_language_type_label", map = "classpath:js/tarif/By_language_label.js")
	override fun findTarificationsByLabel(datastoreInformation: IDatastoreInformation, region: String?, language: String?, type: String?, label: String?, pagination: PaginationOffset<List<String?>>) = flow<ViewQueryResultEvent> {
		val client = couchDbDispatcher.getClient(datastoreInformation)

		val label = label?.let { sanitizeString(it) }

		val startKey = pagination.startKey?.toMutableList()
		startKey?.takeIf { it.size > 3 }?.get(3)?.let { startKey[3] = sanitizeString(it) }
		val from = ComplexKey.of(
			region ?: "\u0000",
			language ?: "\u0000",
			type ?: "\u0000",
			label ?: "\u0000"
		)

		val to = ComplexKey.of(
			if (region == null) ComplexKey.emptyObject() else if (language == null) region + "\ufff0" else region,
			if (language == null) ComplexKey.emptyObject() else if (type == null) language + "\ufff0" else language,
			if (type == null) ComplexKey.emptyObject() else if (label == null) type + "\ufff0" else language,
			if (label == null) ComplexKey.emptyObject() else label + "\ufff0"
		)
		val viewQuery = pagedViewQuery(
			datastoreInformation,
			"by_language_label",
			from,
			to,
			pagination.toPaginationOffset { ComplexKey.of(*it.toTypedArray()) },
			false
		)
		emitAll(client.queryView(viewQuery, Array<String>::class.java, Integer::class.java, Tarification::class.java))
	}
}
