/*
 * Copyright (c) 2020. Taktik SA, All rights reserved.
 */
package org.taktik.icure.db

class Sorting<T>(val field: T, val direction: SortDirection)

@Suppress("EnumEntryName")
enum class SortDirection { asc, desc }
