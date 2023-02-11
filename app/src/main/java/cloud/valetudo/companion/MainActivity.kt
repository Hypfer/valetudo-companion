package cloud.valetudo.companion

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.browser.customtabs.CustomTabColorSchemeParams
import androidx.browser.customtabs.CustomTabsIntent
import androidx.core.content.res.ResourcesCompat
import com.google.android.material.floatingactionbutton.FloatingActionButton
import java.util.concurrent.Semaphore
import kotlin.concurrent.thread


class MainActivity : AppCompatActivity() {
    private val logTag = "cloud.valetudo"

    private var mNsdManager : NsdManager? = null
    private var mValetudoInstances = ArrayList<DiscoveredValetudoInstance>()
    private val resolveSemaphore = Semaphore(1)


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val itemsAdapter = DiscoveredValetudoInstancesAdapter(this, R.layout.discovered_instance_list_item_layout, mValetudoInstances)

        setupUIEventHandlers(itemsAdapter)
        enableEgg()
        startDiscovery(itemsAdapter)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        val inflater: MenuInflater = menuInflater
        inflater.inflate(R.menu.main_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_help -> {
                val builder: AlertDialog.Builder = AlertDialog.Builder(this)
                builder.setTitle(R.string.help_dialog_title)
                builder.setMessage(R.string.help_dialog_content)
                builder.setPositiveButton(R.string.dialog_action_ok, null)

                val alert: AlertDialog = builder.create()
                alert.show()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }


    private fun setupUIEventHandlers(itemsAdapter: DiscoveredValetudoInstancesAdapter) {
        val discoveredList = findViewById<ListView>(R.id.discovered_list)
        val provisionButton = findViewById<FloatingActionButton>(R.id.enterProvisioningActivityButton)

        discoveredList.adapter = itemsAdapter


        discoveredList.onItemClickListener = AdapterView.OnItemClickListener { _, _, position, _ ->
            val instance = mValetudoInstances[position]
            val defaultColors = CustomTabColorSchemeParams.Builder()
                .setToolbarColor(ResourcesCompat.getColor(resources, R.color.valetudo_main, null))
                .build()


            /*
                Even though there's nothing in the docs indicating that this may throw,
                it will do exactly that if there is no browser installed
             */
            try {
                CustomTabsIntent.Builder()
                    .setDefaultColorSchemeParams(defaultColors)
                    .setUrlBarHidingEnabled(false)
                    .build()
                    .launchUrl(this, Uri.parse("http://${instance.host}"))
            } catch (e: ActivityNotFoundException) {
                runOnUiThread {
                    Toast.makeText(
                        this@MainActivity,
                        "No http:// intent handler installed.",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }

        }

        discoveredList.onItemLongClickListener =
            AdapterView.OnItemLongClickListener { _, _, position, _ ->
                val instance = mValetudoInstances[position]
                val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse("http://${instance.host}"))

                try {
                    startActivity(browserIntent)
                    return@OnItemLongClickListener true
                } catch (e: ActivityNotFoundException) {
                    runOnUiThread {
                        Toast.makeText(
                            this@MainActivity,
                            "No http:// intent handler installed.",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                    return@OnItemLongClickListener false
                }

            }


        provisionButton.setOnClickListener {
            val provisioningIntent = Intent(this, ProvisioningWizardPageOneActivity::class.java)

            startActivity(provisioningIntent)
        }
    }

    private fun startDiscovery(itemsAdapter: DiscoveredValetudoInstancesAdapter) {
        mNsdManager = (getSystemService(Context.NSD_SERVICE) as NsdManager)

        mNsdManager!!.discoverServices("_valetudo._tcp.", NsdManager.PROTOCOL_DNS_SD, object : NsdManager.DiscoveryListener {
            override fun onStartDiscoveryFailed(serviceType: String?, errorCode: Int) {
                Log.d(logTag, "Start service discovery failed with error code $errorCode")
            }

            override fun onStopDiscoveryFailed(serviceType: String?, errorCode: Int) {
                Log.d(logTag, "Stop service discovery failed with error code $errorCode")
            }

            override fun onDiscoveryStarted(serviceType: String?) {
                Log.d(logTag, "Service discovery started")
            }

            override fun onDiscoveryStopped(serviceType: String?) {
                Log.d(logTag, "Service discovery stopped successfully")
            }

            override fun onServiceFound(serviceInfo: NsdServiceInfo?) {
                Log.d(logTag, "Service discovery success $serviceInfo")

                if (serviceInfo != null) {
                    tryResolve(serviceInfo, itemsAdapter)
                } else {
                    Log.d(logTag, "ServiceInfo is null")
                }

            }

            override fun onServiceLost(serviceInfo: NsdServiceInfo?) {
                Log.d(logTag, "Service lost $serviceInfo")
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


    private fun addDiscoveredDevice(newInstance: DiscoveredValetudoInstance, itemsAdapter: DiscoveredValetudoInstancesAdapter) {
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

            mValetudoInstances.sortBy { it.id }

            itemsAdapter.notifyDataSetChanged()
        }
    }

    private fun tryResolve(serviceInfo: NsdServiceInfo, itemsAdapter: DiscoveredValetudoInstancesAdapter) {
        thread {
            resolveSemaphore.acquire()

            mNsdManager!!.resolveService(serviceInfo, object : NsdManager.ResolveListener {
                override fun onResolveFailed(serviceInfo: NsdServiceInfo?, errorCode: Int) {
                    resolveSemaphore.release()

                    Log.d(logTag, "Service resolve failed $serviceInfo Error Code: $errorCode")
                }

                override fun onServiceResolved(serviceInfo: NsdServiceInfo?) {
                    resolveSemaphore.release()

                    if (itemsAdapter.isEmpty) {
                        runOnUiThread {
                            val mainText = findViewById<TextView>(R.id.main_text)
                            val helpText = findViewById<TextView>(R.id.help_text)

                            mainText.text = resources.getString(R.string.found_devices)
                            helpText.visibility = View.GONE
                        }
                    }


                    Log.d(logTag, "Service resolve success $serviceInfo")

                    val serviceName = serviceInfo!!.serviceName ?: ""
                    val id = serviceInfo.attributes["id"]

                    val manufacturer = String(serviceInfo.attributes["manufacturer"] ?: byteArrayOf())
                    val model = String(serviceInfo.attributes["model"] ?: byteArrayOf())
                    val version = String(serviceInfo.attributes["version"] ?: byteArrayOf())
                    val name = String(serviceInfo.attributes["name"] ?: byteArrayOf())
                    val hostAddress = serviceInfo.host!!.hostAddress


                    if(id != null) {
                        addDiscoveredDevice(DiscoveredValetudoInstance(
                            String(id),
                            model,
                            manufacturer,
                            version,
                            hostAddress ?: "",
                            serviceName,
                            name
                        ), itemsAdapter)
                    }

                }
            })
        }
    }

    private fun enableEgg() {
        val icon = findViewById<ImageView>(R.id.valetudo_logo)
        var iconClicks = 0

        if ((1..100).random() == 42) {
            icon.setImageResource(R.drawable.ic_valetudog)
        }

        icon.setOnClickListener {
            if (iconClicks == 9) {
                icon.setImageResource(R.drawable.ic_valetudog)
            } else {
                iconClicks++
            }
        }
    }
}