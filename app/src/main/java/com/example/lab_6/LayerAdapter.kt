package com.example.lab_6

import android.graphics.Color
import android.graphics.Typeface
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class LayerAdapter(
    private val layers: List<DrawingView.Layer>,
    private val onLayerClick: (Int) -> Unit
) : RecyclerView.Adapter<LayerAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val title: TextView = view.findViewById(android.R.id.text1)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(android.R.layout.simple_list_item_1, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val layer = layers[position]
        val context = holder.itemView.context

        val drawingView = (context as MainActivity).findViewById<DrawingView>(R.id.drawingView)
        val isActive = position == drawingView.currentLayerIndex

        if (!layer.isVisible) {
            holder.title.text = "${layer.name} (Hidden)"
            holder.title.setTextColor(Color.LTGRAY)
            holder.title.setTypeface(null, Typeface.ITALIC)
        } else {
            holder.title.text = layer.name
            holder.title.setTextColor(Color.BLACK)
            holder.title.setTypeface(null, Typeface.NORMAL)
        }

        if (isActive) {
            holder.itemView.setBackgroundColor(Color.parseColor("#E0E0E0"))
            holder.title.setTextColor(Color.parseColor("#6200EE"))
            holder.title.setTypeface(null, Typeface.BOLD)

            if (!layer.isVisible) {
                holder.title.setTypeface(null, Typeface.BOLD_ITALIC)
            }
        } else {
            holder.itemView.setBackgroundColor(Color.TRANSPARENT)
        }

        holder.itemView.setOnClickListener {
            onLayerClick(position)
        }

        holder.itemView.setOnLongClickListener {
            drawingView.currentLayerIndex = position
            notifyDataSetChanged()
            true
        }
    }

    override fun getItemCount(): Int {
        return layers.size
    }
}