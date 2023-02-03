package cloud.valetudo.companion

import android.content.Intent
import android.net.ConnectivityManager
import android.net.wifi.WifiManager
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import cloud.valetudo.companion.activities.main.MainActivity
import cloud.valetudo.companion.databinding.ActivityProvisioningBinding
import kotlin.concurrent.thread


class ProvisioningActivity : AppCompatActivity() {
    private lateinit var binding: ActivityProvisioningBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityProvisioningBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)


        var newNetworkId: Int? = null
        var withResult = false

        if (intent.extras != null) {
            try {
                newNetworkId = intent.extras!!["newNetworkId"] as Int?
                withResult = intent.extras!!["withResult"] as Boolean
            } catch (ex: Exception) {
                runOnUiThread {
                    Toast.makeText(
                        this@ProvisioningActivity,
                        "Received invalid intent extras",
                        Toast.LENGTH_LONG
                    ).show()

                    this.finish()
                }

                return
            }
        }


        val wifiManager: WifiManager? = getSystemService(WifiManager::class.java)
        val connectivityManager: ConnectivityManager? =
            getSystemService(ConnectivityManager::class.java)
        val provisioningHelper: ValetudoProvisioningHelper

        if (wifiManager != null && connectivityManager != null) {
            provisioningHelper = ValetudoProvisioningHelper(
                wifiManager,
                connectivityManager
            )
        } else {
            Log.e(
                "provisioningActivity",
                "Unable to create new provisioningHelper due to missing wifi- or connectivityManager"
            )

            runOnUiThread {
                this.finish()
            }

            return
        }

        var foundRobot: DiscoveredUnprovisionedValetudoInstance? = null

        val helpText = binding.noValetudoFoundHint

        val scanButton = binding.scanButton
        val connectButton = binding.connectButton

        val foundRobotLabel = binding.foundRobotLabel
        val provisioningInputs = binding.provisioningInputs

        val ssidInput = binding.inputSsid
        val passwordInput = binding.inputPassword

        fun scanForValetudo() {
            thread {
                val scanResult = provisioningHelper.checkForValetudo()

                if (scanResult != null) {
                    foundRobot = scanResult

                    runOnUiThread {
                        provisioningInputs.visibility = View.VISIBLE
                        helpText.visibility = View.GONE

                        foundRobotLabel.text = resources.getString(
                            R.string.provisioning_found_valetudo,

                            scanResult.manufacturer,
                            scanResult.model
                        )
                    }
                } else {
                    foundRobot = null

                    runOnUiThread {
                        provisioningInputs.visibility = View.INVISIBLE
                        helpText.visibility = View.VISIBLE

                        Toast.makeText(
                            this@ProvisioningActivity,
                            "Scan finished without results",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            }
        }

        scanButton.setOnClickListener {
            scanForValetudo()
        }

        scanForValetudo()

        connectButton.setOnClickListener {
            if (foundRobot != null) {
                thread {
                    runOnUiThread {
                        connectButton.isEnabled = false
                    }

                    val connectResult = provisioningHelper.provisionValetudo(
                        ssidInput.text.toString(),
                        passwordInput.text.toString()
                    )

                    if (connectResult == 200) {

                        @Suppress("DEPRECATION")
                        if (newNetworkId != null) { //This is only != null on android versions <= Q
                            wifiManager.removeNetwork(newNetworkId)
                        }

                        runOnUiThread {
                            Toast.makeText(
                                this@ProvisioningActivity,
                                "Provisioning successful",
                                Toast.LENGTH_LONG
                            ).show()

                            if (!withResult) {
                                val intent = Intent(this, MainActivity::class.java)
                                intent.flags =
                                    Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK

                                startActivity(intent)
                            } else {
                                val returnIntent = Intent()
                                setResult(RESULT_OK, returnIntent)

                                finish()
                            }

                        }
                    } else {
                        runOnUiThread {
                            Toast.makeText(
                                this@ProvisioningActivity,
                                "Wifi Provisioning failed with code $connectResult",
                                Toast.LENGTH_LONG
                            ).show()

                            connectButton.isEnabled = true
                        }
                    }
                }
            } else {
                runOnUiThread {
                    Toast.makeText(
                        this@ProvisioningActivity,
                        "Missing foundRobot",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }
}
