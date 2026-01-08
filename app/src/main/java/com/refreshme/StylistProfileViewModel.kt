package com.refreshme

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.firestore.FirebaseFirestore
import com.refreshme.data.Stylist
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class StylistProfileViewModel : ViewModel() {

    private val db = FirebaseFirestore.getInstance()
    private val _stylist = MutableStateFlow<Stylist?>(null)
    val stylist: StateFlow<Stylist?> = _stylist

    fun fetchStylist(stylistId: String) {
        viewModelScope.launch {
            db.collection("stylists").document(stylistId).get()
                .addOnSuccessListener { document ->
                    if (document != null) {
                        _stylist.value = document.toObject(Stylist::class.java)
                    }
                }
        }
    }
}