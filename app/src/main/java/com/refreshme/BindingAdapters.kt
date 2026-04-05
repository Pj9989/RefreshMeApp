package com.refreshme

import android.widget.ImageView
import androidx.databinding.BindingAdapter
import com.bumptech.glide.Glide
import com.google.firebase.Firebase
import com.google.firebase.storage.storage
import com.refreshme.R

@BindingAdapter("storagePath")
fun loadImage(view: ImageView, path: String?) {
    if (!path.isNullOrEmpty()) {
        val storageReference = Firebase.storage.reference.child(path)
        Glide.with(view.context)
            .load(storageReference)
            .placeholder(R.drawable.ic_profile) // Fallback avatar
            .error(R.drawable.ic_profile)
            .into(view)
    } else {
        view.setImageResource(R.drawable.ic_profile)
    }
}

@BindingAdapter("imageUrl")
fun loadImageFromUrl(view: ImageView, url: String?) {
    if (!url.isNullOrEmpty()) {
        Glide.with(view.context)
            .load(url)
            .placeholder(R.drawable.ic_profile) // Fallback avatar
            .error(R.drawable.ic_profile)
            .into(view)
    } else {
        view.setImageResource(R.drawable.ic_profile)
    }
}
