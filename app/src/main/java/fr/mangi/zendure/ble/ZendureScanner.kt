package fr.mangi.zendure.ble

import android.annotation.SuppressLint
import android.bluetooth.BluetoothManager
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context
import android.util.Log
import fr.mangi.zendure.model.DiscoveredDevice
import fr.mangi.zendure.model.ZendureModel
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

private const val TAG = "ZendureBle"

class ZendureScanner(private val context: Context) {

    /**
     * Flux d'appareils Zendure détectés. Pas de ScanFilter matériel : le filtre par nom
     * exige une égalité exacte, or les noms sont "zenX" + suffixe variable — filtrage logiciel.
     * Le scan s'arrête à l'annulation de la collecte.
     */
    @SuppressLint("MissingPermission")
    fun scan(): Flow<DiscoveredDevice> = callbackFlow {
        val manager = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        val scanner = manager.adapter?.takeIf { it.isEnabled }?.bluetoothLeScanner
        if (scanner == null) {
            close(IllegalStateException("Bluetooth off"))
            return@callbackFlow
        }

        val callback = object : ScanCallback() {
            override fun onScanResult(callbackType: Int, result: ScanResult) {
                val name = result.scanRecord?.deviceName ?: result.device.name
                if (!ZendureModel.isZendure(name)) return
                trySend(
                    DiscoveredDevice(
                        name = name!!,
                        address = result.device.address,
                        rssi = result.rssi,
                        model = ZendureModel.fromBleName(name),
                        device = result.device,
                    )
                )
            }

            override fun onScanFailed(errorCode: Int) {
                Log.w(TAG, "Scan failed with code $errorCode")
                close(IllegalStateException("code $errorCode"))
            }
        }

        val settings = ScanSettings.Builder()
            .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
            .build()
        scanner.startScan(null, settings, callback)
        Log.d(TAG, "BLE scan started")

        awaitClose {
            runCatching { scanner.stopScan(callback) }
            Log.d(TAG, "BLE scan stopped")
        }
    }
}
