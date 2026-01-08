package com.refreshme

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.refreshme.data.TimeSlot
import com.refreshme.databinding.ItemTimeSlotBinding

class TimeSlotAdapter(
    private val timeSlots: List<TimeSlot>,
    private val onTimeSlotClick: (TimeSlot) -> Unit
) : RecyclerView.Adapter<TimeSlotAdapter.TimeSlotViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TimeSlotViewHolder {
        val binding = ItemTimeSlotBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return TimeSlotViewHolder(binding)
    }

    override fun onBindViewHolder(holder: TimeSlotViewHolder, position: Int) {
        holder.bind(timeSlots[position], onTimeSlotClick)
    }

    override fun getItemCount() = timeSlots.size

    class TimeSlotViewHolder(private val binding: ItemTimeSlotBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(timeSlot: TimeSlot, onTimeSlotClick: (TimeSlot) -> Unit) {
            binding.timeSlotButton.text = timeSlot.time
            binding.timeSlotButton.isEnabled = !timeSlot.isBooked
            binding.timeSlotButton.setOnClickListener {
                if (!timeSlot.isBooked) {
                    onTimeSlotClick(timeSlot)
                }
            }
        }
    }
}
