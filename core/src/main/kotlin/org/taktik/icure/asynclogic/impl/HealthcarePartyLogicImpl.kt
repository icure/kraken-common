/*
 * Copyright (c) 2020. Taktik SA, All rights reserved.
 */
package org.taktik.icure.asynclogic.impl

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.dropWhile
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.take
import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Service
import org.taktik.couchdb.DocIdentifier
import org.taktik.couchdb.ViewQueryResultEvent
import org.taktik.couchdb.entity.ComplexKey
import org.taktik.couchdb.exception.DocumentNotFoundException
import org.taktik.icure.asyncdao.HealthcarePartyDAO
import org.taktik.icure.asynclogic.HealthcarePartyLogic
import org.taktik.icure.asynclogic.impl.filter.Filters
import org.taktik.icure.db.PaginationOffset
import org.taktik.icure.domain.filter.chain.FilterChain
import org.taktik.icure.entities.HealthcareParty
import org.taktik.icure.entities.embed.Identifier
import org.taktik.icure.exceptions.DeletionException
import org.taktik.icure.exceptions.MissingRequirementsException
import org.taktik.icure.pagination.PaginatedElement
import org.taktik.icure.pagination.limitIncludingKey
import org.taktik.icure.pagination.toPaginatedFlow
import org.taktik.icure.validation.aspect.Fixer
import java.util.*

@Service
@Profile("app")
class HealthcarePartyLogicImpl(
    private val filters: Filters,
    private val healthcarePartyDAO: HealthcarePartyDAO,
    datastoreInstanceProvider: org.taktik.icure.asynclogic.datastore.DatastoreInstanceProvider,
    fixer: Fixer
) : GenericLogicImpl<HealthcareParty, HealthcarePartyDAO>(fixer, datastoreInstanceProvider), HealthcarePartyLogic {

	companion object {
		private val log = LoggerFactory.getLogger(HealthcarePartyLogicImpl::class.java)
	}

	override fun getGenericDAO(): HealthcarePartyDAO {
		return healthcarePartyDAO
	}

	override suspend fun getHealthcareParty(id: String): HealthcareParty? {
		val datastoreInformation = getInstanceAndGroup()
		return healthcarePartyDAO.get(datastoreInformation, id)
	}

	override fun listHealthcarePartiesBy(searchString: String, offset: Int, limit: Int): Flow<HealthcareParty> =
		flow {
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

	override suspend fun modifyHealthcareParty(healthcareParty: HealthcareParty) =
		fix(healthcareParty) { fixedHealthcareParty ->
			if (fixedHealthcareParty.nihii == null && fixedHealthcareParty.ssin == null && fixedHealthcareParty.name == null && fixedHealthcareParty.lastName == null) {
				throw MissingRequirementsException("modifyHealthcareParty: one of Name or Last name, Nihii or  Ssin are required.")
			}
			modifyEntities(setOf(fixedHealthcareParty)).firstOrNull()
		}

	override fun deleteHealthcareParties(healthcarePartyIds: List<String>): Flow<DocIdentifier> =
		flow {
			try {
				emitAll(
					deleteEntities(healthcarePartyIds)
				)
			} catch (e: Exception) {
				log.error(e.message, e)
				throw DeletionException("The healthcare party (" + healthcarePartyIds + ") not found or " + e.message, e)
			}
		}

	override suspend fun createHealthcareParty(healthcareParty: HealthcareParty) =
		fix(healthcareParty) { fixedHealthcareParty ->
			if (fixedHealthcareParty.nihii == null && fixedHealthcareParty.ssin == null && fixedHealthcareParty.name == null && fixedHealthcareParty.lastName == null) {
				throw MissingRequirementsException("createHealthcareParty: one of Name or Last name, Nihii, and Public key are required.")
			}
			if(fixedHealthcareParty.rev != null) throw IllegalArgumentException("A new entity should not have a rev")
			try {
				createEntities(setOf(fixedHealthcareParty)).firstOrNull()
			} catch (e: Exception) {
				throw IllegalArgumentException("Invalid healthcare party", e)
			}
		}

	override fun findHealthcarePartiesBy(offset: PaginationOffset<String>, desc: Boolean?): Flow<PaginatedElement> =
		flow {
			val datastoreInformation = getInstanceAndGroup()
			val healthcareParties = healthcarePartyDAO
				.findHealthCareParties(datastoreInformation, offset.limitIncludingKey(), desc)
			emitAll(healthcareParties.toPaginatedFlow<HealthcareParty>(offset.limit))
		}

	override fun findHealthcarePartiesBy(fuzzyName: String, offset: PaginationOffset<String>, desc: Boolean?): Flow<PaginatedElement> = flow {
		val datastoreInformation = getInstanceAndGroup()
		val healthcareParties = healthcarePartyDAO
			.findHealthcarePartiesByHcPartyNameContainsFuzzy(datastoreInformation, fuzzyName, offset.limitIncludingKey(), desc)
			.toPaginatedFlow<HealthcareParty>(offset.limit)
		emitAll(healthcareParties)
	}

	override fun listHealthcarePartiesByNihii(nihii: String): Flow<HealthcareParty> =
		flow {
			val datastoreInformation = getInstanceAndGroup()
			emitAll(healthcarePartyDAO.listHealthcarePartiesByNihii(datastoreInformation, nihii))
		}

	override fun listHealthcarePartiesBySsin(ssin: String): Flow<HealthcareParty> =
		flow {
			val datastoreInformation = getInstanceAndGroup()
			emitAll(healthcarePartyDAO.listHealthcarePartiesBySsin(datastoreInformation, ssin))
		}

	override fun listHealthcarePartiesByName(name: String): Flow<HealthcareParty> =
		flow {
			val datastoreInformation = getInstanceAndGroup()
			emitAll(healthcarePartyDAO.listHealthcarePartiesByName(datastoreInformation, name))
		}

	override suspend fun getPublicKey(healthcarePartyId: String): String? {
		val datastoreInformation = getInstanceAndGroup()
		val hcParty = healthcarePartyDAO.get(datastoreInformation, healthcarePartyId)
			?: throw DocumentNotFoundException("Healthcare party ($healthcarePartyId) not found in the database.")
		return hcParty.publicKey
	}

	override fun listHealthcarePartiesBySpecialityAndPostcode(
		type: String,
		spec: String,
		firstCode: String,
		lastCode: String,
		offset: PaginationOffset<ComplexKey>
	): Flow<PaginatedElement> = flow {
		val datastoreInformation = getInstanceAndGroup()
		emitAll(healthcarePartyDAO
			.listHealthcarePartiesBySpecialityAndPostcode(datastoreInformation, type, spec, firstCode, lastCode, offset.limitIncludingKey())
			.toPaginatedFlow<HealthcareParty>(offset.limit)
		)
	}

	override fun getHealthcareParties(ids: List<String>): Flow<HealthcareParty> =
		flow {
			val datastoreInformation = getInstanceAndGroup()
			emitAll(healthcarePartyDAO.getEntities(datastoreInformation, ids))
		}

	override fun findHealthcarePartiesBySsinOrNihii(
		searchValue: String,
		paginationOffset: PaginationOffset<String>,
		desc: Boolean
	): Flow<PaginatedElement> = flow {
		val datastoreInformation = getInstanceAndGroup()
		emitAll(healthcarePartyDAO
			.findHealthcarePartiesBySsinOrNihii(datastoreInformation, searchValue, paginationOffset.limitIncludingKey(), desc)
			.toPaginatedFlow<HealthcareParty>(paginationOffset.limit)
		)
	}

	override fun getHealthcarePartiesByParentId(parentId: String): Flow<HealthcareParty> =
		flow {
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

	override fun filterHealthcareParties(paginationOffset: PaginationOffset<Nothing>, filter: FilterChain<HealthcareParty>) = flow {
		val datastoreInformation = getInstanceAndGroup()
		val ids = filters.resolve(filter.filter)
		val sortedIds = paginationOffset.takeUnless { it.startDocumentId == null }?.let { paginationOffset -> // Sub-set starting from startDocId to the end (including last element)
			ids.dropWhile { id -> id != paginationOffset.startDocumentId }
		} ?: ids

		val selectedIds = sortedIds.take(paginationOffset.limit + 1) // Fetching one more healthcare parties for the start key of the next page
		emitAll(healthcarePartyDAO.findHealthcarePartiesByIds(datastoreInformation, selectedIds))
	}

	override fun listHealthcarePartyIdsByIdentifiers(hcpIdentifiers: List<Identifier>): Flow<String> = flow {
		val datastoreInformation = getInstanceAndGroup()

		emitAll(
			healthcarePartyDAO.listHealthcarePartyIdsByIdentifiers(
				datastoreInformation = datastoreInformation,
				hcpIdentifiers = hcpIdentifiers
			)
		)
	}

	override fun listHealthcarePartyIdsByCode(codeType: String, codeCode: String?): Flow<String> =
		flow {
			val datastoreInformation = getInstanceAndGroup()

			emitAll(
				healthcarePartyDAO.listHealthcarePartyIdsByCode(
					datastoreInformation = datastoreInformation,
					codeType = codeType,
					codeCode = codeCode
				)
			)
		}

	override fun listHealthcarePartyIdsByTag(tagType: String, tagCode: String?): Flow<String> = flow {
		val datastoreInformation = getInstanceAndGroup()

		emitAll(
			healthcarePartyDAO.listHealthcarePartyIdsByTag(
				datastoreInformation = datastoreInformation,
				tagType = tagType,
				tagCode = tagCode
			)
		)
	}

	override fun listHealthcarePartyIdsByName(name: String, desc: Boolean) = flow {
		val datastoreInformation = getInstanceAndGroup()
		emitAll(healthcarePartyDAO.listHealthcarePartyIdsByName(datastoreInformation, name, desc))
	}
}
