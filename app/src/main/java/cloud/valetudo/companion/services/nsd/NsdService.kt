package cloud.valetudo.companion.services.nsd

import android.content.Context
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import androidx.core.content.ContextCompat.getSystemService
import kotlinx.coroutines.channels.ProducerScope
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.channels.trySendBlocking
import kotlinx.coroutines.flow.callbackFlow
import kotlin.coroutines.Continuation
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class NsdService(private val nsdManager: NsdManager) {
    fun startDiscovery(serviceType: String, protocolType: Int) = callbackFlow {
        val listener = FlowDiscoveryListener(this)

        nsdManager.discoverServices(serviceType, protocolType, listener)

        awaitClose {
            nsdManager.stopServiceDiscovery(listener)
        }
    }

    internal class FlowDiscoveryListener(
        private val producer: ProducerScope<DiscoveryEvent>
    ) : NsdManager.DiscoveryListener {
        override fun onStartDiscoveryFailed(serviceType: String?, p1: Int) {
            producer.channel.close()
        }

        override fun onStopDiscoveryFailed(serviceType: String, p1: Int) {
            producer.channel.close()
        }

        override fun onDiscoveryStarted(serviceType: String) {

        }

        override fun onDiscoveryStopped(serviceType: String) {

        }

        override fun onServiceFound(service: NsdServiceInfo) {
            producer.channel.trySendBlocking(DiscoveryEvent.ServiceFound(service))
        }

        override fun onServiceLost(service: NsdServiceInfo) {
            producer.channel.trySendBlocking(DiscoveryEvent.ServiceLost(service))
        }

    }

    suspend fun resolveService(serviceInfo: NsdServiceInfo) = suspendCoroutine {
        nsdManager.resolveService(serviceInfo, SuspendResolveListener(it))
    }

    internal class SuspendResolveListener(
        private val continuation: Continuation<NsdServiceInfo?>
    ) : NsdManager.ResolveListener {
        override fun onResolveFailed(serviceInfo: NsdServiceInfo, errorCode: Int) {
            continuation.resume(null)
        }

        override fun onServiceResolved(serviceInfo: NsdServiceInfo) {
            continuation.resume(serviceInfo)
        }
    }

    sealed class DiscoveryEvent {
        data class ServiceFound(val service: NsdServiceInfo) : DiscoveryEvent()
        data class ServiceLost(val service: NsdServiceInfo) : DiscoveryEvent()
    }

    companion object {
        fun fromContext(context: Context): NsdService? =
            getSystemService(context, NsdManager::class.java)?.let { NsdService(it) }

    }
}
