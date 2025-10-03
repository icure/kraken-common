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
import org.taktik.couchdb.annotation.View
import org.taktik.couchdb.dao.DesignDocumentProvider
import org.taktik.couchdb.entity.ComplexKey
import org.taktik.couchdb.id.IDGenerator
import org.taktik.couchdb.queryViewIncludeDocs
import org.taktik.couchdb.queryViewIncludeDocsNoValue
import org.taktik.icure.asyncdao.CouchDbDispatcher
import org.taktik.icure.asyncdao.MAURICE_PARTITION
import org.taktik.icure.asyncdao.TarificationDAO
import org.taktik.icure.cache.ConfiguredCacheProvider
import org.taktik.icure.cache.getConfiguredCache
import org.taktik.icure.config.DaoConfig
import org.taktik.icure.datastore.IDatastoreInformation
import org.taktik.icure.db.PaginationOffset
import org.taktik.icure.db.sanitizeString
import org.taktik.icure.entities.Tarification
import org.taktik.icure.entities.base.Code

@Repository("tarificationDAO")
@Profile("app")
@View(name = "all", map = "function(doc) { if (doc.java_type == 'org.taktik.icure.entities.Tarification' && !doc.deleted) emit( null, doc._id )}")
class TarificationDAOImpl(
	@Qualifier("baseCouchDbDispatcher") couchDbDispatcher: CouchDbDispatcher,
	idGenerator: IDGenerator,
	entityCacheFactory: ConfiguredCacheProvider,
	designDocumentProvider: DesignDocumentProvider,
	daoConfig: DaoConfig,
) : GenericDAOImpl<Tarification>(Tarification::class.java, couchDbDispatcher, idGenerator, entityCacheFactory.getConfiguredCache(), designDocumentProvider, daoConfig = daoConfig),
	TarificationDAO {

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
							version ?: "\u0000",
						),
					)
					.endKey(
						ComplexKey.of(
							type ?: ComplexKey.emptyObject(),
							code ?: ComplexKey.emptyObject(),
							version ?: ComplexKey.emptyObject(),
						),
					),
			).map { it.doc },
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
							version ?: "\u0000",
						),
					)
					.endKey(
						ComplexKey.of(
							region ?: ComplexKey.emptyObject(),
							type ?: ComplexKey.emptyObject(),
							code ?: ComplexKey.emptyObject(),
							version ?: ComplexKey.emptyObject(),
						),
					),
			).map { it.doc },
		)
	}

	override fun findTarificationsBy(
		datastoreInformation: IDatastoreInformation,
		region: String?,
		type: String?,
		code: String?,
		version: String?,
		pagination: PaginationOffset<ComplexKey>,
	) = flow {
		val client = couchDbDispatcher.getClient(datastoreInformation)
		val from = ComplexKey.of(
			region ?: "\u0000",
			type ?: "\u0000",
			code ?: "\u0000",
			version ?: "\u0000",
		)
		val to = ComplexKey.of(
			region?.let { it + "" } ?: ComplexKey.emptyObject(),
			type?.let { it + "" } ?: ComplexKey.emptyObject(),
			code?.let { it + "" } ?: ComplexKey.emptyObject(),
			version?.let { it + "" } ?: ComplexKey.emptyObject(),
		)
		val viewQuery = pagedViewQuery(
			datastoreInformation,
			"by_region_type_code_version",
			from,
			to,
			pagination,
			false,
		)
		emitAll(client.queryView(viewQuery, ComplexKey::class.java, String::class.java, Tarification::class.java))
	}

	@View(name = "by_language_label", map = "classpath:js/tarif/By_language_label.js")
	override fun findTarificationsByLabel(datastoreInformation: IDatastoreInformation, region: String?, language: String?, label: String?, pagination: PaginationOffset<ComplexKey>) = flow {
		val client = couchDbDispatcher.getClient(datastoreInformation)

		val sanitizedLabel = label?.let { sanitizeString(it) }

		val from = ComplexKey.of(
			region ?: "\u0000",
			language ?: "\u0000",
			sanitizedLabel ?: "\u0000",
		)

		val to = ComplexKey.of(
			if (region == null) {
				ComplexKey.emptyObject()
			} else if (language == null) {
				region + "\ufff0"
			} else {
				region
			},
			if (language == null) {
				ComplexKey.emptyObject()
			} else if (sanitizedLabel == null) {
				language + "\ufff0"
			} else {
				language
			},
			if (sanitizedLabel == null) ComplexKey.emptyObject() else sanitizedLabel + "\ufff0",
		)
		val viewQuery = pagedViewQuery(
			datastoreInformation,
			"by_language_label",
			from,
			to,
			pagination,
			false,
		)
		emitAll(client.queryView(viewQuery, ComplexKey::class.java, Integer::class.java, Tarification::class.java))
	}

	@View(name = "by_language_type_label", map = "classpath:js/tarif/By_language_label.js")
	override fun findTarificationsByLabel(datastoreInformation: IDatastoreInformation, region: String?, language: String?, type: String?, label: String?, pagination: PaginationOffset<List<String?>>) = flow {
		val client = couchDbDispatcher.getClient(datastoreInformation)

		val sanitizedLabel = label?.let { sanitizeString(it) }

		val from = ComplexKey.of(
			region ?: "\u0000",
			language ?: "\u0000",
			type ?: "\u0000",
			sanitizedLabel ?: "\u0000",
		)

		val to = ComplexKey.of(
			if (region == null) {
				ComplexKey.emptyObject()
			} else if (language == null) {
				region + "\ufff0"
			} else {
				region
			},
			if (language == null) {
				ComplexKey.emptyObject()
			} else if (type == null) {
				language + "\ufff0"
			} else {
				language
			},
			if (type == null) {
				ComplexKey.emptyObject()
			} else if (sanitizedLabel == null) {
				type + "\ufff0"
			} else {
				language
			},
			if (sanitizedLabel == null) ComplexKey.emptyObject() else sanitizedLabel + "\ufff0",
		)
		val viewQuery = pagedViewQuery(
			datastoreInformation,
			"by_language_label",
			from,
			to,
			pagination.toPaginationOffset { ComplexKey.of(*it.toTypedArray()) },
			false,
		)
		emitAll(client.queryView(viewQuery, Array<String>::class.java, Integer::class.java, Tarification::class.java))
	}

	@View(name = "conflicts", map = "function(doc) { if (doc.java_type == 'org.taktik.icure.entities.Tarification' && !doc.deleted && doc._conflicts) emit(doc._id )}", secondaryPartition = MAURICE_PARTITION)
	override fun listConflicts(datastoreInformation: IDatastoreInformation) = flow {
		val client = couchDbDispatcher.getClient(datastoreInformation)

		val viewQuery = createQuery(datastoreInformation, "conflicts", MAURICE_PARTITION).includeDocs(true)
		emitAll(client.queryViewIncludeDocsNoValue<String, Tarification>(viewQuery).map { it.doc })
	}

}
