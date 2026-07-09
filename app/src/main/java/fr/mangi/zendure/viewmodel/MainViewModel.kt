package fr.mangi.zendure.viewmodel

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import fr.mangi.zendure.ble.GattClient
import fr.mangi.zendure.ble.ZendureBle
import fr.mangi.zendure.ble.ZendureCommands
import fr.mangi.zendure.ble.ZendureScanner
import fr.mangi.zendure.model.DeviceInfo
import fr.mangi.zendure.model.DiscoveredDevice
import fr.mangi.zendure.model.ProvisioningState
import fr.mangi.zendure.model.ProvisioningStep
import fr.mangi.zendure.util.TimeZoneUtil
import kotlinx.coroutines.Job
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val scanner = ZendureScanner(application)

    // --- Scan ---
    private val _devices = MutableStateFlow<Map<String, DiscoveredDevice>>(emptyMap())
    val devices: StateFlow<Map<String, DiscoveredDevice>> = _devices.asStateFlow()

    private val _scanning = MutableStateFlow(false)
    val scanning: StateFlow<Boolean> = _scanning.asStateFlow()

    private val _scanError = MutableStateFlow<String?>(null)
    val scanError: StateFlow<String?> = _scanError.asStateFlow()

    private var scanJob: Job? = null

    // --- Sélection + formulaire (état partagé entre les écrans) ---
    var selected: DiscoveredDevice? by mutableStateOf(null)
        private set
    var ssid by mutableStateOf("")
    var wifiPassword by mutableStateOf("")
    var brokerHost by mutableStateOf("")
    var timeZone by mutableStateOf(TimeZoneUtil.currentGmtOffset())
    var restoreCloud by mutableStateOf(false)

    // --- Provisioning ---
    private val _provisioning = MutableStateFlow<ProvisioningState>(ProvisioningState.Idle)
    val provisioning: StateFlow<ProvisioningState> = _provisioning.asStateFlow()

    private var provisioningJob: Job? = null

    fun startScan() {
        if (scanJob?.isActive == true) return
        _scanError.value = null
        _scanning.value = true
        scanJob = viewModelScope.launch {
            try {
                scanner.scan().collect { device ->
                    _devices.value = _devices.value + (device.address to device)
                }
            } catch (e: Exception) {
                _scanError.value = e.message
            } finally {
                _scanning.value = false
            }
        }
    }

    fun stopScan() {
        scanJob?.cancel()
        scanJob = null
        _scanning.value = false
    }

    fun selectDevice(device: DiscoveredDevice) {
        selected = device
        _provisioning.value = ProvisioningState.Idle
    }

    /** IP/host nettoyé : schémas et port retirés (le firmware n'accepte qu'un host, port 1883). */
    fun normalizedBroker(): String = brokerHost
        .trim()
        .removePrefix("mqtt://")
        .removePrefix("tcp://")
        .removePrefix("http://")
        .removePrefix("https://")
        .substringBefore('/')
        .substringBefore(':')

    fun isFormValid(): Boolean {
        val brokerOk = restoreCloud || normalizedBroker().isNotBlank()
        return selected != null && ssid.isNotBlank() && wifiPassword.isNotBlank() &&
            TimeZoneUtil.isValid(timeZone) && brokerOk
    }

    fun startProvisioning() {
        val device = selected ?: return
        if (provisioningJob?.isActive == true) return
        val iotUrl = if (restoreCloud) ZendureBle.CLOUD_BROKER else normalizedBroker()

        provisioningJob = viewModelScope.launch {
            stopScan()
            val client = GattClient(getApplication())
            var step = ProvisioningStep.CONNECTING
            try {
                _provisioning.value = ProvisioningState.Running(step)
                client.connect(device.device)

                step = ProvisioningStep.DISCOVERING_SERVICES
                _provisioning.value = ProvisioningState.Running(step)
                client.discoverServices()

                step = ProvisioningStep.NEGOTIATING_MTU
                _provisioning.value = ProvisioningState.Running(step)
                client.negotiateMtu()

                step = ProvisioningStep.ENABLING_NOTIFICATIONS
                _provisioning.value = ProvisioningState.Running(step)
                client.enableNotifications()

                step = ProvisioningStep.READING_INFO
                _provisioning.value = ProvisioningState.Running(step)
                val info = readDeviceInfo(client)

                step = ProvisioningStep.SENDING_CONFIG
                _provisioning.value = ProvisioningState.Running(step)
                client.write(
                    ZendureCommands.tokenPayload(iotUrl, ssid.trim(), wifiPassword, timeZone)
                )
                delay(500)

                step = ProvisioningStep.APPLYING
                _provisioning.value = ProvisioningState.Running(step)
                client.write(ZendureCommands.stationPayload())

                step = ProvisioningStep.AWAITING_REBOOT
                _provisioning.value = ProvisioningState.Running(step)
                client.awaitDisconnect(40_000)

                _provisioning.value = ProvisioningState.Success(info, restoreCloud, iotUrl)
            } catch (e: Exception) {
                _provisioning.value = ProvisioningState.Failure(
                    step,
                    e.message ?: e.javaClass.simpleName,
                )
            } finally {
                client.close()
            }
        }
    }

    /** La lecture des infos n'est pas bloquante : sans deviceId, le provisioning reste possible. */
    private suspend fun readDeviceInfo(client: GattClient): DeviceInfo? {
        repeat(2) {
            try {
                val json = client.writeAndAwaitJson(
                    ZendureCommands.getInfoPayload(System.currentTimeMillis() / 1000),
                    timeoutMs = 5_000,
                ) { ZendureCommands.isGetInfoResponse(it) }
                return ZendureCommands.parseGetInfoResponse(json)
            } catch (_: TimeoutCancellationException) {
                // retry une fois
            }
        }
        return null
    }

    fun resetProvisioning() {
        provisioningJob?.cancel()
        provisioningJob = null
        _provisioning.value = ProvisioningState.Idle
    }
}
