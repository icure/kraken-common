package org.taktik.icure.asyncdao.components

import org.taktik.couchdb.entity.ActiveTask
import org.taktik.icure.asynclogic.datastore.IDatastoreInformation

interface ActiveTasksProvider {
	suspend fun getActiveTasks(datastoreInformation: IDatastoreInformation): List<ActiveTask>
}