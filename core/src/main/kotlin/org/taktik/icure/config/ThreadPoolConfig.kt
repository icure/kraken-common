/*
 * Copyright (c) 2020. Taktik SA, All rights reserved.
 */

package org.taktik.icure.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile
import org.springframework.core.task.TaskExecutor
import org.springframework.scheduling.TaskScheduler
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler

@Configuration
@Profile("app")
class ThreadPoolConfig {
	@Bean
	fun threadPoolTaskExecutor(): TaskExecutor = ThreadPoolTaskExecutor().apply {
		corePoolSize = 8
		maxPoolSize = 24
		setThreadNamePrefix("default_task_executor_thread_pool")
		initialize()
	}

	@Bean
	fun threadPoolTaskScheduler(): TaskScheduler = ThreadPoolTaskScheduler().apply {
		setThreadNamePrefix("default_task_scheduler_thread_pool")
		initialize()
	}
}
