package com.refreshme.stylist

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android:view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.tabs.TabLayout
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.refreshme.R

class StylistBookingsFragment : Fragment() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var tabLayout: TabLayout
    private lateinit var adapter: BookingCardAdapter
    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_stylist_bookings, container, false)
        
        recyclerView = view.findViewById(R.id.bookingsRecyclerView)
        tabLayout = view.findViewById(R.id.tabLayout)
        
        setupRecyclerView()
        setupTabs()
        loadBookings(BookingFilter.UPCOMING)
        
        return view
    }

    private fun setupRecyclerView() {
        adapter = BookingCardAdapter(
            onStartClick = { booking ->
                handleStartBooking(booking)
            },
            onCancelClick = { booking ->
                handleCancelBooking(booking)
            },
            onMessageClick = { booking ->
                handleMessageCustomer(booking)
            }
        )
        
        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        recyclerView.adapter = adapter
    }

    private fun setupTabs() {
        tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                when (tab?.position) {
                    0 -> loadBookings(BookingFilter.UPCOMING)
                    1 -> loadBookings(BookingFilter.COMPLETED)
                    2 -> loadBookings(BookingFilter.CANCELLED)
                }
            }

            override fun onTabUnselected(tab: TabLayout.Tab?) {}
            override fun onTabReselected(tab: TabLayout.Tab?) {}
        })
    }

    private fun loadBookings(filter: BookingFilter) {
        val stylistId = auth.currentUser?.uid ?: return
        
        var query: Query = firestore.collection("bookings")
            .whereEqualTo("stylistId", stylistId)
        
        query = when (filter) {
            BookingFilter.UPCOMING -> query.whereIn("status", listOf("PENDING", "CONFIRMED", "IN_PROGRESS"))
            BookingFilter.COMPLETED -> query.whereEqualTo("status", "COMPLETED")
            BookingFilter.CANCELLED -> query.whereEqualTo("status", "CANCELLED")
        }
        
        query.orderBy("bookingTime", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Toast.makeText(context, "Error loading bookings", Toast.LENGTH_SHORT).show()
                    return@addSnapshotListener
                }
                
                val bookings = snapshot?.documents?.mapNotNull { doc ->
                    try {
                        StylistBooking(
                            id = doc.id,
                            customerId = doc.getString("customerId") ?: "",
                            customerName = doc.getString("customerName") ?: "Unknown",
                            customerPhone = doc.getString("customerPhone") ?: "",
                            serviceName = doc.getString("serviceName") ?: "",
                            servicePrice = doc.getDouble("servicePrice") ?: 0.0,
                            bookingTime = doc.getTimestamp("bookingTime"),
                            location = doc.getString("location") ?: "",
                            status = BookingStatus.valueOf(doc.getString("status") ?: "PENDING"),
                            notes = doc.getString("notes") ?: "",
                            createdAt = doc.getTimestamp("createdAt")
                        )
                    } catch (e: Exception) {
                        null
                    }
                } ?: emptyList()
                
                // If no real bookings, show dummy data for testing
                if (bookings.isEmpty()) {
                    adapter.submitList(getDummyBookings())
                } else {
                    adapter.submitList(bookings)
                }
            }
    }

    private fun handleStartBooking(booking: StylistBooking) {
        val newStatus = when (booking.status) {
            BookingStatus.PENDING, BookingStatus.CONFIRMED -> BookingStatus.IN_PROGRESS
            BookingStatus.IN_PROGRESS -> BookingStatus.COMPLETED
            else -> return
        }
        
        firestore.collection("bookings").document(booking.id)
            .update("status", newStatus.name)
            .addOnSuccessListener {
                val message = if (newStatus == BookingStatus.IN_PROGRESS) 
                    "Booking started" else "Booking completed"
                Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener {
                Toast.makeText(context, "Failed to update booking", Toast.LENGTH_SHORT).show()
            }
    }

    private fun handleCancelBooking(booking: StylistBooking) {
        firestore.collection("bookings").document(booking.id)
            .update("status", BookingStatus.CANCELLED.name)
            .addOnSuccessListener {
                Toast.makeText(context, "Booking cancelled", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener {
                Toast.makeText(context, "Failed to cancel booking", Toast.LENGTH_SHORT).show()
            }
    }

    private fun handleMessageCustomer(booking: StylistBooking) {
        // TODO: Navigate to chat screen with customer
        Toast.makeText(context, "Opening chat with ${booking.customerName}", Toast.LENGTH_SHORT).show()
    }

    /**
     * Dummy bookings for testing UI
     */
    private fun getDummyBookings(): List<StylistBooking> {
        val now = System.currentTimeMillis()
        return listOf(
            StylistBooking(
                id = "dummy1",
                customerName = "Marcus Johnson",
                customerPhone = "(704) 555-0123",
                serviceName = "Haircut + Beard Trim",
                servicePrice = 45.0,
                bookingTime = Timestamp(java.util.Date(now + 3600000)), // 1 hour from now
                location = "123 Main St, Charlotte, NC",
                status = BookingStatus.CONFIRMED,
                notes = "Please bring clippers"
            ),
            StylistBooking(
                id = "dummy2",
                customerName = "Jessica Parker",
                customerPhone = "(704) 555-0456",
                serviceName = "Women's Cut & Style",
                servicePrice = 65.0,
                bookingTime = Timestamp(java.util.Date(now + 7200000)), // 2 hours from now
                location = "456 Oak Ave, Charlotte, NC",
                status = BookingStatus.PENDING,
                notes = ""
            ),
            StylistBooking(
                id = "dummy3",
                customerName = "David Smith",
                customerPhone = "(704) 555-0789",
                serviceName = "Kids Haircut",
                servicePrice = 25.0,
                bookingTime = Timestamp(java.util.Date(now + 10800000)), // 3 hours from now
                location = "789 Elm St, Charlotte, NC",
                status = BookingStatus.CONFIRMED,
                notes = "Child is 8 years old"
            )
        )
    }

    private enum class BookingFilter {
        UPCOMING, COMPLETED, CANCELLED
    }
}
