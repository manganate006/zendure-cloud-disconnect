package fr.mangi.zendure.ble

import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import android.bluetooth.BluetoothProfile
import android.bluetooth.BluetoothStatusCodes
import android.content.Context
import android.os.Build
import android.util.Log
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withTimeout
import org.json.JSONObject
import java.io.IOException

private const val TAG = "ZendureGatt"

/**
 * Client GATT minimal pour le provisioning Zendure : pont entre les callbacks Android
 * et les coroutines via un SharedFlow d'événements typés.
 *
 * L'abonnement au flux d'événements est toujours démarré (UNDISPATCHED) avant de
 * déclencher l'opération GATT correspondante, sinon une réponse rapide serait perdue.
 */
@SuppressLint("MissingPermission")
class GattClient(private val context: Context) {

    sealed interface GattEvent {
        data class ConnectionState(val status: Int, val newState: Int) : GattEvent
        data class ServicesDiscovered(val status: Int) : GattEvent
        data class MtuChanged(val mtu: Int, val status: Int) : GattEvent
        data class DescriptorWrite(val status: Int) : GattEvent
        data class CharacteristicWrite(val status: Int) : GattEvent
        data class Notification(val value: ByteArray) : GattEvent
    }

    private val events = MutableSharedFlow<GattEvent>(extraBufferCapacity = 64)
    private var gatt: BluetoothGatt? = null
    private var writeChar: BluetoothGattCharacteristic? = null
    private var notifyChar: BluetoothGattCharacteristic? = null

    var mtu: Int = 23
        private set

    private val callback = object : BluetoothGattCallback() {
        override fun onConnectionStateChange(g: BluetoothGatt, status: Int, newState: Int) {
            Log.d(TAG, "onConnectionStateChange status=$status newState=$newState")
            events.tryEmit(GattEvent.ConnectionState(status, newState))
        }

        override fun onServicesDiscovered(g: BluetoothGatt, status: Int) {
            events.tryEmit(GattEvent.ServicesDiscovered(status))
        }

        override fun onMtuChanged(g: BluetoothGatt, mtu: Int, status: Int) {
            Log.d(TAG, "onMtuChanged mtu=$mtu status=$status")
            if (status == BluetoothGatt.GATT_SUCCESS) this@GattClient.mtu = mtu
            events.tryEmit(GattEvent.MtuChanged(mtu, status))
        }

        override fun onDescriptorWrite(g: BluetoothGatt, d: BluetoothGattDescriptor, status: Int) {
            events.tryEmit(GattEvent.DescriptorWrite(status))
        }

        override fun onCharacteristicWrite(
            g: BluetoothGatt,
            c: BluetoothGattCharacteristic,
            status: Int,
        ) {
            events.tryEmit(GattEvent.CharacteristicWrite(status))
        }

        @Deprecated("Callback pré-API 33, conservé pour les anciennes versions")
        override fun onCharacteristicChanged(g: BluetoothGatt, c: BluetoothGattCharacteristic) {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
                events.tryEmit(GattEvent.Notification(c.value ?: ByteArray(0)))
            }
        }

        override fun onCharacteristicChanged(
            g: BluetoothGatt,
            c: BluetoothGattCharacteristic,
            value: ByteArray,
        ) {
            events.tryEmit(GattEvent.Notification(value))
        }
    }

    suspend fun connect(device: BluetoothDevice, timeoutMs: Long = 15_000) {
        var lastError: Exception? = null
        repeat(2) { attempt ->
            try {
                gatt = device.connectGatt(context, false, callback, BluetoothDevice.TRANSPORT_LE)
                val event = awaitEvent<GattEvent.ConnectionState>(timeoutMs)
                if (event.newState == BluetoothProfile.STATE_CONNECTED) return
                throw IOException("connexion refusée (status ${event.status})")
            } catch (e: Exception) {
                lastError = e
                Log.w(TAG, "Connect attempt ${attempt + 1} failed: ${e.message}")
                gatt?.close()
                gatt = null
                delay(500)
            }
        }
        throw IOException("Connexion BLE impossible", lastError)
    }

    suspend fun discoverServices() {
        val g = gatt ?: error("non connecté")
        delay(300)
        val event = request<GattEvent.ServicesDiscovered>(10_000) { g.discoverServices() }
        if (event.status != BluetoothGatt.GATT_SUCCESS) {
            throw IOException("découverte des services échouée (status ${event.status})")
        }
        val service = g.getService(ZendureBle.SERVICE_UUID)
            ?: throw IOException("service Zendure introuvable")
        writeChar = service.getCharacteristic(ZendureBle.WRITE_CHAR_UUID)
            ?: throw IOException("caractéristique d'écriture introuvable")
        notifyChar = service.getCharacteristic(ZendureBle.NOTIFY_CHAR_UUID)
            ?: throw IOException("caractéristique de notification introuvable")
    }

    /** L'échec de la négociation MTU n'est pas fatal : on retombe sur 23 (chunking). */
    suspend fun negotiateMtu(desired: Int = 512) {
        val g = gatt ?: error("non connecté")
        runCatching { request<GattEvent.MtuChanged>(5_000) { g.requestMtu(desired) } }
            .onFailure { Log.w(TAG, "MTU negotiation failed, keeping $mtu") }
    }

    suspend fun enableNotifications() {
        val g = gatt ?: error("non connecté")
        val c = notifyChar ?: error("services non découverts")
        if (!g.setCharacteristicNotification(c, true)) {
            throw IOException("activation des notifications refusée")
        }
        val descriptor = c.getDescriptor(ZendureBle.CCCD_UUID)
            ?: throw IOException("descripteur CCCD introuvable")
        val enable = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
        val event = request<GattEvent.DescriptorWrite>(5_000) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                g.writeDescriptor(descriptor, enable) == BluetoothStatusCodes.SUCCESS
            } else {
                @Suppress("DEPRECATION")
                descriptor.value = enable
                @Suppress("DEPRECATION")
                g.writeDescriptor(descriptor)
            }
        }
        if (event.status != BluetoothGatt.GATT_SUCCESS) {
            throw IOException("écriture du CCCD échouée (status ${event.status})")
        }
    }

    /** Écrit le payload en chunks de (mtu - 3) octets, en attendant l'ack local entre chunks. */
    suspend fun write(payload: ByteArray) {
        val c = writeChar ?: error("services non découverts")
        val chunkSize = (mtu - 3).coerceAtLeast(20)
        var offset = 0
        while (offset < payload.size) {
            val end = minOf(offset + chunkSize, payload.size)
            val chunk = payload.copyOfRange(offset, end)
            val event = request<GattEvent.CharacteristicWrite>(5_000) { writeChunk(c, chunk) }
            if (event.status != BluetoothGatt.GATT_SUCCESS) {
                throw IOException("écriture GATT échouée (status ${event.status})")
            }
            offset = end
        }
    }

    private fun writeChunk(c: BluetoothGattCharacteristic, chunk: ByteArray): Boolean {
        val g = gatt ?: return false
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            g.writeCharacteristic(
                c, chunk, BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE
            ) == BluetoothStatusCodes.SUCCESS
        } else {
            @Suppress("DEPRECATION")
            c.writeType = BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE
            @Suppress("DEPRECATION")
            c.value = chunk
            @Suppress("DEPRECATION")
            g.writeCharacteristic(c)
        }
    }

    /**
     * Envoie [payload] et attend une réponse JSON complète satisfaisant [predicate].
     * L'attente est abonnée AVANT l'écriture pour ne pas perdre une réponse rapide.
     * Les notifications arrivent fragmentées (MTU) : on accumule, puis on extrait chaque
     * objet JSON complet par équilibrage d'accolades ; les objets non conformes sont ignorés.
     */
    suspend fun writeAndAwaitJson(
        payload: ByteArray,
        timeoutMs: Long,
        predicate: (JSONObject) -> Boolean,
    ): JSONObject = coroutineScope {
        val waiter = async(start = CoroutineStart.UNDISPATCHED) {
            awaitJsonResponse(timeoutMs, predicate)
        }
        write(payload)
        waiter.await()
    }

    private suspend fun awaitJsonResponse(
        timeoutMs: Long,
        predicate: (JSONObject) -> Boolean,
    ): JSONObject = withTimeout(timeoutMs) {
        val buffer = StringBuilder()
        var result: JSONObject? = null
        events.filterIsInstance<GattEvent.Notification>().first { notification ->
            buffer.append(notification.value.toString(Charsets.UTF_8))
            if (buffer.length > 16_384) buffer.setLength(0)
            var found = false
            while (true) {
                val json = extractJson(buffer) ?: break
                Log.d(TAG, "Notification JSON: ${json.optString("method")}")
                if (predicate(json)) {
                    result = json
                    found = true
                    break
                }
            }
            found
        }
        result ?: error("réponse JSON absente")
    }

    /** Attend la déconnexion (signal de succès après l'envoi de "station"). */
    suspend fun awaitDisconnect(timeoutMs: Long): Boolean = runCatching {
        awaitEvent<GattEvent.ConnectionState>(timeoutMs) {
            it.newState == BluetoothProfile.STATE_DISCONNECTED
        }
    }.isSuccess

    fun close() {
        runCatching { gatt?.close() }
        gatt = null
        writeChar = null
        notifyChar = null
    }

    /** Extrait le premier objet JSON complet du buffer (équilibrage d'accolades). */
    private fun extractJson(buffer: StringBuilder): JSONObject? {
        val start = buffer.indexOf("{")
        if (start < 0) {
            buffer.setLength(0)
            return null
        }
        var depth = 0
        for (i in start until buffer.length) {
            when (buffer[i]) {
                '{' -> depth++
                '}' -> {
                    depth--
                    if (depth == 0) {
                        val candidate = buffer.substring(start, i + 1)
                        buffer.delete(0, i + 1)
                        return runCatching { JSONObject(candidate) }.getOrNull()
                    }
                }
            }
        }
        return null
    }

    private suspend inline fun <reified T : GattEvent> awaitEvent(
        timeoutMs: Long,
        crossinline predicate: (T) -> Boolean = { true },
    ): T = withTimeout(timeoutMs) {
        events.filterIsInstance<T>().first { predicate(it) }
    }

    /**
     * Démarre l'attente de l'événement AVANT de déclencher l'opération GATT
     * (UNDISPATCHED : la collecte est active avant que op() ne s'exécute).
     */
    private suspend inline fun <reified T : GattEvent> request(
        timeoutMs: Long,
        crossinline op: () -> Boolean,
    ): T = coroutineScope {
        val waiter = async(start = CoroutineStart.UNDISPATCHED) {
            awaitEvent<T>(timeoutMs)
        }
        if (!op()) {
            waiter.cancel()
            throw IOException("opération GATT refusée par la pile Bluetooth")
        }
        waiter.await()
    }
}
