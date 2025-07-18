/*
 * Copyright (c) 2020. Taktik SA, All rights reserved.
 */
package org.taktik.commons.serialization

import java.io.Serializable
import java.util.Date
import java.util.UUID

class SerializableValue : Serializable {
	var enumObject: Enum<*>? = null
	var stringObject: String? = null
	var dateObject: Date? = null
	var characterObject: Char? = null
	var byteObject: Byte? = null
	var shortObject: Short? = null
	var integerObject: Int? = null
	var longObject: Long? = null
	var booleanObject: Boolean? = null
	var floatObject: Float? = null
	var doubleObject: Double? = null
	var uuidObject: UUID? = null

	constructor(objectValue: Serializable?) {
		value = objectValue
	}

	var value: Serializable?
		get() {
			if (enumObject != null) {
				return enumObject
			} else if (stringObject != null) {
				return stringObject
			} else if (dateObject != null) {
				return dateObject
			} else if (characterObject != null) {
				return characterObject
			} else if (byteObject != null) {
				return byteObject
			} else if (shortObject != null) {
				return shortObject
			} else if (integerObject != null) {
				return integerObject
			} else if (longObject != null) {
				return longObject
			} else if (booleanObject != null) {
				return booleanObject
			} else if (floatObject != null) {
				return floatObject
			} else if (doubleObject != null) {
				return doubleObject
			} else if (uuidObject != null) {
				return uuidObject
			}
			return null
		}
		set(objectValue) {
			enumObject = null
			stringObject = null
			dateObject = null
			characterObject = null
			byteObject = null
			shortObject = null
			integerObject = null
			longObject = null
			booleanObject = null
			floatObject = null
			doubleObject = null
			uuidObject = null
			if (objectValue is Enum<*>) {
				enumObject = objectValue as Enum<*>?
			} else if (objectValue is String) {
				stringObject = objectValue as String?
			} else if (objectValue is Date) {
				dateObject = Date((objectValue as Date).time)
			} else if (objectValue is Char) {
				characterObject = objectValue as Char?
			} else if (objectValue is Byte) {
				byteObject = objectValue as Byte?
			} else if (objectValue is Short) {
				shortObject = objectValue as Short?
			} else if (objectValue is Int) {
				integerObject = objectValue as Int?
			} else if (objectValue is Long) {
				longObject = objectValue as Long?
			} else if (objectValue is Boolean) {
				booleanObject = objectValue as Boolean?
			} else if (objectValue is Float) {
				floatObject = objectValue as Float?
			} else if (objectValue is Double) {
				doubleObject = objectValue as Double?
			} else if (objectValue is UUID) {
				uuidObject = objectValue as UUID?
			}
		}

	override fun hashCode(): Int {
		val prime = 31
		var result = 1
		result = prime * result + if (booleanObject == null) 0 else booleanObject.hashCode()
		result = prime * result + if (byteObject == null) 0 else byteObject.hashCode()
		result = prime * result + if (characterObject == null) 0 else characterObject.hashCode()
		result = prime * result + if (dateObject == null) 0 else dateObject.hashCode()
		result = prime * result + if (doubleObject == null) 0 else doubleObject.hashCode()
		result = prime * result + if (enumObject == null) 0 else enumObject.hashCode()
		result = prime * result + if (floatObject == null) 0 else floatObject.hashCode()
		result = prime * result + if (integerObject == null) 0 else integerObject.hashCode()
		result = prime * result + if (longObject == null) 0 else longObject.hashCode()
		result = prime * result + if (shortObject == null) 0 else shortObject.hashCode()
		result = prime * result + if (stringObject == null) 0 else stringObject.hashCode()
		result = prime * result + if (uuidObject == null) 0 else uuidObject.hashCode()
		return result
	}

	override fun equals(obj: Any?): Boolean {
		if (this === obj) return true
		if (obj == null) return false
		if (javaClass != obj.javaClass) return false
		val other = obj as SerializableValue
		if (booleanObject == null) {
			if (other.booleanObject != null) return false
		} else if (booleanObject != other.booleanObject) {
			return false
		}
		if (byteObject == null) {
			if (other.byteObject != null) return false
		} else if (byteObject != other.byteObject) {
			return false
		}
		if (characterObject == null) {
			if (other.characterObject != null) return false
		} else if (characterObject != other.characterObject) {
			return false
		}
		if (dateObject == null) {
			if (other.dateObject != null) return false
		} else if (dateObject != other.dateObject) {
			return false
		}
		if (doubleObject == null) {
			if (other.doubleObject != null) return false
		} else if (doubleObject != other.doubleObject) {
			return false
		}
		if (enumObject == null) {
			if (other.enumObject != null) return false
		} else if (enumObject != other.enumObject) {
			return false
		}
		if (floatObject == null) {
			if (other.floatObject != null) return false
		} else if (floatObject != other.floatObject) {
			return false
		}
		if (integerObject == null) {
			if (other.integerObject != null) return false
		} else if (integerObject != other.integerObject) {
			return false
		}
		if (longObject == null) {
			if (other.longObject != null) return false
		} else if (longObject != other.longObject) {
			return false
		}
		if (shortObject == null) {
			if (other.shortObject != null) return false
		} else if (shortObject != other.shortObject) {
			return false
		}
		if (stringObject == null) {
			if (other.stringObject != null) return false
		} else if (stringObject != other.stringObject) {
			return false
		}
		if (uuidObject == null) {
			if (other.uuidObject != null) return false
		} else if (uuidObject != other.uuidObject) {
			return false
		}
		return true
	}

	companion object {
		private const val serialVersionUID = 1L
	}
}
