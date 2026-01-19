/*
 * Copyright (c) 2020. Taktik SA, All rights reserved.
 */
package org.taktik.icure.asynclogic.impl

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.dropWhile
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.take
import org.taktik.couchdb.entity.ComplexKey
import org.taktik.couchdb.exception.DocumentNotFoundException
import org.taktik.icure.asyncdao.HealthcarePartyDAO
import org.taktik.icure.asyncdao.results.filterSuccessfulUpdates
import org.taktik.icure.asynclogic.HealthcarePartyLogic
import org.taktik.icure.asynclogic.impl.filter.Filters
import org.taktik.icure.db.PaginationOffset
import org.taktik.icure.domain.filter.chain.FilterChain
import org.taktik.icure.entities.HealthcareParty
import org.taktik.icure.exceptions.MissingRequirementsException
import org.taktik.icure.pagination.PaginationElement
import org.taktik.icure.pagination.limitIncludingKey
import org.taktik.icure.pagination.toPaginatedFlow
import org.taktik.icure.validation.aspect.Fixer

open class HealthcarePartyLogicImpl(
	filters: Filters,
	private val healthcarePartyDAO: HealthcarePartyDAO,
	datastoreInstanceProvider: org.taktik.icure.datastore.DatastoreInstanceProvider,
	fixer: Fixer,
) : GenericLogicImpl<HealthcareParty, HealthcarePartyDAO>(fixer, datastoreInstanceProvider, filters),
	HealthcarePartyLogic {
	override fun getGenericDAO(): HealthcarePartyDAO = healthcarePartyDAO

	override suspend fun getHealthcareParty(id: String): HealthcareParty? {
		val datastoreInformation = getInstanceAndGroup()
		return healthcarePartyDAO.get(datastoreInformation, id)
	}

	override fun listHealthcarePartiesBy(
		searchString: String,
		offset: Int,
		limit: Int,
	): Flow<HealthcareParty> = flow {
		val datastoreInformation = getInstanceAndGroup()
		emitAll(healthcarePartyDAO.listHealthcareParties(datastoreInformation, searchString, offset, limit))
	}

	@Deprecated("A HCP may now have multiple AES Keys. Use getAesExchangeKeysForDelegate instead")
	@Suppress("DEPRECATION")
	override suspend fun getHcPartyKeysForDelegate(healthcarePartyId: String): Map<String, String> {
		val datastoreInformation = getInstanceAndGroup()
		return healthcarePartyDAO.getHcPartyKeysForDelegate(datastoreInformation, healthcarePartyId)
	}

	override suspend fun getAesExchangeKeysForDelegate(healthcarePartyId: String): Map<String, Map<String, Map<String, String>>> {
		val datastoreInformation = getInstanceAndGroup()
		return healthcarePartyDAO.getAesExchangeKeysForDelegate(datastoreInformation, healthcarePartyId)
	}

	protected fun validateHealthcareParty(healthcareParty: HealthcareParty) {
		if (healthcareParty.nihii == null &&
			healthcareParty.ssin == null &&
			healthcareParty.name == null &&
			healthcareParty.lastName == null
		) {
			throw MissingRequirementsException("One of Name or Last name, Nihii, and Public key are required.")
		}
	}

	protected fun validateHealthcareParties(healthcareParties: List<HealthcareParty>): List<HealthcareParty> =
		healthcareParties.onEach { validateHealthcareParty(it) }

	override suspend fun modifyHealthcareParty(healthcareParty: HealthcareParty) = fix(healthcareParty, isCreate = false) { fixedHealthcareParty ->
		checkValidityForModification(fixedHealthcareParty)
		validateHealthcareParty(fixedHealthcareParty)
		modifyEntity(fixedHealthcareParty)
	}

	override fun modifyHealthcareParties(healthcareParties: List<HealthcareParty>) = flow {
		emitAll(
			healthcarePartyDAO.saveBulk(
				datastoreInformation = getInstanceAndGroup(),
				entities = validateHealthcareParties(healthcareParties.map { fix(it, isCreate = false) }.onEach { checkValidityForModification(it) }),
			).filterSuccessfulUpdates()
		)
	}

	override suspend fun createHealthcareParty(healthcareParty: HealthcareParty) = fix(healthcareParty, isCreate = true) { fixedHealthcareParty ->
		checkValidityForCreation(fixedHealthcareParty)
		validateHealthcareParty(fixedHealthcareParty)
		createEntity(fixedHealthcareParty)
	}

	override fun createHealthcareParties(healthcareParties: List<HealthcareParty>) = flow {
		emitAll(
			healthcarePartyDAO.saveBulk(
				datastoreInformation = getInstanceAndGroup(),
				entities = validateHealthcareParties(healthcareParties.map { fix(it, isCreate = true) }.onEach { checkValidityForCreation(it) }),
			).filterSuccessfulUpdates()
		)
	}

	override fun findHealthcarePartiesBy(
		offset: PaginationOffset<String>,
		desc: Boolean?,
	): Flow<PaginationElement> = flow {
		val datastoreInformation = getInstanceAndGroup()
		val healthcareParties =
			healthcarePartyDAO
				.findHealthCareParties(datastoreInformation, offset.limitIncludingKey(), desc)
		emitAll(healthcareParties.toPaginatedFlow<HealthcareParty>(offset.limit))
	}

	override fun findHealthcarePartiesBy(
		fuzzyName: String,
		offset: PaginationOffset<String>,
		desc: Boolean?,
	): Flow<PaginationElement> = flow {
		val datastoreInformation = getInstanceAndGroup()
		val healthcareParties =
			healthcarePartyDAO
				.findHealthcarePartiesByHcPartyNameContainsFuzzy(datastoreInformation, fuzzyName, offset.limitIncludingKey(), desc)
				.toPaginatedFlow<HealthcareParty>(offset.limit)
		emitAll(healthcareParties)
	}

	override fun listHealthcarePartiesByNihii(nihii: String): Flow<HealthcareParty> = flow {
		val datastoreInformation = getInstanceAndGroup()
		emitAll(healthcarePartyDAO.listHealthcarePartiesByNihii(datastoreInformation, nihii))
	}

	override fun listHealthcarePartiesBySsin(ssin: String): Flow<HealthcareParty> = flow {
		val datastoreInformation = getInstanceAndGroup()
		emitAll(healthcarePartyDAO.listHealthcarePartiesBySsin(datastoreInformation, ssin))
	}

	override fun listHealthcarePartiesByName(name: String): Flow<HealthcareParty> = flow {
		val datastoreInformation = getInstanceAndGroup()
		emitAll(healthcarePartyDAO.listHealthcarePartiesByName(datastoreInformation, name))
	}

	override suspend fun getPublicKey(healthcarePartyId: String): String? {
		val datastoreInformation = getInstanceAndGroup()
		val hcParty =
			healthcarePartyDAO.get(datastoreInformation, healthcarePartyId)
				?: throw DocumentNotFoundException("Healthcare party ($healthcarePartyId) not found in the database.")
		return hcParty.publicKey
	}

	override fun listHealthcarePartiesBySpecialityAndPostcode(
		type: String,
		spec: String,
		firstCode: String,
		lastCode: String,
		offset: PaginationOffset<ComplexKey>,
	): Flow<PaginationElement> = flow {
		val datastoreInformation = getInstanceAndGroup()
		emitAll(
			healthcarePartyDAO
				.listHealthcarePartiesBySpecialityAndPostcode(datastoreInformation, type, spec, firstCode, lastCode, offset.limitIncludingKey())
				.toPaginatedFlow<HealthcareParty>(offset.limit),
		)
	}

	override fun getHealthcareParties(ids: List<String>): Flow<HealthcareParty> = flow {
		val datastoreInformation = getInstanceAndGroup()
		emitAll(healthcarePartyDAO.getEntities(datastoreInformation, ids))
	}

	override fun findHealthcarePartiesBySsinOrNihii(
		searchValue: String,
		paginationOffset: PaginationOffset<String>,
		desc: Boolean,
	): Flow<PaginationElement> = flow {
		val datastoreInformation = getInstanceAndGroup()
		emitAll(
			healthcarePartyDAO
				.findHealthcarePartiesBySsinOrNihii(datastoreInformation, searchValue, paginationOffset.limitIncludingKey(), desc)
				.toPaginatedFlow<HealthcareParty>(paginationOffset.limit),
		)
	}

	override fun getHealthcarePartiesByParentId(parentId: String): Flow<HealthcareParty> = flow {
		val datastoreInformation = getInstanceAndGroup()
		emitAll(healthcarePartyDAO.listHealthcarePartiesByParentId(datastoreInformation, parentId))
	}

	override suspend fun getHcpHierarchyIds(sender: HealthcareParty): HashSet<String> {
		val hcpartyIds = HashSet<String>()
		hcpartyIds.add(sender.id)

		var hcpInHierarchy: HealthcareParty? = sender

		while (hcpInHierarchy?.parentId?.isNotBlank() == true) {
			hcpInHierarchy = getHealthcareParty(hcpInHierarchy.parentId!!)
			hcpInHierarchy?.id?.let { hcpartyIds.add(it) }
		}
		return hcpartyIds
	}

	override fun filterHealthcareParties(
		paginationOffset: PaginationOffset<Nothing>,
		filter: FilterChain<HealthcareParty>,
	) = flow {
		val datastoreInformation = getInstanceAndGroup()
		val ids = filters.resolve(filter.filter, datastoreInformation)
		val sortedIds =
			paginationOffset
				.takeUnless {
					it.startDocumentId == null
				}?.let { paginationOffset ->
					// Sub-set starting from startDocId to the end (including last element)
					ids.dropWhile { id -> id != paginationOffset.startDocumentId }
				} ?: ids

		val selectedIds = sortedIds.take(paginationOffset.limit + 1) // Fetching one more healthcare parties for the start key of the next page
		emitAll(healthcarePartyDAO.findHealthcarePartiesByIds(datastoreInformation, selectedIds))
	}
}
