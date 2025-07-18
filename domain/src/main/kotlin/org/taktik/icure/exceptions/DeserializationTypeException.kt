package org.taktik.icure.exceptions

import kotlin.reflect.KClass

class DeserializationTypeException(
	objectId: String,
	expectedType: KClass<*>,
	actualTypeName: String,
) : IllegalArgumentException(
	"Object with ID $objectId is not of expected type ${expectedType.qualifiedName} but of type $actualTypeName",
)
