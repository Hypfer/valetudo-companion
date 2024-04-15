package cloud.valetudo.companion

import android.content.Context
import android.net.wifi.ScanResult
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.TextView
import cloud.valetudo.companion.databinding.DiscoveredApListItemLayoutBinding
import cloud.valetudo.companion.databinding.DiscoveredInstanceListItemLayoutBinding
import kotlin.collections.ArrayList

class DiscoveredAPsAdapter(
    context: Context,
    resource: Int,
    objects: ArrayList<out ScanResult>
) : ArrayAdapter<ScanResult>(context, resource, objects) {

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val instance = getItem(position)

        val binding: DiscoveredApListItemLayoutBinding =
            if (convertView != null) DiscoveredApListItemLayoutBinding.bind(convertView)
            else DiscoveredApListItemLayoutBinding.inflate(LayoutInflater.from(context), parent, false)

        binding.discoveredApSsid.text = instance!!.SSID

        return binding.root
    }
}