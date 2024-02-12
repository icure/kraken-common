package org.taktik.couchdb.dao

import org.taktik.couchdb.Client
import org.taktik.couchdb.entity.DesignDocument

interface DesignDocumentProvider {
    /**
     * @param entityClass the class of entities that are indexed by the views of the design document.
     * @param secondaryPartition the name of the secondary partition of the design document (if applicable).
     * @return the name of the design documents for entities of the provided class.
     */
    fun baseDesignDocumentId(entityClass: Class<*>, secondaryPartition: String? = null): String

    /**
     * In an environment with versioned design documents this method returns the current version of the design document
     * if it is available (indexing is completed and the views can be queried), or another available version if the
     * current one is not available yet.
     *
     * In an environment without versioned design documents this method is equivalent to [currentDesignDocumentId].
     *
     * @param entityClass the class of entities that are indexed by the views of the design document.
     * @param metaDataSource the class defining the views for entities of class [entityClass] (the DAO for entities of class
     * [entityClass]).
     * @param secondaryPartition the name of the secondary partition of the design document (if applicable).
     */
    suspend fun currentOrAvailableDesignDocumentId(client: Client, entityClass: Class<*>, metaDataSource: Any, secondaryPartition: String? = null): String

    /**
     * The current design document id. In an environment with versioned design document this consist of the
     * [baseDesignDocumentId] plus a hash calculated on the views for that document.
     *
     * In an environment without versioned design documents this is the same as [baseDesignDocumentId].
     * @param entityClass the class of entities that are indexed by the views of the design document.
     * @param metaDataSource the class defining the views for entities of class [entityClass] (the DAO for entities of class
     * [entityClass]).
     * @param secondaryPartition the name of the secondary partition of the design document (if applicable).
     */
    suspend fun currentDesignDocumentId(entityClass: Class<*>, metaDataSource: Any, secondaryPartition: String? = null): String

    /**
     * Generates the design documents from a given metadata source.
     * @param entityClass the class of entities that are indexed by the views of the design document.
     * @param metaDataSource the class defining the views for entities of class [entityClass] (the DAO for entities of class
     * [entityClass]).
     * @param client a [Client] for when is necessary to compare the newly created design documents to the existing one.
     */
    suspend fun generateDesignDocuments(entityClass: Class<*>, metaDataSource: Any, client: Client? = null): Set<DesignDocument>
}
