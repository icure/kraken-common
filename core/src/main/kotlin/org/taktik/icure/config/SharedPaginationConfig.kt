package org.taktik.icure.config

import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Configuration

@Configuration
class SharedPaginationConfig(
	@Value("\${icure.pagination.defaultLimit:1000}") val defaultLimit: Int
)