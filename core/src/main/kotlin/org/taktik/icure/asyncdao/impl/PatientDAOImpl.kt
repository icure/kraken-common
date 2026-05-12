/*
 * Copyright (c) 2020. Taktik SA, All rights reserved.
 */

package org.taktik.icure.asyncdao.impl

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.fold
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.takeWhile
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.flow.transform
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Repository
import org.taktik.couchdb.ViewQueryResultEvent
import org.taktik.couchdb.ViewRowNoDoc
import org.taktik.couchdb.ViewRowWithDoc
import org.taktik.couchdb.annotation.View
import org.taktik.couchdb.annotation.Views
import org.taktik.couchdb.dao.DesignDocumentProvider
import org.taktik.couchdb.entity.ComplexKey
import org.taktik.couchdb.id.IDGenerator
import org.taktik.couchdb.queryView
import org.taktik.couchdb.queryViewIncludeDocs
import org.taktik.couchdb.queryViewIncludeDocsNoValue
import org.taktik.icure.asyncdao.CouchDbDispatcher
import org.taktik.icure.asyncdao.DATA_OWNER_PARTITION
import org.taktik.icure.asyncdao.MAURICE_PARTITION
import org.taktik.icure.asyncdao.Partitions
import org.taktik.icure.asyncdao.PatientDAO
import org.taktik.icure.cache.ConfiguredCacheProvider
import org.taktik.icure.cache.getConfiguredCache
import org.taktik.icure.config.DaoConfig
import org.taktik.icure.dao.QueryProvider
import org.taktik.icure.datastore.IDatastoreInformation
import org.taktik.icure.db.PaginationOffset
import org.taktik.icure.db.sanitizeString
import org.taktik.icure.entities.Patient
import org.taktik.icure.entities.embed.Gender
import org.taktik.icure.entities.embed.Identifier
import org.taktik.icure.utils.DeduplicationMode
import org.taktik.icure.utils.buildComparator
import org.taktik.icure.utils.distinct
import org.taktik.icure.utils.distinctByIdIf
import org.taktik.icure.utils.distinctIf
import org.taktik.icure.utils.interleave
import org.taktik.icure.utils.main
import org.taktik.icure.utils.queryView

@Repository("patientDAO")
@Profile("app")
@View(
	name = "all",
	map = "function(doc) { if (doc.java_type == 'org.taktik.icure.entities.Patient' && !doc.deleted) emit(null, doc._id)}",
)
class PatientDAOImpl(
	@Qualifier("patientCouchDbDispatcher") couchDbDispatcher: CouchDbDispatcher,
	idGenerator: IDGenerator,
	entityCacheFactory: ConfiguredCacheProvider,
	designDocumentProvider: DesignDocumentProvider,
	daoConfig: DaoConfig,
	queryProvider: QueryProvider
) : ConflictDAOImpl<Patient>(
	entityClass = Patient::class.java,
	couchDbDispatcher = couchDbDispatcher,
	idGenerator = idGenerator,
	cacheChain = entityCacheFactory.getConfiguredCache(),
	designDocumentProvider = designDocumentProvider,
	daoConfig = daoConfig,
	queryProvider = queryProvider
),
	PatientDAO {

	@Views(
		View(name = "by_hcparty_ssin", map = "classpath:js/patient/By_hcparty_ssin_map.js", reduce = "_count"),
		View(
			name = "by_data_owner_ssin",
			map = "classpath:js/patient/By_data_owner_ssin_map.js",
			reduce = "_count",
			secondaryPartition = DATA_OWNER_PARTITION,
		),
	)
	override fun listPatientIdsByHcPartyAndSsin(
		datastoreInformation: IDatastoreInformation,
		ssin: String,
		healthcarePartyId: String,
	): Flow<String> = flow {
		val client = couchDbDispatcher.getClient(datastoreInformation)
		val normalizedSsin = ssin.replace(" ".toRegex(), "").replace("\\W".toRegex(), "")
		val startKey = ComplexKey.of(healthcarePartyId, normalizedSsin)
		val endKey = ComplexKey.of(healthcarePartyId, normalizedSsin + "\ufff0")

		val viewQueries = createQueries(
			client = client,
			legacyViews = listOf(
				"by_hcparty_ssin".main(),
				"by_data_owner_ssin" to DATA_OWNER_PARTITION,
			),
			configurationViews = listOf("by_all_delegates_ssin")
		)
			.startKey(startKey)
			.endKey(endKey)
			.reduce(false)
			.doNotIncludeDocs()

		emitAll(
			client.interleave<ComplexKey, String>(
				viewQueries,
				compareBy({ it.components[0] as? String }, { it.components[1] as? String }),
			).filterIsInstance<ViewRowNoDoc<ComplexKey, String>>().map { it.id },
		)
	}

	@Views(
		View(name = "by_hcparty_active", map = "classpath:js/patient/By_hcparty_active.js", reduce = "_count"),
		View(
			name = "by_data_owner_active",
			map = "classpath:js/patient/By_data_owner_active.js",
			reduce = "_count",
			secondaryPartition = DATA_OWNER_PARTITION,
		),
	)
	override fun listPatientIdsByActive(
		datastoreInformation: IDatastoreInformation,
		active: Boolean,
		searchKeys: Set<String>,
	): Flow<String> = listPatientIdsForActive(
		datastoreInformation = datastoreInformation,
		active = active,
		searchKeys = searchKeys,
		legacyViews = listOf("by_hcparty_active".main(), "by_data_owner_active" to DATA_OWNER_PARTITION),
		configurationViews = listOf("by_all_delegates_active")
	)

	@Views(
		View(
			name = "by_data_owner_tag",
			map = "classpath:js/patient/By_data_owner_tag_map.js",
			secondaryPartition = MAURICE_PARTITION,
		),
	)
	override fun listPatientIdsByDataOwnerTag(
		datastoreInformation: IDatastoreInformation,
		searchKeys: Set<String>,
		tagType: String,
		tagCode: String?,
	) = flow {
		val client = couchDbDispatcher.getClient(datastoreInformation)

		val viewQuery = createQuery(
			client = client,
			legacyView = "by_data_owner_tag" to MAURICE_PARTITION,
			configurationView = "by_all_delegates_tag"
		)
			.keys(
				if (tagCode != null) {
					searchKeys.map { ComplexKey.of(it, tagType, tagCode) }
				} else {
					searchKeys.map { ComplexKey.of(it, tagType) }
				},
			)
			.reduce(false)
			.includeDocs(false)

		emitAll(client.queryView<ComplexKey, Void>(viewQuery).map { it.id })
	}

	@View(name = "merged_by_date", map = "classpath:js/patient/Merged_by_date.js")
	override fun listOfMergesAfter(datastoreInformation: IDatastoreInformation, date: Long?) = flow {
		val client = couchDbDispatcher.getClient(datastoreInformation)
		val viewQuery = createQuery(
			client = client,
			legacyView = "merged_by_date".main(),
			configurationView = "merged_by_date"
		).startKey(date).includeDocs(true)
		emitAll(client.queryViewIncludeDocs<Long, String, Patient>(viewQuery).map { it.doc })
	}

	override suspend fun countByHcParty(datastoreInformation: IDatastoreInformation, healthcarePartyId: String): Int {
		val client = couchDbDispatcher.getClient(datastoreInformation)

		val queries = createQueries(
			client = client,
			legacyViews = listOf(
				"by_hcparty_ssin".main(),
				"by_data_owner_ssin" to DATA_OWNER_PARTITION
			),
			configurationViews = listOf("by_all_delegates_ssin")
		).reduce(true)
			.startKey(ComplexKey.of(healthcarePartyId, null))
			.endKey(ComplexKey.of(healthcarePartyId, ComplexKey.emptyObject()))
			.doNotIncludeDocs()


		return try {
			queries.fold(0) { acc, query ->
				acc + (client.queryView<Array<String>, Int>(query).first().value ?: 0)
			}
		} catch (_: NoSuchElementException) {
			return 0
		}
	}

	override suspend fun countOfHcParty(datastoreInformation: IDatastoreInformation, healthcarePartyId: String): Int {
		val client = couchDbDispatcher.getClient(datastoreInformation)
		val viewQuery = createQuery(
			client = client,
			legacyView = "of_hcparty_ssin".main(),
			configurationView = "of_hcparty_ssin"
		)
			.reduce(true)
			.startKey(ComplexKey.of(healthcarePartyId, null))
			.endKey(ComplexKey.of(healthcarePartyId, ComplexKey.emptyObject()))
			.includeDocs(false)
		return try {
			client.queryView<Array<String>, Int>(viewQuery).first().value ?: 0
		} catch (_: NoSuchElementException) {
			return 0
		}
	}

	@Views(
		View(name = "by_hcparty_date_of_birth", map = "classpath:js/patient/By_hcparty_date_of_birth_map.js"),
		View(
			name = "by_data_owner_date_of_birth",
			map = "classpath:js/patient/By_data_owner_date_of_birth_map.js",
			secondaryPartition = DATA_OWNER_PARTITION,
		),
	)
	override fun listPatientIdsByHcParty(datastoreInformation: IDatastoreInformation, healthcarePartyId: String) = flow {
		val client = couchDbDispatcher.getClient(datastoreInformation)

		val viewQueries = createQueries(
			client = client,
			legacyViews = listOf(
				"by_hcparty_date_of_birth".main(),
				"by_data_owner_date_of_birth" to DATA_OWNER_PARTITION,
			),
			configurationViews = listOf("by_all_delegates_date_of_birth")
		).startKey(ComplexKey.of(healthcarePartyId, null))
			.endKey(ComplexKey.of(healthcarePartyId, ComplexKey.emptyObject()))
			.doNotIncludeDocs()
		emitAll(
			client.interleave<ComplexKey, String>(
				viewQueries,
				compareBy({ it.components[0] as? String }, { (it.components[1] as? Number)?.toLong() }),
			).filterIsInstance<ViewRowNoDoc<ComplexKey, String>>().map { it.id },
		)
	}

	override fun listPatientIdsByHcPartyAndDateOfBirth(
		datastoreInformation: IDatastoreInformation,
		date: Int?,
		searchKeys: Set<String>,
	) = flow {
		val client = couchDbDispatcher.getClient(datastoreInformation)

		val viewQueries = createQueries(
			client = client,
			legacyViews = listOf(
				"by_hcparty_date_of_birth".main(),
				"by_data_owner_date_of_birth" to DATA_OWNER_PARTITION,
			),
			configurationViews = listOf("by_all_delegates_date_of_birth")
		).keys(searchKeys.map { ComplexKey.of(it, date) })
			.doNotIncludeDocs()
		emitAll(
			client.interleave<ComplexKey, String>(
				viewQueries,
				compareBy({ it.components[0] as? String }, { (it.components[1] as? Number)?.toLong() }),
			).filterIsInstance<ViewRowNoDoc<ComplexKey, String>>().map { it.id },
		)
	}.distinctIf(searchKeys.size > 1)

	override fun listPatientIdsByHcPartyAndDateOfBirth(
		datastoreInformation: IDatastoreInformation,
		startDate: Int?,
		endDate: Int?,
		healthcarePartyId: String,
	) = flow {
		val client = couchDbDispatcher.getClient(datastoreInformation)
		val viewQueries = createQueries(
			client = client,
			legacyViews = listOf(
				"by_hcparty_date_of_birth".main(),
				"by_data_owner_date_of_birth" to DATA_OWNER_PARTITION,
			),
			configurationViews = listOf("by_all_delegates_date_of_birth")
		).startKey(ComplexKey.of(healthcarePartyId, startDate))
			.endKey(ComplexKey.of(healthcarePartyId, endDate ?: ComplexKey.emptyObject()))
			.doNotIncludeDocs()
		emitAll(
			client.interleave<ComplexKey, String>(
				viewQueries,
				compareBy({ it.components[0] as? String }, { (it.components[1] as? Number)?.toLong() }),
			).filterIsInstance<ViewRowNoDoc<ComplexKey, String>>().map { it.id },
		)
	}

	@Views(
		View(
			name = "by_hcparty_gender_education_profession",
			map = "classpath:js/patient/By_hcparty_gender_education_profession_map.js",
		),
		View(
			name = "by_data_owner_gender_education_profession",
			map = "classpath:js/patient/By_data_owner_gender_education_profession_map.js",
			secondaryPartition = DATA_OWNER_PARTITION,
		),
	)
	override fun listPatientIdsByHcPartyGenderEducationProfession(
		datastoreInformation: IDatastoreInformation,
		healthcarePartyId: String,
		gender: Gender?,
		education: String?,
		profession: String?,
	) = flow {
		val client = couchDbDispatcher.getClient(datastoreInformation)
		val endKey = ComplexKey.of(
			healthcarePartyId,
			gender?.name ?: ComplexKey.emptyObject(),
			education
				?: ComplexKey.emptyObject(),
			profession ?: ComplexKey.emptyObject(),
		)

		val viewQueries = createQueries(
			client = client,
			legacyViews = listOf(
				"by_hcparty_gender_education_profession".main(),
				"by_data_owner_gender_education_profession" to DATA_OWNER_PARTITION,
			),
			configurationViews = listOf("by_all_delegates_gender_education_profession")
		)
			.startKey(ComplexKey.of(healthcarePartyId, gender?.name, education, profession))
			.endKey(endKey)
			.doNotIncludeDocs()

		emitAll(
			client.interleave<ComplexKey, String>(
				viewQueries,
				compareBy(
					{ it.components[0] as? String },
					{ it.components[1] as? String },
					{ it.components[2] as? String },
					{ it.components[3] as? String },
				),
			).filterIsInstance<ViewRowNoDoc<ComplexKey, String>>().map { it.id },
		)
	}

	@Views(
		View(name = "by_hcparty_contains_name", map = "classpath:js/patient/By_hcparty_contains_name_map.js"),
		View(
			name = "by_data_owner_contains_name",
			map = "classpath:js/patient/By_data_owner_contains_name_map.js",
			secondaryPartition = DATA_OWNER_PARTITION,
		),
	)
	override fun listPatientIdsByHcPartyNameContainsFuzzy(
		datastoreInformation: IDatastoreInformation,
		searchString: String?,
		healthcarePartyId: String,
		limit: Int?,
	) = flow {
		val client = couchDbDispatcher.getClient(datastoreInformation)

		val name = if (searchString != null) sanitizeString(searchString) else null
		val viewQueries = createQueries(
			client = client,
			legacyViews = listOf(
				"by_hcparty_contains_name".main(),
				"by_data_owner_contains_name" to DATA_OWNER_PARTITION,
			),
			configurationViews = listOf("by_all_delegates_contains_name")
		)
			.startKey(ComplexKey.of(healthcarePartyId, name))
			.endKey(ComplexKey.of(healthcarePartyId, if (name == null) ComplexKey.emptyObject() else name + "\ufff0"))
			.also { q -> limit?.let { q.limit(it) } ?: q }
			.doNotIncludeDocs()

		emitAll(
			client
				.interleave<ComplexKey, String>(
					viewQueries,
					compareBy({ it.components[0] as? String }, { it.components[1] as? String }),
				)
				.filterIsInstance<ViewRowNoDoc<ComplexKey, String>>()
				.map { it.id },
		)
	}

	private fun listPatientIdsForActive(
		datastoreInformation: IDatastoreInformation,
		active: Boolean,
		searchKeys: Set<String>,
		legacyViews: List<Pair<String, String?>>,
		configurationViews: List<String>
	) = flow {
		val client = couchDbDispatcher.getClient(datastoreInformation)
		val viewQueries = createQueries(
			client = client,
			legacyViews = legacyViews,
			configurationViews = configurationViews,
		)
			.keys(searchKeys.map { ComplexKey.of(it, if (active) 1 else 0) })
			.reduce(false)
			.doNotIncludeDocs()

		emitAll(
			client.interleave<ComplexKey, String>(
				viewQueries,
				compareBy({ it.components[0] as? String }, { it.components[1] as? String }),
			).filterIsInstance<ViewRowNoDoc<ComplexKey, String>>().map { it.id },
		)
	}.distinctIf(searchKeys.size > 1)

	@Views(
		View(name = "by_hcparty_externalid", map = "classpath:js/patient/By_hcparty_externalid_map.js"),
		View(
			name = "by_data_owner_externalid",
			map = "classpath:js/patient/By_data_owner_externalid_map.js",
			secondaryPartition = DATA_OWNER_PARTITION,
		),
	)
	override fun listPatientIdsByHcPartyAndExternalId(
		datastoreInformation: IDatastoreInformation,
		externalId: String?,
		healthcarePartyId: String,
	) = flow {
		val client = couchDbDispatcher.getClient(datastoreInformation)
		val startKey: ComplexKey
		val endKey: ComplexKey

		// Not transactional aware
		if (externalId != null) {
			val cexternalId = externalId.replace(" ".toRegex(), "").replace("\\W".toRegex(), "")
			startKey = ComplexKey.of(healthcarePartyId, cexternalId)
			endKey = ComplexKey.of(healthcarePartyId, cexternalId + "\ufff0")
		} else {
			startKey = ComplexKey.of(healthcarePartyId, null)
			endKey = ComplexKey.of(healthcarePartyId, "\ufff0")
		}

		val viewQueries = createQueries(
			client = client,
			legacyViews = listOf(
				"by_hcparty_externalid".main(),
				"by_data_owner_externalid" to DATA_OWNER_PARTITION,
			),
			configurationViews = listOf("by_all_delegates_externalid")
		)
			.startKey(startKey)
			.endKey(endKey)
			.doNotIncludeDocs()

		emitAll(
			client.interleave<ComplexKey, String>(
				viewQueries,
				compareBy({ it.components[0] as? String }, { it.components[1] as? String }),
			)
				.filterIsInstance<ViewRowNoDoc<ComplexKey, String>>().map { it.id },
		)
	}

	@Views(
		View(name = "by_hcparty_telecom", map = "classpath:js/patient/By_hcparty_telecom.js"),
		View(
			name = "by_data_owner_telecom",
			map = "classpath:js/patient/By_data_owner_telecom.js",
			secondaryPartition = DATA_OWNER_PARTITION,
		),
	)
	override fun listPatientIdsByHcPartyAndTelecom(
		datastoreInformation: IDatastoreInformation,
		searchString: String?,
		healthcarePartyId: String,
	): Flow<String> = flow {
		val client = couchDbDispatcher.getClient(datastoreInformation)
		val startKey: ComplexKey
		val endKey: ComplexKey

		if (searchString != null) {
			val csearchString = searchString.replace(" ".toRegex(), "").replace("\\W".toRegex(), "")
			startKey = ComplexKey.of(healthcarePartyId, csearchString)
			endKey = ComplexKey.of(healthcarePartyId, csearchString + "\ufff0")
		} else {
			startKey = ComplexKey.of(healthcarePartyId, null)
			endKey = ComplexKey.of(healthcarePartyId, "\ufff0")
		}

		val viewQueries = createQueries(
			client = client,
			legacyViews = listOf(
				"by_hcparty_telecom".main(),
				"by_data_owner_telecom" to DATA_OWNER_PARTITION,
			),
			configurationViews = listOf("by_all_delegates_telecom")
		)
			.startKey(startKey)
			.endKey(endKey)
			.doNotIncludeDocs()

		emitAll(
			client.interleave<ComplexKey, String>(
				viewQueries,
				compareBy({ it.components[0] as? String }, { it.components[1] as? String }),
			)
				.filterIsInstance<ViewRowNoDoc<ComplexKey, String>>().map { it.id },
		)
	}

	@Views(
		View(name = "by_hcparty_address", map = "classpath:js/patient/By_hcparty_address.js"),
		View(
			name = "by_data_owner_address",
			map = "classpath:js/patient/By_data_owner_address.js",
			secondaryPartition = DATA_OWNER_PARTITION,
		),
	)
	override fun listPatientIdsByHcPartyAndAddress(
		datastoreInformation: IDatastoreInformation,
		searchString: String?,
		healthcarePartyId: String,
	): Flow<String> = flow {
		val client = couchDbDispatcher.getClient(datastoreInformation)
		val startKey: ComplexKey
		val endKey: ComplexKey

		if (searchString != null) {
			val csearchString = searchString.replace(" ".toRegex(), "").replace("\\W".toRegex(), "")
			startKey = ComplexKey.of(healthcarePartyId, csearchString)
			endKey = ComplexKey.of(healthcarePartyId, csearchString + "\ufff0")
		} else {
			startKey = ComplexKey.of(healthcarePartyId, null)
			endKey = ComplexKey.of(healthcarePartyId, "\ufff0")
		}

		val viewQueries = createQueries(
			client = client,
			legacyViews = listOf(
				"by_hcparty_address".main(),
				"by_data_owner_address" to DATA_OWNER_PARTITION,
			),
			configurationViews = listOf("by_all_delegates_address")
		)
			.startKey(startKey)
			.endKey(endKey)
			.doNotIncludeDocs()

		emitAll(
			client.interleave<ComplexKey, String>(
				viewQueries,
				compareBy({ it.components[0] as? String }, { it.components[1] as? String }),
			).filterIsInstance<ViewRowNoDoc<ComplexKey, String>>().map { it.id },
		)
	}

	@Views(
		View(
			name = "by_hcparty_address_postalcode_housenumber",
			map = "classpath:js/patient/By_hcparty_address_postalcode_housenumber.js",
		),
		View(
			name = "by_data_owner_address_postalcode_housenumber",
			map = "classpath:js/patient/By_data_owner_address_postalcode_housenumber.js",
			secondaryPartition = DATA_OWNER_PARTITION,
		),
	)
	override fun listPatientIdsByHcPartyAndAddress(
		datastoreInformation: IDatastoreInformation,
		streetAndCity: String?,
		postalCode: String?,
		houseNumber: String?,
		healthcarePartyId: String,
	): Flow<String> = flow {
		val client = couchDbDispatcher.getClient(datastoreInformation)
		val startKey: ComplexKey
		val endKey: ComplexKey

		if (streetAndCity != null) {
			val cstreetAndCity = streetAndCity.replace(" ".toRegex(), "").replace("\\W".toRegex(), "")
			startKey = ComplexKey.of(healthcarePartyId, cstreetAndCity, postalCode, houseNumber)
			endKey = ComplexKey.of(
				healthcarePartyId,
				cstreetAndCity + "\ufff0",
				postalCode + "\ufff0",
				houseNumber + "\ufff0",
			)
		} else {
			startKey = ComplexKey.of(healthcarePartyId, null)
			endKey = ComplexKey.of(healthcarePartyId, "\ufff0")
		}

		val viewQueries = createQueries(
			client = client,
			legacyViews = listOf(
				"by_hcparty_address_postalcode_housenumber".main(),
				"by_data_owner_address_postalcode_housenumber" to DATA_OWNER_PARTITION,
			),
			configurationViews = listOf("by_all_delegates_address_postalcode_housenumber")
		)
			.startKey(startKey)
			.endKey(endKey)
			.doNotIncludeDocs()

		emitAll(
			client.interleave<ComplexKey, String>(
				viewQueries,
				compareBy({ it.components[0] as? String }, { it.components[1] as? String }),
			)
				.filterIsInstance<ViewRowNoDoc<ComplexKey, String>>().filter {
					(houseNumber.isNullOrEmpty() || it.key?.components?.get(3) == houseNumber) &&
						(postalCode.isNullOrEmpty() || it.key?.components?.get(2) == postalCode)
				}.map { it.id },
		)
	}

	override fun findPatientIdsByHcParty(
		datastoreInformation: IDatastoreInformation,
		healthcarePartyId: String,
		pagination: PaginationOffset<ComplexKey>,
	) = flow {
		val client = couchDbDispatcher.getClient(datastoreInformation)
		val viewQuery = pagedViewQueryOfIds(
			client = client,
			legacyView = if (daoConfig.useObsoleteViews) {
				"by_hcparty_name".main()
			} else {
				"by_hcparty_name_id_as_value" to MAURICE_PARTITION
			},
			configurationView = "by_all_delegates_name",
			startKey = ComplexKey.of(healthcarePartyId, null),
			endKey = ComplexKey.of(healthcarePartyId, ComplexKey.emptyObject()),
			pagination = pagination,
		)
		emitAll(client.queryView<Array<String>, String, Any>(viewQuery))
	}

	@Views(
		View(
			name = "by_hcparty_name_id_as_value",
			map = "classpath:js/patient/By_hcparty_name_id_as_value_map.js",
			reduce = "_count",
			secondaryPartition = MAURICE_PARTITION,
		),
		View(name = "by_hcparty_name", map = "classpath:js/patient/By_hcparty_name_map.js", reduce = "_count"),
		View(
			name = "by_data_owner_name",
			map = "classpath:js/patient/By_data_owner_name_map.js",
			reduce = "_count",
			secondaryPartition = DATA_OWNER_PARTITION,
		),
	)
	override fun findPatientsByHcPartyAndName(
		datastoreInformation: IDatastoreInformation,
		name: String?,
		healthcarePartyId: String,
		pagination: PaginationOffset<ComplexKey>,
		descending: Boolean,
	): Flow<ViewQueryResultEvent> = findPatientsByName(
		datastoreInformation = datastoreInformation,
		name = name,
		healthcarePartyId = healthcarePartyId,
		pagination = pagination,
		descending = descending,
		legacyViews = listOf(
			if (daoConfig.useObsoleteViews) "by_hcparty_name".main() else "by_hcparty_name_id_as_value" to MAURICE_PARTITION,
			"by_data_owner_name" to DATA_OWNER_PARTITION,
		),
		configurationViews = listOf("by_all_delegates_name"),
	)

	@View(name = "of_hcparty_name", map = "classpath:js/patient/Of_hcparty_name_map.js")
	override fun findPatientsOfHcPartyAndName(
		datastoreInformation: IDatastoreInformation,
		name: String?,
		healthcarePartyId: String,
		pagination: PaginationOffset<ComplexKey>,
		descending: Boolean,
	): Flow<ViewQueryResultEvent> = findPatientsByName(
		datastoreInformation = datastoreInformation,
		name = name,
		healthcarePartyId = healthcarePartyId,
		pagination = pagination,
		descending = descending,
		legacyViews = listOf("of_hcparty_name".main()),
		configurationViews = listOf("of_hcparty_name"),
	)

	override fun findPatientsByHcPartyAndSsin(
		datastoreInformation: IDatastoreInformation,
		ssin: String?,
		healthcarePartyId: String,
		pagination: PaginationOffset<ComplexKey>,
		descending: Boolean,
	): Flow<ViewQueryResultEvent> = findPatientsBySsin(
		datastoreInformation = datastoreInformation,
		ssin = ssin,
		healthcarePartyId = healthcarePartyId,
		pagination = pagination,
		descending = descending,
		legacyViews = listOf("by_hcparty_ssin".main(), "by_data_owner_ssin" to DATA_OWNER_PARTITION),
		configurationViews = listOf("by_all_delegates_ssin"),
	)

	@View(name = "of_hcparty_ssin", map = "classpath:js/patient/Of_hcparty_ssin_map.js", reduce = "_count")
	override fun findPatientsOfHcPartyAndSsin(
		datastoreInformation: IDatastoreInformation,
		ssin: String?,
		healthcarePartyId: String,
		pagination: PaginationOffset<ComplexKey>,
		descending: Boolean,
	): Flow<ViewQueryResultEvent> = findPatientsBySsin(
		datastoreInformation = datastoreInformation,
		ssin = ssin,
		healthcarePartyId = healthcarePartyId,
		pagination = pagination,
		descending = descending,
		legacyViews = listOf("of_hcparty_ssin".main()),
		configurationViews = listOf("of_hcparty_ssin"),
	)

	override fun findPatientsByHcPartyDateOfBirth(
		datastoreInformation: IDatastoreInformation,
		startDate: Int?,
		endDate: Int?,
		healthcarePartyId: String,
		pagination: PaginationOffset<ComplexKey>,
		descending: Boolean,
	): Flow<ViewQueryResultEvent> = findPatientsByDateOfBirth(
		datastoreInformation = datastoreInformation,
		startDate = startDate,
		endDate = endDate,
		healthcarePartyId = healthcarePartyId,
		pagination = pagination,
		descending = descending,
		legacyViews = listOf("by_hcparty_date_of_birth".main(), "by_data_owner_date_of_birth" to DATA_OWNER_PARTITION),
		configurationViews = listOf("by_all_delegates_date_of_birth"),
	)

	@View(name = "of_hcparty_date_of_birth", map = "classpath:js/patient/Of_hcparty_date_of_birth_map.js")
	override fun findPatientsOfHcPartyDateOfBirth(
		datastoreInformation: IDatastoreInformation,
		startDate: Int?,
		endDate: Int?,
		healthcarePartyId: String,
		pagination: PaginationOffset<ComplexKey>,
		descending: Boolean,
	): Flow<ViewQueryResultEvent> = findPatientsByDateOfBirth(
		datastoreInformation = datastoreInformation,
		startDate = startDate,
		endDate = endDate,
		healthcarePartyId = healthcarePartyId,
		pagination = pagination,
		descending = descending,
		legacyViews = listOf("of_hcparty_date_of_birth".main()),
		configurationViews = listOf("of_hcparty_date_of_birth"),
	)

	override fun findPatientsByHcPartyNameContainsFuzzy(
		datastoreInformation: IDatastoreInformation,
		searchString: String?,
		healthcarePartyId: String,
		pagination: PaginationOffset<ComplexKey>,
		descending: Boolean,
	) = findPatientsForHcPartyNameContainsFuzzy(
		datastoreInformation = datastoreInformation,
		searchString = searchString,
		healthcarePartyId = healthcarePartyId,
		pagination = pagination,
		descending = descending,
		legacyViews = listOf("by_hcparty_contains_name".main(), "by_data_owner_contains_name" to DATA_OWNER_PARTITION),
		configurationViews = listOf("by_all_delegates_contains_name")
	)

	@View(name = "of_hcparty_contains_name", map = "classpath:js/patient/Of_hcparty_contains_name_map.js")
	override fun findPatientsOfHcPartyNameContainsFuzzy(
		datastoreInformation: IDatastoreInformation,
		searchString: String?,
		healthcarePartyId: String,
		pagination: PaginationOffset<ComplexKey>,
		descending: Boolean,
	) = findPatientsForHcPartyNameContainsFuzzy(
		datastoreInformation = datastoreInformation,
		searchString = searchString,
		healthcarePartyId = healthcarePartyId,
		pagination = pagination,
		descending = descending,
		legacyViews = listOf("of_hcparty_contains_name".main()),
		configurationViews = listOf("of_hcparty_contains_name")
	)

	private fun findPatientsForHcPartyNameContainsFuzzy(
		datastoreInformation: IDatastoreInformation,
		searchString: String?,
		healthcarePartyId: String,
		pagination: PaginationOffset<ComplexKey>,
		descending: Boolean,
		legacyViews: List<Pair<String, String?>>,
		configurationViews: List<String>,
	): Flow<ViewQueryResultEvent> = flow {
		val client = couchDbDispatcher.getClient(datastoreInformation)

		val name = if (searchString != null) sanitizeString(searchString) else null
		val startKey: ComplexKey
		val endKey: ComplexKey
		if (descending) {
			endKey = ComplexKey.of(healthcarePartyId, name)
			startKey = ComplexKey.of(healthcarePartyId, if (name == null) ComplexKey.emptyObject() else name + "\ufff0")
		} else {
			startKey = ComplexKey.of(healthcarePartyId, name)
			endKey = ComplexKey.of(healthcarePartyId, if (name == null) ComplexKey.emptyObject() else name + "\ufff0")
		}
		val viewQueries = createPagedQueries(
			client = client,
			legacyViewQueries = legacyViews,
			configurationViews = configurationViews,
			startKey = startKey,
			endKey = endKey,
			pagination = pagination,
			descending = descending
		)

		emitAll(
			client.interleave<ComplexKey, String, Patient>(
				viewQueries,
				buildComparator(descending, { it.components[0] as? String }, { it.components[1] as? String }),
			),
		)
	}

	private fun findPatientsByName(
		datastoreInformation: IDatastoreInformation,
		name: String?,
		healthcarePartyId: String,
		pagination: PaginationOffset<ComplexKey>,
		descending: Boolean,
		legacyViews: List<Pair<String, String?>>,
		configurationViews: List<String>
	) = flow {
		val client = couchDbDispatcher.getClient(datastoreInformation)

		val startKeyNameKeySuffix = if (descending) "\ufff0" else "\u0000"
		val endKeyNameKeySuffix = if (descending) "\u0000" else "\ufff0"
		val smallestKey = if (descending) ComplexKey.emptyObject() else null
		val largestKey = if (descending) null else ComplexKey.emptyObject()

		val startKey: ComplexKey
		val endKey: ComplexKey
		if (name == null) {
			startKey = ComplexKey.of(healthcarePartyId, smallestKey)
			endKey = ComplexKey.of(healthcarePartyId, largestKey)
		} else {
			startKey = ComplexKey.of(healthcarePartyId, sanitizeString(name) + startKeyNameKeySuffix)
			endKey = ComplexKey.of(healthcarePartyId, sanitizeString(name) + endKeyNameKeySuffix)
		}

		val viewQueries = createPagedQueries(
			client = client,
			legacyViewQueries = legacyViews,
			configurationViews = configurationViews,
			startKey = startKey,
			endKey = endKey,
			pagination = pagination,
			descending = descending
		)
		emitAll(
			client.interleave<ComplexKey, String, Patient>(
				viewQueries,
				buildComparator(descending, { it.components[0] as? String }, { it.components[1] as? String }),
			),
		)
	}

	private fun findPatientsBySsin(
		datastoreInformation: IDatastoreInformation,
		ssin: String?,
		healthcarePartyId: String,
		pagination: PaginationOffset<ComplexKey>,
		descending: Boolean,
		legacyViews: List<Pair<String, String?>>,
		configurationViews: List<String>
	) = flow {
		val client = couchDbDispatcher.getClient(datastoreInformation)

		val startKeyNameKeySuffix = if (descending) "\ufff0" else "\u0000"
		val endKeyNameKeySuffix = if (descending) "\u0000" else "\ufff0"
		val smallestKey = if (descending) ComplexKey.emptyObject() else null
		val largestKey = if (descending) null else ComplexKey.emptyObject()

		val startKey: ComplexKey
		val endKey: ComplexKey
		if (ssin == null) {
			startKey = ComplexKey.of(healthcarePartyId, smallestKey)
			endKey = ComplexKey.of(healthcarePartyId, largestKey)
		} else {
			val ssinSearchString = ssin.replace(" ".toRegex(), "").replace("\\W".toRegex(), "")
			startKey = ComplexKey.of(healthcarePartyId, ssinSearchString + startKeyNameKeySuffix)
			endKey = ComplexKey.of(healthcarePartyId, ssinSearchString + endKeyNameKeySuffix)
		}

		val viewQueries = createPagedQueries(
			client = client,
			legacyViewQueries = legacyViews,
			configurationViews = configurationViews,
			startKey = startKey,
			endKey = endKey,
			pagination = pagination,
			descending = descending
		)
		emitAll(
			client.interleave<ComplexKey, String, Patient>(
				viewQueries,
				buildComparator(descending, { it.components[0] as? String }, { it.components[1] as? String }),
			),
		)
	}

	private fun findPatientsByDateOfBirth(
		datastoreInformation: IDatastoreInformation,
		startDate: Int?,
		endDate: Int?,
		healthcarePartyId: String,
		pagination: PaginationOffset<ComplexKey>,
		descending: Boolean,
		legacyViews: List<Pair<String, String?>>,
		configurationViews: List<String>
	) = flow {
		val client = couchDbDispatcher.getClient(datastoreInformation)
		val from: ComplexKey
		val to: ComplexKey
		if (descending) {
			from = ComplexKey.of(healthcarePartyId, endDate ?: ComplexKey.emptyObject())
			to = ComplexKey.of(healthcarePartyId, startDate)
		} else {
			from = ComplexKey.of(healthcarePartyId, startDate)
			to = ComplexKey.of(healthcarePartyId, endDate ?: ComplexKey.emptyObject())
		}

		val viewQueries = createPagedQueries(
			client = client,
			legacyViewQueries = legacyViews,
			configurationViews = configurationViews,
			startKey = from,
			endKey = to,
			pagination = pagination,
			descending = descending
		)
		emitAll(
			client.interleave<ComplexKey, String, Patient>(
				viewQueries,
				buildComparator(descending, { it.components[0] as? String }, { (it.components[1] as? Number)?.toLong() }),
			),
		)
	}

	@Views(
		View(
			name = "by_hcparty_modification_date",
			map = "classpath:js/patient/By_hcparty_modification_date_map.js",
			secondaryPartition = MAURICE_PARTITION,
		),
		View(
			name = "by_data_owner_modification_date",
			map = "classpath:js/patient/By_data_owner_modification_date_map.js",
			secondaryPartition = DATA_OWNER_PARTITION,
		),
	)
	override fun listPatientIdsByDataOwnerModificationDate(
		datastoreInformation: IDatastoreInformation,
		searchKeys: Set<String>,
		startDate: Long?,
		endDate: Long?,
		descending: Boolean,
	) = flow {
		val client = couchDbDispatcher.getClient(datastoreInformation)

		val viewQueries = createQueries(
			client = client,
			legacyViews = listOf(
				"by_hcparty_modification_date" to MAURICE_PARTITION,
				"by_data_owner_modification_date" to DATA_OWNER_PARTITION,
			),
			configurationViews = listOf("by_all_delegates_modification_date")
		).keys(searchKeys)

		client.interleave<String, Long>(viewQueries, compareBy { it })
			.filterIsInstance<ViewRowNoDoc<String, Long>>()
			.mapNotNull {
				if (it.value !== null && (startDate == null || it.value!! >= startDate) && (endDate == null || it.value!! <= endDate)) {
					it.id to it.value!!
				} else {
					null
				}
			}
			.toList()
			.sortedWith(
				if (descending) {
					Comparator { o1, o2 ->
						o2.second.compareTo(o1.second).let {
							if (it == 0) o2.first.compareTo(o1.first) else it
						}
					}
				} else {
					compareBy({ it.second }, { it.first })
				},
			)
			.forEach { emit(it.first) }
	}.distinctUntilChanged()

	@View(
		name = "by_user_id",
		map = "function(doc) { if (doc.java_type == 'org.taktik.icure.entities.Patient' && !doc.deleted && doc.userId) emit( doc.userId, doc._id )}",
	)
	override suspend fun findPatientsByUserId(datastoreInformation: IDatastoreInformation, id: String): Patient? {
		val client = couchDbDispatcher.getClient(datastoreInformation)

		val queryView = createQuery(
			client = client,
			legacyView = "by_user_id".main(),
			configurationView = "by_user_id"
		).includeDocs(true).key(id)
		return client.queryViewIncludeDocs<String, String, Patient>(queryView).firstOrNull()?.doc
	}

	override fun getPatients(datastoreInformation: IDatastoreInformation, patIds: Collection<String>) = flow {
		emitAll(getEntities(datastoreInformation, patIds))
	}

	@View(
		name = "by_external_id",
		map = "function(doc) { if (doc.java_type == 'org.taktik.icure.entities.Patient' && !doc.deleted && doc.externalId) emit( doc.externalId, doc._id )}",
	)
	override suspend fun getPatientByExternalId(
		datastoreInformation: IDatastoreInformation,
		externalId: String,
	): Patient? {
		val client = couchDbDispatcher.getClient(datastoreInformation)

		val queryView = createQuery(
			client = client,
			legacyView = "by_external_id".main(),
			configurationView = "by_external_id"
		).includeDocs(true).key(externalId)
		return client.queryViewIncludeDocs<String, String, Patient>(queryView).firstOrNull()?.doc
	}

	@View(
		name = "deleted_by_delete_date",
		map = "function(doc) {\n" +
			"    if (doc.java_type == 'org.taktik.icure.entities.Patient' && doc.deleted){\n" +
			"      emit(doc.deleted)\n" +
			"    }\n" +
			"}",
	)
	override fun findDeletedPatientsByDeleteDate(
		datastoreInformation: IDatastoreInformation,
		start: Long,
		end: Long?,
		descending: Boolean,
		paginationOffset: PaginationOffset<Long>,
	) = flow {
		val client = couchDbDispatcher.getClient(datastoreInformation)
		val viewQuery = pagedViewQuery(
			client = client,
			legacyView = "deleted_by_delete_date".main(),
			configurationView = "deleted_by_delete_date",
			startKey = if (descending) end else start,
			endKey = if (descending) start else end,
			pagination = paginationOffset,
			descending = descending,
		)
		emitAll(client.queryView<Long, Any, Patient>(viewQuery))
	}

	@View(name = "deleted_by_names", map = "classpath:js/patient/Deleted_by_names.js")
	override fun findDeletedPatientsByNames(
		datastoreInformation: IDatastoreInformation,
		firstName: String?,
		lastName: String?,
	) = flow {
		val client = couchDbDispatcher.getClient(datastoreInformation)

		val normalizedFirstName = if (firstName == null) null else sanitizeString(firstName)
		val normalizedLastName = if (lastName == null) null else sanitizeString(lastName)

		val startKey: ComplexKey
		val endKey: ComplexKey
		if (normalizedLastName == null && normalizedFirstName == null) {
			startKey = ComplexKey.of(null, null)
			endKey = ComplexKey.of(ComplexKey.of(), ComplexKey.emptyObject())
		} else if (normalizedLastName == null) {
			startKey = ComplexKey.of(ComplexKey.emptyObject(), normalizedFirstName)
			endKey = ComplexKey.of(ComplexKey.emptyObject(), normalizedFirstName!! + "\ufff0")
		} else if (normalizedFirstName == null) {
			startKey = ComplexKey.of(normalizedLastName)
			endKey = ComplexKey.of(normalizedLastName + "\ufff0")
		} else {
			startKey = ComplexKey.of(normalizedLastName, normalizedFirstName)
			endKey = ComplexKey.of(normalizedLastName + "\ufff0", normalizedFirstName + "\ufff0")
		}

		val queryView =
			createQuery(
				client  = client,
				legacyView = "deleted_by_names".main(),
				configurationView = "deleted_by_names",
			).startKey(startKey).endKey(endKey).includeDocs(true)
		val deletedByNames = client.queryViewIncludeDocsNoValue<Array<String>, Patient>(queryView).map { it.doc }

		emitAll(
			if (normalizedFirstName == null || normalizedLastName == null) {
				deletedByNames
			} else {
				// filter firstName because main filtering is done on lastName
				deletedByNames
					.filter { p -> p.firstName != null && sanitizeString(p.firstName)?.startsWith(normalizedFirstName) == true }
			},
		)
	}

	@View(
		name = "conflicts",
		map = "function(doc) { if (doc.java_type == 'org.taktik.icure.entities.Patient' && !doc.deleted && doc._conflicts) emit(doc._id) }",
	)
	override fun listConflicts(datastoreInformation: IDatastoreInformation) =
		doListConflicts<Patient>(datastoreInformation, "conflicts", null)

	override fun listIdsOfEntitiesWithConflicts(datastoreInformation: IDatastoreInformation) =
		doListIdsOfEntitiesWithConflicts<Patient>(datastoreInformation, "conflicts", null)

	@View(
		name = "by_modification_date",
		map = "function(doc) { if (doc.java_type == 'org.taktik.icure.entities.Patient' && doc.modified) emit(doc.modified)}",
	)
	override fun findPatientsModifiedAfter(
		datastoreInformation: IDatastoreInformation,
		date: Long,
		paginationOffset: PaginationOffset<Long>,
	) = flow {
		val client = couchDbDispatcher.getClient(datastoreInformation)
		val viewQuery = pagedViewQuery(
			client = client,
			legacyView = "by_modification_date".main(),
			configurationView = "by_modification_date",
			startKey = date,
			endKey = Long.MAX_VALUE,
			pagination = paginationOffset,
			descending = false,
		)
		emitAll(client.queryView<Long, Any, Patient>(viewQuery))
	}

	override fun listPatientIdsByHcPartyAndSsins(
		datastoreInformation: IDatastoreInformation,
		ssins: Collection<String>,
		healthcarePartyId: String,
	): Flow<String> = flow {
		val client = couchDbDispatcher.getClient(datastoreInformation)
		val keys = ssins.map { ssin ->
			ssin.replace(" ".toRegex(), "").replace("\\W".toRegex(), "")
		}.map { ComplexKey.of(healthcarePartyId, it) }

		val viewQueries = createQueries(
			client = client,
			legacyViews = listOf(
				"by_hcparty_ssin".main(),
				"by_data_owner_ssin" to DATA_OWNER_PARTITION,
			),
			configurationViews = listOf("by_all_delegates_ssin")
		)
			.keys(keys)
			.reduce(false)
			.doNotIncludeDocs()

		emitAll(
			client.interleave<ComplexKey, String>(
				viewQueries,
				compareBy({ it.components[0] as? String }, { it.components[1] as? String }),
			).filterIsInstance<ViewRowNoDoc<ComplexKey, String>>().map { it.id },
		)
	}

	@Deprecated("A Data Owner may now have multiple AES Keys. Use getAesExchangeKeysForDelegate instead")
	@View(name = "by_hcparty_delegate_keys", map = "classpath:js/patient/By_hcparty_delegate_keys_map.js")
	override suspend fun getHcPartyKeysForDelegate(
		datastoreInformation: IDatastoreInformation,
		healthcarePartyId: String,
	): Map<String, String> {
		// Not transactional aware
		val result = couchDbDispatcher.getClient(datastoreInformation).queryView<String, List<String>>(
			createQuery(datastoreInformation, "by_hcparty_delegate_keys")
				.includeDocs(false)
				.key(healthcarePartyId),
		)

		val resultMap = HashMap<String, String>()
		result.collect { row ->
			row.value?.let {
				resultMap[it[0]] = it[1]
			}
		}

		return resultMap
	}

	@View(name = "by_delegate_aes_exchange_keys", map = "classpath:js/patient/By_delegate_aes_exchange_keys_map.js")
	override suspend fun getAesExchangeKeysForDelegate(
		datastoreInformation: IDatastoreInformation,
		healthcarePartyId: String,
	): Map<String, Map<String, Map<String, String>>> {
		val client = couchDbDispatcher.getClient(datastoreInformation)
		val result = client.queryView<String, List<String>>(
			createQuery(
				client = client,
				legacyView = "by_delegate_aes_exchange_keys".main(),
				configurationView = "by_delegate_aes_exchange_keys"
			)
				.key(healthcarePartyId)
				.includeDocs(false),
		).map { it.key to it.value }

		return result.fold(emptyMap()) { acc, (key, value) ->
			if (key != null && value != null) {
				acc +
					(
						value[0] to (acc[value[0]] ?: emptyMap()).let { m ->
							m +
								(
									value[1].let { it.substring((it.length - 32).coerceAtLeast(0)) } to (
										m[value[1]]
											?: emptyMap()
										).let { dels ->
										dels + (value[2] to value[3])
									}
									)
						}
						)
			} else {
				acc
			}
		}
	}

	override fun listPatientsByHcPartyAndIdentifier(
		datastoreInformation: IDatastoreInformation,
		searchKeys: Set<String>,
		system: String,
		value: String,
	) = flow {
		val client = couchDbDispatcher.getClient(datastoreInformation)

		val queryView = createQuery(
			client = client,
			legacyView = "by_data_owner_identifier" to DATA_OWNER_PARTITION,
			configurationView = "by_all_delegates_identifier"
		).includeDocs(true)
			.keys(searchKeys.map { ComplexKey.of(it, system, value) })
		emitAll(client.queryViewIncludeDocs<ComplexKey, Void, Patient>(queryView).map { it.doc })
	}.distinctByIdIf(searchKeys.size > 1)

	override fun getDuplicatePatientsBySsin(
		datastoreInformation: IDatastoreInformation,
		healthcarePartyId: String,
		paginationOffset: PaginationOffset<ComplexKey>,
	): Flow<ViewQueryResultEvent> = getDuplicatesFromView(
		datastoreInformation = datastoreInformation,
		legacyViews = listOf("by_hcparty_ssin".main(), "by_data_owner_ssin" to DATA_OWNER_PARTITION),
		configurationViews = listOf("by_all_delegates_ssin"),
		healthcarePartyId = healthcarePartyId,
		paginationOffset = paginationOffset,
	)

	override fun getDuplicatePatientsByName(
		datastoreInformation: IDatastoreInformation,
		healthcarePartyId: String,
		paginationOffset: PaginationOffset<ComplexKey>,
	): Flow<ViewQueryResultEvent> = getDuplicatesFromView(
		datastoreInformation = datastoreInformation,
		legacyViews = listOf(
			if (daoConfig.useObsoleteViews) "by_hcparty_name".main() else "by_hcparty_name_id_as_value" to MAURICE_PARTITION,
			"by_data_owner_name" to DATA_OWNER_PARTITION,
		),
		configurationViews = listOf("by_all_delegates_name"),
		healthcarePartyId = healthcarePartyId,
		paginationOffset = paginationOffset,
	)

	override fun findPatients(
		datastoreInformation: IDatastoreInformation,
		ids: Collection<String>,
	): Flow<ViewQueryResultEvent> = flow {
		val client = couchDbDispatcher.getClient(datastoreInformation)
		emitAll(client.getForPagination(ids, Patient::class.java))
	}

	override fun findPatients(
		datastoreInformation: IDatastoreInformation,
		ids: Flow<String>,
	): Flow<ViewQueryResultEvent> = flow {
		val client = couchDbDispatcher.getClient(datastoreInformation)
		emitAll(client.getForPagination(ids, Patient::class.java))
	}

	@View(
		name = "by_data_owner_identifier",
		map = "classpath:js/patient/By_data_owner_identifier_map.js",
		secondaryPartition = DATA_OWNER_PARTITION,
	)
	override fun listPatientIdsByHcPartyAndIdentifiers(
		datastoreInformation: IDatastoreInformation,
		searchKeys: Set<String>,
		identifiers: List<Identifier>,
	) = flow {
		val client = couchDbDispatcher.getClient(datastoreInformation)

		val keys = identifiers.flatMap {
			searchKeys.map { key ->
				if (it.value != null) {
					ComplexKey.of(key, it.system, it.value)
				} else {
					ComplexKey.of(key, it.system)
				}
			}
		}

		val viewQuery = createQuery(
			client = client,
			legacyView = "by_data_owner_identifier" to DATA_OWNER_PARTITION,
			configurationView = "by_all_delegates_identifier"
		).keys(keys).includeDocs(false)

		emitAll(client.queryView<ComplexKey, Int>(viewQuery).map { it.id })
	}.distinct()

	private fun getDuplicatesFromView(
		datastoreInformation: IDatastoreInformation,
		legacyViews: List<Pair<String, String?>>,
		configurationViews: List<String>,
		healthcarePartyId: String,
		paginationOffset: PaginationOffset<ComplexKey>,
	): Flow<ViewQueryResultEvent> = flow {
		val client = couchDbDispatcher.getClient(datastoreInformation)

		val from = paginationOffset.startKey ?: ComplexKey.of(healthcarePartyId, "")
		val to = ComplexKey.of(healthcarePartyId, ComplexKey.emptyObject())
		val viewQueries = createQueries(
			client = client,
			legacyViews = legacyViews,
			configurationViews = configurationViews,
		)
			.startKey(from)
			.endKey(to)
			.reduce(true)
			.group(true)
			.groupLevel(2)
			.doNotIncludeDocs()

		var duplicates = 0
		// This is needed because pagination on grouped entities cannot rely on startDocumentId.
		// If the document id  is the last document of a group, then the pagination will end even if there
		// are more elements. Therefore, it's better to take a group more.
		var takeNextGroup = true
		val viewResult = client.interleave<ComplexKey, Int>(
			viewQueries,
			compareBy({ it.components[0] as? String }, { it.components[1] as? String }),
			deduplicationMode = DeduplicationMode.NONE,
		).filterIsInstance<ViewRowNoDoc<ComplexKey, Int>>()
		val keysWithDuplicates = viewResult
			.transform {
				if (it.value?.let { count -> count > 1 } == true) {
					takeNextGroup = duplicates <= paginationOffset.limit
					duplicates += it.value ?: 0
					emit(it.key)
				}
			}.takeWhile { duplicates <= paginationOffset.limit || takeNextGroup }.toList()

		val startDocumentId = paginationOffset.startDocumentId
		var sentElements = 0
		var lastVisited: ViewRowWithDoc<ComplexKey, String, Patient>? = null
		val duplicatePatients = client.interleave<ComplexKey, String, Patient>(
			createQueries(
				client = client,
				legacyViews = legacyViews,
				configurationViews = configurationViews,
			).keys(keysWithDuplicates).reduce(false).includeDocs(),
			compareBy({ it.components[0] as? String }, { it.components[1] as? String }),
		).filterIsInstance<ViewRowWithDoc<ComplexKey, String, Patient>>()
			.transform {
				// Skips all the documents of the start key group until we reach the start document id
				if (it.doc.active && (it.key != from || startDocumentId == null || it.doc.id >= startDocumentId)) {
					sentElements++
					if (lastVisited != null) {
						emit(lastVisited!!)
					}
					lastVisited = it
				}
			}.onCompletion {
				if (duplicates >= paginationOffset.limit && sentElements < paginationOffset.limit && lastVisited != null) {
					emitAll(
						getDuplicatesFromView(
							datastoreInformation = datastoreInformation,
							legacyViews = legacyViews,
							configurationViews = configurationViews,
							healthcarePartyId = healthcarePartyId,
							paginationOffset = paginationOffset.copy(
								startKey = lastVisited.key,
								startDocumentId = lastVisited.id,
								limit = paginationOffset.limit - sentElements,
							),
						),
					)
				} else if (lastVisited != null) {
					emit(lastVisited)
				}
			}
		emitAll(duplicatePatients)
	}

	override suspend fun warmupPartition(datastoreInformation: IDatastoreInformation, partition: Partitions) {
		when (partition) {
			Partitions.DataOwner -> {
				val client = couchDbDispatcher.getClient(datastoreInformation)
				val viewQueries = createQueries(
					datastoreInformation,
					"by_data_owner_ssin" to DATA_OWNER_PARTITION,
				).doNotIncludeDocs()
				client.interleave<Array<String>, String>(viewQueries, compareBy { it[0] }).firstOrNull()
			}

			Partitions.Maurice -> {
				val client = couchDbDispatcher.getClient(datastoreInformation)
				val viewQueries = createQueries(
					datastoreInformation,
					"by_hcparty_name_id_as_value" to MAURICE_PARTITION,
				).doNotIncludeDocs()
				client.interleave<Array<String>, String>(viewQueries, compareBy { it[0] }).firstOrNull()
			}

			else -> super.warmupPartition(datastoreInformation, partition)
		}
	}
}
