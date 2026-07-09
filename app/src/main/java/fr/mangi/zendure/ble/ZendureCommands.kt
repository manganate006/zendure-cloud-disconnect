package fr.mangi.zendure.ble

import fr.mangi.zendure.model.DeviceInfo
import fr.mangi.zendure.model.Firmware
import org.json.JSONObject

/**
 * Payloads JSON du protocole Zendure, tels qu'observés dans zendure-cloud-disconnector (C#)
 * et solarflow-bt-manager (Python). Les messageId "1002"/"1003" et le token "ababcdefgh"
 * sont des valeurs fixes attendues par le firmware.
 */
object ZendureCommands {

    fun tokenPayload(iotUrl: String, ssid: String, password: String, timeZone: String): ByteArray =
        JSONObject()
            .put("iotUrl", iotUrl)
            .put("messageId", "1002")
            .put("method", "token")
            .put("password", password)
            .put("ssid", ssid)
            .put("timeZone", timeZone)
            .put("token", "ababcdefgh")
            .toString()
            .toByteArray(Charsets.UTF_8)

    fun stationPayload(): ByteArray =
        JSONObject()
            .put("messageId", "1003")
            .put("method", "station")
            .toString()
            .toByteArray(Charsets.UTF_8)

    fun getInfoPayload(timestampSeconds: Long): ByteArray =
        JSONObject()
            .put("messageId", "none")
            .put("method", "getInfo")
            .put("timestamp", timestampSeconds)
            .toString()
            .toByteArray(Charsets.UTF_8)

    fun isGetInfoResponse(json: JSONObject): Boolean =
        json.optString("method") == "getInfo-rsp"

    fun parseGetInfoResponse(json: JSONObject): DeviceInfo? {
        if (!isGetInfoResponse(json)) return null
        val deviceId = json.optString("deviceId").takeIf { it.isNotBlank() } ?: return null
        val firmwares = buildList {
            val array = json.optJSONArray("firmwares") ?: return@buildList
            for (i in 0 until array.length()) {
                val fw = array.optJSONObject(i) ?: continue
                add(Firmware(fw.optString("type"), fw.optInt("version")))
            }
        }
        return DeviceInfo(
            deviceId = deviceId,
            deviceSn = json.optString("deviceSn").takeIf { it.isNotBlank() },
            firmwares = firmwares,
        )
    }
}
