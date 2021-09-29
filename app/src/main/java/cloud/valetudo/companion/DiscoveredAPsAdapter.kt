package cloud.valetudo.companion

import android.content.Context
import android.net.wifi.ScanResult
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.TextView
import kotlin.collections.ArrayList

class DiscoveredAPsAdapter(
    context: Context,
    resource: Int,
    objects: ArrayList<out ScanResult>
) : ArrayAdapter<ScanResult>(context, resource, objects) {

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val instance = getItem(position)
        var newView = convertView

        if (newView == null) {
            newView = LayoutInflater.from(context).inflate(R.layout.discovered_ap_list_item_layout, parent, false)
        }

        val ssid = newView!!.findViewById<TextView>(R.id.discovered_ap_ssid)

        ssid.text = instance!!.SSID

        return newView
    }
}