package org.taktik.icure.exceptions

class UnsupportedConfigViewException(
	viewName: String,
	entity: String,
) : Exception("View $viewName is not supported for $entity in groups that do not have a design doc config") {
	companion object {
		const val EXCEPTION_DETAIL = "UnsupportedConfigViewException"
	}
}