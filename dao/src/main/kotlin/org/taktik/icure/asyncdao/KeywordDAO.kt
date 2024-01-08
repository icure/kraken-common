/*
 * Copyright (c) 2020. Taktik SA, All rights reserved.
 */

package org.taktik.icure.asyncdao

import kotlinx.coroutines.flow.Flow
import org.taktik.icure.asynclogic.datastore.IDatastoreInformation
import org.taktik.icure.entities.Keyword

interface KeywordDAO : GenericDAO<Keyword> {
	suspend fun getKeyword(datastoreInformation: IDatastoreInformation, keywordId: String): Keyword?

	fun getKeywordsByUserId(datastoreInformation: IDatastoreInformation, userId: String): Flow<Keyword>
}
