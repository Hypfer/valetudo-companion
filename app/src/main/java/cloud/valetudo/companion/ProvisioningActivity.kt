package cloud.valetudo.companion

import android.content.Intent
import android.net.ConnectivityManager
import android.net.wifi.WifiManager
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import kotlin.concurrent.thread




class ProvisioningActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_provisioning)


        var newNetworkId: Int? = null
        var withResult = false

        if (intent.extras != null) {
            newNetworkId = intent.extras!!["newNetworkId"] as Int?
            withResult = intent.extras!!["withResult"] as Boolean
        }



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

            runOnUiThread {
                this.finish()
            }
        }

        var foundRobot : DiscoveredUnprovisionedValetudoInstance? = null

        val helpText = findViewById<TextView>(R.id.no_valetudo_found_hint)

        val scanButton = findViewById<Button>(R.id.scan_button)
        val connectButton = findViewById<Button>(R.id.connect_button)

        val foundRobotLabel = findViewById<TextView>(R.id.found_robot_label)
        val provisioningInputs = findViewById<LinearLayout>(R.id.provisioning_inputs)

        val ssidInput = findViewById<EditText>(R.id.input_ssid)
        val passwordInput = findViewById<EditText>(R.id.input_password)

        fun scanForValetudo() {
            if (provisioningHelper != null) {
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

        scanButton.setOnClickListener {
           scanForValetudo()
        }

        scanForValetudo()

        connectButton.setOnClickListener {
            if (provisioningHelper != null) {
                if(foundRobot != null) {
                    thread {
                        runOnUiThread {
                            connectButton.isEnabled = false
                        }

                        val connectResult = provisioningHelper.provisionValetudo(ssidInput.text.toString(), passwordInput.text.toString())

                        if (connectResult == 200) {

                            @Suppress("DEPRECATION")
                            if (newNetworkId != null) { //This is only != null on android versions <= Q
                                wifiManager!!.removeNetwork(newNetworkId)
                            }

                            runOnUiThread {
                                Toast.makeText(this@ProvisioningActivity, "Provisioning successful", Toast.LENGTH_LONG).show()

                                if (!withResult) {
                                    val intent = Intent(this, MainActivity::class.java)
                                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK

                                    startActivity(intent)
                                } else {
                                    val returnIntent = Intent()
                                    setResult(RESULT_OK, returnIntent)

                                    finish()
                                }

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