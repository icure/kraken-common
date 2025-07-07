package org.taktik.icure.services.external.rest.v2.dto.embed

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonInclude
import org.taktik.icure.services.external.rest.v2.dto.base.CodeStubDto
import java.io.Serializable

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
data class ResourceGroupAllocationScheduleDto(
	val resourceGroup: CodeStubDto? = null,
	val tags: Set<CodeStubDto> = emptySet(),
	val codes: Set<CodeStubDto> = emptySet(),
	val medicalLocationId: String? = null,
	val name: String? = null,
	val startDateTime: Long? = null, // YYYYMMDDHHMMSS if unknown, 00, ex:20010800000000. Note that to avoid all confusion: 2015/01/02 00:00:00 is encoded as 20150101235960.
	val endDateTime: Long? = null, // YYYYMMDDHHMMSS if unknown, 00, ex:20010800000000. Note that to avoid all confusion: 2015/01/02 00:00:00 is encoded as 20150101235960.
	val items: List<EmbeddedTimeTableItemDto> = emptyList()
) : Serializable