package com.refreshme.stylist

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.refreshme.R

class PortfolioAdapter(
    private var imageUrls: List<String>,
    private val onDeleteClick: (String) -> Unit
) : RecyclerView.Adapter<PortfolioAdapter.ImageViewHolder>() {

    class ImageViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val imageView: ImageView = view.findViewById(R.id.portfolio_image)
        val deleteIcon: ImageView = view.findViewById(R.id.delete_icon)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ImageViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_portfolio_photo, parent, false)
        return ImageViewHolder(view)
    }

    override fun onBindViewHolder(holder: ImageViewHolder, position: Int) {
        val imageUrl = imageUrls[position]

        // Use Glide to load the image from the URL
        Glide.with(holder.itemView.context)
            .load(imageUrl)
            .placeholder(R.drawable.ic_launcher_background) // Assuming a default placeholder
            .centerCrop()
            .into(holder.imageView)

        holder.deleteIcon.setOnClickListener {
            onDeleteClick(imageUrl)
        }
    }

    override fun getItemCount() = imageUrls.size

    fun updateImages(newUrls: List<String>) {
        imageUrls = newUrls
        notifyDataSetChanged()
    }
}
