/*
 * Copyright (c) 2020. Taktik SA, All rights reserved.
 */

package org.taktik.icure.services.external.rest.v1.mapper.embed

import org.mapstruct.InjectionStrategy
import org.mapstruct.Mapper
import org.mapstruct.Mapping
import org.mapstruct.Mappings
import org.taktik.icure.entities.embed.Episode
import org.taktik.icure.services.external.rest.v1.dto.embed.EpisodeDto

@Mapper(componentModel = "spring", injectionStrategy = InjectionStrategy.CONSTRUCTOR)
interface EpisodeMapper {
	@Mappings(
		Mapping(target = "extensions", ignore = true),
	)
	fun map(episodeDto: EpisodeDto): Episode
	fun map(episode: Episode): EpisodeDto {
		require(episode.extensions == null) { "Episode has extensions and can't be used with v1 endpoints" }
		return doMap(episode)
	}
	fun doMap(episode: Episode): EpisodeDto
}
