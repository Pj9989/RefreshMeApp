package com.refreshme.home

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.refreshme.R
import com.refreshme.data.Stylist
import java.util.Locale

class StylistPreviewAdapter(
    private val onStylistClicked: (Stylist, ImageView) -> Unit
) : ListAdapter<Stylist, StylistPreviewAdapter.StylistViewHolder>(StylistDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): StylistViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_stylist_preview, parent, false)
        return StylistViewHolder(view)
    }

    override fun onBindViewHolder(holder: StylistViewHolder, position: Int) {
        val stylist = getItem(position)
        holder.bind(stylist)
        holder.itemView.setOnClickListener { onStylistClicked(stylist, holder.stylistImage) }
    }

    class StylistViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val stylistImage: ImageView = itemView.findViewById(R.id.stylist_image)
        private val stylistName: TextView = itemView.findViewById(R.id.stylist_name)
        private val stylistService: TextView = itemView.findViewById(R.id.stylist_service)
        private val stylistRating: TextView = itemView.findViewById(R.id.stylist_rating)
        private val stylistDistance: TextView = itemView.findViewById(R.id.stylist_distance)
        private val tvMatchBadge: TextView = itemView.findViewById(R.id.tvMatchBadge)
        private val tvVibeBadge: TextView = itemView.findViewById(R.id.tvVibeBadge)
        private val tvMatchExplanation: TextView = itemView.findViewById(R.id.tvMatchExplanation)
        private val flashDealBadge: TextView = itemView.findViewById(R.id.flash_deal_badge)

        fun bind(stylist: Stylist) {
            stylistImage.transitionName = "stylist_image_${stylist.id}"
            
            if (stylist.profileImageUrl.isNullOrEmpty()) {
                stylistImage.setImageResource(R.drawable.ic_profile)
            } else {
                Glide.with(itemView.context)
                    .load(stylist.profileImageUrl)
                    .placeholder(R.drawable.ic_profile)
                    .error(R.drawable.ic_profile)
                    .into(stylistImage)
            }
                
            val displayName = stylist.name.takeIf { it.isNotBlank() } ?: "Unknown Stylist"
            stylistName.text = displayName
            
            val displayService = stylist.services?.getOrNull(0)?.name?.takeIf { it.isNotBlank() } 
                ?: stylist.specialty?.takeIf { it.isNotBlank() } 
                ?: "Full Service"
            stylistService.text = displayService
            
            if (stylist.rating == 0.0) {
                stylistRating.text = "New"
            } else {
                stylistRating.text = String.format(Locale.US, "%.1f", stylist.rating)
            }
            
            if (stylist.distance == 0.0) {
                stylistDistance.visibility = View.GONE
            } else {
                stylistDistance.visibility = View.VISIBLE
                stylistDistance.text = String.format(Locale.US, "%.1f mi", stylist.distance)
            }
            
            // Match Score Badge
            if (stylist.matchScore > 0) {
                tvMatchBadge.visibility = View.VISIBLE
                tvMatchBadge.text = "${stylist.matchScore}% Match"
                
                if (!stylist.matchExplanation.isNullOrBlank()) {
                    tvMatchExplanation.visibility = View.VISIBLE
                    tvMatchExplanation.text = stylist.matchExplanation
                } else {
                    tvMatchExplanation.visibility = View.GONE
                }
            } else {
                tvMatchBadge.visibility = View.GONE
                tvMatchExplanation.visibility = View.GONE
            }

            // Vibe Badge
            if (stylist.vibes?.isNotEmpty() == true) {
                tvVibeBadge.visibility = View.VISIBLE
                tvVibeBadge.text = stylist.vibes[0]
            } else {
                tvVibeBadge.visibility = View.GONE
            }

            // Flash Deal Badge
            if (stylist.hasActiveFlashDeal) {
                flashDealBadge.visibility = View.VISIBLE
                flashDealBadge.text = "FLASH"
            } else {
                flashDealBadge.visibility = View.GONE
            }
        }
    }

    class StylistDiffCallback : DiffUtil.ItemCallback<Stylist>() {
        override fun areItemsTheSame(oldItem: Stylist, newItem: Stylist): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Stylist, newItem: Stylist): Boolean {
            return oldItem == newItem
        }
    }
}