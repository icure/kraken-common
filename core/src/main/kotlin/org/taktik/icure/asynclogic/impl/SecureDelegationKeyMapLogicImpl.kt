package org.taktik.icure.asynclogic.impl

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Service
import org.taktik.icure.asyncdao.SecureDelegationKeyMapDAO
import org.taktik.icure.asynclogic.SessionInformationProvider
import org.taktik.icure.asynclogic.ExchangeDataMapLogic
import org.taktik.icure.asynclogic.SecureDelegationKeyMapLogic
import org.taktik.icure.asynclogic.base.impl.EntityWithEncryptionMetadataLogic
import org.taktik.icure.asynclogic.datastore.DatastoreInstanceProvider
import org.taktik.icure.asynclogic.impl.filter.Filters
import org.taktik.icure.entities.SecureDelegationKeyMap
import org.taktik.icure.entities.embed.SecurityMetadata
import org.taktik.icure.validation.aspect.Fixer

open class SecureDelegationKeyMapLogicImpl(
    sessionLogic: SessionInformationProvider,
    exchangeDataMapLogic: ExchangeDataMapLogic,
    private val datastoreInstanceProvider: DatastoreInstanceProvider,
    private val secureDelegationKeyMapDAO: SecureDelegationKeyMapDAO,
    fixer: Fixer,
    filters: Filters
) : EntityWithEncryptionMetadataLogic<SecureDelegationKeyMap, SecureDelegationKeyMapDAO>(fixer, sessionLogic, datastoreInstanceProvider, exchangeDataMapLogic, filters),
    SecureDelegationKeyMapLogic {
    override fun entityWithUpdatedSecurityMetadata(
        entity: SecureDelegationKeyMap,
        updatedMetadata: SecurityMetadata
    ): SecureDelegationKeyMap =
        entity.copy(securityMetadata = updatedMetadata).also { it.validate() }


    override suspend fun createSecureDelegationKeyMap(map: SecureDelegationKeyMap): SecureDelegationKeyMap =
        checkNotNull(secureDelegationKeyMapDAO.create(datastoreInstanceProvider.getInstanceAndGroup(), map.also { it.validate() })) {
            "DAO returned null on entity creation"
        }

    override fun findByDelegationKeys(delegationKeys: List<String>): Flow<SecureDelegationKeyMap> = flow {
        emitAll(secureDelegationKeyMapDAO.findByDelegationKeys(datastoreInstanceProvider.getInstanceAndGroup(), delegationKeys))
    }

    private fun SecureDelegationKeyMap.validate() =
        require(delegator == null && delegate == null && encryptedSelf != null) {
            "Delegator and delegate fields should have been encrypted."
        }

    override fun getGenericDAO(): SecureDelegationKeyMapDAO = secureDelegationKeyMapDAO
}