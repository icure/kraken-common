/*
 * Copyright (c) 2020. Taktik SA, All rights reserved.
 */

package org.taktik.icure.config

import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.taktik.couchdb.id.UUIDGenerator
import org.taktik.icure.asynclogic.impl.filter.Filters
import org.taktik.icure.serialization.IcureDomainObjectMapper

@Configuration
class SharedCoreConfig {
	@Bean
	fun uuidGenerator() = UUIDGenerator()

	@Bean
	fun filters() = Filters()

	@Bean
	fun objectMapper(): ObjectMapper = IcureDomainObjectMapper.new()
}
