/*
 * Copyright (c) 2020. Taktik SA, All rights reserved.
 */

package org.taktik.icure.asyncdao

import kotlinx.coroutines.flow.Flow
import org.taktik.icure.asynclogic.datastore.IDatastoreInformation
import org.taktik.icure.entities.Agenda

interface AgendaDAO : GenericDAO<Agenda> {
	fun getAgendasByUser(datastoreInformation: IDatastoreInformation, userId: String): Flow<Agenda>
	fun getReadableAgendaByUser(datastoreInformation: IDatastoreInformation, userId: String): Flow<Agenda>
}
