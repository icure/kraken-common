/*
 * Copyright (c) 2020. Taktik SA, All rights reserved.
 */
package org.taktik.icure.entities

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import com.fasterxml.jackson.databind.annotation.JsonSerialize
import org.taktik.couchdb.entity.Attachment
import org.taktik.icure.annotations.entities.ContentValue
import org.taktik.icure.annotations.entities.ContentValues
import org.taktik.icure.constants.Users
import org.taktik.icure.entities.base.BaseUser
import org.taktik.icure.entities.base.PropertyStub
import org.taktik.icure.entities.base.StoredDocument
import org.taktik.icure.entities.embed.DelegationTag
import org.taktik.icure.entities.embed.RevisionInfo
import org.taktik.icure.entities.security.AuthenticationToken
import org.taktik.icure.entities.security.Permission
import org.taktik.icure.entities.security.Principal
import org.taktik.icure.entities.utils.MergeUtil.mergeMapsOfSetsDistinct
import org.taktik.icure.security.credentials.SecretType
import org.taktik.icure.utils.DynamicInitializer
import org.taktik.icure.utils.InstantDeserializer
import org.taktik.icure.utils.InstantSerializer
import org.taktik.icure.utils.invoke
import org.taktik.icure.validation.AutoFix
import org.taktik.icure.validation.NotNull
import java.io.Serializable
import java.time.Instant

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)

/**
 * A User
 * This entity is a root level object. It represents an user that can log in to the iCure platform. It is serialized in JSON and saved in the underlying icure-base CouchDB database.
 * A User conforms to a series of interfaces:
 * - StoredDocument
 * - Principal
 *
 * @property id the Id of the user. We encourage using either a v4 UUID or a HL7 Id.
 * @property rev the revision of the user in the database, used for conflict management / optimistic locking.
 * @property created the timestamp (unix epoch in ms) of creation of the user, will be filled automatically if missing. Not enforced by the application server.
 * @property deletionDate hard delete (unix epoch in ms) timestamp of the object. Filled automatically when user is deleted.
 * @property name Last name of the user. This is the official last name that should be used for official administrative purposes.
 * @property properties Extra properties for the user. Those properties are typed (see class Property)
 * @property roles Roles specified for the user
 * @property permissions If permission to modify patient data is granted or revoked
 * @property type Authorization source for user. 'Database', 'ldap' or 'token'
 * @property status State of user's activeness: 'Active', 'Disabled' or 'Registering'
 * @property login Username for this user. We encourage using an email address
 * @property groupId id of the group (practice/hospital) the user is member of
 * @property healthcarePartyId Id of the healthcare party if the user is a healthcare party.
 * @property patientId Id of the patient if the user is a patient
 * @property autoDelegations Delegations that are automatically generated client side when a new database object is created by this user
 * @property createdDate the timestamp (unix epoch in ms) of creation of the user, will be filled automatically if missing. Not enforced by the application server.
 * @property termsOfUseDate the timestamp (unix epoch in ms) of the latest validation of the terms of use of the application
 * @property email email address of the user.
 */

data class User(
    @param:ContentValue(ContentValues.UUID) @JsonProperty("_id") override val id: String,
    @JsonProperty("_rev") override val rev: String? = null,
    @JsonProperty("deleted") override val deletionDate: Long? = null,
    @field:NotNull(autoFix = AutoFix.NOW) val created: Long? = null,

    @param:ContentValue(ContentValues.ANY_STRING) override val name: String? = null,
    override val properties: Set<PropertyStub> = emptySet(),
    /**
     * Local roles of the user. May not actually reflect the roles the user has in the cloud environment.
     */
    val roles: Set<String> = emptySet(),
    /**
     * Local permissions of the user. May not actually reflect the permissions the user has in the cloud system.
     */
    val permissions: Set<Permission> = emptySet(),
    val type: Users.Type? = null,
    val status: Users.Status? = null,
    val login: String? = null,
    override val passwordHash: String? = null,
    val groupId: String? = null,
    val healthcarePartyId: String? = null,
    @param:ContentValue(ContentValues.UUID) val patientId: String? = null,
    val deviceId: String? = null,
    val autoDelegations: Map<DelegationTag, Set<String>> = emptyMap(), //DelegationTag -> healthcarePartyIds
    @JsonSerialize(using = InstantSerializer::class)
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @JsonDeserialize(using = InstantDeserializer::class)
    val createdDate: Instant? = null, // TODO remove if unused (use created insted)

    @JsonSerialize(using = InstantSerializer::class)
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @JsonDeserialize(using = InstantDeserializer::class)
    val termsOfUseDate: Instant? = null,

    @param:ContentValue(ContentValues.EMAIL) val email: String? = null,
    val mobilePhone: String? = null,

    @Deprecated("Application tokens stocked in clear and eternal. Replaced by authenticationTokens")
    override val applicationTokens: Map<String, String>? = null,

    override val authenticationTokens: Map<String, AuthenticationToken> = emptyMap(),

    /**
     * Metadata used to enrich the user with information from the cloud environment. Not actually stored as part of the
     * user the database, can't be changed through changes of the local/replicated user.
     */
    @JsonIgnore val systemMetadata: SystemMetadata? = null,

    @JsonProperty("_attachments") override val attachments: Map<String, Attachment>? = emptyMap(),
    @JsonProperty("_revs_info") override val revisionsInfo: List<RevisionInfo>? = emptyList(),
    @JsonProperty("_conflicts") override val conflicts: List<String>? = emptyList(),
    @JsonProperty("rev_history") override val revHistory: Map<String, String>? = emptyMap(),
) : StoredDocument, Principal, Cloneable, Serializable, BaseUser {
    companion object : DynamicInitializer<User> {
        data class EnhancementMetadata(val groupId: String, val systemMetadata: SystemMetadata?)
    }

	@JsonIgnore override val secret: String? = null
	@JsonIgnore override val use2fa: Boolean? = null

    fun merge(other: User) = User(args = this.solveConflictsWith(other))
    fun solveConflictsWith(other: User) = super.solveConflictsWith(other) + mapOf(
        "created" to (this.created?.coerceAtMost(other.created ?: Long.MAX_VALUE) ?: other.created),
        "name" to (this.name ?: other.name),
        "properties" to (other.properties + this.properties),
        "permissions" to (other.permissions + this.permissions),
        "type" to (this.type ?: other.type),
        "status" to (this.status ?: other.status),
        "login" to (this.login ?: other.login),
        "passwordHash" to (this.passwordHash ?: other.passwordHash),
        "secret" to (this.secret ?: other.secret),
        "isUse2fa" to (this.use2fa ?: other.use2fa),
        "groupId" to (this.groupId ?: other.groupId),
        "healthcarePartyId" to (this.healthcarePartyId ?: other.healthcarePartyId),
        "patientId" to (this.patientId ?: other.patientId),
        "autoDelegations" to mergeMapsOfSetsDistinct(this.autoDelegations, other.autoDelegations),
        "createdDate" to (this.createdDate ?: other.createdDate),
        "termsOfUseDate" to (this.termsOfUseDate ?: other.termsOfUseDate),
        "email" to (this.email ?: other.email),
        "applicationTokens" to (other.applicationTokens?.let { it + (this.applicationTokens ?: emptyMap()) } ?: this.applicationTokens),
        "authenticationTokens" to (other.authenticationTokens + this.authenticationTokens)
    )

    override fun withIdRev(id: String?, rev: String) = if (id != null) this.copy(id = id, rev = rev) else this.copy(rev = rev)
    override fun withDeletionDate(deletionDate: Long?) = this.copy(deletionDate = deletionDate)

    @JsonInclude(JsonInclude.Include.NON_NULL)
    @JsonIgnoreProperties(ignoreUnknown = true)
    data class SystemMetadata(
        /**
         * The roles that the user for acting on the cloud environment.
         */
        val roles: Set<String>,
        /**
         * Specifies if the user is an admin in the cloud environment. An admin user is considered to have the permissions
         * to do anything on his group and on children groups.
         */
        val isAdmin: Boolean,
        /**
         * Specifies if the roles of the user are inherited from the group configuration (true), or if they are custom
         * for the user (false).
         */
        val inheritsRoles: Boolean
    ) : Serializable

    /**
     * Hashes all the secrets (password, applicationTokens, authenticationTokens) of the [User], according to the
     * strategy passed as parameter.
     *
     * @param encodeAndValidatePassword a function that receives to parameters a String and a [SecretType]. If the secret
     * is not already hashed, the function will verify its validity according to the [SecretType] and, if it is valid,
     * it will hash it.
     * @return a copy of the [User] with all secrets hashed.
     */
    fun hashPasswordAndTokens(encodeAndValidatePassword: (secret: String, secretType: SecretType) -> String): User {
        val encodedPassword = passwordHash?.let { password ->
            encodeAndValidatePassword(password, SecretType.PASSWORD)
        }
        val convertedApplicationTokens = applicationTokens?.mapValues { (_, rawToken) ->
            AuthenticationToken(rawToken, validity = AuthenticationToken.LONG_LIVING_TOKEN_VALIDITY)
        } ?: emptyMap()
        val encodedTokens = ( // Converted application tokens also need hashing. If any converted application tokens have the same key as
                convertedApplicationTokens + authenticationTokens
                ).mapValues { (_, authToken) ->
                authToken.copy(
                    token = encodeAndValidatePassword(authToken.token, if(authToken.isShortLived) SecretType.SHORT_TOKEN else SecretType.LONG_TOKEN)
                )
            }
        return copy(
            passwordHash = encodedPassword,
            authenticationTokens = encodedTokens,
            applicationTokens = emptyMap()
        )
    }

    /**
     * Enhances the current user with some [EnhancementMetadata] (usually security information coming from the fallback).
     *
     * @param meta an instance of [EnhancementMetadata] or null.
     * @return an [EnhancedUser].
     */
    fun enhanceWith(meta: EnhancementMetadata?): EnhancedUser = meta?.let {
        copy(
            groupId = it.groupId,
            systemMetadata = it.systemMetadata
        )
    } ?: this
}

/**
 * Id of the user on the fallback db. Includes the group id.
 * @throws IllegalArgumentException if the user has no group id
 */
val User.globalId: String get() = this.groupId?.let { "$it:${id}" }
    ?: throw IllegalArgumentException("User $id has no group id")

// Note if we change the extension methods to be properties of User we have to @JsonIgnore them or they will be serialized

fun User.isHealthcareParty(): Boolean = healthcarePartyId != null
fun User.isDevice(): Boolean = deviceId != null
fun User.isPatient(): Boolean = patientId != null

fun User.omittingSecrets(): User = TODO()

fun User.withSecretsFilledFrom(currentUser: User): User = TODO()

fun User.getUserType(): UserType =
    when {
        healthcarePartyId != null -> UserType.HCP
        patientId != null -> UserType.PATIENT
        deviceId != null -> UserType.DEVICE
        else -> UserType.USER
    }

fun User.getDataOwnerTypeOrNull(): DataOwnerType? =
    when {
        healthcarePartyId != null -> DataOwnerType.HCP
        patientId != null -> DataOwnerType.PATIENT
        deviceId != null -> DataOwnerType.DEVICE
        else -> null
    }

fun User.isSuperAdmin(): Boolean = checkNotNull(systemMetadata) {
    "Checking if a user is admin but the system metadata was not filled from the global user"
}.isAdmin
