/*
 * Copyright (c) 2020. Taktik SA, All rights reserved.
 */

package org.taktik.icure.asyncdao.impl

import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Repository
import org.taktik.couchdb.annotation.View
import org.taktik.couchdb.dao.DesignDocumentProvider
import org.taktik.couchdb.id.IDGenerator
import org.taktik.icure.asyncdao.ApplicationSettingsDAO
import org.taktik.icure.asyncdao.CouchDbDispatcher
import org.taktik.icure.config.DaoConfig
import org.taktik.icure.entities.ApplicationSettings

@Repository("ApplicationSettingsDAO")
@Profile("app")
@View(
	name = "all",
	map = "function(doc) { if (doc.java_type == 'org.taktik.icure.entities.ApplicationSettings' && !doc.deleted) emit( null, doc._id )}",
)
class ApplicationSettingsDAOImpl(
	@Qualifier("baseCouchDbDispatcher") couchDbDispatcher: CouchDbDispatcher,
	idGenerator: IDGenerator,
	designDocumentProvider: DesignDocumentProvider,
	daoConfig: DaoConfig,
) : GenericDAOImpl<ApplicationSettings>(
	ApplicationSettings::class.java,
	couchDbDispatcher,
	idGenerator,
	designDocumentProvider = designDocumentProvider,
	daoConfig = daoConfig,
),
	ApplicationSettingsDAO
