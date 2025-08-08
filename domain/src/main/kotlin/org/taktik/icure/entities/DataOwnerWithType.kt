package org.taktik.icure.entities

import org.taktik.icure.entities.base.CryptoActor
import org.taktik.icure.entities.base.asCryptoActorStub
import java.io.Serializable

// Note: interface and classes intentionally not serializable
sealed interface DataOwnerWithType : Serializable {
	/**
	 * The data owner entity.
	 */
	val dataOwner: CryptoActor

	/**
	 * The type of the data owner.
	 */
	val type: DataOwnerType

	/**
	 * The id of the data owner.
	 */
	val id: String

	/**
	 * The revision of the data owner.
	 */
	val rev: String?

	/**
	 * A stub containing only the crypto-actor and versionbable properites of the data owner and its type information.
	 */
	fun asCryptoActorStub(): CryptoActorStubWithType?

	data class HcpDataOwner(override val dataOwner: HealthcareParty) : DataOwnerWithType {
		override val type: DataOwnerType get() = DataOwnerType.HCP
		override val id: String get() = dataOwner.id
		override val rev: String? get() = dataOwner.rev

		override fun asCryptoActorStub(): CryptoActorStubWithType? = dataOwner.asCryptoActorStub()?.let {
			CryptoActorStubWithType(DataOwnerType.HCP, it)
		}
	}

	data class PatientDataOwner(override val dataOwner: Patient) : DataOwnerWithType {
		override val type: DataOwnerType get() = DataOwnerType.PATIENT
		override val id: String get() = dataOwner.id
		override val rev: String? get() = dataOwner.rev

		override fun asCryptoActorStub(): CryptoActorStubWithType? = dataOwner.asCryptoActorStub()?.let {
			CryptoActorStubWithType(DataOwnerType.PATIENT, it)
		}
	}

	data class DeviceDataOwner(override val dataOwner: Device) : DataOwnerWithType {
		override val type: DataOwnerType get() = DataOwnerType.DEVICE
		override val id: String get() = dataOwner.id
		override val rev: String? get() = dataOwner.rev

		override fun asCryptoActorStub(): CryptoActorStubWithType? = dataOwner.asCryptoActorStub()?.let {
			CryptoActorStubWithType(DataOwnerType.DEVICE, it)
		}
	}
}

fun Patient.toDataOwnerWithType() = DataOwnerWithType.PatientDataOwner(this)
fun HealthcareParty.toDataOwnerWithType() = DataOwnerWithType.HcpDataOwner(this)
fun Device.toDataOwnerWithType() = DataOwnerWithType.DeviceDataOwner(this)
