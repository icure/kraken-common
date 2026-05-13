package org.taktik.icure.exceptions

class MissingViewException(
	viewName: String,
	entity: String,
	designDocConfigId: String
) : Exception("View $viewName not found for $entity in designDocConfigId: $designDocConfigId") {
	companion object {
		const val EXCEPTION_DETAIL = "MissingViewException"
	}
}