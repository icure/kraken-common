/*
 * Copyright (c) 2020. Taktik SA, All rights reserved.
 */

package org.taktik.icure.asyncdao.impl

import kotlinx.coroutines.flow.*
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Repository
import org.taktik.couchdb.*
import org.taktik.couchdb.annotation.View
import org.taktik.couchdb.annotation.Views
import org.taktik.couchdb.dao.DesignDocumentProvider
import org.taktik.couchdb.entity.ComplexKey
import org.taktik.couchdb.id.IDGenerator
import org.taktik.icure.asyncdao.ContactDAO
import org.taktik.icure.asyncdao.CouchDbDispatcher
import org.taktik.icure.asyncdao.DATA_OWNER_PARTITION
import org.taktik.icure.asynclogic.datastore.IDatastoreInformation
import org.taktik.icure.cache.EntityCacheFactory
import org.taktik.icure.db.PaginationOffset
import org.taktik.icure.domain.ContactIdServiceId
import org.taktik.icure.entities.Contact
import org.taktik.icure.entities.embed.Identifier
import org.taktik.icure.entities.embed.Service
import org.taktik.icure.utils.*

@Repository("contactDAO")
@Profile("app")
@View(name = "all", map = "function(doc) { if (doc.java_type == 'org.taktik.icure.entities.Contact' && !doc.deleted) emit( null, doc._id )}")
class ContactDAOImpl(
	@Qualifier("healthdataCouchDbDispatcher") couchDbDispatcher: CouchDbDispatcher,
	idGenerator: IDGenerator,
	entityCacheFactory: EntityCacheFactory,
	designDocumentProvider: DesignDocumentProvider
) : GenericDAOImpl<Contact>(Contact::class.java, couchDbDispatcher, idGenerator, entityCacheFactory.localOnlyCache(Contact::class.java), designDocumentProvider), ContactDAO {

	companion object {
		private const val TERTIARY_CONTACT_PARTITION = "Maurice"
	}

	override suspend fun getContact(datastoreInformation: IDatastoreInformation, id: String): Contact? {
		return get(datastoreInformation, id)
	}

	override fun getContacts(datastoreInformation: IDatastoreInformation, contactIds: Flow<String>): Flow<Contact> = flow {
		val client = couchDbDispatcher.getClient(datastoreInformation)
		emitAll(client.get(contactIds, Contact::class.java))
	}

	override fun getContacts(datastoreInformation: IDatastoreInformation, contactIds: Collection<String>): Flow<Contact> = flow {
		val client = couchDbDispatcher.getClient(datastoreInformation)
		emitAll(client.get(contactIds, Contact::class.java))
	}

	@Views(
    	View(name = "by_hcparty_openingdate", map = "classpath:js/contact/By_hcparty_openingdate.js"),
    	View(name = "by_data_owner_openingdate", map = "classpath:js/contact/By_data_owner_openingdate.js", secondaryPartition = DATA_OWNER_PARTITION),
	)
	override fun listContactsByOpeningDate(datastoreInformation: IDatastoreInformation, hcPartyId: String, startOpeningDate: Long?, endOpeningDate: Long?, pagination: PaginationOffset<ComplexKey>) = flow {
		val client = couchDbDispatcher.getClient(datastoreInformation)

		val startKey = ComplexKey.of(hcPartyId, startOpeningDate)
		val endKey = ComplexKey.of(hcPartyId, endOpeningDate ?: ComplexKey.emptyObject())

		val viewQueries = createPagedQueries(
			datastoreInformation,
			"by_hcparty_openingdate",
			"by_data_owner_openingdate" to DATA_OWNER_PARTITION,
			startKey,
			endKey,
			pagination,
			false
		)
		emitAll(client.interleave<ComplexKey, String, Contact>(viewQueries, compareBy({it.components[0] as String}, {(it.components[1] as? Number)?.toLong()})))
	}

	@Views(
		View(name = "by_hcparty", map = "classpath:js/contact/By_hcparty.js"),
		View(name = "by_data_owner", map = "classpath:js/contact/By_data_owner.js", secondaryPartition = DATA_OWNER_PARTITION),
	)
	override fun findContactsByHcParty(datastoreInformation: IDatastoreInformation, hcPartyId: String, pagination: PaginationOffset<String>) = flow {
		val client = couchDbDispatcher.getClient(datastoreInformation)
		val viewQueries = createPagedQueries(
			datastoreInformation,
			"by_hcparty",
			"by_data_owner" to DATA_OWNER_PARTITION,
			hcPartyId,
			hcPartyId,
			pagination,
			false
		)
        emitAll(client.interleave<String, String, Contact>(viewQueries, compareBy({ it })))
	}

	@Views(
    	View(name = "by_hcparty_identifier", map = "classpath:js/contact/By_hcparty_identifier.js"),
    	View(name = "by_data_owner_identifier", map = "classpath:js/contact/By_data_owner_identifier.js", secondaryPartition = DATA_OWNER_PARTITION),
	)
	override fun listContactIdsByHcPartyAndIdentifiers(datastoreInformation: IDatastoreInformation, searchKeys: Set<String>, identifiers: List<Identifier>): Flow<String> =
		flow {
			val client = couchDbDispatcher.getClient(datastoreInformation)

			val keys = identifiers.flatMap { identifier ->
				searchKeys.map { key -> arrayOf(key, identifier.system, identifier.value) }
			}

			val viewQueries = createQueries(
				datastoreInformation,
				"by_hcparty_identifier",
				"by_data_owner_identifier" to DATA_OWNER_PARTITION
			).keys(keys).doNotIncludeDocs()
			emitAll(client.interleave<Array<String>, String>(viewQueries, compareBy({it[0]}, {it[1]}, {it[2]})).filterIsInstance<ViewRowNoDoc<Array<String>, String>>().map { it.id })
		}.distinct()

	override fun findContactsByIds(datastoreInformation: IDatastoreInformation, contactIds: Flow<String>): Flow<ViewQueryResultEvent> = flow {
		val client = couchDbDispatcher.getClient(datastoreInformation)
		emitAll(client.getForPagination(contactIds, Contact::class.java))
	}

	override fun findContactsByIds(datastoreInformation: IDatastoreInformation, contactIds: Collection<String>): Flow<ViewQueryResultEvent> = flow {
		val client = couchDbDispatcher.getClient(datastoreInformation)
		emitAll(client.getForPagination(contactIds, Contact::class.java))
	}

	override fun listContactIdsByHealthcareParty(datastoreInformation: IDatastoreInformation, hcPartyId: String) = flow {
		val client = couchDbDispatcher.getClient(datastoreInformation)

        val viewQueries = createQueries(datastoreInformation, "by_hcparty", "by_data_owner" to DATA_OWNER_PARTITION).startKey(hcPartyId)
			.endKey(hcPartyId)
            .doNotIncludeDocs()
        emitAll(client.interleave<String, String>(viewQueries, compareBy({it})).filterIsInstance<ViewRowNoDoc<Array<String>, String>>().map { it.id }.distinctUntilChanged())
	}

	@Views(
    	View(name = "by_hcparty_patientfk", map = "classpath:js/contact/By_hcparty_patientfk_map.js"),
    	View(name = "by_data_owner_patientfk", map = "classpath:js/contact/By_data_owner_patientfk_map.js", secondaryPartition = DATA_OWNER_PARTITION),
	)
	override fun listContactsByHcPartyAndPatient(datastoreInformation: IDatastoreInformation, searchKeys: Set<String>, secretPatientKeys: List<String>) = flow {
		val client = couchDbDispatcher.getClient(datastoreInformation)

		val keys = secretPatientKeys.flatMap { fk ->
			searchKeys.map { key -> arrayOf(key, fk) } }

		val viewQueries = createQueries(
			datastoreInformation,
			"by_hcparty_patientfk",
			"by_data_owner_patientfk" to DATA_OWNER_PARTITION
		).keys(keys).includeDocs()
		emitAll(relink(client.interleave<Array<String>, String, Contact>(viewQueries, compareBy({it[0]}, {it[1]}))
			.filterIsInstance<ViewRowWithDoc<Array<String>, String, Contact>>().map { it.doc }))
	}.distinctById()

	@Views(
    	View(name = "by_hcparty_patientfk", map = "classpath:js/contact/By_hcparty_patientfk_map.js"),
    	View(name = "by_data_owner_patientfk", map = "classpath:js/contact/By_data_owner_patientfk_map.js", secondaryPartition = DATA_OWNER_PARTITION),
	)
	override fun listContactIdsByHcPartyAndPatient(datastoreInformation: IDatastoreInformation, searchKeys: Set<String>, secretPatientKeys: List<String>): Flow<String> = flow {
		val client = couchDbDispatcher.getClient(datastoreInformation)

		val keys = secretPatientKeys.flatMap { fk ->
			searchKeys.map { key -> arrayOf(key, fk) } }

		val viewQueries = createQueries(
			datastoreInformation,
			"by_hcparty_patientfk",
			"by_data_owner_patientfk" to DATA_OWNER_PARTITION
		).keys(keys).doNotIncludeDocs()
		emitAll(client.interleave<Array<String>, String>(viewQueries, compareBy({it[0]}, {it[1]}))
			.filterIsInstance<ViewRowNoDoc<Array<String>, String>>().mapNotNull { it.id })
	}.distinct()

	@Views(
    	View(name = "by_hcparty_formid", map = "classpath:js/contact/By_hcparty_formid_map.js"),
    	View(name = "by_data_owner_formid", map = "classpath:js/contact/By_data_owner_formid_map.js", secondaryPartition = DATA_OWNER_PARTITION),
	)
	override fun listContactsByHcPartyAndFormId(datastoreInformation: IDatastoreInformation, searchKeys: Set<String>, formId: String) = flow {
		val client = couchDbDispatcher.getClient(datastoreInformation)

		val viewQueries = createQueries(
			datastoreInformation,
			"by_hcparty_formid",
			"by_data_owner_formid" to DATA_OWNER_PARTITION
		).keys(searchKeys.map { arrayOf(it, formId) }).includeDocs()
		emitAll(relink(client.interleave<Array<String>, String, Contact>(viewQueries, compareBy({it[0]}, {it[1]}))
			.filterIsInstance<ViewRowWithDoc<Array<String>, String, Contact>>().map { it.doc }))
	}.distinctByIdIf(searchKeys.size > 1)

	override fun listContactsByHcPartyAndFormIds(datastoreInformation: IDatastoreInformation, searchKeys: Set<String>, ids: List<String>) = flow {
		val client = couchDbDispatcher.getClient(datastoreInformation)

		val viewQueries = createQueries(
			datastoreInformation,
			"by_hcparty_formid",
			"by_data_owner_formid" to DATA_OWNER_PARTITION
		).keys(ids.flatMap { k ->
			searchKeys.map { arrayOf(it, k) }
		}).doNotIncludeDocs()
		val result = client.interleave<Array<String>, String>(viewQueries, compareBy({it[0]}, {it[1]}))
			.filterIsInstance<ViewRowNoDoc<Array<String>, String>>().mapNotNull { it.id }.distinct()

		emitAll(relink(getContacts(datastoreInformation, result)))
	}

	@Views(
    	View(name = "by_hcparty_serviceid", map = "classpath:js/contact/By_hcparty_serviceid_map.js"),
    	View(name = "by_data_owner_serviceid", map = "classpath:js/contact/By_data_owner_serviceid_map.js", secondaryPartition = DATA_OWNER_PARTITION),
	)
	override fun listContactsByHcPartyAndServiceId(datastoreInformation: IDatastoreInformation, searchKeys: Set<String>, serviceId: String) = flow {
		val client = couchDbDispatcher.getClient(datastoreInformation)

		val viewQueries = createQueries(
			datastoreInformation,
			"by_hcparty_serviceid",
			"by_data_owner_serviceid" to DATA_OWNER_PARTITION
		).keys(searchKeys.map { arrayOf(it, serviceId) }).includeDocs()
		emitAll(relink(client.interleave<Array<String>, String, Contact>(viewQueries, compareBy({it[0]}, {it[1]}))
			.filterIsInstance<ViewRowWithDoc<Array<String>, String, Contact>>().map { it.doc }))
	}.distinctByIdIf(searchKeys.size > 1)

	override fun findContactsByHcPartyServiceId(datastoreInformation: IDatastoreInformation, hcPartyId: String, serviceId: String) = flow {
		val client = couchDbDispatcher.getClient(datastoreInformation)

		val viewQueries = createQueries(
			datastoreInformation,
			"by_hcparty_serviceid",
			"by_data_owner_serviceid" to DATA_OWNER_PARTITION
		).key(ComplexKey.of(hcPartyId, serviceId)).includeDocs()
		emitAll(relink(client.interleave<ComplexKey, String, Contact>(viewQueries, compareBy({it.components[0] as? String}, {it.components[1] as? String}))
			.filterIsInstance<ViewRowWithDoc<Array<String>, String, Contact>>().map { it.doc }))
	}

	@View(name = "service_by_linked_id", map = "classpath:js/contact/Service_by_linked_id.js", secondaryPartition = TERTIARY_CONTACT_PARTITION)
	override fun findServiceIdsByIdQualifiedLink(datastoreInformation: IDatastoreInformation, ids: List<String>, linkType: String?) = flow {
		val client = couchDbDispatcher.getClient(datastoreInformation)
		val viewQuery = createQuery(datastoreInformation, "service_by_linked_id", TERTIARY_CONTACT_PARTITION)
			.keys(ids)
			.includeDocs(false)
		val res = client.queryView<String, Array<String>>(viewQuery)
		emitAll(
			(linkType?.let { lt -> res.filter { it.value!![0] == lt } } ?: res)
				.map { it.value!![1] }
		)
	}

	@View(name = "service_by_association_id", map = "classpath:js/contact/Service_by_association_id.js")
	override fun listServiceIdsByAssociationId(datastoreInformation: IDatastoreInformation, associationId: String) = flow {
		val client = couchDbDispatcher.getClient(datastoreInformation)
		val viewQuery = createQuery(datastoreInformation, "service_by_association_id")
			.key(associationId)
			.includeDocs(true)
		val res = client.queryViewIncludeDocs<String, String, Contact>(viewQuery)
		emitAll(
			res.mapNotNull { it.doc }
				.flatMapConcat { it.services.filter { it.qualifiedLinks.values.flatMap { it.keys }.contains(associationId) }.asFlow() }
		)
	}

	@Views(
		View(name = "service_by_hcparty", map = "classpath:js/contact/Service_by_hcparty_map.js"),
		View(name = "service_by_data_owner", map = "classpath:js/contact/Service_by_data_owner_map.js", secondaryPartition = DATA_OWNER_PARTITION)
	)
	override fun listServiceIdsByHcParty(datastoreInformation: IDatastoreInformation, searchKeys: Set<String>) = flow {
		val client = couchDbDispatcher.getClient(datastoreInformation)
		val viewQueries = createQueries(
			datastoreInformation,
			"service_by_hcparty",
			"service_by_data_owner" to DATA_OWNER_PARTITION
		)
			.keys(searchKeys)
			.doNotIncludeDocs()
		emitAll(client.interleave<String, String>(viewQueries, compareBy({it}), DeduplicationMode.ID_AND_VALUE).filterIsInstance<ViewRowNoDoc<String, String>>().mapNotNull { it.value })
	}.distinctIf(searchKeys.size > 1)

	@View(name = "service_by_association_id", map = "classpath:js/contact/Service_by_association_id.js")
	override fun findServiceIdsByAssociationId(datastoreInformation: IDatastoreInformation, associationId: String) = flow {
		val client = couchDbDispatcher.getClient(datastoreInformation)
		val viewQuery = createQuery(datastoreInformation, "service_by_association_id")
			.key(associationId)
			.includeDocs(true)

		val res = client.queryViewIncludeDocs<String, String, Contact>(viewQuery)
		emitAll(
			res.mapNotNull { it.doc }
				.flatMapConcat { it.services.filter { it.qualifiedLinks.values.flatMap { it.keys }.contains(associationId) }.asFlow() }
		)
	}

	@Views(
		View(name = "service_by_hcparty_tag", map = "classpath:js/contact/Service_by_hcparty_tag.js"),
		View(name = "service_by_data_owner_tag", map = "classpath:js/contact/Service_by_data_owner_tag.js", secondaryPartition = DATA_OWNER_PARTITION)
	)
	override fun listServiceIdsByTag(datastoreInformation: IDatastoreInformation, hcPartyId: String, tagType: String?, tagCode: String?, startValueDate: Long?, endValueDate: Long?, descending: Boolean) = flow {
		val client = couchDbDispatcher.getClient(datastoreInformation)

		val canonicalStartValueDate = startValueDate?.takeIf { it < 99999999 }?.let { it * 1000000 } ?: startValueDate
		val canonicalEndValueDate = endValueDate?.takeIf { it < 99999999 }?.let { it * 1000000 } ?: endValueDate

		val from = ComplexKey.of(
			hcPartyId,
			tagType,
			tagCode,
			canonicalStartValueDate
		)
		val to = ComplexKey.of(
			hcPartyId,
			tagType ?: ComplexKey.emptyObject(),
			tagCode ?: ComplexKey.emptyObject(),
			canonicalEndValueDate ?: ComplexKey.emptyObject()
		)

		val viewQueries = createQueries(
			datastoreInformation,
			"service_by_hcparty_tag",
			"service_by_data_owner_tag" to DATA_OWNER_PARTITION
		)
			.startKey(if (descending) to else from)
			.endKey(if (descending) from else to)
			.descending(descending)
			.doNotIncludeDocs()

		emitAll(client.interleave<ComplexKey, String>(viewQueries, compareBy(
			{ it.components[0] as? String },
			{ it.components[1] as? String },
			{ it.components[2] as? String },
			{ (it.components[3] as? Number)?.toLong() },
		), DeduplicationMode.ID_AND_VALUE).filterIsInstance<ViewRowNoDoc<String, String>>().mapNotNull { it.value }.distinct())
	}

	@Views(
		View(name = "service_by_hcparty_patient_tag", map = "classpath:js/contact/Service_by_hcparty_patient_tag.js"),
		View(name = "service_by_data_owner_patient_tag", map = "classpath:js/contact/Service_by_data_owner_patient_tag.js", secondaryPartition = DATA_OWNER_PARTITION)
	)
	override fun listServiceIdsByPatientAndTag(datastoreInformation: IDatastoreInformation, hcPartyId: String, patientSecretForeignKeys: List<String>, tagType: String?, tagCode: String?, startValueDate: Long?, endValueDate: Long?, descending: Boolean) = flow {
		val client = couchDbDispatcher.getClient(datastoreInformation)

		val canonicalStartValueDate = startValueDate?.takeIf { it < 99999999 }?.let { it * 1000000 } ?: startValueDate
		val canonicalEndValueDate = endValueDate?.takeIf { it < 99999999 }?.let { it * 1000000 } ?: endValueDate

		val idFlows = mutableListOf<Flow<String>>()
		for (patientSecretForeignKey in patientSecretForeignKeys) {
			val from = ComplexKey.of(
				hcPartyId,
				patientSecretForeignKey,
				tagType,
				tagCode,
				canonicalStartValueDate
			)
			val to = ComplexKey.of(
				hcPartyId,
				patientSecretForeignKey,
				tagType ?: ComplexKey.emptyObject(),
				tagCode ?: ComplexKey.emptyObject(),
				canonicalEndValueDate ?: ComplexKey.emptyObject()
			)

			val viewQueries = createQueries(
				datastoreInformation,
				"service_by_hcparty_patient_tag",
				"service_by_data_owner_patient_tag" to DATA_OWNER_PARTITION
			)
				.startKey(if (descending) to else from)
				.endKey(if (descending) from else to)
				.descending(descending)
				.doNotIncludeDocs()

			idFlows.add(client.interleave<ComplexKey, String>(viewQueries, compareBy(
				{ it.components[0] as? String },
				{ it.components[1] as? String },
				{ it.components[2] as? String },
				{ it.components[3] as? String },
				{ (it.components[4] as? Number)?.toLong() }
			), DeduplicationMode.ID_AND_VALUE).filterIsInstance<ViewRowNoDoc<ComplexKey, String>>().mapNotNull { it.value })
		}
		emitAll(idFlows.asFlow().flattenConcat().distinct())
	}

	@Views(
		View(name = "service_by_hcparty_code", map = "classpath:js/contact/Service_by_hcparty_code.js", reduce = "_count"),
		View(name = "service_by_data_owner_code", map = "classpath:js/contact/Service_by_data_owner_code.js", reduce = "_count", secondaryPartition = DATA_OWNER_PARTITION)
	)
	override fun listServiceIdsByCode(datastoreInformation: IDatastoreInformation, hcPartyId: String, codeType: String?, codeCode: String?, startValueDate: Long?, endValueDate: Long?, descending: Boolean) = flow {
		val client = couchDbDispatcher.getClient(datastoreInformation)

		val canonicalStartValueDate = startValueDate?.takeIf { it < 99999999 }?.let { it * 1000000 } ?: startValueDate
		val canonicalEndValueDate = endValueDate?.takeIf { it < 99999999 }?.let { it * 1000000 } ?: endValueDate

		val from = ComplexKey.of(
			hcPartyId,
			codeType,
			codeCode,
			canonicalStartValueDate
		)
		val to = ComplexKey.of(
			hcPartyId,
			codeType ?: ComplexKey.emptyObject(),
			codeCode ?: ComplexKey.emptyObject(),
			canonicalEndValueDate ?: ComplexKey.emptyObject()
		)

		val viewQueries = createQueries(
			datastoreInformation,
			"service_by_hcparty_code",
			"service_by_data_owner_code" to DATA_OWNER_PARTITION
		)
			.startKey(if (descending) to else from)
			.endKey(if (descending) from else to)
			.descending(descending)
			.reduce(false)
			.doNotIncludeDocs()

		emitAll(client.interleave<ComplexKey, String>(viewQueries, compareBy(
			{ it.components[0] as? String },
			{ it.components[1] as? String },
			{ it.components[2] as? String },
			{ (it.components[3] as? Number)?.toLong() },
		), DeduplicationMode.ID_AND_VALUE).filterIsInstance<ViewRowNoDoc<String, String>>().mapNotNull { it.value }.distinct())
	}

	@Views(
    	View(name = "by_hcparty_tag", map = "classpath:js/contact/By_hcparty_tag.js", reduce = "_count"),
    	View(name = "by_data_owner_tag", map = "classpath:js/contact/By_data_owner_tag.js", reduce = "_count", secondaryPartition = DATA_OWNER_PARTITION),
	)
	override fun listContactIdsByTag(datastoreInformation: IDatastoreInformation, hcPartyId: String, tagType: String?, tagCode: String?, startValueDate: Long?, endValueDate: Long?) = flow {
		val client = couchDbDispatcher.getClient(datastoreInformation)

		var startValueDate = startValueDate
		var endValueDate = endValueDate
		if (startValueDate != null && startValueDate < 99999999) {
			startValueDate = startValueDate * 1000000
		}
		if (endValueDate != null && endValueDate < 99999999) {
			endValueDate = endValueDate * 1000000
		}
		val from = ComplexKey.of(
			hcPartyId,
			tagType,
			tagCode,
			startValueDate
		)
		val to = ComplexKey.of(
			hcPartyId,
			tagType ?: ComplexKey.emptyObject(),
			tagCode ?: ComplexKey.emptyObject(),
			endValueDate ?: ComplexKey.emptyObject()
		)

		val viewQueries = createQueries(
			datastoreInformation,
			"by_hcparty_tag",
			"by_data_owner_tag" to DATA_OWNER_PARTITION
		)
			.startKey(from)
			.endKey(to)
			.reduce(false)
			.doNotIncludeDocs()

		emitAll(client.interleave<ComplexKey, String>(viewQueries, compareBy(
			{ it.components[0] as? String },
			{ it.components[1] as? String },
			{ it.components[2] as? String },
			{ (it.components[3] as? Number)?.toLong() },
		), DeduplicationMode.ID_AND_VALUE).filterIsInstance<ViewRowNoDoc<ComplexKey, String>>().mapNotNull { it.id }.distinct())
	}

	@Views(
		View(name = "service_id_by_hcparty_helements", map = "classpath:js/contact/Service_id_by_hcparty_helement_ids.js"),
		View(name = "service_id_by_data_owner_helements", map = "classpath:js/contact/Service_id_by_data_owner_helement_ids.js", secondaryPartition = DATA_OWNER_PARTITION)
	)
	override fun listServiceIdsByHcPartyHealthElementIds(datastoreInformation: IDatastoreInformation, searchKeys: Set<String>, healthElementIds: List<String>) = flow {
		val client = couchDbDispatcher.getClient(datastoreInformation)
		val viewQueries = createQueries(
			datastoreInformation,
			"service_id_by_hcparty_helements",
			"service_id_by_data_owner_helements" to DATA_OWNER_PARTITION
		)
			.keys(
				healthElementIds.flatMap {
					searchKeys.map { key ->
						ComplexKey.of(key, it)
					}
				}
			)
			.doNotIncludeDocs()

		emitAll(
			client
				.interleave<ComplexKey, String>(viewQueries, compareBy({ it.components[0] as? String }, { it.components[1] as? String },), DeduplicationMode.ID_AND_VALUE)
				.filterIsInstance<ViewRowNoDoc<ComplexKey, String>>().mapNotNull { it.value }
		)
	}.distinct()

	@Views(
		View(name = "service_by_hcparty_identifier", map = "classpath:js/contact/Service_by_hcparty_identifier.js"),
		View(name = "service_by_data_owner_identifier", map = "classpath:js/contact/Service_by_data_owner_identifier.js", secondaryPartition = DATA_OWNER_PARTITION)
	)
	override fun listServiceIdsByHcPartyAndIdentifiers(datastoreInformation: IDatastoreInformation, searchKeys: Set<String>, identifiers: List<Identifier>) = flow {
		val client = couchDbDispatcher.getClient(datastoreInformation)

		val viewQueries = createQueries(
			datastoreInformation,
			"service_by_hcparty_identifier",
			"service_by_data_owner_identifier" to DATA_OWNER_PARTITION
		)
			.keys(
				identifiers.flatMap {
					searchKeys.map { key ->
						ComplexKey.of(key, it.system, it.value)
					}
				}
			).doNotIncludeDocs()

		emitAll(
			client.interleave<ComplexKey, String>(viewQueries, compareBy({ it.components[0] as? String }, { it.components[1] as? String }), DeduplicationMode.ID_AND_VALUE)
				.filterIsInstance<ViewRowNoDoc<ComplexKey, String>>()
				.mapNotNull { it.value }
		)
	}.distinct()

	@Views(
    	View(name = "by_hcparty_code", map = "classpath:js/contact/By_hcparty_code.js", reduce = "_count"),
    	View(name = "by_data_owner_code", map = "classpath:js/contact/By_data_owner_code.js", reduce = "_count", secondaryPartition = DATA_OWNER_PARTITION),
	)
	override fun listContactIdsByCode(datastoreInformation: IDatastoreInformation, hcPartyId: String, codeType: String?, codeCode: String?, startValueDate: Long?, endValueDate: Long?) = flow {
		val client = couchDbDispatcher.getClient(datastoreInformation)

		var startValueDate = startValueDate
		var endValueDate = endValueDate
		if (startValueDate != null && startValueDate < 99999999) {
			startValueDate = startValueDate * 1000000
		}
		if (endValueDate != null && endValueDate < 99999999) {
			endValueDate = endValueDate * 1000000
		}
		val from = ComplexKey.of(
			hcPartyId,
			codeType,
			codeCode,
			startValueDate
		)
		val to = ComplexKey.of(
			hcPartyId,
			codeType ?: ComplexKey.emptyObject(),
			codeCode ?: ComplexKey.emptyObject(),
			endValueDate ?: ComplexKey.emptyObject()
		)

		val viewQueries = createQueries(
			datastoreInformation,
			"by_hcparty_code",
			"by_data_owner_code" to DATA_OWNER_PARTITION
		)
			.startKey(from)
			.endKey(to)
			.reduce(false)
			.doNotIncludeDocs()

		emitAll(client.interleave<ComplexKey, String>(viewQueries, compareBy({it.components[0] as? String}, {it.components[1] as? String}), DeduplicationMode.ID_AND_VALUE)
			.filterIsInstance<ViewRowNoDoc<Array<String>, String>>().map { it.id })
	}

	override fun listCodesFrequencies(datastoreInformation: IDatastoreInformation, hcPartyId: String, codeType: String) = flow {
		val client = couchDbDispatcher.getClient(datastoreInformation)

		val from = ComplexKey.of(
			hcPartyId,
			codeType,
			null
		)
		val to = ComplexKey.of(
			hcPartyId,
			codeType,
			ComplexKey.emptyObject()
		)

		val viewQuery = createQuery(datastoreInformation, "service_by_hcparty_code").startKey(from).endKey(to).includeDocs(false).reduce(true).group(true).groupLevel(3)

		emitAll(client.queryView<Array<String>, Long>(viewQuery).map { Pair(ComplexKey.of(*(it.key as Array<String>)), it.value) })
	}


	@Views(
		View(name = "service_by_hcparty_patient_code", map = "classpath:js/contact/Service_by_hcparty_patient_code.js"),
		View(name = "service_by_data_owner_patient_code", map = "classpath:js/contact/Service_by_data_owner_patient_code.js", secondaryPartition = DATA_OWNER_PARTITION)
	)
	override fun listServicesIdsByPatientAndCode(datastoreInformation: IDatastoreInformation, hcPartyId: String, patientSecretForeignKeys: List<String>, codeType: String?, codeCode: String?, startValueDate: Long?, endValueDate: Long?, descending: Boolean) = flow {
		val client = couchDbDispatcher.getClient(datastoreInformation)

		val canonicalStartValueDate = startValueDate?.takeIf { it < 99999999 }?.let { it * 1000000 } ?: startValueDate
		val canonicalEndValueDate = endValueDate?.takeIf { it < 99999999 }?.let { it * 1000000 } ?: endValueDate

		val idFlows = mutableListOf<Flow<String>>()
		for (patientSecretForeignKey in patientSecretForeignKeys) {
			val from = ComplexKey.of(
				hcPartyId,
				patientSecretForeignKey,
				codeType,
				codeCode,
				canonicalStartValueDate
			)
			val to = ComplexKey.of(
				hcPartyId,
				patientSecretForeignKey,
				codeType ?: ComplexKey.emptyObject(),
				codeCode ?: ComplexKey.emptyObject(),
				canonicalEndValueDate ?: ComplexKey.emptyObject()
			)

			val viewQueries = createQueries(
				datastoreInformation,
				"service_by_hcparty_patient_code",
				"service_by_data_owner_patient_code" to DATA_OWNER_PARTITION
			)
				.startKey(if (descending) to else from)
				.endKey(if (descending) from else to)
				.descending(descending)
				.doNotIncludeDocs()

			idFlows.add(client.interleave<ComplexKey, String>(viewQueries, compareBy(
				{ it.components[0] as? String },
				{ it.components[1] as? String },
				{ it.components[2] as? String },
				{ it.components[3] as? String },
				{ (it.components[4] as? Number)?.toLong() }
			), DeduplicationMode.ID_AND_VALUE).filterIsInstance<ViewRowNoDoc<ComplexKey, String>>().mapNotNull { it.value })
		}
		emitAll(idFlows.asFlow().flattenConcat().distinct())
	}

	override fun listServicesIdsByPatientForeignKeys(datastoreInformation: IDatastoreInformation, searchKeys: Set<String>, patientSecretForeignKeys: Set<String>): Flow<String> =
		listContactsByHcPartyAndPatient(datastoreInformation, searchKeys, patientSecretForeignKeys.toList())
			.mapNotNull { c ->
				c.services.map { it.id }.asFlow()
			}.flattenConcat() // no distinct ?

	@View(name = "by_service", map = "classpath:js/contact/By_service.js")
	fun legacy() {
	}

	@View(name = "by_service_emit_modified", map = "classpath:js/contact/By_service_emit_modified.js")
	override fun listIdsByServices(datastoreInformation: IDatastoreInformation, services: Collection<String>) = flow {
		val client = couchDbDispatcher.getClient(datastoreInformation)

		val viewQuery = createQuery(datastoreInformation, "by_service_emit_modified").keys(services).includeDocs(false)
		emitAll(client.queryView<String, ContactIdServiceId>(viewQuery).mapNotNull { it.value })
	}

	override fun listContactsByServices(datastoreInformation: IDatastoreInformation, services: Collection<String>): Flow<Contact> {
		return getContacts(datastoreInformation, this.listIdsByServices(datastoreInformation, services).map { it.contactId })
	}

	override fun relink(cs: Flow<Contact>): Flow<Contact> {
		return cs.map { c ->
			val services = mutableMapOf<String, Service?>()
			c.services.forEach { s -> s.id.let { services[it] = s } }
			c.subContacts.forEach { ss ->
				ss.services.forEach { s ->
					val ssvc = services[s.serviceId]
					//If it is null, leave it null...
					s.service = ssvc
				}
			}
			c
		}
	}

	@View(name = "by_externalid", map = "classpath:js/contact/By_externalid.js")
	override fun findContactsByExternalId(datastoreInformation: IDatastoreInformation, externalId: String) = flow {
		val client = couchDbDispatcher.getClient(datastoreInformation)

		val viewQuery = createQuery(datastoreInformation, "by_externalid")
			.key(externalId)
			.includeDocs(true)

		emitAll(client.queryViewIncludeDocs<String, String, Contact>(viewQuery).mapNotNull { it.doc })
	}

	@View(name = "conflicts", map = "function(doc) { if (doc.java_type == 'org.taktik.icure.entities.Contact' && !doc.deleted && doc._conflicts) emit(doc._id )}")
	override fun listConflicts(datastoreInformation: IDatastoreInformation) = flow {
		val client = couchDbDispatcher.getClient(datastoreInformation)

		val viewQuery = createQuery(datastoreInformation, "conflicts").includeDocs(true)
		emitAll(client.queryViewIncludeDocsNoValue<String, Contact>(viewQuery).map { it.doc })
	}
}
