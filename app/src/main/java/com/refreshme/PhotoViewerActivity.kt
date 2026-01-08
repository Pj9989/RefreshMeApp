package com.refreshme

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide

class PhotoViewerActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_photo_viewer)

        val photoView = findViewById<ImageView>(R.id.photoImageView)
        val photoUrl = intent.getStringExtra(EXTRA_URL)

        Glide.with(this)
            .load(photoUrl)
            .into(photoView)
    }

    companion object {
        private const val EXTRA_URL = "extra_url"

        fun newIntent(context: Context, url: String): Intent {
            return Intent(context, PhotoViewerActivity::class.java).apply {
                putExtra(EXTRA_URL, url)
            }
        }
    }
}
