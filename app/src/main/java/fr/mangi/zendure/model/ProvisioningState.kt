package fr.mangi.zendure.model

enum class ProvisioningStep {
    CONNECTING,
    DISCOVERING_SERVICES,
    NEGOTIATING_MTU,
    ENABLING_NOTIFICATIONS,
    READING_INFO,
    SENDING_CONFIG,
    APPLYING,
    AWAITING_REBOOT,
}

sealed interface ProvisioningState {
    data object Idle : ProvisioningState

    data class Running(val step: ProvisioningStep) : ProvisioningState

    data class Success(
        val info: DeviceInfo?,
        val restoredCloud: Boolean,
        val brokerHost: String,
    ) : ProvisioningState

    data class Failure(val step: ProvisioningStep, val message: String) : ProvisioningState
}
