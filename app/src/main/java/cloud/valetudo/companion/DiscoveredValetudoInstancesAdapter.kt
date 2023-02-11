package cloud.valetudo.companion

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.TextView
import java.util.*
import kotlin.collections.ArrayList

class DiscoveredValetudoInstancesAdapter(
    context: Context,
    resource: Int,
    objects: ArrayList<out DiscoveredValetudoInstance>
) : ArrayAdapter<DiscoveredValetudoInstance>(context, resource, objects) {

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val instance = getItem(position)
        val newView = convertView ?: LayoutInflater.from(context).inflate(R.layout.discovered_instance_list_item_layout, parent, false)

        if (instance == null) {
            return newView;
        }

        val label = newView.findViewById<TextView>(R.id.label)
        val valetudoVersion = newView.findViewById<TextView>(R.id.valetudoVersion)
        val uniqueIdentifier = newView.findViewById<TextView>(R.id.uniqueIdentifier)
        val domainAndHost = newView.findViewById<TextView>(R.id.domainAndHost)

        if (instance.name.isNotEmpty()) {
            label.text = context.getString(
                R.string.discovered_valetudo_instance_list_item_name,

                instance.name
            )
        } else {
            label.text = context.getString(
                R.string.discovered_valetudo_instance_list_item_manufacturer_and_model,

                instance.manufacturer,
                instance.model
            )
        }

        valetudoVersion.text = context.getString(
            R.string.discovered_valetudo_instance_list_item_valetudo_version,

            instance.valetudoVersion
        )

        uniqueIdentifier.text = instance.id

        domainAndHost.text = context.getString(
            R.string.discovered_valetudo_instance_list_item_domain_and_host,

            instance.id.lowercase(Locale.getDefault()),
            instance.host
        )

        return newView
    }
}