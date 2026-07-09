package fr.mangi.zendure

import fr.mangi.zendure.ble.ZendureCommands
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ZendureCommandsTest {

    @Test
    fun `payload token contient les champs attendus`() {
        val payload = ZendureCommands.tokenPayload(
            iotUrl = "192.168.1.245",
            ssid = "MonReseau",
            password = "Secret!",
            timeZone = "GMT+02:00",
        )
        val json = JSONObject(payload.toString(Charsets.UTF_8))
        assertEquals("192.168.1.245", json.getString("iotUrl"))
        assertEquals("1002", json.getString("messageId"))
        assertEquals("token", json.getString("method"))
        assertEquals("Secret!", json.getString("password"))
        assertEquals("MonReseau", json.getString("ssid"))
        assertEquals("GMT+02:00", json.getString("timeZone"))
        assertEquals("ababcdefgh", json.getString("token"))
    }

    @Test
    fun `payload station est minimal`() {
        val json = JSONObject(ZendureCommands.stationPayload().toString(Charsets.UTF_8))
        assertEquals("1003", json.getString("messageId"))
        assertEquals("station", json.getString("method"))
        assertEquals(2, json.length())
    }

    @Test
    fun `payload getInfo`() {
        val json = JSONObject(
            ZendureCommands.getInfoPayload(1691699019L).toString(Charsets.UTF_8)
        )
        assertEquals("none", json.getString("messageId"))
        assertEquals("getInfo", json.getString("method"))
        assertEquals(1691699019L, json.getLong("timestamp"))
    }

    @Test
    fun `parse d'une reponse getInfo complete`() {
        val response = JSONObject(
            """
            {"messageId":"123","method":"getInfo-rsp","deviceId":"5ak8yGU7","timestamp":0,
             "deviceSn":"PO1HLC9LDR01938",
             "firmwares":[{"type":"MASTER","version":8220},{"type":"BMS","version":8200}]}
            """.trimIndent()
        )
        val info = ZendureCommands.parseGetInfoResponse(response)!!
        assertEquals("5ak8yGU7", info.deviceId)
        assertEquals("PO1HLC9LDR01938", info.deviceSn)
        assertEquals(2, info.firmwares.size)
        assertEquals("MASTER", info.firmwares[0].type)
        assertEquals(8220, info.firmwares[0].version)
    }

    @Test
    fun `parse rejette les autres methodes`() {
        assertNull(ZendureCommands.parseGetInfoResponse(JSONObject("""{"method":"report"}""")))
    }

    @Test
    fun `parse rejette une reponse sans deviceId`() {
        assertNull(ZendureCommands.parseGetInfoResponse(JSONObject("""{"method":"getInfo-rsp"}""")))
    }
}
