package cloud.valetudo.companion

import android.content.Context
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import kotlin.collections.ArrayList
import android.content.Intent
import android.net.Uri
import android.view.View
import android.widget.*
import java.util.concurrent.Semaphore
import kotlin.collections.HashMap
import kotlin.concurrent.thread


class MainActivity : AppCompatActivity() {
    private val TAG = "cloud.valetudo"

    private var mNsdManager : NsdManager? = null

    private var mValetudoDevices = HashMap<String, String>()
    private var mValetudoDeviceDescriptions = ArrayList<String>()


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val mainText = findViewById<TextView>(R.id.main_text)
        val listLayout = findViewById<LinearLayout>(R.id.list_layout)

        val itemsAdapter = ArrayAdapter(this, R.layout.list_item_layout, mValetudoDeviceDescriptions)
        val discoveredList = findViewById<ListView>(R.id.discovered_list)
        discoveredList.adapter = itemsAdapter
        discoveredList.onItemClickListener = AdapterView.OnItemClickListener { _, _, position, _ ->
            val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse("http://${mValetudoDevices[mValetudoDeviceDescriptions[position]]}"))

            startActivity(browserIntent)
        }

        fun addDiscoveredDevice(deviceDescription: String, hostAddress: String ) {
            runOnUiThread {
                mValetudoDevices[deviceDescription] = hostAddress
                mValetudoDeviceDescriptions.add(deviceDescription)

                itemsAdapter.notifyDataSetChanged();
            }
        }

        val resolveSemaphore = Semaphore(1)
        fun tryResolve(serviceInfo: NsdServiceInfo) {
            thread {
                resolveSemaphore.acquire()

                mNsdManager!!.resolveService(serviceInfo, object : NsdManager.ResolveListener {
                    override fun onResolveFailed(serviceInfo: NsdServiceInfo?, errorCode: Int) {
                        resolveSemaphore.release();

                        Log.d(TAG, "Service resolve failed $serviceInfo Error Code: $errorCode")
                    }

                    override fun onServiceResolved(serviceInfo: NsdServiceInfo?) {
                        resolveSemaphore.release()

                        runOnUiThread {
                            mainText.visibility = View.GONE
                            listLayout.visibility = View.VISIBLE
                        }

                        Log.d(TAG, "Service resolve success $serviceInfo")

                        var deviceLabel = serviceInfo!!.serviceName

                        val vendor = serviceInfo.attributes.get("manufacturer");
                        val model = serviceInfo.attributes.get("model");

                        if(vendor != null && model != null) {
                            deviceLabel = "${String(vendor)} ${String(model)}"
                        }


                        val deviceDescription = "$deviceLabel (${serviceInfo.host.hostAddress})"

                        addDiscoveredDevice(deviceDescription, serviceInfo.host.hostAddress);
                    }
                })
            }
        }


        mNsdManager = (getSystemService(Context.NSD_SERVICE) as NsdManager)

        mNsdManager!!.discoverServices("_valetudo._tcp.", NsdManager.PROTOCOL_DNS_SD, object : NsdManager.DiscoveryListener {
            override fun onStartDiscoveryFailed(serviceType: String?, errorCode: Int) {}

            override fun onStopDiscoveryFailed(serviceType: String?, errorCode: Int) {}

            override fun onDiscoveryStarted(serviceType: String?) {
                Log.d(TAG, "Service discovery started")
            }

            override fun onDiscoveryStopped(serviceType: String?) {}

            override fun onServiceFound(serviceInfo: NsdServiceInfo?) {
                Log.d(TAG, "Service discovery success $serviceInfo")

                if (serviceInfo != null) {
                    tryResolve(serviceInfo);
                } else {
                    Log.d(TAG, "ServiceInfo is null")
                }

            }

            override fun onServiceLost(serviceInfo: NsdServiceInfo?) {
                Log.d(TAG, "Service lost $serviceInfo")
            }
        })
    }
}