package org.taktik.icure.services.external.rest.v1.dto.base

/**
 * Enum class representing the participant types for an encounter/contact
 *
 * Mostly based on HL7 FHIR R4
 */
enum class ParticipantTypeDto {
    /**
     * The practitioner who is responsible for admitting a patient to a patient encounter.
     */
    Admitter,

    /**
     * The practitioner that has responsibility for overseeing a patient's care during a patient encounter.
     */
    Attender,

    /**
     * A person or organization who should be contacted for follow-up questions about the act in place of the author.
     */
    CallbackContact,

    /**
     * An advisor participating in the service by performing evaluations and making recommendations.
     */
    Consultant,

    /**
     * The practitioner who is responsible for the discharge of a patient from a patient encounter.
     */
    Discharger,

    /**
     * Only with Transportation services. A person who escorts the patient.
     */
    Escort,

    /**
     * A person having referred the subject of the service to the performer (referring physician). Typically, a referring physician will receive a report.
     */
    Referrer,

    /**
     * A person assisting in an act through his substantial presence and involvement. This includes: assistants, technicians, associates, or whatever the job titles may be.
     */
    SecondaryPerformer,

    /**
     * The principal or primary performer of the act.
     */
    PrimaryPerformer,

    /**
     * Indicates that the target of the participation is involved in some manner in the act, but does not qualify how.
     */
    Participation,

    /**
     * A translator who is facilitating communication with the patient during the encounter.
     */
    Translator,

    /**
     * A person to be contacted in case of an emergency during the encounter.
     */
    Emergency,

    /**
     * Location where the contact happened. If the contact happened in a Hospital, Retirement House, ...
     */
    Location
}