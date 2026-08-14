package org.taktik.icure.services.external.rest.v2.dto

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonInclude
import io.swagger.v3.oas.annotations.media.Schema
import org.taktik.icure.dto.annotations.filtering.ActiveField
import java.io.Serializable

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
@Schema(
	description = """The error that aborted a page of results. When present the rows of the paginated list are valid but
incomplete, and there is no cursor to resume from: the request has to be retried from the beginning once the cause of
the error is solved. It is reported in the body, and not as a failed request, because the response was already committed
with a success status by the time the error was found.""",
)
/**
 * The error that aborted a page of results, reported in the body of an otherwise successful response.
 */
data class PaginationErrorDto(
	@param:Schema(
		description = """The http status code the request would have failed with, had the error been found before any
result was returned.""",
		required = true,
	)
	/** The status code the request would have failed with, had the error been found before returning any result. */
	@ActiveField val statusCode: Int,
	@param:Schema(description = """A human readable description of what went wrong.""", required = true)
	/** A human readable description of what went wrong. */
	@ActiveField val message: String,
	@param:Schema(description = """A machine readable discriminator for the error, for the errors that define one.""")
	/** A machine readable discriminator for the error, for the errors that define one. */
	@ActiveField val exceptionDetail: String? = null,
) : Serializable
