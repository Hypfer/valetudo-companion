package cloud.valetudo.companion.activities.main

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import cloud.valetudo.companion.R
import cloud.valetudo.companion.databinding.DiscoveredInstanceListItemLayoutBinding
import cloud.valetudo.companion.utils.setText
import java.util.*

class DiscoveredValetudoInstancesAdapter(
    private val onItemClickListener: (DiscoveredValetudoInstance) -> Unit,
    private val onItemLongClickListener: (DiscoveredValetudoInstance) -> Boolean
) : RecyclerView.Adapter<DiscoveredValetudoInstancesAdapter.ViewHolder>() {

    private val internalInstances = mutableListOf<DiscoveredValetudoInstance>()

    var instances: List<DiscoveredValetudoInstance>
        get() = internalInstances.toList()
        set(value) {
            internalInstances.clear()
            internalInstances.addAll(value)
            notifyDataSetChanged()
        }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val binding = DiscoveredInstanceListItemLayoutBinding.inflate(inflater, parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(internalInstances[position])
    }

    override fun getItemCount(): Int = internalInstances.size

    inner class ViewHolder(private val binding: DiscoveredInstanceListItemLayoutBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(instance: DiscoveredValetudoInstance) {
            itemView.setOnClickListener {
                onItemClickListener(instance)
            }

            itemView.setOnLongClickListener {
                onItemLongClickListener(instance)
            }

            with(binding) {
                manufacturerAndModel.setText(
                    R.string.discovered_valetudo_instance_list_item_manufacturer_and_model,
                    instance.manufacturer,
                    instance.model
                )

                valetudoVersion.setText(
                    R.string.discovered_valetudo_instance_list_item_valetudo_version,
                    instance.valetudoVersion
                )

                uniqueIdentifier.text = instance.id

                domainAndHost.setText(
                    R.string.discovered_valetudo_instance_list_item_domain_and_host,
                    instance.id.lowercase(Locale.getDefault()),
                    instance.host
                )
            }
        }
    }
}
