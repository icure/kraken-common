package org.taktik.icure.asyncdao

import kotlinx.coroutines.flow.Flow
import org.taktik.couchdb.ViewQueryResultEvent
import org.taktik.icure.datastore.IDatastoreInformation
import org.taktik.icure.db.PaginationOffset
import org.taktik.icure.entities.ExchangeData

interface ExchangeDataDAO : GenericDAO<ExchangeData> {
    /**
     * Get all exchange data where the provided data owner is the delegator and/or delegate for the exchange data.
     */
    fun findExchangeDataByParticipant(
        datastoreInformation: IDatastoreInformation,
        dataOwnerId: String,
        paginationOffset: PaginationOffset<String>
    ): Flow<ViewQueryResultEvent>

    /**
     * Get all exchange data where the provided data owners are delegator and delegate of the exchange data, with no
     * requirement on which of the two should be delegate and which should be the delegator.
     */
    fun findExchangeDataByDelegatorDelegatePair(
        datastoreInformation: IDatastoreInformation,
        delegatorId: String,
        delegateId: String
    ): Flow<ExchangeData>
}
