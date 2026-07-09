package fr.mangi.zendure.model

import android.bluetooth.BluetoothDevice

data class DiscoveredDevice(
    val name: String,
    val address: String,
    val rssi: Int,
    val model: ZendureModel?,
    val device: BluetoothDevice,
)
