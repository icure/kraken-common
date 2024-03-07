package org.taktik.icure.asyncdao.impl

import kotlinx.coroutines.flow.flow
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Repository
import org.taktik.couchdb.annotation.View
import org.taktik.couchdb.dao.DesignDocumentProvider
import org.taktik.couchdb.id.IDGenerator
import org.taktik.couchdb.queryViewIncludeDocsNoValue
import org.taktik.icure.asyncdao.CouchDbDispatcher
import org.taktik.icure.asyncdao.SecureDelegationKeyMapDAO
import org.taktik.icure.asynclogic.datastore.IDatastoreInformation
import org.taktik.icure.cache.EntityCacheFactory
import org.taktik.icure.cache.localOnlyCache
import org.taktik.icure.entities.SecureDelegationKeyMap

@Repository("secureDelegationKeyMapDAO")
@Profile("app")
@View(name = "all", map = "function(doc) { if (doc.java_type == 'org.taktik.icure.entities.SecureDelegationKeyMap' && !doc.deleted) emit(null, doc._id)}")
class SecureDelegationKeyMapDAOImpl(
	@Qualifier("baseCouchDbDispatcher") couchDbDispatcher: CouchDbDispatcher,
	idGenerator: IDGenerator,
	entityCacheFactory: EntityCacheFactory,
	designDocumentProvider: DesignDocumentProvider
) : GenericDAOImpl<SecureDelegationKeyMap>(
    SecureDelegationKeyMap::class.java,
    couchDbDispatcher,
    idGenerator,
    entityCacheFactory.localOnlyCache(),
    designDocumentProvider
), SecureDelegationKeyMapDAO {
    @View(name = "by_delegation_key", map = "function(doc) { if (doc.java_type == 'org.taktik.icure.entities.SecureDelegationKeyMap' && !doc.deleted) emit(doc.delegationKey, null)}")
    override suspend fun findByDelegationKeys(datastoreInformation: IDatastoreInformation, delegationKeys: List<String>) = flow {
        val client = couchDbDispatcher.getClient(datastoreInformation)
        val viewQuery = createQuery(datastoreInformation, "by_delegation_key")
            .keys(delegationKeys)
            .includeDocs(true)
            .reduce(false)
            .descending(false)
        client.queryViewIncludeDocsNoValue<String, SecureDelegationKeyMap>(viewQuery).collect {
            emit(it.doc)
        }
    }
}
