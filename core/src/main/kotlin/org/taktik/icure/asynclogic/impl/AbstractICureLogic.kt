/*
 * Copyright (c) 2020. Taktik SA, All rights reserved.
 */
package org.taktik.icure.asynclogic.impl

import org.slf4j.LoggerFactory
import org.springframework.security.crypto.password.PasswordEncoder
import org.taktik.icure.asyncdao.ICureDAO
import org.taktik.icure.asynclogic.ICureLogic
import org.taktik.icure.asynclogic.VersionLogic
import org.taktik.icure.datastore.DatastoreInstanceProvider
import org.taktik.icure.datastore.IDatastoreInformation
import org.taktik.icure.domain.IndexingInfo
import org.taktik.icure.entities.ReplicationInfo
import org.taktik.icure.entities.embed.DatabaseSynchronization
import org.taktik.icure.properties.CouchDbProperties
import java.lang.management.ManagementFactory
import java.net.URI

abstract class AbstractICureLogic(
    couchDbProperties: CouchDbProperties,
    private val iCureDAO: ICureDAO,
    private val passwordEncoder: PasswordEncoder,
    private val versionLogic: VersionLogic,
    private val datastoreInstanceProvider: DatastoreInstanceProvider
) : ICureLogic {

    protected val dbInstanceUri = URI(couchDbProperties.url)

    private val log = LoggerFactory.getLogger(this::class.java)
    suspend fun getInstanceAndGroup() = datastoreInstanceProvider.getInstanceAndGroup()

    protected suspend fun makeReplicationInfo(
        datastoreInformation: IDatastoreInformation,
        userDbInstanceUris: List<URI>,
        filterPendingChanges: (Map<DatabaseSynchronization, Long>) -> Map<DatabaseSynchronization, Long>
    ): ReplicationInfo {
        val changes: Map<DatabaseSynchronization, Long> =
            filterPendingChanges(iCureDAO.getPendingChanges(datastoreInformation))
        val userHosts = userDbInstanceUris.map { it.host }.takeIf { it.isNotEmpty() } ?: listOf(dbInstanceUri.host)
        return changes.toList().fold(ReplicationInfo()) { r, (db, pending) ->
            r.copy(
                active = true,
                pendingFrom = if (db.source?.let { userHosts.any { h -> it.contains(h) } } == true) ((r.pendingFrom
                    ?: 0) + pending).toInt() else r.pendingFrom,
                pendingTo = if (db.target?.let { userHosts.any { h -> it.contains(h) } } == true) ((r.pendingTo
                    ?: 0) + pending).toInt() else r.pendingTo
            )
        }
    }

    override suspend fun getIndexingStatus(): IndexingInfo {
        val datastoreInformation = datastoreInstanceProvider.getInstanceAndGroup()
        return IndexingInfo(iCureDAO.getIndexingStatus(datastoreInformation))
    }

    override fun getVersion(): String {
        return versionLogic.getVersion()
    }

    override fun getSemanticVersion(): String {
        return versionLogic.getSemanticVersion()
    }

    override fun getProcessInfo(): String = ManagementFactory.getRuntimeMXBean().name
}
