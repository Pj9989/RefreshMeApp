package com.refreshme.stylist

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.refreshme.R
import com.refreshme.data.Service
import java.text.NumberFormat
import java.util.Locale

class ServiceAdapter(
    private var services: List<Service>,
    private val onServiceClick: (Service) -> Unit
) : RecyclerView.Adapter<ServiceAdapter.ServiceViewHolder>() {

    class ServiceViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val name: TextView = view.findViewById(R.id.service_name)
        val price: TextView = view.findViewById(R.id.service_price)
        val duration: TextView = view.findViewById(R.id.service_duration)
        // Removed reference to val moreIcon: ImageView = view.findViewById(R.id.icon_more)

        fun bind(service: Service, onServiceClick: (Service) -> Unit) {
            name.text = service.name

            // Format price as currency
            val format: NumberFormat = NumberFormat.getCurrencyInstance(Locale.getDefault())
            price.text = format.format(service.price)

            // Display duration in a friendly format
            // FIX: Use durationMinutes instead of duration
            duration.text = "${service.durationMinutes ?: 0} minutes"

            // Removed click handler for moreIcon, keeping only the item click
            itemView.setOnClickListener { onServiceClick(service) }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ServiceViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_service, parent, false)
        return ServiceViewHolder(view)
    }

    override fun onBindViewHolder(holder: ServiceViewHolder, position: Int) {
        holder.bind(services[position], onServiceClick)
    }

    override fun getItemCount() = services.size

    fun updateServices(newServices: List<Service>) {
        services = newServices
        notifyDataSetChanged()
    }
}