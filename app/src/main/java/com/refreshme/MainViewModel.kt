package com.refreshme

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore
) : ViewModel() {

    private val _currentUserData = MutableStateFlow<User?>(null)
    val currentUserData = _currentUserData.asStateFlow()

    private var userListener: ListenerRegistration? = null

    init {
        observeAuthState()
    }

    private fun observeAuthState() {
        auth.addAuthStateListener { firebaseAuth ->
            val user = firebaseAuth.currentUser
            if (user != null) {
                startListeningToUser(user.uid)
            } else {
                stopListening()
                _currentUserData.value = null
            }
        }
    }

    private fun startListeningToUser(uid: String) {
        userListener?.remove()
        userListener = firestore.collection("users").document(uid)
            .addSnapshotListener { snapshot, error ->
                if (error != null) return@addSnapshotListener
                if (snapshot != null && snapshot.exists()) {
                    _currentUserData.value = snapshot.toObject(User::class.java)?.copy(uid = uid)
                }
            }
    }

    private fun stopListening() {
        userListener?.remove()
        userListener = null
    }

    override fun onCleared() {
        super.onCleared()
        stopListening()
    }
}