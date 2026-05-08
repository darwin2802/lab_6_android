package com.example.lab_6

import android.graphics.Bitmap
import java.util.UUID

data class ProjectItem(
    val name: String,
    val preview: Bitmap?,
    val width: Int,
    val height: Int,
    val id: String = UUID.randomUUID().toString(),
    val date: Long = System.currentTimeMillis()
)

object ProjectHistory {
    val savedProjects = mutableListOf<ProjectItem>()

    fun addProject(project: ProjectItem) {
        savedProjects.add(0, project)
    }

    fun deleteProject(id: String) {
        savedProjects.removeAll { it.id == id }
    }

    fun clearHistory() {
        savedProjects.clear()
    }
}