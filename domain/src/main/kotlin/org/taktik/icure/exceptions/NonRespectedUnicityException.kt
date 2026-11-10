/*
 * Copyright (c) 2020. Taktik SA, All rights reserved.
 */
package org.taktik.icure.exceptions

/**
 * This exception is intended to be used only when a unique requirement on an entity field is not respected, except for
 * id and rev, that should be handled with CouchDB exceptions.
 */
open class NonRespectedUnicityException(
	msg: String,
) : IllegalArgumentException(msg)
