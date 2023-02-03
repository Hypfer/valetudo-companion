package cloud.valetudo.companion.repositories

import android.content.Context
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import cloud.valetudo.companion.activities.main.DiscoveredValetudoInstance
import cloud.valetudo.companion.services.nsd.NsdService
import cloud.valetudo.companion.services.nsd.NsdService.DiscoveryEvent
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.scan

class ValetudoInstancesRepository(
    nsdService: NsdService
) {
    val valetudoInstances =
        nsdService.startDiscovery(VALETUDO_SERVICE, NsdManager.PROTOCOL_DNS_SD)
            .scan(setOf<NsdServiceInfo>()) { instances, value ->
                when (value) {
                    is DiscoveryEvent.ServiceFound ->
                        instances.addByName(value.service)
                    is DiscoveryEvent.ServiceLost ->
                        instances.removeByName(value.service)
                }
            }.map { serviceInfos ->
                serviceInfos
                    .mapNotNull { nsdServiceInfo -> nsdService.resolveService(nsdServiceInfo) }
                    .map { DiscoveredValetudoInstance.fromServiceInfo(it) }
            }

    companion object {
        const val VALETUDO_SERVICE = "_valetudo._tcp."

        fun fromContext(context: Context): ValetudoInstancesRepository {
            val nsdService =
                NsdService.fromContext(context) ?: throw Exception("Failed to get NsdService")

            return ValetudoInstancesRepository(nsdService)
        }

        private fun Set<NsdServiceInfo>.addByName(new: NsdServiceInfo): Set<NsdServiceInfo> {
            if (this.none { it.serviceName == new.serviceName }) {
                return this + new
            }
            return this
        }

        private fun Set<NsdServiceInfo>.removeByName(new: NsdServiceInfo): Set<NsdServiceInfo> {
            val target = this.find { it.serviceName == new.serviceName }
            if (target != null) {
                return this - target
            }
            return this
        }
    }
}
