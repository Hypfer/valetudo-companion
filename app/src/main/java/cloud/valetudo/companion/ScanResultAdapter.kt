package cloud.valetudo.companion

import android.net.wifi.ScanResult
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import cloud.valetudo.companion.databinding.DiscoveredApListItemLayoutBinding


class ScanResultAdapter(
    private val onItemClickListener: (ScanResult) -> Unit
) : RecyclerView.Adapter<ScanResultAdapter.ViewHolder>() {

    private val internalResults = mutableListOf<ScanResult>()

    var scanResults: List<ScanResult>
        get() = internalResults.toList()
        set(value) {
            internalResults.clear()
            internalResults.addAll(value)
            notifyDataSetChanged()
        }
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val binding = DiscoveredApListItemLayoutBinding.inflate(inflater, parent, false)
        return ViewHolder(binding)
    }

    override fun getItemCount(): Int = scanResults.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(scanResults[position])
    }

    inner class ViewHolder(private val binding: DiscoveredApListItemLayoutBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(scanResult: ScanResult) {
            binding.discoveredApSsid.text = scanResult.SSID

            itemView.setOnClickListener {
                onItemClickListener(scanResult)
            }
        }
    }
}