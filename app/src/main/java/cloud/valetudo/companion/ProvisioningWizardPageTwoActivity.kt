package cloud.valetudo.companion

import android.content.Intent
import android.net.*
import android.net.wifi.ScanResult
import android.net.wifi.WifiManager
import android.net.wifi.WifiNetworkSpecifier
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.AdapterView
import android.widget.Button
import android.widget.ListView
import androidx.appcompat.app.AppCompatActivity
import android.net.Network
import android.net.LinkProperties
import android.net.NetworkCapabilities
import android.net.ConnectivityManager
import android.net.ConnectivityManager.NetworkCallback
import android.view.View
import android.widget.TextView
import java.lang.Exception
import kotlin.collections.ArrayList


class ProvisioningWizardPageTwoActivity: AppCompatActivity() {

    private var mRobotSSIDs = ArrayList<ScanResult>()
    private var mConnectivityManager: ConnectivityManager? = null
    private var mNetworkCallback : NetworkCallback? = null

    @Suppress("DEPRECATION")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (mNetworkCallback != null && mConnectivityManager != null) {
            try {
                mConnectivityManager!!.unregisterNetworkCallback(mNetworkCallback!!)
            } catch(ex: Exception) {
                Log.e("ProvisioningWizardPageTwoActivity", ex.toString())
            }
        }

        if (requestCode == 5 && resultCode == RESULT_OK) {
            val intent = Intent(this, MainActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK

            startActivity(intent)
        }
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_provisioning_page2)

        val helpText = findViewById<TextView>(R.id.no_ssids_found_hint)

        val discoveredList = findViewById<ListView>(R.id.wizard_page_2_wifi_network_list)
        val itemsAdapter = DiscoveredAPsAdapter(this, R.layout.discovered_ap_list_item_layout, mRobotSSIDs)
        discoveredList.adapter = itemsAdapter


        val wifiManager: WifiManager? = getSystemService(WifiManager::class.java)
        mConnectivityManager = getSystemService(ConnectivityManager::class.java)

        if (wifiManager == null || mConnectivityManager == null) {
            Log.e("ProvisioningWizardPageTwoActivity", "Missing wifi- or connectivityManager")

            runOnUiThread {
                this.finish()
            }

            return
        }

        fun updateScanResults() {
            val results = wifiManager.scanResults
            val filteredResults = results.filter {
                it.SSID.startsWith("roborock-vacuum-") ||
                it.SSID.startsWith("viomi-vacuum-") ||
                it.SSID.startsWith("dreame-vacuum-")
            }

            runOnUiThread {
                mRobotSSIDs.clear()
                mRobotSSIDs.addAll(filteredResults)

                if (filteredResults.isNotEmpty()) {
                    helpText.visibility = View.GONE
                } else {
                    helpText.visibility = View.VISIBLE
                }

                itemsAdapter.notifyDataSetChanged()
            }

        }

        updateScanResults()

        val scanButton = findViewById<Button>(R.id.wizard_page_2_scan_button)

        @Suppress("DEPRECATION")
        scanButton.setOnClickListener {
            wifiManager.startScan()

            updateScanResults()
        }

        fun navigateToProvisioningActivity(newNetworkId: Int? = null, withResult: Boolean = false) {
            val provisioningIntent = Intent(this, ProvisioningActivity::class.java)

            if (newNetworkId != null) {
               provisioningIntent.putExtra("newNetworkId", newNetworkId)
            }

            @Suppress("DEPRECATION")
            if (withResult) {
                provisioningIntent.putExtra("withResult", true)

                startActivityForResult(provisioningIntent, 5)
            } else {
                startActivity(provisioningIntent)
            }


        }


        discoveredList.onItemClickListener = AdapterView.OnItemClickListener { _, _, position, _ ->
            val result = mRobotSSIDs[position]

            @Suppress("DEPRECATION")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val builder = WifiNetworkSpecifier.Builder()
                builder.setSsid(result.SSID)

                val wifiNetworkSpecifier = builder.build()
                val networkBuilder = NetworkRequest.Builder()

                networkBuilder.addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
                networkBuilder.setNetworkSpecifier(wifiNetworkSpecifier)

                val networkRequest = networkBuilder.build()

                mNetworkCallback = object : NetworkCallback() {
                    override fun onAvailable(network: Network) {
                        Log.d("ProvisioningWizardPageTwoActivity", "requestNetwork onAvailable()")

                        navigateToProvisioningActivity(null, true)
                    }

                    override fun onCapabilitiesChanged(
                        network: Network,
                        networkCapabilities: NetworkCapabilities
                    ) {
                        Log.d("ProvisioningWizardPageTwoActivity", "requestNetwork onCapabilitiesChanged()")
                    }

                    override fun onLinkPropertiesChanged(
                        network: Network,
                        linkProperties: LinkProperties
                    ) {
                        Log.d("ProvisioningWizardPageTwoActivity", "requestNetwork onLinkPropertiesChanged()")
                    }

                    override fun onLosing(network: Network, maxMsToLive: Int) {
                        Log.d("ProvisioningWizardPageTwoActivity", "requestNetwork onLosing()")
                    }

                    override fun onLost(network: Network) {
                        Log.d("ProvisioningWizardPageTwoActivity", "requestNetwork onLost()")
                    }
                }


                mConnectivityManager!!.requestNetwork(networkRequest, mNetworkCallback!!)
            } else {
                val wifiConfig = android.net.wifi.WifiConfiguration()

                wifiConfig.allowedAuthAlgorithms.clear()
                wifiConfig.allowedGroupCiphers.clear()
                wifiConfig.allowedKeyManagement.clear()
                wifiConfig.allowedPairwiseCiphers.clear()
                wifiConfig.allowedProtocols.clear()

                wifiConfig.SSID = "\"" + result.SSID + "\""
                wifiConfig.allowedKeyManagement.set(android.net.wifi.WifiConfiguration.KeyMgmt.NONE)

                val newNetworkId = wifiManager.addNetwork(wifiConfig)


                wifiManager.saveConfiguration()

                wifiManager.enableNetwork(newNetworkId, true)

                navigateToProvisioningActivity(newNetworkId)
            }
        }
    }
}