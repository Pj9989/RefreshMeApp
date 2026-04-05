package com.refreshme

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.Gravity
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

/**
 * Subscription system removed. Stylists are charged a 10% platform fee per booking automatically.
 */
class StylistSubscriptionActivity : AppCompatActivity() {

    companion object {
        fun newIntent(context: Context): Intent {
            return Intent(context, StylistSubscriptionActivity::class.java)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportActionBar?.apply {
            title = "Payouts"
            setDisplayHomeAsUpEnabled(true)
        }
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(64, 64, 64, 64)
        }
        val tv = TextView(this).apply {
            text = "No subscription needed!\n\nRefreshMe takes a small 10% platform fee per completed booking automatically.\n\nSet up your payout account from your profile to start receiving payments."
            textSize = 18f
            gravity = Gravity.CENTER
        }
        layout.addView(tv)
        setContentView(layout)
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
}