package cloud.valetudo.companion

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.*
import android.net.ConnectivityManager.NetworkCallback
import android.net.wifi.ScanResult
import android.net.wifi.WifiManager
import android.net.wifi.WifiNetworkSpecifier
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import cloud.valetudo.companion.activities.main.MainActivity
import cloud.valetudo.companion.databinding.ActivityProvisioningPage2Binding


class ProvisioningWizardPageTwoActivity : AppCompatActivity() {
    private var mConnectivityManager: ConnectivityManager? = null
    private var mNetworkCallback: NetworkCallback? = null

    private lateinit var binding: ActivityProvisioningPage2Binding

    @Suppress("DEPRECATION")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (mNetworkCallback != null && mConnectivityManager != null) {
            try {
                mConnectivityManager!!.unregisterNetworkCallback(mNetworkCallback!!)
            } catch (ex: Exception) {
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

        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            // Return to the previous activity if the user revoked the permission while we were in the background
            runOnUiThread {
                this.finish()
            }

            return
        }

        binding = ActivityProvisioningPage2Binding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)

        val helpText = binding.noSsidsFoundHint

        val discoveredList = binding.wizardPage2WifiNetworkList

        val wifiManager: WifiManager? = getSystemService(WifiManager::class.java)
        mConnectivityManager = getSystemService(ConnectivityManager::class.java)

        if (wifiManager == null || mConnectivityManager == null) {
            Log.e("ProvisioningWizardPageTwoActivity", "Missing wifi- or connectivityManager")

            runOnUiThread {
                this.finish()
            }

            return
        }

        fun navigateToProvisioningActivity(newNetworkId: Int? = null, withResult: Boolean = false) {
            val provisioningIntent = Intent(this, ProvisioningActivity::class.java)

            if (newNetworkId != null) {
                provisioningIntent.putExtra("newNetworkId", newNetworkId)
            }

            provisioningIntent.putExtra("withResult", withResult)

            @Suppress("DEPRECATION")
            if (withResult) {
                startActivityForResult(provisioningIntent, 5)
            } else {
                startActivity(provisioningIntent)
            }


        }

        val clickListener = { result: ScanResult ->
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
                        Log.d(
                            "ProvisioningWizardPageTwoActivity",
                            "requestNetwork onCapabilitiesChanged()"
                        )
                    }

                    override fun onLinkPropertiesChanged(
                        network: Network,
                        linkProperties: LinkProperties
                    ) {
                        Log.d(
                            "ProvisioningWizardPageTwoActivity",
                            "requestNetwork onLinkPropertiesChanged()"
                        )
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

        val itemsAdapter = ScanResultAdapter(clickListener)
        discoveredList.adapter = itemsAdapter

        fun updateScanResults() {
            val results = wifiManager.scanResults
            val filteredResults = results.filter {
                it.SSID.startsWith("roborock-vacuum-") ||
                        it.SSID.startsWith("rockrobo-vacuum-") ||
                        it.SSID.startsWith("viomi-vacuum-") ||
                        it.SSID.startsWith("dreame-vacuum-")
            }

            runOnUiThread {
                itemsAdapter.scanResults = results

                if (filteredResults.isNotEmpty()) {
                    helpText.visibility = View.GONE
                } else {
                    helpText.visibility = View.VISIBLE
                }
            }

        }

        @Suppress("DEPRECATION")
        binding.wizardPage2ScanButton.setOnClickListener {
            wifiManager.startScan()
            updateScanResults()
        }

        updateScanResults()
    }
}
