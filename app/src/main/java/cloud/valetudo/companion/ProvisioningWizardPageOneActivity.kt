package cloud.valetudo.companion

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.net.wifi.WifiManager
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

class ProvisioningWizardPageOneActivity: AppCompatActivity()  {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_provisioning_page1)

        val wifiManager: WifiManager? = getSystemService(WifiManager::class.java)
        val connectivityManager: ConnectivityManager? = getSystemService(ConnectivityManager::class.java)
        var provisioningHelper: ValetudoProvisioningHelper? = null

        if (wifiManager != null && connectivityManager != null) {
            provisioningHelper = ValetudoProvisioningHelper(
                wifiManager,
                connectivityManager
            )
        } else {
            Log.e("ProvisioningWizardPageOneActivity", "Unable to create new provisioningHelper due to missing wifi- or connectivityManager")

            runOnUiThread {
                this.finish()
            }
        }

        val nextButton = findViewById<Button>(R.id.wizard_page_1_next_button)
        val skipButton = findViewById<Button>(R.id.wizard_page_1_skip_button)


        nextButton.setOnClickListener {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                val wizardPageTwoIntent = Intent(this, ProvisioningWizardPageTwoActivity::class.java)

                startActivity(wizardPageTwoIntent)
            } else {
                ActivityCompat.requestPermissions(this,
                    arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                    1234
                )
            }
        }

        skipButton.setOnClickListener {
            if (provisioningHelper!!.getRobotWifiNetwork() != null) {
                val provisioningIntent = Intent(this, ProvisioningActivity::class.java)

                startActivity(provisioningIntent)
            } else {
                runOnUiThread {
                    Toast.makeText(this@ProvisioningWizardPageOneActivity, "No you're not", Toast.LENGTH_SHORT).show()
                }
            }
        }

    }
}