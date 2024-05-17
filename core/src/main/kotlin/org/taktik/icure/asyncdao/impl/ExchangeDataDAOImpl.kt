package org.taktik.icure.asyncdao.impl

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Repository
import org.taktik.couchdb.ViewQueryResultEvent
import org.taktik.couchdb.annotation.View
import org.taktik.couchdb.dao.DesignDocumentProvider
import org.taktik.couchdb.entity.ComplexKey
import org.taktik.couchdb.id.IDGenerator
import org.taktik.couchdb.queryViewIncludeDocsNoValue
import org.taktik.icure.asyncdao.CouchDbDispatcher
import org.taktik.icure.asyncdao.ExchangeDataDAO
import org.taktik.icure.asynclogic.datastore.IDatastoreInformation
import org.taktik.icure.cache.ConfiguredCacheProvider
import org.taktik.icure.cache.EntityCacheFactory
import org.taktik.icure.cache.getConfiguredCache
import org.taktik.icure.config.DaoConfig
import org.taktik.icure.db.PaginationOffset
import org.taktik.icure.entities.ExchangeData

@Repository("ExchangeDataDAO")
@Profile("app")
@View(name = "all", map = "function(doc) { if (doc.java_type == 'org.taktik.icure.entities.ExchangeData' && !doc.deleted) emit( null, doc._id )}")
class ExchangeDataDAOImpl(
	@Qualifier("baseCouchDbDispatcher") couchDbDispatcher: CouchDbDispatcher,
	idGenerator: IDGenerator,
	entityCacheFactory: ConfiguredCacheProvider,
	designDocumentProvider: DesignDocumentProvider,
    daoConfig: DaoConfig
) : GenericDAOImpl<ExchangeData>(ExchangeData::class.java, couchDbDispatcher, idGenerator, entityCacheFactory.getConfiguredCache(), designDocumentProvider, daoConfig = daoConfig), ExchangeDataDAO {

    @View(name = "by_participant", map = "classpath:js/exchangedata/By_participant_map.js")
    override fun findExchangeDataByParticipant(
        datastoreInformation: IDatastoreInformation,
        dataOwnerId: String,
        paginationOffset: PaginationOffset<String>
    ): Flow<ViewQueryResultEvent> = flow {
        val client = couchDbDispatcher.getClient(datastoreInformation)
        require(paginationOffset.startKey == null || paginationOffset.startKey == dataOwnerId) {
            "Pagination key should be the same as the data owner id if present."
        }
        val viewQuery =  createQuery(datastoreInformation, "by_participant")
            .key(dataOwnerId)
            .includeDocs(true)
            .reduce(false)
            .startDocId(paginationOffset.startDocumentId)
            .limit(paginationOffset.limit)
            .descending(false)
        emitAll(client.queryView(viewQuery, String::class.java, Nothing::class.java, ExchangeData::class.java))
    }

    @View(name = "by_delegator_delegate", map = "classpath:js/exchangedata/By_delegator_delegate_map.js")
    override fun findExchangeDataByDelegatorDelegatePair(
        datastoreInformation: IDatastoreInformation,
        delegatorId: String,
        delegateId: String
    ): Flow<ExchangeData> = flow {
        val client = couchDbDispatcher.getClient(datastoreInformation)
        val viewQuery =  createQuery(datastoreInformation, "by_delegator_delegate")
            .key(ComplexKey.of(delegatorId, delegateId))
            .includeDocs(true)
            .reduce(false)
            .descending(false)
        client.queryViewIncludeDocsNoValue<ComplexKey, ExchangeData>(viewQuery).collect {
            emit(it.doc)
        }
    }
}
