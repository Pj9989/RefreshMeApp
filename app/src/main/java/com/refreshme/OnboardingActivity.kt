package com.refreshme

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.github.appintro.AppIntro
import com.github.appintro.AppIntroFragment
import com.github.appintro.model.SliderPage
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

        setIndicatorColor(
            selectedIndicatorColor = ContextCompat.getColor(this, R.color.black),
            unselectedIndicatorColor = ContextCompat.getColor(this, R.color.gray)
        )
        
        setColorDoneText(ContextCompat.getColor(this, R.color.black))
        setColorSkipButton(ContextCompat.getColor(this, R.color.black))
        setNextArrowColor(ContextCompat.getColor(this, R.color.black))
        setBackArrowColor(ContextCompat.getColor(this, R.color.black))

        addSlide(
            AppIntroFragment.createInstance(
                SliderPage(
                    title = getString(R.string.onboarding_title_1),
                    description = getString(R.string.onboarding_desc_1),
                    imageDrawable = R.mipmap.ic_launcher_foreground,
                    titleColorRes = R.color.black,
                    descriptionColorRes = R.color.black
                )
            )
        )
        addSlide(
            AppIntroFragment.createInstance(
                SliderPage(
                    title = getString(R.string.onboarding_title_2),
                    description = getString(R.string.onboarding_desc_2),
                    imageDrawable = R.mipmap.ic_launcher_foreground,
                    titleColorRes = R.color.black,
                    descriptionColorRes = R.color.black
                )
            )
        )
        addSlide(
            AppIntroFragment.createInstance(
                SliderPage(
                    title = getString(R.string.onboarding_title_3),
                    description = getString(R.string.onboarding_desc_3),
                    imageDrawable = R.mipmap.ic_launcher_foreground,
                    titleColorRes = R.color.black,
                    descriptionColorRes = R.color.black
                )
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
        completeOnboarding()
    }

    override fun onDonePressed(currentFragment: Fragment?) {
        super.onDonePressed(currentFragment)
        completeOnboarding()
    }
}
