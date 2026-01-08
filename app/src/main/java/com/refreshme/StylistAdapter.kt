package com.refreshme

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.refreshme.data.Stylist
import com.refreshme.data.StylistItem
import com.refreshme.databinding.ItemStylistBinding

class StylistAdapter(
    private val onStylistClicked: (Stylist) -> Unit,
    private val onFavoriteClicked: (Stylist) -> Unit
) : ListAdapter<StylistItem, StylistAdapter.StylistViewHolder>(StylistDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): StylistViewHolder {
        val binding: ItemStylistBinding = DataBindingUtil.inflate(
            LayoutInflater.from(parent.context),
            R.layout.item_stylist,
            parent,
            false
        )
        val viewHolder = StylistViewHolder(binding)

        viewHolder.itemView.setOnClickListener {
            val position = viewHolder.bindingAdapterPosition
            if (position != RecyclerView.NO_POSITION) {
                onStylistClicked(getItem(position).stylist)
            }
        }

        viewHolder.binding.favoriteButton.setOnClickListener {
            val position = viewHolder.bindingAdapterPosition
            if (position != RecyclerView.NO_POSITION) {
                onFavoriteClicked(getItem(position).stylist)
            }
        }

        return viewHolder
    }

    override fun onBindViewHolder(holder: StylistViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class StylistViewHolder(
        val binding: ItemStylistBinding
    ) : RecyclerView.ViewHolder(binding.root) {
        fun bind(item: StylistItem) {
            binding.stylist = item.stylist
            binding.isFavorite = item.isFavorite
            binding.executePendingBindings()
        }
    }
}

class StylistDiffCallback : DiffUtil.ItemCallback<StylistItem>() {

    override fun areItemsTheSame(oldItem: StylistItem, newItem: StylistItem):
            Boolean {
        return oldItem.stylist.id == newItem.stylist.id
    }

    override fun areContentsTheSame(oldItem: StylistItem, newItem: StylistItem):
            Boolean {
        return oldItem == newItem
    }
}
