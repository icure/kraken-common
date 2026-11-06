/*
 * Copyright (c) 2020. Taktik SA, All rights reserved.
 */
package org.taktik.icure.exceptions

class TooManyRequestsException(
	msg: String,
) : IllegalArgumentException(msg)
