package com.refreshme.stylist

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.refreshme.R

/**
 * Adapter for displaying booking cards in RecyclerView
 */
class BookingCardAdapter(
    private val onStartClick: (StylistBooking) -> Unit,
    private val onCancelClick: (StylistBooking) -> Unit,
    private val onMessageClick: (StylistBooking) -> Unit
) : ListAdapter<StylistBooking, BookingCardAdapter.BookingViewHolder>(BookingDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BookingViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_booking_card, parent, false)
        return BookingViewHolder(view)
    }

    override fun onBindViewHolder(holder: BookingViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class BookingViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val clientName: TextView = itemView.findViewById(R.id.clientName)
        private val bookingTime: TextView = itemView.findViewById(R.id.bookingTime)
        private val bookingLocation: TextView = itemView.findViewById(R.id.bookingLocation)
        private val serviceDetails: TextView = itemView.findViewById(R.id.serviceDetails)
        private val priceText: TextView = itemView.findViewById(R.id.priceText)
        private val btnStart: Button = itemView.findViewById(R.id.btnStart)
        private val btnCancel: Button = itemView.findViewById(R.id.btnCancel)
        private val btnMessage: Button = itemView.findViewById(R.id.btnMessage)

        fun bind(booking: StylistBooking) {
            clientName.text = booking.customerName
            bookingTime.text = booking.getFormattedTime()
            bookingLocation.text = booking.location
            serviceDetails.text = booking.serviceName
            priceText.text = booking.getFormattedPrice()

            // Configure buttons based on booking status
            when (booking.status) {
                BookingStatus.PENDING, BookingStatus.CONFIRMED -> {
                    btnStart.visibility = View.VISIBLE
                    btnStart.text = "Start"
                    btnStart.setOnClickListener { onStartClick(booking) }
                }
                BookingStatus.IN_PROGRESS -> {
                    btnStart.visibility = View.VISIBLE
                    btnStart.text = "Complete"
                    btnStart.setOnClickListener { onStartClick(booking) }
                }
                BookingStatus.COMPLETED, BookingStatus.CANCELLED -> {
                    btnStart.visibility = View.GONE
                }
            }

            btnCancel.setOnClickListener { onCancelClick(booking) }
            btnMessage.setOnClickListener { onMessageClick(booking) }
        }
    }

    private class BookingDiffCallback : DiffUtil.ItemCallback<StylistBooking>() {
        override fun areItemsTheSame(oldItem: StylistBooking, newItem: StylistBooking): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: StylistBooking, newItem: StylistBooking): Boolean {
            return oldItem == newItem
        }
    }
}
