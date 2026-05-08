package com.example.lab_6

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButtonToggleGroup
import com.google.android.material.materialswitch.MaterialSwitch
import com.google.android.material.textfield.TextInputEditText

class StartActivity : AppCompatActivity() {
    private var isPanelTop = false
    private lateinit var rvHistory: RecyclerView
    private lateinit var tvNoHistory: TextView
    private lateinit var historyAdapter: HistoryAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_start)

        val switchDark = findViewById<MaterialSwitch>(R.id.switchDarkTheme)
        val toggleGroup = findViewById<MaterialButtonToggleGroup>(R.id.togglePanelPosition)
        val btnCreate = findViewById<Button>(R.id.btnShowCreateDialog)

        rvHistory = findViewById(R.id.rvRecentHistory)
        tvNoHistory = findViewById(R.id.tvEmptyHistory)

        setupRecyclerView()

        switchDark?.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
            } else {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
            }
        }

        toggleGroup?.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (isChecked) {
                isPanelTop = (checkedId == R.id.btnPosTop)
            }
        }

        btnCreate?.setOnClickListener {
            showCreateCanvasDialog()
        }
    }

    override fun onResume() {
        super.onResume()
        updateHistoryVisibility()
    }

    private fun setupRecyclerView() {
        rvHistory.layoutManager = LinearLayoutManager(this)

        historyAdapter = HistoryAdapter(ProjectHistory.savedProjects) { selectedProject: ProjectItem ->
            startDrawing(selectedProject.width, selectedProject.height, selectedProject.name)
        }
        rvHistory.adapter = historyAdapter
    }

    private fun updateHistoryVisibility() {
        if (ProjectHistory.savedProjects.isEmpty()) {
            tvNoHistory.visibility = View.VISIBLE
            rvHistory.visibility = View.GONE
        } else {
            tvNoHistory.visibility = View.GONE
            rvHistory.visibility = View.VISIBLE
            if (::historyAdapter.isInitialized) {
                historyAdapter.notifyDataSetChanged()
            }
        }
    }

    private fun showCreateCanvasDialog() {
        val view = LayoutInflater.from(this).inflate(R.layout.dialog_create_canvas, null)
        val editWidth = view.findViewById<TextInputEditText>(R.id.editWidth)
        val editHeight = view.findViewById<TextInputEditText>(R.id.editHeight)
        val editName = view.findViewById<TextInputEditText>(R.id.editProjectName)

        val dialog = AlertDialog.Builder(this)
            .setTitle("New Canvas")
            .setView(view)
            .setPositiveButton("Create") { _, _ ->
                val wText = editWidth?.text.toString()
                val hText = editHeight?.text.toString()
                val name = if (editName?.text.isNullOrEmpty()) "Untitled Art" else editName.text.toString()

                if (wText.isNotEmpty() && hText.isNotEmpty()) {
                    val width = try { wText.toInt() } catch (e: Exception) { 0 }
                    val height = try { hText.toInt() } catch (e: Exception) { 0 }

                    if (width in 100..5000 && height in 100..5000) {
                        startDrawing(width, height, name)
                    } else {
                        Toast.makeText(this, "Size must be between 100 and 5000 px", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Toast.makeText(this, "Please enter width and height", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .create()

        view.findViewById<Button>(R.id.btnFormatA4)?.setOnClickListener {
            startDrawing(2480, 3508, "A4 Project")
            dialog.dismiss()
        }

        view.findViewById<Button>(R.id.btnFormatSquare)?.setOnClickListener {
            startDrawing(2000, 2000, "Square Project")
            dialog.dismiss()
        }

        dialog.show()
    }

    private fun startDrawing(width: Int, height: Int, name: String) {
        val intent = Intent(this, MainActivity::class.java).apply {
            putExtra("PROJECT_NAME", name)
            putExtra("CANVAS_WIDTH", width)
            putExtra("CANVAS_HEIGHT", height)
            putExtra("PANEL_TOP", isPanelTop)
        }
        startActivity(intent)
    }
}