/*
 * Copyright (c) 2020. Taktik SA, All rights reserved.
 */
package org.taktik.icure.asynclogic.impl

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flattenConcat
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.single
import kotlinx.coroutines.flow.singleOrNull
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.flow.toSet
import org.apache.commons.beanutils.PropertyUtilsBean
import org.apache.commons.lang3.StringUtils
import org.apache.commons.text.similarity.LevenshteinDistance
import org.slf4j.LoggerFactory
import org.taktik.couchdb.TotalCount
import org.taktik.couchdb.ViewQueryResultEvent
import org.taktik.couchdb.ViewRowWithDoc
import org.taktik.couchdb.entity.ComplexKey
import org.taktik.couchdb.entity.IdAndRev
import org.taktik.couchdb.entity.Option
import org.taktik.icure.asyncdao.PatientDAO
import org.taktik.icure.asyncdao.results.filterSuccessfulUpdates
import org.taktik.icure.asynclogic.ExchangeDataMapLogic
import org.taktik.icure.asynclogic.PatientLogic
import org.taktik.icure.asynclogic.PatientLogic.Companion.PatientSearchField
import org.taktik.icure.asynclogic.SessionInformationProvider
import org.taktik.icure.asynclogic.UserLogic
import org.taktik.icure.asynclogic.base.impl.EntityWithEncryptionMetadataLogic
import org.taktik.icure.asynclogic.datastore.DatastoreInstanceProvider
import org.taktik.icure.asynclogic.datastore.IDatastoreInformation
import org.taktik.icure.asynclogic.impl.filter.Filters
import org.taktik.icure.db.PaginationOffset
import org.taktik.icure.db.SortDirection
import org.taktik.icure.db.Sorting
import org.taktik.icure.domain.filter.chain.FilterChain
import org.taktik.icure.entities.Patient
import org.taktik.icure.entities.base.encryptableMetadataEquals
import org.taktik.icure.entities.embed.Delegation
import org.taktik.icure.entities.embed.PatientHealthCareParty
import org.taktik.icure.entities.embed.ReferralPeriod
import org.taktik.icure.entities.embed.SecurityMetadata
import org.taktik.icure.entities.utils.MergeUtil
import org.taktik.icure.exceptions.ConflictRequestException
import org.taktik.icure.exceptions.MissingRequirementsException
import org.taktik.icure.exceptions.NotFoundRequestException
import org.taktik.icure.pagination.limitIncludingKey
import org.taktik.icure.pagination.toPaginatedFlow
import org.taktik.icure.pagination.toPaginatedFlowOfIds
import org.taktik.icure.utils.FuzzyValues
import org.taktik.icure.utils.aggregateResults
import org.taktik.icure.utils.toComplexKeyPaginationOffset
import org.taktik.icure.validation.aspect.Fixer
import java.time.Instant
import java.util.*

open class PatientLogicImpl(
	private val sessionLogic: SessionInformationProvider,
	protected val patientDAO: PatientDAO,
	filters: Filters,
	exchangeDataMapLogic: ExchangeDataMapLogic,
	datastoreInstanceProvider: DatastoreInstanceProvider,
	fixer: Fixer
) : EntityWithEncryptionMetadataLogic<Patient, PatientDAO>(fixer, sessionLogic, datastoreInstanceProvider, exchangeDataMapLogic, filters), PatientLogic {
	companion object {
		private val log = LoggerFactory.getLogger(PatientLogicImpl::class.java)
	}

	private val levenshtein = LevenshteinDistance()

	private suspend fun checkCanUseViewByHcp(healthcarePartyId: String) {
		require(sessionLogic.getAllSearchKeysIfCurrentDataOwner(healthcarePartyId).size == 1) {
			"This method can't be used to search data for anonymous data owners. Use filters instead"
		}
	}

	override suspend fun countByHcParty(healthcarePartyId: String): Int {
		checkCanUseViewByHcp(healthcarePartyId)
		val datastoreInformation = getInstanceAndGroup()
		return patientDAO.countByHcParty(datastoreInformation, healthcarePartyId)
	}

	override fun listOfMergesAfter(date: Long?) = flow {
		val datastoreInformation = getInstanceAndGroup()
		emitAll(patientDAO.listOfMergesAfter(datastoreInformation, date))
	}

	override fun findByHcPartyIdsOnly(healthcarePartyId: String, offset: PaginationOffset<ComplexKey>) = flow {
		checkCanUseViewByHcp(healthcarePartyId)
		val datastoreInformation = getInstanceAndGroup()
		emitAll(patientDAO
			.findPatientIdsByHcParty(datastoreInformation, healthcarePartyId, offset.limitIncludingKey())
			.toPaginatedFlowOfIds(offset.limit)
		)
	}

	override fun findByHcPartyAndSsinOrDateOfBirthOrNameContainsFuzzy(healthcarePartyId: String, offset: PaginationOffset<ComplexKey>, searchString: String?, sorting: Sorting<PatientSearchField>) = flow {
		checkCanUseViewByHcp(healthcarePartyId)
		val descending = SortDirection.desc == sorting.direction
		val datastoreInformation = getInstanceAndGroup()

		val offsetIncludingNextKey = offset.limitIncludingKey()
		if (searchString.isNullOrEmpty()) {
			emitAll(
				when (sorting.field) {
					PatientSearchField.ssin -> {
						patientDAO.findPatientsByHcPartyAndSsin(datastoreInformation, null, healthcarePartyId, offsetIncludingNextKey, descending)
					}

					PatientSearchField.dateOfBirth -> {
						patientDAO.findPatientsByHcPartyDateOfBirth(datastoreInformation, null, null, healthcarePartyId, offsetIncludingNextKey, descending)
					}

					PatientSearchField.patientName -> {
						patientDAO.findPatientsByHcPartyAndName(datastoreInformation, null, healthcarePartyId, offsetIncludingNextKey, descending)
					}
				}
			)
		} else {
			emitAll(
				when {
					FuzzyValues.isSsin(searchString) -> {
						patientDAO.findPatientsByHcPartyAndSsin(datastoreInformation, searchString, healthcarePartyId, offsetIncludingNextKey, false)
					}

					FuzzyValues.isDate(searchString) -> {
						patientDAO.findPatientsByHcPartyDateOfBirth(datastoreInformation, FuzzyValues.toYYYYMMDD(searchString), FuzzyValues.getMaxRangeOf(searchString), healthcarePartyId, offsetIncludingNextKey, false)
					}

					else -> {
						patientDAO.findPatientsByHcPartyNameContainsFuzzy(datastoreInformation, searchString, healthcarePartyId, offsetIncludingNextKey, descending)
					}
				}
			)
		}
	}.toPaginatedFlow<Patient>(offset.limit)

	override fun listPatients(
		paginationOffset: PaginationOffset<*>,
		filterChain: FilterChain<Patient>,
		sort: String?,
		desc: Boolean?
	) = flow {
		val datastoreInformation = getInstanceAndGroup()
		val ids = filters.resolve(filterChain.filter, datastoreInformation).toSet(TreeSet())

		val forPagination = aggregateResults(
			ids = ids,
			limit = paginationOffset.limit,
			supplier = { patientIds: Collection<String> -> patientDAO.findPatients(datastoreInformation, patientIds) },
			filter = { queryResult: ViewQueryResultEvent ->
				filterChain.predicate?.let { queryResult is ViewRowWithDoc<*, *, *> && it.apply(queryResult.doc as Patient) }
					?: (queryResult is ViewRowWithDoc<*, *, *> && queryResult.doc is Patient)
			},
			filteredOutAccumulator = ids.size,
			filteredOutElementsReducer = { totalCount, _ -> totalCount },
			startDocumentId = paginationOffset.startDocumentId
		)
		if (sort != null && sort != "id") { // TODO MB is this the correct way to sort here ?
			var patientsListToSort = forPagination.second.toList()
			val pub = PropertyUtilsBean()
			patientsListToSort = patientsListToSort.sortedWith { a, b ->
				try {
					val ap = pub.getProperty(a, sort) as Comparable<*>?
					val bp = pub.getProperty(b, sort) as Comparable<*>?
					if (ap is String && bp is String) {
						if (desc != null && desc) {
							StringUtils.compareIgnoreCase(bp, ap)
						} else {
							StringUtils.compareIgnoreCase(ap, bp)
						}
					} else {
						@Suppress("UNCHECKED_CAST")
						ap as Comparable<Any>?
						@Suppress("UNCHECKED_CAST")
						bp as Comparable<Any>?
						if (desc != null && desc) {
							ap?.let { bp?.compareTo(it) ?: 1 } ?: bp?.let { -1 } ?: 0
						} else {
							bp?.let { ap?.compareTo(it) ?: 1 } ?: 0
						}
					}
				} catch (e: Exception) {
					0
				}
			}
			emitAll(patientsListToSort.asFlow())
		} else {
			forPagination.second.forEach { emit(it) }
		}
		emit(TotalCount(forPagination.first))
	}

	override fun findByHcPartyNameContainsFuzzy(
		searchString: String?,
		healthcarePartyId: String,
		offset: PaginationOffset<ComplexKey>,
		descending: Boolean
	): Flow<ViewQueryResultEvent> = flow {
		checkCanUseViewByHcp(healthcarePartyId)
		val datastoreInformation = getInstanceAndGroup()
		emitAll(
			patientDAO.findPatientsByHcPartyNameContainsFuzzy(datastoreInformation, searchString, healthcarePartyId, offset, descending)
		)
	}

	override fun findOfHcPartyNameContainsFuzzy(
		searchString: String?,
		healthcarePartyId: String,
		offset: PaginationOffset<ComplexKey>,
		descending: Boolean
	): Flow<ViewQueryResultEvent> = flow {
		checkCanUseViewByHcp(healthcarePartyId)
		val datastoreInformation = getInstanceAndGroup()
		emitAll(
			patientDAO.findPatientsOfHcPartyNameContainsFuzzy(datastoreInformation, searchString, healthcarePartyId, offset, descending)
		)
	}

	override fun findOfHcPartyAndSsinOrDateOfBirthOrNameContainsFuzzy(
		healthcarePartyId: String,
		offset: PaginationOffset<ComplexKey>,
		searchString: String?,
		sorting: Sorting<PatientSearchField>
	) = flow {
		val descending = SortDirection.desc == sorting.direction
		val datastoreInformation = getInstanceAndGroup()
		val offsetIncludingNextKey = offset.limitIncludingKey()

		emitAll(
			if (searchString.isNullOrEmpty()) {
				when (sorting.field) {
					PatientSearchField.ssin -> {
						patientDAO.findPatientsOfHcPartyAndSsin(datastoreInformation, null, healthcarePartyId, offsetIncludingNextKey, descending)
					}

					PatientSearchField.dateOfBirth -> {
						patientDAO.findPatientsOfHcPartyDateOfBirth(datastoreInformation, null, null, healthcarePartyId, offsetIncludingNextKey, descending)
					}

					PatientSearchField.patientName -> {
						patientDAO.findPatientsOfHcPartyAndName(datastoreInformation, null, healthcarePartyId, offsetIncludingNextKey, descending)
					}
				}
			} else {
				when {
					FuzzyValues.isSsin(searchString) -> {
						patientDAO.findPatientsOfHcPartyAndSsin(datastoreInformation, searchString, healthcarePartyId, offsetIncludingNextKey, false)
					}

					FuzzyValues.isDate(searchString) -> {
						patientDAO.findPatientsOfHcPartyDateOfBirth(
							datastoreInformation, FuzzyValues.toYYYYMMDD(searchString),
							FuzzyValues.getMaxRangeOf(searchString), healthcarePartyId, offsetIncludingNextKey, false
						)
					}

					else -> {
						patientDAO.findPatientsOfHcPartyNameContainsFuzzy(datastoreInformation, searchString, healthcarePartyId, offsetIncludingNextKey, descending)
					}
				}
			}
		)
	}.toPaginatedFlow<Patient>(offset.limit)

	override fun findByHcPartyAndSsin(ssin: String?, healthcarePartyId: String, paginationOffset: PaginationOffset<List<String>>) = flow {
		checkCanUseViewByHcp(healthcarePartyId)
		val datastoreInformation = getInstanceAndGroup()
		emitAll(patientDAO.findPatientsByHcPartyAndSsin(datastoreInformation, ssin!!, healthcarePartyId, paginationOffset.toComplexKeyPaginationOffset(), false))
	}

	override fun findByHcPartyDateOfBirth(date: Int?, healthcarePartyId: String, paginationOffset: PaginationOffset<List<String>>) = flow {
		checkCanUseViewByHcp(healthcarePartyId)
		val datastoreInformation = getInstanceAndGroup()
		emitAll(patientDAO.findPatientsByHcPartyDateOfBirth(datastoreInformation, date, date, healthcarePartyId, paginationOffset.toComplexKeyPaginationOffset(), false))
	}

	override suspend fun findByUserId(id: String): Patient? {
		val datastoreInformation = getInstanceAndGroup()
		return patientDAO.findPatientsByUserId(datastoreInformation, id)
	}

	override suspend fun getPatient(patientId: String): Patient? = getEntity(patientId)

	override fun findByHcPartyAndIdentifier(healthcarePartyId: String, system: String, id: String) = flow {
		val datastoreInformation = getInstanceAndGroup()
		emitAll(patientDAO.listPatientsByHcPartyAndIdentifier(datastoreInformation, getAllSearchKeysIfCurrentDataOwner(healthcarePartyId), system, id))
	}

	override fun getPatients(patientIds: Collection<String>) = getEntities(patientIds)

	override suspend fun addDelegation(patientId: String, delegation: Delegation): Patient? {
		val patient = getPatient(patientId)
		val datastoreInformation = getInstanceAndGroup()
		return delegation.delegatedTo?.let { healthcarePartyId ->
			patient?.let { c ->
				patientDAO.save(
					datastoreInformation,
					c.copy(
						delegations = c.delegations + mapOf(
							healthcarePartyId to setOf(delegation)
						)
					)
				)
			}
		} ?: patient
	}

	override suspend fun addDelegations(patientId: String, delegations: Collection<Delegation>): Patient? {
		val patient = getPatient(patientId)
		val datastoreInformation = getInstanceAndGroup()
		return patient?.let {
			patientDAO.save(
				datastoreInformation,
				it.copy(
					delegations = it.delegations +
						delegations.mapNotNull { d -> d.delegatedTo?.let { delegateTo -> delegateTo to setOf(d) } }
				)
			)
		}
	}

	override suspend fun createPatient(patient: Patient) = fix(patient, isCreate = true) { fixedPatient ->
		if(fixedPatient.rev != null) throw IllegalArgumentException("A new entity should not have a rev")
		checkRequirements(fixedPatient)
		createEntities(setOf(fixedPatient)).singleOrNull()
	}

	override fun createPatients(patients: List<Patient>): Flow<Patient> = flow {
		val fixedPatients = patients.map { fix(it, isCreate = true) }
		emitAll(createEntities(fixedPatients))
	}

	override suspend fun modifyPatient(patient: Patient): Patient? = fix(patient, isCreate = false) { fixedPatient -> // access control already done by modify entities
		log.debug("Modifying patient with id:" + fixedPatient.id)
		checkRequirements(fixedPatient)
		modifyEntities(listOf(fixedPatient)).firstOrNull()
	}

	override fun modifyPatients(patients: Collection<Patient>): Flow<Patient> = flow { // access control already done by modify entities
		val fixedPatients = patients.map { fix(it, isCreate = false) }
		emitAll(modifyEntities(fixedPatients))
	}

	override fun createEntities(entities: Collection<Patient>): Flow<Patient> = flow {
		entities.forEach { checkRequirements(it) }
		emitAll(super.createEntities(entities))
	}

	override fun modifyEntities(entities: Collection<Patient>): Flow<Patient> = flow {
		entities.forEach { checkRequirements(it) }
		emitAll(super.modifyEntities(entities))
	}

	private fun checkRequirements(patient: Patient) {
		if (!patient.isValidForStore()) {
			throw MissingRequirementsException("modifyPatient: Name, Last name  are required.")
		}
	}

	override suspend fun modifyPatientReferral(patient: Patient, referralId: String?, start: Instant?, end: Instant?): Patient? {
		val startOrNow = start ?: Instant.now()
		//Close referrals relative to other healthcare parties
		val fixedPhcp = patient.patientHealthCareParties.map { phcp ->
			if (phcp.referral && (referralId == null || referralId != phcp.healthcarePartyId)) {
				phcp.copy(
					referral = false,
					referralPeriods = phcp.referralPeriods.map { p ->
						if (p.endDate == null || p.endDate != startOrNow) {
							p.copy(endDate = startOrNow)
						} else p
					}.toSortedSet()
				)
			} else if (referralId != null && referralId == phcp.healthcarePartyId) {
				(
					if (!phcp.referral) {
						phcp.copy(referral = true)
					} else phcp
					).copy(
						referralPeriods = phcp.referralPeriods.map { rp ->
							if (start == rp.startDate) {
								rp.copy(endDate = end)
							} else rp
						}.toSortedSet()
					)
			} else phcp
		}
		return (
			if (!fixedPhcp.any { it.referral && it.healthcarePartyId == referralId }) {
				fixedPhcp + PatientHealthCareParty(
					referral = true,
					healthcarePartyId = referralId,
					referralPeriods = sortedSetOf(ReferralPeriod(startOrNow, end))
				)
			} else fixedPhcp
			).let {
				if (it != patient.patientHealthCareParties) {
					modifyPatient(patient.copy(patientHealthCareParties = it))
				} else
					patient
			}
	}

	override fun getEntityIds() = flow {
		val datastoreInformation = getInstanceAndGroup()
		emitAll(patientDAO.getEntityIds(datastoreInformation))
	}

	override suspend fun getByExternalId(externalId: String): Patient? {
		val datastoreInformation = getInstanceAndGroup()
		return patientDAO.getPatientByExternalId(datastoreInformation, externalId)
	}

	override fun solveConflicts(limit: Int?, ids: List<String>?) = flow { emitAll(doSolveConflicts(
		ids,
		limit,
		getInstanceAndGroup()
	)) }

	protected fun doSolveConflicts(
		ids: List<String>?,
		limit: Int?,
		datastoreInformation: IDatastoreInformation,
	) =  flow {
		val flow = ids?.asFlow()?.mapNotNull { patientDAO.get(datastoreInformation, it, Option.CONFLICTS) }
			?: patientDAO.listConflicts(datastoreInformation)
				.mapNotNull { patientDAO.get(datastoreInformation, it.id, Option.CONFLICTS) }
		(limit?.let { flow.take(it) } ?: flow)
			.mapNotNull { patient ->
				patient.conflicts?.mapNotNull { conflictingRevision ->
					patientDAO.get(
						datastoreInformation, patient.id, conflictingRevision
					)
				}?.fold(patient to emptyList<Patient>()) { (kept, toBePurged), conflict ->
					kept.merge(conflict) to toBePurged + conflict
				}?.let { (mergedPatient, toBePurged) ->
					patientDAO.save(datastoreInformation, mergedPatient).also {
						toBePurged.forEach {
							if (it.rev != null && it.rev != mergedPatient.rev) {
								patientDAO.purge(datastoreInformation, listOf(it)).single()
							}
						}
					}
				}
			}
			.collect { emit(IdAndRev(it.id, it.rev)) }
	}

	@Deprecated("A DataOwner may now have multiple AES Keys. Use getAesExchangeKeysForDelegate instead")
	@Suppress("DEPRECATION")
	override suspend fun getHcPartyKeysForDelegate(healthcarePartyId: String): Map<String, String> {
		val datastoreInformation = getInstanceAndGroup()
		return patientDAO.getHcPartyKeysForDelegate(datastoreInformation, healthcarePartyId)
	}

	override suspend fun getAesExchangeKeysForDelegate(healthcarePartyId: String): Map<String, Map<String, Map<String, String>>> {
		val datastoreInformation = getInstanceAndGroup()
		return patientDAO.getAesExchangeKeysForDelegate(datastoreInformation, healthcarePartyId)
	}

	override fun listOfPatientsModifiedAfter(date: Long, paginationOffset: PaginationOffset<Long>) = flow {
		val datastoreInformation = getInstanceAndGroup()
		emitAll(
			patientDAO
				.findPatientsModifiedAfter(datastoreInformation, date, paginationOffset.limitIncludingKey())
				.toPaginatedFlow<Patient>(paginationOffset.limit)
		)
	}

	override fun getDuplicatePatientsBySsin(healthcarePartyId: String, paginationOffset: PaginationOffset<ComplexKey>) = flow {
		checkCanUseViewByHcp(healthcarePartyId)
		val datastoreInformation = getInstanceAndGroup()
		emitAll(patientDAO
			.getDuplicatePatientsBySsin(datastoreInformation, healthcarePartyId, paginationOffset.limitIncludingKey())
			.toPaginatedFlow<Patient>(paginationOffset.limit)
		)
	}

	override fun getDuplicatePatientsByName(healthcarePartyId: String, paginationOffset: PaginationOffset<ComplexKey>) = flow {
		checkCanUseViewByHcp(healthcarePartyId)
		val datastoreInformation = getInstanceAndGroup()
		emitAll(patientDAO
			.getDuplicatePatientsByName(datastoreInformation, healthcarePartyId, paginationOffset.limitIncludingKey())
			.toPaginatedFlow<Patient>(paginationOffset.limit)
		)
	}

	@OptIn(ExperimentalCoroutinesApi::class)
	override fun fuzzySearchPatients(firstName: String?, lastName: String?, dateOfBirth: Int?, healthcarePartyId: String?) = flow {
		val currentHealthcarePartyId = healthcarePartyId ?: sessionLogic.getCurrentHealthcarePartyId()
		checkCanUseViewByHcp(currentHealthcarePartyId)
		if (dateOfBirth != null) { //Patients with the right date of birth
			val combined: Flow<Flow<ViewQueryResultEvent>>
			val patients = findByHcPartyDateOfBirth(dateOfBirth, currentHealthcarePartyId, PaginationOffset(1000))

			//Patients for which the date of birth is unknown
			combined = if (firstName != null && lastName != null) {
				val patientsNoBirthDate = findByHcPartyDateOfBirth(null, currentHealthcarePartyId, PaginationOffset(1000))
				flowOf(patients, patientsNoBirthDate)
			} else {
				flowOf(patients)
			}
			emitAll(
				combined.flattenConcat()
					.filterIsInstance<ViewRowWithDoc<*, *, *>>()
					.map { it.doc as Patient }
					.filter { p: Patient -> firstName == null || p.firstName == null || p.firstName.toString().lowercase().startsWith(firstName.lowercase()) || firstName.lowercase().startsWith(p.firstName.toString().lowercase()) || levenshtein.apply(firstName.lowercase(), p.firstName.toString().lowercase()) <= 2 }
					.filter { p: Patient -> lastName == null || p.lastName == null || levenshtein.apply(lastName.lowercase(), p.lastName.toString().lowercase()) <= 2 }
					.filter { p: Patient -> p.firstName != null && p.firstName.toString().length >= 3 || p.lastName != null && p.lastName.toString().length >= 3 }

			)
		} else if (lastName != null) {
			val datastore = getInstanceAndGroup()
			emitAll(
				patientDAO.findPatientsByHcPartyNameContainsFuzzy(
					datastore,
					lastName.substring(0, (lastName.length - 2).coerceAtLeast(6).coerceAtMost(lastName.length)),
					currentHealthcarePartyId,
					PaginationOffset(1000),
					false
				).filterIsInstance<ViewRowWithDoc<*, *, *>>()
					.map { it.doc as Patient }
					.filter { p: Patient -> firstName == null || p.firstName == null || p.firstName.toString().lowercase().startsWith(firstName.lowercase()) || firstName.lowercase().startsWith(p.firstName.toString().lowercase()) || levenshtein.apply(firstName.lowercase(), p.firstName.toString().lowercase()) <= 2 }
					.filter { p: Patient -> p.lastName == null || levenshtein.apply(lastName.lowercase(), p.lastName.toString().lowercase()) <= 2 }
					.filter { p: Patient -> p.firstName != null && p.firstName.toString().length >= 3 || p.lastName != null && p.lastName.toString().length >= 3 }
			)
		}
	}

	override fun findDeletedPatientsByDeleteDate(start: Long, end: Long?, descending: Boolean, paginationOffset: PaginationOffset<Long>) = flow {
		val datastoreInformation = getInstanceAndGroup()
		emitAll(patientDAO
			.findDeletedPatientsByDeleteDate(datastoreInformation, start, end, descending, paginationOffset.limitIncludingKey())
			.toPaginatedFlow<Patient>(paginationOffset.limit)
		)
	}

	override fun listDeletedPatientsByNames(firstName: String?, lastName: String?) = flow {
		val datastoreInformation = getInstanceAndGroup()
		emitAll(patientDAO.findDeletedPatientsByNames(datastoreInformation, firstName, lastName))
	}

	override fun getGenericDAO(): PatientDAO {
		return patientDAO
	}

	override fun entityWithUpdatedSecurityMetadata(entity: Patient, updatedMetadata: SecurityMetadata): Patient =
		entity.copy(securityMetadata = updatedMetadata)

	override suspend fun mergePatients(fromId: String, expectedFromRev: String, updatedInto: Patient): Patient {
		require (fromId != updatedInto.id) { "Impossible to merge an entity with itself" }
		val dbInfo = getInstanceAndGroup()
		val originalPatients = patientDAO.getPatients(dbInfo, listOf(fromId, updatedInto.id)).toList()
		val ogFrom = originalPatients.firstOrNull { it.id == fromId }
			?: throw NotFoundRequestException("Patient with id $fromId not found")
		val ogInto = originalPatients.firstOrNull { it.id == updatedInto.id }
			?: throw NotFoundRequestException("Patient with id ${updatedInto.id} not found")
		if (expectedFromRev != ogFrom.rev || updatedInto.rev != ogInto.rev) {
			throw ConflictRequestException("Outdated patient revisions provided")
		}
		val updatedFrom = ogFrom.copy(
			deletionDate = Instant.now().toEpochMilli(),
			mergeToPatientId = ogInto.id
		)
		val mergedInto = mergePatientsMetadata(ogFrom, ogInto, updatedInto)
		/*
		 * In this time ogFrom or ogInto may have changed (unlikely but possible), meaning that if we do a simple bulk
		 * modify we may modify only one of the two patients, which goes against the contract of this method.
		 * We can make the possibility of partial modification even more unlikely by using the following optimistic
		 * locking strategy:
		 * 1. Update only the revision of the original patients. This invalidates all the revisions already available
		 * to any other process (other request on the iCure kraken which may be in progress, some process in the client,
		 * ...).
		 * 2. Actually modify the content (and revisions).
		 * This way it is extremely unlikely that someone is able to perform a client->backend->couch->backend->client
		 * get followed by a client->backend->couch modify before we can do two backend->couch updates.
		 */
		val updatedRevs = patientDAO.saveBulk(dbInfo, listOf(ogFrom, ogInto)).filterSuccessfulUpdates().toList()
		if (updatedRevs.size != 2) throw ConflictRequestException("Outdated patient revisions provided")
		val updatedData = patientDAO.saveBulk(
			dbInfo,
			listOf(
				updatedFrom.withIdRev(rev = checkNotNull(updatedRevs.first { it.id == updatedFrom.id }.rev)),
				mergedInto.withIdRev(rev = checkNotNull(updatedRevs.first { it.id == mergedInto.id }.rev)),
			)
		).filterSuccessfulUpdates().toList()
		if (updatedData.size != 2) {
			val message = "Optimistic locking for patient merge failed (from: $fromId, into: ${mergedInto.id})"
			log.error(message)
			throw IllegalStateException(message)
		}
		return updatedData.first { it.id == mergedInto.id }
	}

	private fun mergePatientsMetadata(
		originalFrom: Patient,
		originalInto: Patient,
		updatedInto: Patient
	): Patient {
		require(
			originalInto.encryptableMetadataEquals(updatedInto) && originalInto.mergedIds == updatedInto.mergedIds
		) {
			"You must not change metadata of the updated into patient: it will be automatically updated during the entity merging"
		}
		return updatedInto.copy(
			securityMetadata = originalInto.securityMetadata?.let { intoMetadata ->
				originalFrom.securityMetadata?.let { fromMetadata ->
					intoMetadata.mergeForDuplicatedEntityIntoThisFrom(fromMetadata)
				} ?: intoMetadata
			} ?: originalFrom.securityMetadata,
			delegations = MergeUtil.mergeMapsOfSets(originalFrom.delegations, originalInto.delegations),
			encryptionKeys = originalInto.encryptionKeys,
			cryptedForeignKeys = MergeUtil.mergeMapsOfSets(originalFrom.cryptedForeignKeys, originalInto.cryptedForeignKeys),
			secretForeignKeys = originalFrom.secretForeignKeys + originalInto.secretForeignKeys,
			mergedIds = originalInto.mergedIds + originalFrom.id
		)
	}
}
