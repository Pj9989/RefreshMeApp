package com.refreshme.stylist

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.refreshme.data.WaitlistEntry
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

@HiltViewModel
class StylistWaitlistViewModel @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth
) : ViewModel() {

    private val _waitlistEntries = MutableStateFlow<List<WaitlistEntry>>(emptyList())
    val waitlistEntries = _waitlistEntries.asStateFlow()

    private val _isLoading = MutableStateFlow(true)
    val isLoading = _isLoading.asStateFlow()

    init {
        loadWaitlist()
    }

    fun loadWaitlist() {
        val uid = auth.currentUser?.uid ?: return
        viewModelScope.launch {
            _isLoading.value = true
            try {
                // Get all waitlist entries for this stylist, sort by targetDate
                val snapshot = firestore.collection("waitlists")
                    .whereEqualTo("stylistId", uid)
                    .orderBy("targetDate", Query.Direction.ASCENDING)
                    .get()
                    .await()

                val entries = snapshot.documents.mapNotNull { doc ->
                    val entry = doc.toObject(WaitlistEntry::class.java)
                    if (entry != null) {
                        entry.id = doc.id
                    }
                    entry
                }
                
                // Filter out past dates if desired, but for now show all upcoming
                val currentTime = System.currentTimeMillis()
                val upcomingEntries = entries.filter { it.targetDate > (currentTime - 24 * 60 * 60 * 1000) } // Keep if not older than 1 day

                _waitlistEntries.value = upcomingEntries
            } catch (e: Exception) {
                Log.e("StylistWaitlistVM", "Error loading waitlist", e)
            } finally {
                _isLoading.value = false
            }
        }
    }
}