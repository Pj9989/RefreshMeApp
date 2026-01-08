package com.refreshme.stylist

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.refreshme.R
import com.refreshme.data.WorkingHours

class AvailabilityFragment : Fragment() {

    private val firestore by lazy { FirebaseFirestore.getInstance() }
    private val auth by lazy { FirebaseAuth.getInstance() }
    private lateinit var workingHoursRecyclerView: RecyclerView
    private lateinit var saveButton: Button
    private lateinit var workingHoursAdapter: WorkingHoursAdapter
    private val workingHours = mutableListOf<WorkingHours>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_availability, container, false)

        workingHoursRecyclerView = view.findViewById(R.id.working_hours_recycler_view)
        saveButton = view.findViewById(R.id.save_button)

        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        workingHoursAdapter = WorkingHoursAdapter(workingHours)
        workingHoursRecyclerView.layoutManager = LinearLayoutManager(context)
        workingHoursRecyclerView.adapter = workingHoursAdapter

        fetchWorkingHours()

        saveButton.setOnClickListener {
            saveWorkingHours()
        }
    }

    private fun fetchWorkingHours() {
        val userId = auth.currentUser?.uid ?: return
        firestore.collection("stylists").document(userId).get()
            .addOnSuccessListener { document ->
                val stylist = document.toObject(com.refreshme.data.Stylist::class.java)
                stylist?.workingHours?.let {
                    workingHours.clear()
                    workingHours.addAll(it)
                    workingHoursAdapter.notifyDataSetChanged()
                }
            }
    }

    private fun saveWorkingHours() {
        val userId = auth.currentUser?.uid ?: return
        firestore.collection("stylists").document(userId)
            .update("workingHours", workingHours)
            .addOnSuccessListener {
                Toast.makeText(context, "Working hours saved", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener {
                Toast.makeText(context, "Error saving working hours", Toast.LENGTH_SHORT).show()
            }
    }
}