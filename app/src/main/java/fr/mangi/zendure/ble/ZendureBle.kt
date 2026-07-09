package fr.mangi.zendure.ble

import java.util.UUID

object ZendureBle {
    val SERVICE_UUID: UUID = UUID.fromString("0000a002-0000-1000-8000-00805f9b34fb")
    val WRITE_CHAR_UUID: UUID = UUID.fromString("0000c304-0000-1000-8000-00805f9b34fb")
    val NOTIFY_CHAR_UUID: UUID = UUID.fromString("0000c305-0000-1000-8000-00805f9b34fb")
    val CCCD_UUID: UUID = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb")

    /** Broker MQTT du cloud Zendure (état d'usine). */
    const val CLOUD_BROKER = "mq.zen-iot.com"
}
