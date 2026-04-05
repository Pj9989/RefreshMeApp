package com.refreshme.stylist

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.TextView // Import added
import android.widget.Toast
import androidx.fragment.app.DialogFragment
import com.refreshme.R
import com.refreshme.data.Service

class AddServiceDialogFragment : DialogFragment() {

    // Interface to communicate results back to the activity
    interface ServiceDialogListener {
        fun onServiceSave(service: Service)
    }

    private var listener: ServiceDialogListener? = null
    private var isEditing = false
    private var serviceToEdit: Service? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Set up listener safely to avoid NullPointerException when the parent is not an Activity or Fragment
        listener = (targetFragment as? ServiceDialogListener) ?: (activity as? ServiceDialogListener)

        // Check if we are editing an existing service
        serviceToEdit = arguments?.getParcelable("SERVICE_TO_EDIT")
        isEditing = serviceToEdit != null
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Set dialog width to match parent for better appearance
        dialog?.window?.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        return inflater.inflate(R.layout.dialog_add_service, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val nameEditText: EditText = view.findViewById(R.id.edit_service_name)
        val priceEditText: EditText = view.findViewById(R.id.edit_service_price)
        val durationEditText: EditText = view.findViewById(R.id.edit_service_duration)
        val saveButton: Button = view.findViewById(R.id.button_save_service)
        val titleTextView: TextView = view.findViewById(R.id.dialog_title)

        if (isEditing) {
            // Populate fields if editing
            serviceToEdit?.let { service ->
                titleTextView.text = "Edit Service"
                nameEditText.setText(service.name)
                // Use a non-nullable safe call, price will be non-nullable double
                priceEditText.setText(String.format("%.2f", service.price))
                // FIX: Use durationMinutes property
                durationEditText.setText(service.durationMinutes?.toString() ?: "")
                saveButton.text = "Update"
            }
        } else {
            titleTextView.text = "Add New Service"
            saveButton.text = "Save"
        }

        saveButton.setOnClickListener {
            saveService(nameEditText, priceEditText, durationEditText)
        }
    }

    private fun saveService(nameEditText: EditText, priceEditText: EditText, durationEditText: EditText) {
        val name = nameEditText.text.toString().trim()
        val priceStr = priceEditText.text.toString().trim()
        val durationStr = durationEditText.text.toString().trim()

        if (name.isEmpty() || priceStr.isEmpty() || durationStr.isEmpty()) {
            Toast.makeText(context, "Please fill all service details", Toast.LENGTH_SHORT).show()
            return
        }

        val price = priceStr.toDoubleOrNull()
        val duration = durationStr.toIntOrNull()

        if (price == null || duration == null || price <= 0 || duration <= 0) {
            Toast.makeText(context, "Please enter valid numbers for price and duration", Toast.LENGTH_SHORT).show()
            return
        }

        // Create a copy of the service being edited, or a new instance
        val service = (serviceToEdit ?: Service()).copy(
            name = name,
            price = price,
            // FIX: Use durationMinutes parameter
            durationMinutes = duration
        )

        listener?.onServiceSave(service)
        dismiss()
    }
}