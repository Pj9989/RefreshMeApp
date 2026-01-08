package com.refreshme.stylist

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.refreshme.R
import com.refreshme.data.WorkingHours
import java.text.DateFormatSymbols

class WorkingHoursAdapter(private val workingHours: MutableList<WorkingHours>) :
    RecyclerView.Adapter<WorkingHoursAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val dayOfWeek: TextView = view.findViewById(R.id.day_of_week)
        val startTime: EditText = view.findViewById(R.id.start_time)
        val endTime: EditText = view.findViewById(R.id.end_time)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_working_hours, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val day = workingHours[position]
        holder.dayOfWeek.text = DateFormatSymbols().weekdays[day.dayOfWeek]
        holder.startTime.setText(day.startTime)
        holder.endTime.setText(day.endTime)
    }

    override fun getItemCount() = workingHours.size
}