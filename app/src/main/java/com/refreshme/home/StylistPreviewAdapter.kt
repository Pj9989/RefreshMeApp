package com.refreshme.home

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.refreshme.R
import com.refreshme.data.Stylist
import java.util.Locale

class StylistPreviewAdapter(
    private val stylists: List<Stylist>,
    private val onStylistClicked: (Stylist, ImageView) -> Unit
) : RecyclerView.Adapter<StylistPreviewAdapter.StylistViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): StylistViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_stylist_preview, parent, false)
        return StylistViewHolder(view)
    }

    override fun onBindViewHolder(holder: StylistViewHolder, position: Int) {
        val stylist = stylists[position]
        holder.bind(stylist)
        holder.itemView.setOnClickListener { onStylistClicked(stylist, holder.stylistImage) }
    }

    override fun getItemCount() = stylists.size

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
            
            Glide.with(itemView.context)
                .load(stylist.profileImageUrl)
                .placeholder(R.drawable.ic_launcher_background)
                .into(stylistImage)
                
            stylistName.text = stylist.name
            stylistService.text = stylist.services?.getOrNull(0)?.name ?: stylist.specialty ?: "Full Service"
            stylistRating.text = String.format(Locale.US, "%.1f", stylist.rating)
            stylistDistance.text = String.format(Locale.US, "%.1f mi", stylist.distance)
            
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
}