package org.taktik.icure.asyncdao

import kotlinx.coroutines.flow.Flow
import org.taktik.icure.datastore.IDatastoreInformation
import org.taktik.icure.entities.SecureDelegationKeyMap

interface SecureDelegationKeyMapDAO : GenericDAO<SecureDelegationKeyMap> {
    /**
     * All the secure delegation key maps for a given delegation key.
     */
    suspend fun findByDelegationKeys(datastoreInformation: IDatastoreInformation, delegationKeys: List<String>): Flow<SecureDelegationKeyMap>
}
