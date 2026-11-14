/*
 * Copyright (c) 2020. Taktik SA, All rights reserved.
 */

package org.taktik.icure.asynclogic

import org.taktik.couchdb.DocIdentifier
import org.taktik.icure.asynclogic.base.EntityWithSecureDelegationsLogic
import org.taktik.icure.entities.MaintenanceTask

interface MaintenanceTaskLogic :
	EntityPersister<MaintenanceTask>,
	EntityWithSecureDelegationsLogic<MaintenanceTask> {
	suspend fun createMaintenanceTask(maintenanceTask: MaintenanceTask): MaintenanceTask
}
