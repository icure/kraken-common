package org.taktik.icure.asyncdao.impl

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Repository
import org.taktik.couchdb.annotation.View
import org.taktik.couchdb.dao.DesignDocumentProvider
import org.taktik.couchdb.entity.ComplexKey
import org.taktik.couchdb.id.IDGenerator
import org.taktik.couchdb.queryView
import org.taktik.icure.asyncdao.CouchDbDispatcher
import org.taktik.icure.asyncdao.RecoveryDataDAO
import org.taktik.icure.asynclogic.datastore.IDatastoreInformation
import org.taktik.icure.cache.EntityCacheFactory
import org.taktik.icure.cache.localOnlyCache
import org.taktik.icure.entities.RecoveryData
import org.taktik.couchdb.entity.IdAndRev

@Repository("RecoveryDataDAO")
@Profile("app")
@View(name = "all", map = "function(doc) { if (doc.java_type == 'org.taktik.icure.entities.RecoveryData' && !doc.deleted) emit( null, doc._id )}")
class RecoveryDataDAOImpl(
	@Qualifier("baseCouchDbDispatcher") couchDbDispatcher: CouchDbDispatcher,
	idGenerator: IDGenerator,
	entityCacheFactory: EntityCacheFactory,
	designDocumentProvider: DesignDocumentProvider
) : GenericDAOWithMinimalPurgeImpl<RecoveryData>(
    RecoveryData::class.java,
    couchDbDispatcher,
    idGenerator,
    entityCacheFactory.localOnlyCache(),
    designDocumentProvider
), RecoveryDataDAO{
    @View(name = "by_recipient_and_type", map = "function(doc) { if (doc.java_type == 'org.taktik.icure.entities.RecoveryData' && !doc.deleted && doc.recipient && doc.type) emit([doc.recipient, doc.type], doc._rev)}")
    override fun findRecoveryDataIdsByRecipientAndType(
        datastoreInformation: IDatastoreInformation,
        recipient: String,
        type: RecoveryData.Type?
    ): Flow<IdAndRev> = flow {
        val client = couchDbDispatcher.getClient(datastoreInformation)
        val query = createQuery(datastoreInformation, "by_recipient_and_type")
            .run {
                if (type == null) {
                    startKey(ComplexKey.of(recipient, null)).endKey(ComplexKey.of(recipient, ComplexKey.emptyObject()))
                } else {
                    key(ComplexKey.of(recipient, type))
                }
            }
            .includeDocs(false)
            .reduce(false)
            .descending(false)
        emitAll(client.queryView<ComplexKey, String>(query).map { IdAndRev(it.id, it.value) })
    }

    @View(name = "by_expiration", map = "function(doc) { if (doc.java_type == 'org.taktik.icure.entities.RecoveryData' && !doc.deleted && doc.expirationInstant !== undefined && doc.expirationInstant !== null) emit(doc.expirationInstant, doc._rev)}")
    override fun findRecoveryDataIdsWithExpirationLessThan(
        datastoreInformation: IDatastoreInformation,
        expiration: Long
    ): Flow<IdAndRev> = flow {
        val client = couchDbDispatcher.getClient(datastoreInformation)
        val query = createQuery(datastoreInformation, "by_expiration")
            .startKey(null)
            .endKey(expiration)
            .includeDocs(false)
            .reduce(false)
            .descending(false)
        emitAll(client.queryView<String, String>(query).map { IdAndRev(it.id, it.value) })
    }
}
