/*
 * Copyright (c) 2020. Taktik SA, All rights reserved.
 */

package org.taktik.icure.asyncdao

import kotlinx.coroutines.flow.Flow
import org.taktik.icure.datastore.IDatastoreInformation
import org.taktik.icure.entities.Receipt

interface ReceiptDAO :
	GenericDAO<Receipt>,
	AttachmentManagementDAO<Receipt> {
	fun listByReference(datastoreInformation: IDatastoreInformation, ref: String): Flow<Receipt>

	fun listReceiptsAfterDate(datastoreInformation: IDatastoreInformation, date: Long): Flow<Receipt>

	fun listReceiptsByCategory(datastoreInformation: IDatastoreInformation, category: String, subCategory: String, startDate: Long, endDate: Long): Flow<Receipt>

	fun listReceiptsByDocId(datastoreInformation: IDatastoreInformation, date: Long): Flow<Receipt>
}
