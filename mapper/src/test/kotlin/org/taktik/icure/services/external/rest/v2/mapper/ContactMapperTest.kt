package org.taktik.icure.services.external.rest.v2.mapper

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.maps.shouldBeEmpty
import io.kotest.matchers.maps.shouldContainExactly
import io.kotest.matchers.shouldBe
import org.taktik.icure.entities.base.ParticipantType
import org.taktik.icure.entities.embed.ContactParticipant
import org.taktik.icure.services.external.rest.v2.dto.ContactDto
import org.taktik.icure.services.external.rest.v2.dto.base.ParticipantTypeDto
import org.taktik.icure.services.external.rest.v2.dto.embed.ContactParticipantDto
import org.taktik.icure.services.external.rest.v2.mapper.embed.ContactParticipantV2MapperImpl

class ContactMapperTest : StringSpec({


	"mapParticipants should handle participants map correctly" {
		val contactDto = ContactDto(
			id = "contact-1",
			participants = mapOf(
				ParticipantTypeDto.Admitter to "hcp-1",
				ParticipantTypeDto.Attender to "hcp-2",
				ParticipantTypeDto.Consultant to "hcp-3",
			),
			participantList = emptyList(),
		)

		val participants = ContactV2Mapper.mapParticipants(contactDto)
		val participantList = ContactV2Mapper.mapParticipantList(contactDto, ContactParticipantV2MapperImpl())

		participants shouldContainExactly mapOf(
			ParticipantType.Admitter to "hcp-1",
			ParticipantType.Attender to "hcp-2",
			ParticipantType.Consultant to "hcp-3",
		)

		participantList shouldBe emptyList()
	}

	"mapParticipants should convert participantList without duplicates to participants map" {
		val contactDto = ContactDto(
			id = "contact-1",
			participants = emptyMap(),
			participantList = listOf(
				ContactParticipantDto(type = ParticipantTypeDto.Admitter, hcpId = "hcp-1"),
				ContactParticipantDto(type = ParticipantTypeDto.Attender, hcpId = "hcp-2"),
				ContactParticipantDto(type = ParticipantTypeDto.Consultant, hcpId = "hcp-3"),
			),
		)

		val participants = ContactV2Mapper.mapParticipants(contactDto)
		val participantList = ContactV2Mapper.mapParticipantList(contactDto, ContactParticipantV2MapperImpl())

		participants shouldContainExactly mapOf(
			ParticipantType.Admitter to "hcp-1",
			ParticipantType.Attender to "hcp-2",
			ParticipantType.Consultant to "hcp-3",
		)
		participantList.shouldBeEmpty()
	}

	"mapParticipants should handle participantList with duplicate types" {
		val contactDto = ContactDto(
			id = "contact-1",
			participants = emptyMap(),
			participantList = listOf(
				ContactParticipantDto(type = ParticipantTypeDto.Admitter, hcpId = "hcp-1"),
				ContactParticipantDto(type = ParticipantTypeDto.Admitter, hcpId = "hcp-2"),
				ContactParticipantDto(type = ParticipantTypeDto.Consultant, hcpId = "hcp-3"),
			),
		)

		val participants = ContactV2Mapper.mapParticipants(contactDto)
		val participantList = ContactV2Mapper.mapParticipantList(contactDto, ContactParticipantV2MapperImpl())

		participants.shouldBeEmpty()
		participantList shouldContainExactlyInAnyOrder listOf(
			ContactParticipant(type = ParticipantType.Admitter, hcpId = "hcp-1"),
			ContactParticipant(type = ParticipantType.Admitter, hcpId = "hcp-2"),
			ContactParticipant(type = ParticipantType.Consultant, hcpId = "hcp-3"),
		)
	}

	"mapParticipants should throw exception when both participants and participantList are populated" {
		val contactDto = ContactDto(
			id = "contact-1",
			participants = mapOf(
				ParticipantTypeDto.Admitter to "hcp-1",
			),
			participantList = listOf(
				ContactParticipantDto(type = ParticipantTypeDto.Attender, hcpId = "hcp-2"),
			),
		)

		shouldThrow<IllegalArgumentException> {
			ContactV2Mapper.mapParticipants(contactDto)
		}.message shouldBe "ContactDto cannot have both participants map and participantList populated"
	}

	"mapParticipants should return empty map when both are empty" {
		val contactDto = ContactDto(
			id = "contact-1",
			participants = emptyMap(),
			participantList = emptyList(),
		)

		val participants = ContactV2Mapper.mapParticipants(contactDto)
		val participantList = ContactV2Mapper.mapParticipantList(contactDto, ContactParticipantV2MapperImpl())

		participants.shouldBeEmpty()
		participantList.shouldBeEmpty()
	}

	"mapParticipants should convert ParticipantTypeDto to ParticipantType correctly" {
		val contactDto = ContactDto(
			id = "contact-1",
			participants = mapOf(
				ParticipantTypeDto.Discharger to "hcp-1",
				ParticipantTypeDto.Referrer to "hcp-2",
			),
			participantList = emptyList(),
		)

		val participants = ContactV2Mapper.mapParticipants(contactDto)
		val participantList = ContactV2Mapper.mapParticipantList(contactDto, ContactParticipantV2MapperImpl())

		participants shouldContainExactly mapOf(
			ParticipantType.Discharger to "hcp-1",
			ParticipantType.Referrer to "hcp-2",
		)

		participantList.shouldBeEmpty()
	}

	"mapParticipantList should return empty list when no duplicates" {
		val contactDto = ContactDto(
			id = "contact-1",
			participants = emptyMap(),
			participantList = listOf(
				ContactParticipantDto(type = ParticipantTypeDto.Admitter, hcpId = "hcp-1"),
				ContactParticipantDto(type = ParticipantTypeDto.Attender, hcpId = "hcp-2"),
			),
		)

		val participantMapper = ContactParticipantV2MapperImpl()
		val participantList = ContactV2Mapper.mapParticipantList(contactDto, participantMapper)

		participantList.shouldBeEmpty()
	}

	"mapParticipantList should return mapped list when duplicates exist" {
		val contactDto = ContactDto(
			id = "contact-1",
			participants = emptyMap(),
			participantList = listOf(
				ContactParticipantDto(type = ParticipantTypeDto.Admitter, hcpId = "hcp-1"),
				ContactParticipantDto(type = ParticipantTypeDto.Admitter, hcpId = "hcp-2"),
			),
		)

		val participants = ContactV2Mapper.mapParticipants(contactDto)
		val participantMapper = ContactParticipantV2MapperImpl()
		val participantList = ContactV2Mapper.mapParticipantList(contactDto, participantMapper)

		participants.shouldBeEmpty()
		participantList shouldContainExactlyInAnyOrder listOf(
			ContactParticipant(type = ParticipantType.Admitter, hcpId = "hcp-1"),
			ContactParticipant(type = ParticipantType.Admitter, hcpId = "hcp-2"),
		)
	}

	"mapParticipants should handle multiple different participant types" {
		val contactDto = ContactDto(
			id = "contact-1",
			participants = emptyMap(),
			participantList = listOf(
				ContactParticipantDto(type = ParticipantTypeDto.PrimaryPerformer, hcpId = "hcp-1"),
				ContactParticipantDto(type = ParticipantTypeDto.Referrer, hcpId = "hcp-2"),
				ContactParticipantDto(type = ParticipantTypeDto.Attender, hcpId = "hcp-3"),
				ContactParticipantDto(type = ParticipantTypeDto.Admitter, hcpId = "hcp-4"),
			),
		)

		val participants = ContactV2Mapper.mapParticipants(contactDto)
		val participantList = ContactV2Mapper.mapParticipantList(contactDto, ContactParticipantV2MapperImpl())

		participants shouldContainExactly mapOf(
			ParticipantType.PrimaryPerformer to "hcp-1",
			ParticipantType.Referrer to "hcp-2",
			ParticipantType.Attender to "hcp-3",
			ParticipantType.Admitter to "hcp-4",
		)
		participantList.shouldBeEmpty()
	}

	"mapParticipants should handle Recorder type by using participantList" {
		val contactDto = ContactDto(
			id = "contact-1",
			participants = emptyMap(),
			participantList = listOf(
				ContactParticipantDto(type = ParticipantTypeDto.Recorder, hcpId = "hcp-1"),
				ContactParticipantDto(type = ParticipantTypeDto.Attender, hcpId = "hcp-2"),
			),
		)

		val participants = ContactV2Mapper.mapParticipants(contactDto)
		val participantList = ContactV2Mapper.mapParticipantList(contactDto, ContactParticipantV2MapperImpl())

		participants.shouldBeEmpty()
		participantList shouldContainExactlyInAnyOrder listOf(
			ContactParticipant(type = ParticipantType.Recorder, hcpId = "hcp-1"),
			ContactParticipant(type = ParticipantType.Attender, hcpId = "hcp-2"),
		)
	}

	"mapParticipantList should return mapped list when Recorder type is present" {
		val contactDto = ContactDto(
			id = "contact-1",
			participants = emptyMap(),
			participantList = listOf(
				ContactParticipantDto(type = ParticipantTypeDto.Recorder, hcpId = "hcp-1"),
				ContactParticipantDto(type = ParticipantTypeDto.Admitter, hcpId = "hcp-2"),
			),
		)

		val participants = ContactV2Mapper.mapParticipants(contactDto)
		val participantMapper = ContactParticipantV2MapperImpl()
		val participantList = ContactV2Mapper.mapParticipantList(contactDto, participantMapper)

		participants.shouldBeEmpty()
		participantList shouldContainExactlyInAnyOrder listOf(
			ContactParticipant(type = ParticipantType.Recorder, hcpId = "hcp-1"),
			ContactParticipant(type = ParticipantType.Admitter, hcpId = "hcp-2"),
		)
	}
})

