package cloud.valetudo.companion.activities.main

import android.net.Uri
import android.net.nsd.NsdServiceInfo

data class DiscoveredValetudoInstance(
    val id: String,
    val model: String,
    val manufacturer: String,
    val valetudoVersion: String,
    val host: String,
    val serviceName: String
) {
    val hostUri: Uri by lazy {
        Uri.parse("http://$host")
    }

    override fun toString(): String = "$manufacturer $model ($host)"

    companion object {
        fun fromServiceInfo(serviceInfo: NsdServiceInfo): DiscoveredValetudoInstance {
            return DiscoveredValetudoInstance(
                id = String(serviceInfo.attributes["id"] ?: byteArrayOf()),
                model = String(serviceInfo.attributes["model"] ?: byteArrayOf()),
                manufacturer = String(serviceInfo.attributes["manufacturer"] ?: byteArrayOf()),
                valetudoVersion = String(serviceInfo.attributes["version"] ?: byteArrayOf()),
                host = serviceInfo.host?.hostAddress ?: "",
                serviceName = serviceInfo.serviceName
            )
        }
    }
}
