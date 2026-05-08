package com.example.lab_6

import android.app.Activity
import android.content.ContentValues
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.ImageDecoder
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.view.View
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.lab_6.databinding.ActivityMainBinding
import java.io.OutputStream
import java.util.*

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private var activeTool: ImageButton? = null

    private val pickImage = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val uri = result.data?.data ?: return@registerForActivityResult
            try {
                val bitmap = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    ImageDecoder.decodeBitmap(ImageDecoder.createSource(contentResolver, uri))
                } else {
                    @Suppress("DEPRECATION")
                    MediaStore.Images.Media.getBitmap(contentResolver, uri)
                }.copy(Bitmap.Config.ARGB_8888, true)

                binding.drawingView.addImageToCanvas(bitmap)
                Toast.makeText(this, "Image added to current layer", Toast.LENGTH_SHORT).show()
                updateLayersList()
            } catch (e: Exception) {
                Toast.makeText(this, "Error loading photo: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val canvasWidth = intent.getIntExtra("CANVAS_WIDTH", 1080)
        val canvasHeight = intent.getIntExtra("CANVAS_HEIGHT", 1080)
        val isTop = intent.getBooleanExtra("PANEL_TOP", false)

        setupPanelPosition(isTop)

        binding.drawingView.post {
            binding.drawingView.initCanvas(canvasWidth, canvasHeight)
            setupLayersUI()
        }

        initControls()
    }

    private fun initControls() {
        highlightTool(binding.btnBrush)

        binding.brushSizeSlider.addOnChangeListener { _, value, _ ->
            binding.drawingView.setBrushSize(value)
        }

        binding.alphaSlider.addOnChangeListener { _, value, _ ->
            binding.drawingView.setBrushAlpha(value.toInt())
        }

        binding.btnBrush.setOnClickListener {
            highlightTool(it as ImageButton)
            binding.drawingView.setEraserMode(false)
            binding.drawingView.setFillMode(false)
            binding.drawingView.setTextMode(false)
            showBrushDialog()
        }

        binding.btnEraser.setOnClickListener {
            highlightTool(it as ImageButton)
            binding.drawingView.setEraserMode(true)
        }

        binding.btnFill.setOnClickListener {
            highlightTool(it as ImageButton)
            binding.drawingView.setFillMode(true)
        }

        binding.btnUndo.setOnClickListener {
            binding.drawingView.undo()
            Toast.makeText(this, "Undo successful", Toast.LENGTH_SHORT).show()
        }

        binding.btnText.setOnClickListener {
            highlightTool(it as ImageButton)
            binding.drawingView.setTextMode(true)

            val selected = binding.drawingView.getSelectedText()
            if (selected != null) {
                showEditDialogForText(selected)
            } else {
                showTextInputDialog()
            }
        }

        binding.btnImage.setOnClickListener {
            val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
            pickImage.launch(intent)
        }

        binding.btnPickColor.setOnClickListener { showColorPicker() }

        binding.btnShowLayers.setOnClickListener {
            val isVisible = binding.layersPanel.visibility == View.VISIBLE
            binding.layersPanel.visibility = if (isVisible) View.GONE else View.VISIBLE
            if (!isVisible) updateLayersList()
        }

        binding.btnAddLayer.setOnClickListener {
            binding.drawingView.addLayer(binding.drawingView.width, binding.drawingView.height)
            updateLayersList()
            Toast.makeText(this, "New Layer Added", Toast.LENGTH_SHORT).show()
        }

        binding.btnSave.setOnClickListener {
            showExportFormatDialog()
        }

        binding.btnBack.setOnClickListener {
            autoSaveToHistoryAndExit()
        }
    }

    private fun showBrushDialog() {
        val brushes = arrayOf("Standard", "Marker", "Airbrush", "Watercolor", "Pencil", "Pastel")
        val keys = arrayOf("NORMAL", "MARKER", "AIRBRUSH", "WATERCOLOR", "PENCIL", "PASTEL")

        AlertDialog.Builder(this)
            .setTitle("Select Brush Type")
            .setItems(brushes) { _, i ->
                binding.drawingView.setBrushType(keys[i])
                Toast.makeText(this, "Brush: ${brushes[i]}", Toast.LENGTH_SHORT).show()
            }.show()
    }

    private fun showTextInputDialog() {
        val input = EditText(this)
        input.hint = "Type something..."
        AlertDialog.Builder(this)
            .setTitle("Add Text")
            .setView(input)
            .setPositiveButton("Add") { _, _ ->
                val text = input.text.toString()
                if (text.isNotEmpty()) {
                    binding.drawingView.addTextObject(text)
                    updateLayersList()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showEditDialogForText(textObj: DrawingView.TextObject) {
        val options = arrayOf("Edit Content", "Change Color", "Delete Text")
        AlertDialog.Builder(this)
            .setTitle("Text Options")
            .setItems(options) { _, i ->
                when(i) {
                    0 -> {
                        val input = EditText(this).apply { setText(textObj.content) }
                        AlertDialog.Builder(this).setTitle("Edit").setView(input)
                            .setPositiveButton("Apply") { _, _ ->
                                textObj.content = input.text.toString()
                                binding.drawingView.invalidate()
                            }.show()
                    }
                    1 -> showColorPicker { color ->
                        textObj.color = color
                        binding.drawingView.invalidate()
                    }
                    2 -> {
                        binding.drawingView.removeSelectedText()
                        Toast.makeText(this, "Deleted", Toast.LENGTH_SHORT).show()
                    }
                }
            }.show()
    }

    private fun showExportFormatDialog() {
        val formats = arrayOf("PNG (Transparent)", "JPG (High Quality)")
        AlertDialog.Builder(this)
            .setTitle("Export Format")
            .setItems(formats) { _, i ->
                val format = if (i == 0) Bitmap.CompressFormat.PNG else Bitmap.CompressFormat.JPEG
                val ext = if (i == 0) "png" else "jpg"
                val mime = if (i == 0) "image/png" else "image/jpeg"
                exportImageToGallery(binding.drawingView.getBitmap(), format, ext, mime)
            }.show()
    }

    private fun exportImageToGallery(bitmap: Bitmap, format: Bitmap.CompressFormat, ext: String, mime: String) {
        val name = "Art_${System.currentTimeMillis()}.$ext"
        var out: OutputStream? = null
        try {
            val vals = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, name)
                put(MediaStore.MediaColumns.MIME_TYPE, mime)
                put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/ArtStudio")
            }
            val uri = contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, vals)
            out = uri?.let { contentResolver.openOutputStream(it) }
            if (out != null) {
                bitmap.compress(format, 100, out)
                Toast.makeText(this, "Saved to Gallery", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            Toast.makeText(this, "Failed: ${e.message}", Toast.LENGTH_SHORT).show()
        } finally {
            out?.close()
        }
    }

    private fun autoSaveToHistoryAndExit() {
        val previewBmp = binding.drawingView.getBitmap()
        val projectName = intent.getStringExtra("PROJECT_NAME") ?: "Project ${ProjectHistory.savedProjects.size + 1}"

        val project = ProjectItem(
            name = projectName,
            preview = previewBmp,
            width = previewBmp.width,
            height = previewBmp.height
        )

        ProjectHistory.addProject(project)

        Toast.makeText(this, "Project Saved to History", Toast.LENGTH_SHORT).show()
        finish()
    }

    private fun setupLayersUI() {
        binding.rvLayers.layoutManager = LinearLayoutManager(this)
        updateLayersList()

        val helper = ItemTouchHelper(object : ItemTouchHelper.SimpleCallback(
            ItemTouchHelper.UP or ItemTouchHelper.DOWN, 0
        ) {
            override fun onMove(rv: RecyclerView, vh: RecyclerView.ViewHolder, target: RecyclerView.ViewHolder): Boolean {
                val from = vh.adapterPosition
                val to = target.adapterPosition

                Collections.swap(binding.drawingView.layers, from, to)

                rv.adapter?.notifyItemMoved(from, to)
                binding.drawingView.invalidate()
                return true
            }

            override fun onSwiped(vh: RecyclerView.ViewHolder, dir: Int) {}
        })
        helper.attachToRecyclerView(binding.rvLayers)
    }

    private fun updateLayersList() {
        val adapter = LayerAdapter(binding.drawingView.layers) { position ->
            showLayerOptions(position)
        }
        binding.rvLayers.adapter = adapter
    }

    private fun showLayerOptions(pos: Int) {
        val layer = binding.drawingView.layers[pos]
        val options = arrayOf(
            "Activate (Draw)",
            "Merge Down",
            "Rename Layer",
            if (layer.isVisible) "Hide Layer" else "Show Layer",
            "Delete Layer"
        )

        AlertDialog.Builder(this)
            .setTitle(layer.name)
            .setItems(options) { _, i ->
                when (i) {
                    0 -> {
                        binding.drawingView.currentLayerIndex = pos
                        Toast.makeText(this, "${layer.name} Active", Toast.LENGTH_SHORT).show()
                    }
                    1 -> {
                        if (pos < binding.drawingView.layers.size - 1) {
                            binding.drawingView.mergeLayersDown(pos)
                        } else {
                            Toast.makeText(this, "Bottom layer reached", Toast.LENGTH_SHORT).show()
                        }
                    }
                    2 -> {
                        val et = EditText(this).apply { setText(layer.name) }
                        AlertDialog.Builder(this).setTitle("Rename").setView(et)
                            .setPositiveButton("Save") { _, _ ->
                                layer.name = et.text.toString()
                                updateLayersList()
                            }.show()
                    }
                    3 -> layer.isVisible = !layer.isVisible
                    4 -> if (binding.drawingView.layers.size > 1 && !layer.isBackground) {
                        binding.drawingView.layers.removeAt(pos)
                        binding.drawingView.currentLayerIndex = 0
                    } else {
                        Toast.makeText(this, "Cannot delete background", Toast.LENGTH_SHORT).show()
                    }
                }
                updateLayersList()
                binding.drawingView.invalidate()
            }.show()
    }

    private fun highlightTool(btn: ImageButton) {
        activeTool?.let {
            it.background = null
            it.imageTintList = ColorStateList.valueOf(Color.parseColor("#424242"))
        }

        val shape = GradientDrawable().apply {
            shape = GradientDrawable.OVAL
            setColor(Color.parseColor("#E0E0E0"))
        }

        btn.background = shape
        btn.imageTintList = ColorStateList.valueOf(Color.parseColor("#6200EE"))
        activeTool = btn
    }

    private fun showColorPicker(onSelected: ((Int) -> Unit)? = null) {
        val wheel = ColorWheelView(this)

        val initialColor = if (onSelected != null) Color.BLACK else binding.drawingView.drawPaint.color
        wheel.setOldColor(initialColor)

        val frame = FrameLayout(this).apply {
            setPadding(64, 64, 64, 64)
            addView(wheel)
        }

        AlertDialog.Builder(this)
            .setTitle("Pick Color")
            .setView(frame)
            .setPositiveButton("Select") { _, _ ->
                val color = wheel.getSelectedColor()
                if (onSelected != null) {
                    onSelected(color)
                } else {
                    binding.drawingView.setColor(color)
                    binding.btnPickColor.imageTintList = ColorStateList.valueOf(color)
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun setupPanelPosition(isTop: Boolean) {
        if (isTop) {
            val pParams = binding.bottomPanel.layoutParams as ConstraintLayout.LayoutParams
            pParams.bottomToBottom = ConstraintLayout.LayoutParams.UNSET
            pParams.topToTop = ConstraintLayout.LayoutParams.PARENT_ID
            binding.bottomPanel.layoutParams = pParams

            val cParams = binding.canvasContainer.layoutParams as ConstraintLayout.LayoutParams
            cParams.topToBottom = binding.bottomPanel.id
            cParams.bottomToBottom = ConstraintLayout.LayoutParams.PARENT_ID
            binding.canvasContainer.layoutParams = cParams
        }
    }
}