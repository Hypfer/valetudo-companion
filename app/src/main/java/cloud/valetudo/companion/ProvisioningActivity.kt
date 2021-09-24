package cloud.valetudo.companion

import android.net.ConnectivityManager
import android.net.wifi.WifiManager
import android.os.Bundle
import android.text.Layout
import android.util.Log
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import kotlin.concurrent.thread

class ProvisioningActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_provisioning)

        val wifiManager: WifiManager? = getSystemService(WifiManager::class.java)
        val connectivityManager: ConnectivityManager? = getSystemService(ConnectivityManager::class.java)
        var provisioningHelper: ValetudoProvisioningHelper? = null

        if (wifiManager != null && connectivityManager != null) {
            provisioningHelper = ValetudoProvisioningHelper(
                wifiManager,
                connectivityManager
            )
        } else {
            Log.e("provisioningActivity", "Unable to create new provisioningHelper due to missing wifi- or connectivityManager")
        }

        var foundRobot : DiscoveredUnprovisionedValetudoInstance? = null

        val scanButton = findViewById<Button>(R.id.scan_button)
        val connectButton = findViewById<Button>(R.id.connect_button)

        val foundRobotLabel = findViewById<TextView>(R.id.found_robot_label)
        val provisioningInputs = findViewById<LinearLayout>(R.id.provisioning_inputs)

        val ssidInput = findViewById<EditText>(R.id.input_ssid)
        val passwordInput = findViewById<EditText>(R.id.input_password)


        scanButton.setOnClickListener {
            if (provisioningHelper != null) {
                thread {
                    val scanResult = provisioningHelper.checkForValetudo()

                    if (scanResult != null) {
                        foundRobot = scanResult

                        runOnUiThread {
                            provisioningInputs.visibility = View.VISIBLE
                            foundRobotLabel.visibility = View.VISIBLE

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

                            Toast.makeText(this@ProvisioningActivity, "Scan finished without results", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            } else {
                runOnUiThread {
                    Toast.makeText(this@ProvisioningActivity, "Missing provisioningHelper", Toast.LENGTH_LONG).show()
                }
            }
        }

        connectButton.setOnClickListener {
            if (provisioningHelper != null) {
                if(foundRobot != null) {
                    thread {
                        runOnUiThread {
                            connectButton.isEnabled = false
                        }

                        val connectResult = provisioningHelper.provisionValetudo(ssidInput.text.toString(), passwordInput.text.toString())

                        if (connectResult == 200) {
                            runOnUiThread {
                                Toast.makeText(this@ProvisioningActivity, "Provisioning successful", Toast.LENGTH_LONG).show()
                                this.finish()
                            }
                        } else {
                            runOnUiThread {
                                Toast.makeText(this@ProvisioningActivity, "Wifi Provisioning failed with code $connectResult", Toast.LENGTH_LONG).show()

                                connectButton.isEnabled = true
                            }
                        }
                    }
                } else {
                    runOnUiThread {
                        Toast.makeText(this@ProvisioningActivity, "Missing foundRobot", Toast.LENGTH_LONG).show()
                    }
                }
            } else {
                runOnUiThread {
                    Toast.makeText(this@ProvisioningActivity, "Missing provisioningHelper", Toast.LENGTH_LONG).show()
                }
            }
        }
    }
}