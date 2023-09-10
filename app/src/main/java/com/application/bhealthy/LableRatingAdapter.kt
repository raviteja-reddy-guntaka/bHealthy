package com.application.bhealthy

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.RatingBar
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class LabelRatingAdapter(private val data: List<LabelRating>) :
    RecyclerView.Adapter<LabelRatingAdapter.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.list_item, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = data[position]
        holder.textLabel.text = item.label
        holder.ratingBar.rating = item.rating
    }

    override fun getItemCount(): Int {
        return data.size
    }

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val textLabel: TextView = itemView.findViewById(R.id.textLabel)
        val ratingBar: RatingBar = itemView.findViewById(R.id.ratingBar)
    }
}
