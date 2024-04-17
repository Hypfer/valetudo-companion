package cloud.valetudo.companion.data

import android.net.Uri
import android.net.nsd.NsdServiceInfo
import org.json.JSONObject

sealed class DiscoveredValetudoInstance {
    abstract val model: String
    abstract val manufacturer: String
    abstract val valetudoVersion: String
    abstract val host: String
    override fun toString(): String = "$manufacturer $model ($host)"

    val hostUri: Uri by lazy {
        Uri.parse("http://$host")
    }

    data class Unprovisioned(
        override val model: String,
        override val manufacturer: String,
        override val valetudoVersion: String,
        override val host: String
    ) : DiscoveredValetudoInstance() {
        companion object {
            fun fromJsonWithHost(json: JSONObject, host: String): Unprovisioned {
                return Unprovisioned(
                    json.getString("modelName"),
                    json.getString("manufacturer"),
                    json.getString("release"),
                    host
                )
            }
        }
    }

    data class Provisioned(
        val id: String,
        val serviceName: String,
        val name: String,
        override val model: String,
        override val manufacturer: String,
        override val valetudoVersion: String,
        override val host: String,
    ) : DiscoveredValetudoInstance() {
        companion object {
            fun fromServiceInfo(serviceInfo: NsdServiceInfo): Provisioned {
                return Provisioned(
                    id = String(serviceInfo.attributes["id"] ?: byteArrayOf()),
                    model = String(serviceInfo.attributes["model"] ?: byteArrayOf()),
                    manufacturer = String(serviceInfo.attributes["manufacturer"] ?: byteArrayOf()),
                    valetudoVersion = String(serviceInfo.attributes["version"] ?: byteArrayOf()),
                    host = serviceInfo.host?.hostAddress ?: "",
                    serviceName = serviceInfo.serviceName,
                    name = String(serviceInfo.attributes["name"] ?: byteArrayOf())
                )
            }
        }
    }
}
