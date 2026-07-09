package fr.mangi.zendure.model

data class Firmware(val type: String, val version: Int)

data class DeviceInfo(
    val deviceId: String,
    val deviceSn: String?,
    val firmwares: List<Firmware>,
)
