package org.taktik.icure.asyncdao.impl

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.toList
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Repository
import org.taktik.couchdb.DocIdentifier
import org.taktik.couchdb.annotation.View
import org.taktik.couchdb.dao.DesignDocumentProvider
import org.taktik.couchdb.entity.ComplexKey
import org.taktik.couchdb.id.IDGenerator
import org.taktik.couchdb.queryViewIncludeDocs
import org.taktik.icure.asyncdao.CouchDbDispatcher
import org.taktik.icure.asyncdao.RecoveryDataDAO
import org.taktik.icure.asyncdao.results.BulkSaveResult
import org.taktik.icure.asyncdao.results.entityOrNull
import org.taktik.icure.asyncdao.results.filterSuccessfulUpdates
import org.taktik.icure.asynclogic.datastore.IDatastoreInformation
import org.taktik.icure.cache.ConfiguredCacheProvider
import org.taktik.icure.cache.getConfiguredCache
import org.taktik.icure.config.DaoConfig
import org.taktik.icure.entities.RecoveryData
import org.taktik.icure.utils.error

@Repository("RecoveryDataDAO")
@Profile("app")
@View(name = "all", map = "function(doc) { if (doc.java_type == 'org.taktik.icure.entities.RecoveryData' && !doc.deleted) emit( null, doc._id )}")
class RecoveryDataDAOImpl(
	@Qualifier("baseCouchDbDispatcher") couchDbDispatcher: CouchDbDispatcher,
	idGenerator: IDGenerator,
	entityCacheFactory: ConfiguredCacheProvider,
	designDocumentProvider: DesignDocumentProvider,
    daoConfig: DaoConfig
) : GenericDAOImpl<RecoveryData>(
    RecoveryData::class.java,
    couchDbDispatcher,
    idGenerator,
    entityCacheFactory.getConfiguredCache(),
    designDocumentProvider,
    daoConfig = daoConfig
), RecoveryDataDAO {

    companion object {
        private val log = LoggerFactory.getLogger(this::class.java)
    }

    @View(name = "by_recipient_and_type", map = "function(doc) { if (doc.java_type == 'org.taktik.icure.entities.RecoveryData' && !doc.deleted && doc.recipient && doc.type) emit([doc.recipient, doc.type], doc._rev)}")
    override fun findRecoveryDataIdsByRecipientAndType(
        datastoreInformation: IDatastoreInformation,
        recipient: String,
        type: RecoveryData.Type?
    ): Flow<RecoveryData> = flow {
        val client = couchDbDispatcher.getClient(datastoreInformation)
        val query = createQuery(datastoreInformation, "by_recipient_and_type")
            .run {
                if (type == null) {
                    startKey(ComplexKey.of(recipient, null)).endKey(ComplexKey.of(recipient, ComplexKey.emptyObject()))
                } else {
                    key(ComplexKey.of(recipient, type))
                }
            }
            .includeDocs(true)
            .reduce(false)
            .descending(false)
        emitAll(client.queryViewIncludeDocs<ComplexKey, String, RecoveryData>(query).map { it.doc })
    }

    @View(name = "by_expiration", map = "function(doc) { if (doc.java_type == 'org.taktik.icure.entities.RecoveryData' && !doc.deleted && doc.expirationInstant !== undefined && doc.expirationInstant !== null) emit(doc.expirationInstant, doc._rev)}")
    override fun findRecoveryDataIdsWithExpirationLessThan(
        datastoreInformation: IDatastoreInformation,
        expiration: Long
    ): Flow<RecoveryData> = flow {
        val client = couchDbDispatcher.getClient(datastoreInformation)
        val query = createQuery(datastoreInformation, "by_expiration")
            .startKey(null)
            .endKey(expiration)
            .includeDocs(true)
            .reduce(false)
            .descending(false)
        emitAll(client.queryViewIncludeDocs<String, String, RecoveryData>(query).map { it.doc })
    }

    override fun purge(
        datastoreInformation: IDatastoreInformation,
        entities: Collection<RecoveryData>
    ): Flow<BulkSaveResult<DocIdentifier>> = flow {
        val client = couchDbDispatcher.getClient(datastoreInformation)
        if (log.isDebugEnabled) {
            log.debug(entityClass.simpleName + ".purge: " + entities)
        }
        emitAll(
            client.bulkDelete(entities).toSaveResult { id, rev ->
                cacheChain?.evictFromCache(datastoreInformation.getFullIdFor(id))
                DocIdentifier(id, rev)
            }
        )
    }
}
