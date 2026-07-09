# Zendure Cloud Disconnector — Android

Android app to disconnect **Zendure SolarFlow** devices from the Zendure cloud and point them to your **own local MQTT broker**, entirely over **Bluetooth LE** — no PC required. It can also restore the factory cloud connection at any time.

This is the Android counterpart of the Windows tool [nograx/zendure-cloud-disconnector](https://github.com/nograx/zendure-cloud-disconnector) and the Python tool [reinhard-brandstaedter/solarflow-bt-manager](https://github.com/reinhard-brandstaedter/solarflow-bt-manager).

## Supported devices

| Device | BLE name prefix | Product key |
|---|---|---|
| SolarFlow Hub 1200 | `zenp` | `73bkTV` |
| SolarFlow Hub 2000 | `zenh` | `A8yh63` |
| AIO 2400 | `zenr` | `yWF7hV` |
| Hyper 2000 | `zene` | `ja72U0ha` |
| ACE 1500 | `zenf` | `8bM93H` |

> Newer devices (SolarFlow 800 / 800 Pro / 2400 AC) don't need this app: the official Zendure app lets you configure a local MQTT broker directly (disable HEMS first).

## Requirements

- Android 7.0+ (Android 12+ recommended), Bluetooth LE
- A local MQTT broker (e.g. Mosquitto) that **accepts anonymous connections** — the device sends no credentials:

  ```
  # mosquitto.conf
  listener 1883 0.0.0.0
  allow_anonymous true
  ```

- A **2.4 GHz** Wi-Fi network (these devices don't support 5 GHz)

## Usage

1. **Close the official Zendure app on every phone.** The device only advertises over Bluetooth when nothing is connected to it.
2. Open the app and pick your device from the scan list.
3. Enter your Wi-Fi SSID/password and the IP of your MQTT broker (port 1883 implied), then tap **Apply**.
4. The device reboots (~30 s) and starts publishing to your broker. The app shows the device ID and ready-to-copy MQTT topics:
   - Telemetry: `/<productKey>/<deviceId>/properties/report`
   - Commands: `iot/<productKey>/<deviceId>/properties/write`
5. Verify from your computer:

   ```
   mosquitto_sub -h <broker-ip> -v -t '/<productKey>/#'
   ```

To go back to stock behaviour, use the **Restore cloud** mode: it reconfigures the device to the Zendure cloud broker (`mq.zen-iot.com`) and it reappears in the official app after a few minutes.

**AIO 2400 note:** press the connection button for ~3 s to reset the network connection before provisioning.

## Integrations

Once the device publishes to your broker, you can consume the data with:

- [ioBroker.zendure-solarflow](https://github.com/nograx/ioBroker.zendure-solarflow) (local MQTT mode)
- [Home Assistant MQTT integration](https://www.home-assistant.io/integrations/mqtt/) with manual sensors, or [z-master42/solarflow](https://github.com/z-master42/solarflow) as a starting point
- [solarflow-control](https://github.com/reinhard-brandstaedter/solarflow-control) for smart output control

## How it works

The device exposes a BLE GATT service (`0xA002`) with a write characteristic (`0xC304`) and a notify characteristic (`0xC305`). The app writes two JSON payloads:

```json
{"iotUrl":"<broker>","messageId":"1002","method":"token","password":"<wifi>","ssid":"<ssid>","timeZone":"GMT+02:00","token":"ababcdefgh"}
```

```json
{"messageId":"1003","method":"station"}
```

The device then reboots, joins your Wi-Fi and connects to the configured broker. Protocol documented by [epicRE/zendure_ble](https://github.com/epicRE/zendure_ble). Note that the BLE interface has **no authentication** — anyone in range can reconfigure the device.

## Building

```bash
./gradlew assembleDebug        # debug APK
./gradlew testDebugUnitTest    # unit tests
```

Release builds are signed via environment variables (`KEYSTORE_FILE`, `KEYSTORE_PASSWORD`, `KEY_ALIAS`, `KEY_PASSWORD`); see `.github/workflows/release.yml`. Pushing a `v*` tag builds and attaches the signed APK to a GitHub release.

## Disclaimer

Not affiliated with Zendure. Reconfiguring your device disconnects it from the official app (telemetry, schedules, and firmware updates stop working until you restore the cloud connection). Use at your own risk.

## Credits

- [nograx](https://github.com/nograx/zendure-cloud-disconnector) — original Windows tool
- [reinhard-brandstaedter](https://github.com/reinhard-brandstaedter/solarflow-bt-manager) — original Python implementation
- [epicRE](https://github.com/epicRE/zendure_ble) — BLE protocol documentation

## License

[MIT](LICENSE)
