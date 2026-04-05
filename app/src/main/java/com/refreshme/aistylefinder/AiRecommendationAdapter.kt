package com.refreshme.aistylefinder

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.refreshme.R

class AiRecommendationAdapter(
    private var recommendations: List<AiStyleRecommendation> = emptyList()
) : RecyclerView.Adapter<AiRecommendationAdapter.RecommendationViewHolder>() {

    fun updateData(newData: List<AiStyleRecommendation>) {
        recommendations = newData
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecommendationViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_ai_recommendation, parent, false)
        return RecommendationViewHolder(view)
    }

    override fun onBindViewHolder(holder: RecommendationViewHolder, position: Int) {
        holder.bind(recommendations[position])
    }

    override fun getItemCount() = recommendations.size

    class RecommendationViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val ivStyleImage: ImageView = itemView.findViewById(R.id.ivStyleImage)
        private val tvStyleName: TextView = itemView.findViewById(R.id.tvStyleName)
        private val tvReasoning: TextView = itemView.findViewById(R.id.tvReasoning)

        fun bind(recommendation: AiStyleRecommendation) {
            tvStyleName.text = recommendation.styleName
            tvReasoning.text = recommendation.reasoning

            if (!recommendation.imageUrl.isNullOrBlank()) {
                ivStyleImage.visibility = View.VISIBLE
                Glide.with(itemView.context)
                    .load(recommendation.imageUrl)
                    .centerCrop()
                    .placeholder(R.drawable.ic_style_finder)
                    .into(ivStyleImage)
            } else {
                ivStyleImage.visibility = View.GONE
            }
        }
    }
}