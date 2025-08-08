/*
 * Copyright (c) 2020. Taktik SA, All rights reserved.
 */
package org.taktik.icure.exceptions

import java.lang.IllegalArgumentException

class DuplicateDocumentException(
	msg: String,
) : IllegalArgumentException(msg)
