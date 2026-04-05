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
import com.refreshme.data.BookingStatus

/**
 * Adapter for displaying booking cards in RecyclerView
 */
class BookingCardAdapter(
    private val onStartClick: (StylistBooking) -> Unit,
    private val onCancelClick: (StylistBooking) -> Unit,
    private val onMessageClick: (StylistBooking) -> Unit,
    private val onAcceptClick: (StylistBooking) -> Unit, // New handler
    private val onDeclineClick: (StylistBooking) -> Unit // New handler
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

        // Existing buttons
        private val btnStart: Button = itemView.findViewById(R.id.btnStart)
        private val btnCancel: Button = itemView.findViewById(R.id.btnCancel)
        private val btnMessage: Button = itemView.findViewById(R.id.btnMessage)

        // New buttons for request flow
        private val btnAccept: Button = itemView.findViewById(R.id.btnAccept)
        private val btnDecline: Button = itemView.findViewById(R.id.btnDecline)


        fun bind(booking: StylistBooking) {
            clientName.text = booking.customerName
            
            // For a REQUESTED booking, booking time and location might not be set yet,
            // so we show the "requestedAt" time if available and simplify location.
            if (booking.status == BookingStatus.REQUESTED) {
                bookingTime.text = booking.createdAt?.let { 
                    val formatter = java.text.SimpleDateFormat("'Requested' EEE, MMM dd 'at' h:mm a", java.util.Locale.getDefault())
                    formatter.format(it.toDate())
                } ?: "Time not set"
                bookingLocation.text = "New Booking Request"
                priceText.text = booking.getFormattedPrice()
            } else {
                bookingTime.text = booking.getFormattedTime()
                bookingLocation.text = booking.location
                priceText.text = booking.getFormattedPrice()
            }

            serviceDetails.text = booking.serviceName

            // --- Configure buttons based on booking status ---
            
            // Reset visibility for all action buttons
            btnStart.visibility = View.GONE
            btnCancel.visibility = View.GONE // Cancel is only for accepted/upcoming bookings
            btnAccept.visibility = View.GONE
            btnDecline.visibility = View.GONE

            btnMessage.visibility = View.VISIBLE // Message is almost always available

            when (booking.status) {
                BookingStatus.REQUESTED -> {
                    btnAccept.visibility = View.VISIBLE
                    btnDecline.visibility = View.VISIBLE
                    btnCancel.visibility = View.GONE // Decline handles the cancellation of a request

                    btnAccept.setOnClickListener { onAcceptClick(booking) }
                    btnDecline.setOnClickListener { onDeclineClick(booking) }
                }
                BookingStatus.PENDING, BookingStatus.ACCEPTED, BookingStatus.DEPOSIT_PAID, BookingStatus.ON_THE_WAY -> {
                    btnStart.visibility = View.VISIBLE
                    btnCancel.visibility = View.VISIBLE
                    btnStart.text = "Start"
                    btnStart.setOnClickListener { onStartClick(booking) }
                    btnCancel.setOnClickListener { onCancelClick(booking) }
                }
                BookingStatus.IN_PROGRESS -> {
                    btnStart.visibility = View.VISIBLE
                    btnCancel.visibility = View.VISIBLE
                    btnStart.text = "Complete"
                    btnStart.setOnClickListener { onStartClick(booking) }
                    btnCancel.setOnClickListener { onCancelClick(booking) }
                }
                BookingStatus.COMPLETED, BookingStatus.CANCELLED, BookingStatus.DECLINED, BookingStatus.REFUND_PROCESSING -> { 
                    // Only message button is visible (set outside of the when block)
                    btnCancel.visibility = View.GONE
                }
            }

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