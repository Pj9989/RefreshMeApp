package com.refreshme.auth

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.refreshme.ui.theme.RefreshMeTheme

class RoleSelectActivity : AppCompatActivity() {

    private val viewModel: RoleSelectViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            RefreshMeTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    RoleSelectScreen(
                        onCustomerSelected = {
                            viewModel.onRoleSelected("CUSTOMER") {
                                startActivity(CustomerOnboardingActivity.newIntent(this))
                                finish()
                            }
                        },
                        onStylistSelected = {
                            viewModel.onRoleSelected("STYLIST") {
                                startActivity(StylistOnboardingActivity.newIntent(this))
                                finish()
                            }
                        }
                    )
                }
            }
        }
    }

    companion object {
        fun newIntent(context: Context) = Intent(context, RoleSelectActivity::class.java)
    }
}