package com.example.lab_6

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class HistoryAdapter(
    private val projects: List<ProjectItem>,
    private val onClick: (ProjectItem) -> Unit
) : RecyclerView.Adapter<HistoryAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val imgPreview: ImageView = view.findViewById(R.id.imgPreview)
        val tvName: TextView = view.findViewById(R.id.tvProjectName)
        val tvInfo: TextView = view.findViewById(R.id.tvProjectInfo)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_project_history, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val project = projects[position]
        holder.tvName.text = project.name
        holder.tvInfo.text = "${project.width} x ${project.height} px"

        if (project.preview != null) {
            holder.imgPreview.setImageBitmap(project.preview)
        } else {
            holder.imgPreview.setImageResource(android.R.drawable.ic_menu_gallery)
        }

        holder.itemView.setOnClickListener { onClick(project) }
    }

    override fun getItemCount(): Int = projects.size
}