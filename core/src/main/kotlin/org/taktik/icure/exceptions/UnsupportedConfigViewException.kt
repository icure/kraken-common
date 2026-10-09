package org.taktik.icure.exceptions

class UnsupportedConfigViewException(
	viewName: String,
	entity: String,
	message: String = "View $viewName is not supported for $entity in groups that do not have a design doc config",
) : Exception(message) {
	companion object {
		const val EXCEPTION_DETAIL = "UnsupportedConfigViewException"
	}
}
