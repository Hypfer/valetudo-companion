package cloud.valetudo.companion

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.TextView
import cloud.valetudo.companion.databinding.DiscoveredInstanceListItemLayoutBinding
import java.util.*
import kotlin.collections.ArrayList

class DiscoveredValetudoInstancesAdapter(
    context: Context,
    resource: Int,
    objects: ArrayList<out DiscoveredValetudoInstance>
) : ArrayAdapter<DiscoveredValetudoInstance>(context, resource, objects) {

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val instance = getItem(position)

        val binding: DiscoveredInstanceListItemLayoutBinding =
            if (convertView != null) DiscoveredInstanceListItemLayoutBinding.bind(convertView)
            else DiscoveredInstanceListItemLayoutBinding.inflate(LayoutInflater.from(context), parent, false)

        if (instance == null) {
            return binding.root
        }

        if (instance.name.isNotEmpty()) {
            binding.label.text = context.getString(
                R.string.discovered_valetudo_instance_list_item_name,

                instance.name
            )
        } else {
            binding.label.text = context.getString(
                R.string.discovered_valetudo_instance_list_item_manufacturer_and_model,

                instance.manufacturer,
                instance.model
            )
        }

        binding.valetudoVersion.text = context.getString(
            R.string.discovered_valetudo_instance_list_item_valetudo_version,

            instance.valetudoVersion
        )

        binding.uniqueIdentifier.text = instance.id

        binding.domainAndHost.text = context.getString(
            R.string.discovered_valetudo_instance_list_item_domain_and_host,

            instance.id.lowercase(Locale.getDefault()),
            instance.host
        )

        return binding.root
    }
}