package com.refreshme

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.refreshme.data.Stylist
import com.refreshme.databinding.ItemFeaturedStylistBinding

class FeaturedStylistAdapter(
    private val stylists: List<Stylist>,
    private val onStylistClicked: (Stylist) -> Unit
) : RecyclerView.Adapter<FeaturedStylistAdapter.FeaturedStylistViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FeaturedStylistViewHolder {
        val binding = ItemFeaturedStylistBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return FeaturedStylistViewHolder(binding)
    }

    override fun onBindViewHolder(holder: FeaturedStylistViewHolder, position: Int) {
        holder.bind(stylists[position])
    }

    override fun getItemCount() = stylists.size

    inner class FeaturedStylistViewHolder(private val binding: ItemFeaturedStylistBinding) :
        RecyclerView.ViewHolder(binding.root) {

        init {
            itemView.setOnClickListener {
                onStylistClicked(stylists[bindingAdapterPosition])
            }
        }

        fun bind(stylist: Stylist) {
            binding.stylistName.text = stylist.name
            Glide.with(itemView.context)
                .load(stylist.profileImageUrl)
                .circleCrop()
                .placeholder(R.drawable.ic_launcher_background)
                .error(R.drawable.ic_launcher_foreground)
                .into(binding.stylistImage)
        }
    }
}