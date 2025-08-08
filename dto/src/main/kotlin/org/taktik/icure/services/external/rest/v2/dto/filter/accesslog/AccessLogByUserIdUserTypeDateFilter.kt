package org.taktik.icure.services.external.rest.v2.dto.filter.accesslog

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.databind.JsonDeserializer
import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import org.taktik.icure.handlers.JsonPolymorphismRoot
import org.taktik.icure.services.external.rest.v2.dto.AccessLogDto
import org.taktik.icure.services.external.rest.v2.dto.filter.AbstractFilterDto
import java.time.Instant

@JsonPolymorphismRoot(AbstractFilterDto::class)
@JsonDeserialize(using = JsonDeserializer.None::class)
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
data class AccessLogByUserIdUserTypeDateFilter(
	val userId: String,
	val accessType: String?,
	val startDate: Instant?,
	val descending: Boolean?,
	override val desc: String? = null,
) : AbstractFilterDto<AccessLogDto>
