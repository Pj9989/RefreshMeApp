package com.refreshme.stylist

import android.content.Context // Added for onAttach
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import android.widget.TextView // Added import for TextView
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

    // Task 3: Badge update listener interface
    interface BadgeUpdateListener {
        fun updateBookingsBadge(count: Int)
    }
    
    private lateinit var recyclerView: RecyclerView
    private lateinit var tabLayout: TabLayout
    private lateinit var emptyStateTextView: TextView // Added
    private lateinit var adapter: BookingCardAdapter
    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    
    private var badgeUpdateListener: BadgeUpdateListener? = null // Listener instance

    override fun onAttach(context: Context) {
        super.onAttach(context)
        // Check if the hosting context (Activity/Parent Fragment) implements the interface
        if (context is BadgeUpdateListener) {
            badgeUpdateListener = context
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_stylist_bookings, container, false)
        
        recyclerView = view.findViewById(R.id.bookingsRecyclerView)
        tabLayout = view.findViewById(R.id.tabLayout)
        emptyStateTextView = view.findViewById(R.id.emptyStateTextView) // Initialized
        
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
        
        // Add sorting by startTime (renamed from bookingTime) in ascending order for UPCOMING filter for better UX,
        // and descending for COMPLETED/CANCELLED (most recent first).
        val direction = if (filter == BookingFilter.UPCOMING) Query.Direction.ASCENDING else Query.Direction.DESCENDING
        query.orderBy("startTime", direction) // Changed "bookingTime" to "startTime"
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    // Error = warning (handled by Toast)
                    Toast.makeText(context, "Error loading bookings", Toast.LENGTH_SHORT).show()
                    emptyStateTextView.visibility = View.GONE
                    recyclerView.visibility = View.GONE
                    // adapter.submitList(emptyList()) // Keep existing data if possible, or clear on error
                    badgeUpdateListener?.updateBookingsBadge(0) // Clear badge on error
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
                            price = doc.getDouble("price") ?: 0.0, // Changed "servicePrice" to "price"
                            startTime = doc.getTimestamp("startTime"), // Changed "bookingTime" to "startTime"
                            location = doc.getString("location") ?: "",
                            status = BookingStatus.valueOf(doc.getString("status") ?: "PENDING"),
                            notes = doc.getString("notes") ?: "",
                            createdAt = doc.getTimestamp("createdAt")
                        )
                    } catch (e: Exception) {
                        null
                    }
                } ?: emptyList()
                
                // Task 3: Calculate and update badge count (only for UPCOMING tab)
                if (filter == BookingFilter.UPCOMING) {
                    val pendingCount = bookings.count { it.status == BookingStatus.PENDING }
                    badgeUpdateListener?.updateBookingsBadge(pendingCount)
                }
                
                // Differentiate EMPTY vs DATA (Empty = calm message)
                if (bookings.isEmpty()) {
                    emptyStateTextView.text = when(filter) {
                        BookingFilter.UPCOMING -> "No upcoming bookings\nGo online to start receiving requests"
                        BookingFilter.COMPLETED -> "No completed bookings yet"
                        BookingFilter.CANCELLED -> "No cancelled bookings"
                    }
                    emptyStateTextView.visibility = View.VISIBLE
                    recyclerView.visibility = View.GONE
                    adapter.submitList(emptyList())
                } else {
                    emptyStateTextView.visibility = View.GONE
                    recyclerView.visibility = View.VISIBLE
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

    private enum class BookingFilter {
        UPCOMING, COMPLETED, CANCELLED
    }
}