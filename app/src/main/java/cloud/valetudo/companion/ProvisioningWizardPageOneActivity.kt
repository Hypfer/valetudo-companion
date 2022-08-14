package cloud.valetudo.companion

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager.PERMISSION_GRANTED
import android.net.ConnectivityManager
import android.net.wifi.WifiManager
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

val PERMISSIONS_REQUIRED = arrayOf(
    Manifest.permission.ACCESS_COARSE_LOCATION,
    Manifest.permission.ACCESS_FINE_LOCATION
);
const val PERMISSION_REQUEST_CODE = 1234;


class ProvisioningWizardPageOneActivity: AppCompatActivity()  {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_provisioning_page1)

        val wifiManager: WifiManager? = getSystemService(WifiManager::class.java)
        val connectivityManager: ConnectivityManager? = getSystemService(ConnectivityManager::class.java)
        val provisioningHelper: ValetudoProvisioningHelper

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

            return
        }

        val nextButton = findViewById<Button>(R.id.wizard_page_1_next_button)
        val skipButton = findViewById<Button>(R.id.wizard_page_1_skip_button)


        nextButton.setOnClickListener {
            if (
                ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PERMISSION_GRANTED
            ) {
                val wizardPageTwoIntent = Intent(this, ProvisioningWizardPageTwoActivity::class.java)

                startActivity(wizardPageTwoIntent)
            } else {
                if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.ACCESS_FINE_LOCATION)) {
                    ActivityCompat.requestPermissions(this,
                        PERMISSIONS_REQUIRED,
                        PERMISSION_REQUEST_CODE
                    )
                } else {
                    runOnUiThread {
                        Toast.makeText(
                            this@ProvisioningWizardPageOneActivity,
                            "Wi-Fi SSID scanning requires the ACCESS_FINE_LOCATION permission.\nDid you select \"Don't ask again\"?",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }

            }
        }

        skipButton.setOnClickListener {
            if (provisioningHelper.getRobotWifiNetwork() != null) {
                val provisioningIntent = Intent(this, ProvisioningActivity::class.java)

                startActivity(provisioningIntent)
            } else {
                runOnUiThread {
                    Toast.makeText(this@ProvisioningWizardPageOneActivity, "No you're not", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String?>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (
                permissions contentEquals PERMISSIONS_REQUIRED &&
                grantResults.all { it == PERMISSION_GRANTED }
            ) {
                val wizardPageTwoIntent = Intent(this, ProvisioningWizardPageTwoActivity::class.java)

                startActivity(wizardPageTwoIntent)
            } else {
                runOnUiThread {
                    Toast.makeText(
                        this@ProvisioningWizardPageOneActivity,
                        "Wi-Fi SSID scanning requires the ACCESS_FINE_LOCATION permission",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }
}