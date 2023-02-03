package cloud.valetudo.companion.activities.main

import android.content.ActivityNotFoundException
import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.browser.customtabs.CustomTabColorSchemeParams
import androidx.browser.customtabs.CustomTabsIntent
import androidx.core.content.res.ResourcesCompat
import cloud.valetudo.companion.ProvisioningWizardPageOneActivity
import cloud.valetudo.companion.R
import cloud.valetudo.companion.databinding.ActivityMainBinding
import cloud.valetudo.companion.utils.setVisibility


class MainActivity : AppCompatActivity() {
    private val viewModel: MainActivityViewModel by viewModels()

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.enterProvisioningActivityButton.setOnClickListener(::startProvisioning)

        val instancesAdapter = DiscoveredValetudoInstancesAdapter(
            ::launchInAppBrowser,
            ::launchExternalBrowser
        )

        binding.discoveredList.adapter = instancesAdapter

        viewModel.devicesLiveData.observe(this) {
            instancesAdapter.instances = it

            binding.helpText.setVisibility(it.isEmpty())

            binding.mainText.setText(
                if (it.isEmpty()) {
                    R.string.discovering_valetudo_instances
                } else {
                    R.string.found_devices
                }
            )
        }

        enableEgg()
    }

    private val customTabsIntent by lazy {
        val defaultColors = CustomTabColorSchemeParams.Builder().setToolbarColor(
            ResourcesCompat.getColor(resources, R.color.valetudo_main, null)
        )
            .build()

        CustomTabsIntent.Builder()
            .setDefaultColorSchemeParams(defaultColors)
            .setUrlBarHidingEnabled(false)
            .build()
    }

    private fun launchInAppBrowser(instance: DiscoveredValetudoInstance) {
        try {
            customTabsIntent.launchUrl(
                this,
                instance.hostUri
            )
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

    private fun launchExternalBrowser(instance: DiscoveredValetudoInstance): Boolean {
        val browserIntent = Intent(
            Intent.ACTION_VIEW,
            instance.hostUri
        )

        return try {
            startActivity(browserIntent)
            true
        } catch (e: ActivityNotFoundException) {
            runOnUiThread {
                Toast.makeText(
                    this@MainActivity,
                    "No http:// intent handler installed.",
                    Toast.LENGTH_LONG
                ).show()
            }
            false
        }
    }

    private fun startProvisioning(view: View) {
        val provisioningIntent = Intent(
            this,
            ProvisioningWizardPageOneActivity::class.java
        )
        startActivity(provisioningIntent)
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
                builder.create().show()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun enableEgg() {
        val icon = binding.valetudoLogo
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
