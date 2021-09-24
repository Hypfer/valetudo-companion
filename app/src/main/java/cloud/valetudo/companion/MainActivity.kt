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
import com.google.android.material.floatingactionbutton.FloatingActionButton
import java.util.concurrent.Semaphore
import kotlin.concurrent.thread




class MainActivity : AppCompatActivity() {
    private val TAG = "cloud.valetudo"

    private var mNsdManager : NsdManager? = null
    private var mValetudoInstances = ArrayList<DiscoveredValetudoInstance>()
    private val resolveSemaphore = Semaphore(1)


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val mainText = findViewById<TextView>(R.id.main_text)
        val helpText = findViewById<TextView>(R.id.help_text)
        val provisionButton = findViewById<FloatingActionButton>(R.id.enterProvisioningActivityButton)
        val listLayout = findViewById<LinearLayout>(R.id.list_layout)

        val itemsAdapter = DiscoveredValetudoInstancesAdapter(this, R.layout.discovered_instance_list_item_layout, mValetudoInstances)

        val discoveredList = findViewById<ListView>(R.id.discovered_list)
        discoveredList.adapter = itemsAdapter

        discoveredList.onItemClickListener = AdapterView.OnItemClickListener { _, _, position, _ ->
            val instance = mValetudoInstances[position]
            val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse("http://${instance.host}"))

            startActivity(browserIntent)
        }

        provisionButton.setOnClickListener {
            val provisioningIntent = Intent(this, ProvisioningActivity::class.java)

            startActivity(provisioningIntent)
        }

        fun addDiscoveredDevice(newInstance: DiscoveredValetudoInstance) {
            val oldInstance: DiscoveredValetudoInstance? = mValetudoInstances.find {it.id == newInstance.id}
            var idx: Int = -1

            if (oldInstance != null) {
                idx = mValetudoInstances.indexOf(oldInstance)
            }

            runOnUiThread {
                if (idx > -1) {
                    mValetudoInstances[idx] = newInstance
                } else {
                    mValetudoInstances.add(newInstance)
                }

                itemsAdapter.notifyDataSetChanged()
            }
        }

        fun tryResolve(serviceInfo: NsdServiceInfo) {
            thread {
                resolveSemaphore.acquire()

                mNsdManager!!.resolveService(serviceInfo, object : NsdManager.ResolveListener {
                    override fun onResolveFailed(serviceInfo: NsdServiceInfo?, errorCode: Int) {
                        resolveSemaphore.release()

                        Log.d(TAG, "Service resolve failed $serviceInfo Error Code: $errorCode")
                    }

                    override fun onServiceResolved(serviceInfo: NsdServiceInfo?) {
                        resolveSemaphore.release()

                        runOnUiThread {
                            mainText.text = resources.getString(R.string.found_devices)
                            helpText.visibility = View.GONE
                            listLayout.visibility = View.VISIBLE
                        }

                        Log.d(TAG, "Service resolve success $serviceInfo")

                        val serviceName = serviceInfo!!.serviceName ?: ""
                        val id = serviceInfo.attributes.get("id")

                        val manufacturer = String(serviceInfo.attributes.get("manufacturer")?: byteArrayOf())
                        val model = String(serviceInfo.attributes.get("model") ?: byteArrayOf())
                        val version = String(serviceInfo.attributes.get("version") ?: byteArrayOf())
                        val hostAddress = serviceInfo.host.hostAddress


                        if(id != null) {
                            addDiscoveredDevice(DiscoveredValetudoInstance(
                                String(id),
                                model,
                                manufacturer,
                                version,
                                hostAddress,
                                serviceName
                            ))
                        }

                    }
                })
            }
        }


        mNsdManager = (getSystemService(Context.NSD_SERVICE) as NsdManager)

        mNsdManager!!.discoverServices("_valetudo._tcp.", NsdManager.PROTOCOL_DNS_SD, object : NsdManager.DiscoveryListener {
            override fun onStartDiscoveryFailed(serviceType: String?, errorCode: Int) {
                Log.d(TAG, "Start service discovery failed with error code $errorCode")
            }

            override fun onStopDiscoveryFailed(serviceType: String?, errorCode: Int) {
                Log.d(TAG, "Stop service discovery failed with error code $errorCode")
            }

            override fun onDiscoveryStarted(serviceType: String?) {
                Log.d(TAG, "Service discovery started")
            }

            override fun onDiscoveryStopped(serviceType: String?) {
                Log.d(TAG, "Service discovery stopped successfully")
            }

            override fun onServiceFound(serviceInfo: NsdServiceInfo?) {
                Log.d(TAG, "Service discovery success $serviceInfo")

                if (serviceInfo != null) {
                    tryResolve(serviceInfo)
                } else {
                    Log.d(TAG, "ServiceInfo is null")
                }

            }

            override fun onServiceLost(serviceInfo: NsdServiceInfo?) {
                Log.d(TAG, "Service lost $serviceInfo")
                /*
                    It shall be noted that this is useless because there's no way to find out
                    which service the serviceInfo belongs to since there's no unique ID or similar

                    Because of that, we'll just keep results indefinitely and check if they already
                    exist on adding a new one in which case we'll replace the old one based on
                    the unique system ID provided by Valetudo in the id TXT record
                 */
            }
        })
    }
}