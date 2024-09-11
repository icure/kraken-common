/*
 * Copyright (c) 2020. Taktik SA, All rights reserved.
 */
package org.taktik.icure.exceptions

import org.taktik.couchdb.exception.UpdateConflictException
import org.taktik.icure.entities.base.StoredDocument

class BulkUpdateConflictException(
    val conflicts: List<UpdateConflictException>,
    val savedDocuments: List<StoredDocument?>
) : PersistenceException()
