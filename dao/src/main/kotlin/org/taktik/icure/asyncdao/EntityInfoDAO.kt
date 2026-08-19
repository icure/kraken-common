package org.taktik.icure.asyncdao

import kotlinx.coroutines.flow.Flow
import org.taktik.icure.datastore.IDatastoreInformation
import org.taktik.icure.entities.base.EntityInfo

interface EntityInfoDAO {
	@Deprecated($$"""
	Very inefficient retrieves the full entity from the DB; two alternatives for the future:
		- If the query spans entities of many different types we need to create a new "system" view that will emit java type and revs by id (currently only have revs by java type)
		- If the query uses only a few types do multiple queries on the all view of the corresponding DAO; currently however the all views are heterogeneous and many arent be indexed by ids
		- Mango queries do not work, search with $in and _id does full scan regardless of indexes... 
	""")
	fun getEntitiesInfo(datastoreInformation: IDatastoreInformation, ids: Collection<String>): Flow<EntityInfo>
}
