package com.refreshme.auth

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.refreshme.databinding.ActivityStylistAuthBinding

class StylistAuthActivity : AppCompatActivity() {

    private lateinit var binding: ActivityStylistAuthBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityStylistAuthBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // TODO: Implement login and registration logic for stylists.
    }
}