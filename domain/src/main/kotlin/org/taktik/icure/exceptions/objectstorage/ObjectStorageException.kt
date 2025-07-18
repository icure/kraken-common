package org.taktik.icure.exceptions.objectstorage

import org.taktik.icure.exceptions.ICureException

/**
 * Base for all object-storage related exceptions
 */
abstract class ObjectStorageException(message: String, cause: Throwable?) : ICureException(message, cause)
