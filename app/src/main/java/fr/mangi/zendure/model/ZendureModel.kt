package fr.mangi.zendure.model

/**
 * Modèles Zendure supportés, identifiés par le préfixe de leur nom d'advertising BLE.
 * Le productKey est fixe par modèle et sert à construire les topics MQTT.
 */
enum class ZendureModel(val blePrefix: String, val productKey: String, val label: String) {
    HUB_1200("zenp", "73bkTV", "SolarFlow Hub 1200"),
    HUB_2000("zenh", "A8yh63", "SolarFlow Hub 2000"),
    AIO_2400("zenr", "yWF7hV", "AIO 2400"),
    HYPER_2000("zene", "ja72U0ha", "Hyper 2000"),
    ACE_1500("zenf", "8bM93H", "ACE 1500");

    companion object {
        private const val GENERIC_PREFIX = "zen"

        fun isZendure(name: String?): Boolean =
            name?.startsWith(GENERIC_PREFIX, ignoreCase = true) == true

        fun fromBleName(name: String?): ZendureModel? {
            val lower = name?.lowercase() ?: return null
            return entries.firstOrNull { lower.startsWith(it.blePrefix) }
        }
    }
}
