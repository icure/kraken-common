package org.taktik.icure.services.external.rest.v1.dto

data class GroupDeletionReportDto(
	val type: String,
	val id: String,
	val server: String,
	val status: Status = Status.DELETED,
	val message: String? = null,
) {
	enum class Status {
		DELETED,
		ABSENT,
		FAILED,
	}
}
