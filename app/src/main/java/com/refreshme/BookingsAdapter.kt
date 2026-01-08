/*
package com.refreshme

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.refreshme.data.Booking
import com.refreshme.databinding.ItemBookingBinding
import java.text.SimpleDateFormat
import java.util.Locale

class BookingsAdapter(
    private val bookings: MutableList<Booking>,
    private val onRateReviewClicked: (Booking) -> Unit
) : RecyclerView.Adapter<BookingsAdapter.BookingViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BookingViewHolder {
        val binding = ItemBookingBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return BookingViewHolder(binding)
    }

    override fun onBindViewHolder(holder: BookingViewHolder, position: Int) {
        holder.bind(bookings[position])
    }

    override fun getItemCount() = bookings.size

    @SuppressLint("NotifyDataSetChanged")
    fun updateBookings(newBookings: List<Booking>) {
        bookings.clear()
        bookings.addAll(newBookings)
        notifyDataSetChanged()
    }

    inner class BookingViewHolder(private val binding: ItemBookingBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(booking: Booking) {
            binding.stylistName.text = booking.stylistName
            binding.bookingService.text = booking.service

            val sdf = SimpleDateFormat("MM/dd/yyyy 'at' hh:mm a", Locale.getDefault())
            binding.bookingDate.text = booking.date?.let { sdf.format(it) } ?: "Date not available"

            binding.bookingStatus.text = booking.status

            if (booking.status == "Completed") {
                binding.rateReviewButton.visibility = View.VISIBLE
                binding.rateReviewButton.setOnClickListener {
                    onRateReviewClicked(booking)
                }
            } else {
                binding.rateReviewButton.visibility = View.GONE
            }
        }
    }
}
*/