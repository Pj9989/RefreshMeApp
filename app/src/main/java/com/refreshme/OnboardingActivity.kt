package com.refreshme

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.fragment.app.Fragment
import com.github.appintro.AppIntro
import com.github.appintro.AppIntroFragment
import com.refreshme.auth.SignInActivity

class OnboardingActivity : AppIntro() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val prefs = getSharedPreferences("prefs", Context.MODE_PRIVATE)
        val onboardingCompleted = prefs.getBoolean("onboarding_completed", false)

        if (onboardingCompleted) {
            startActivity(Intent(this, SignInActivity::class.java))
            finish()
            return
        }


        // Call addSlide passing your Fragments.
        // You can use AppIntroFragment to use a pre-built fragment
        addSlide(
            AppIntroFragment.createInstance(
                title = "Welcome to RefreshMe!",
                description = "Find and book your next appointment with the best barbers and stylists in your area.",
                imageDrawable = R.drawable.ic_launcher_foreground
            )
        )
        addSlide(
            AppIntroFragment.createInstance(
                title = "Discover new stylists",
                description = "Browse through a curated list of stylists and barbers, view their portfolios, and read reviews from other users.",
                imageDrawable = R.drawable.ic_launcher_foreground
            )
        )
        addSlide(
            AppIntroFragment.createInstance(
                title = "Book with ease",
                description = "Booking your next appointment is just a few taps away. Choose a stylist, select a service, and pick a time that works for you.",
                imageDrawable = R.drawable.ic_launcher_foreground
            )
        )
    }

    private fun completeOnboarding() {
        val prefs = getSharedPreferences("prefs", Context.MODE_PRIVATE)
        prefs.edit().putBoolean("onboarding_completed", true).apply()
        startActivity(Intent(this, SignInActivity::class.java))
        finish()
    }

    override fun onSkipPressed(currentFragment: Fragment?) {
        super.onSkipPressed(currentFragment)
        // Decide what to do when the user clicks on Skip
        completeOnboarding()
    }

    override fun onDonePressed(currentFragment: Fragment?) {
        super.onDonePressed(currentFragment)
        // Decide what to do when the user clicks on Done
        completeOnboarding()
    }
}