package com.jx.claude

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import android.util.Base64
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Spinner
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.switchmaterial.SwitchMaterial
import com.google.android.material.textfield.TextInputEditText
import com.jx.claude.adapter.ChatAdapter
import com.jx.claude.adapter.ChatListAdapter
import com.jx.claude.databinding.ActivityMainBinding
import com.jx.claude.models.Attachment
import com.jx.claude.models.ModelInfo
import com.jx.claude.viewmodel.ChatViewModel
import java.io.ByteArrayOutputStream

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val viewModel: ChatViewModel by viewModels()
    private lateinit var chatAdapter: ChatAdapter
    private lateinit var chatListAdapter: ChatListAdapter

    private val pickImageLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let { handlePickedImage(it) }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupRecyclerView()
        setupDrawer()
        setupClickListeners()
        observeViewModel()

        if (!viewModel.isApiKeySet()) {
            showSettingsDialog()
        } else {
            viewModel.fetchModels()
        }
    }

    private fun setupRecyclerView() {
        chatAdapter = ChatAdapter()
        binding.recyclerChat.apply {
            layoutManager = LinearLayoutManager(this@MainActivity).apply {
                stackFromEnd = true
            }
            adapter = chatAdapter
        }
    }

    private fun setupDrawer() {
        chatListAdapter = ChatListAdapter(
            onClick = { session ->
                viewModel.switchToSession(session)
                binding.drawerLayout.closeDrawers()
            },
            onDelete = { session ->
                AlertDialog.Builder(this, R.style.Theme_JxClaude_Dialog)
                    .setTitle("Delete chat?")
                    .setMessage("\"${session.title}\" will be permanently deleted.")
                    .setPositiveButton("Delete") { _, _ -> viewModel.deleteSession(session) }
                    .setNegativeButton("Cancel", null)
                    .show()
            }
        )
        binding.recyclerChats.apply {
            layoutManager = LinearLayoutManager(this@MainActivity)
            adapter = chatListAdapter
        }

        binding.btnNewChat.setOnClickListener {
            viewModel.createNewChat()
            binding.drawerLayout.closeDrawers()
        }
    }

    private fun setupClickListeners() {
        binding.btnMenu.setOnClickListener {
            binding.drawerLayout.openDrawer(binding.drawerContent)
        }

        binding.btnSettings.setOnClickListener {
            showSettingsDialog()
        }

        binding.btnAttach.setOnClickListener {
            pickImageLauncher.launch("image/*")
        }

        binding.btnRemoveAttachment.setOnClickListener {
            viewModel.clearAttachments()
        }

        binding.btnSend.setOnClickListener {
            if (viewModel.isLoading.value == true) {
                viewModel.stopStreaming()
                return@setOnClickListener
            }

            val text = binding.etMessage.text.toString().trim()
            if (text.isNotEmpty()) {
                if (!viewModel.isApiKeySet()) {
                    showSettingsDialog()
                    return@setOnClickListener
                }
                viewModel.sendMessage(text)
                binding.etMessage.text?.clear()
            }
        }
    }

    @Deprecated("This method has been deprecated in favor of using the\n      {@link OnBackPressedDispatcher} via {@link #getOnBackPressedDispatcher()}.\n      The OnBackPressedDispatcher controls how back button events are dispatched\n      to one or more {@link OnBackPressedCallback} objects.")
    override fun onBackPressed() {
        if (binding.drawerLayout.isDrawerOpen(binding.drawerContent)) {
            binding.drawerLayout.closeDrawers()
        } else {
            super.onBackPressed()
        }
    }

    // ── Image handling ──────────────────────────────────────────

    private fun handlePickedImage(uri: Uri) {
        try {
            val mimeType = contentResolver.getType(uri) ?: "image/jpeg"
            val inputStream = contentResolver.openInputStream(uri) ?: return
            val bitmap = BitmapFactory.decodeStream(inputStream)
            inputStream.close()

            if (bitmap == null) {
                Toast.makeText(this, "Could not decode image", Toast.LENGTH_SHORT).show()
                return
            }

            // Resize if too large (max 1568px on longest side, Anthropic recommended)
            val maxDim = 1568
            val scaled = if (bitmap.width > maxDim || bitmap.height > maxDim) {
                val scale = maxDim.toFloat() / maxOf(bitmap.width, bitmap.height)
                Bitmap.createScaledBitmap(
                    bitmap,
                    (bitmap.width * scale).toInt(),
                    (bitmap.height * scale).toInt(),
                    true
                )
            } else {
                bitmap
            }

            val outputStream = ByteArrayOutputStream()
            val format = if (mimeType == "image/png") Bitmap.CompressFormat.PNG
            else Bitmap.CompressFormat.JPEG
            scaled.compress(format, 85, outputStream)

            val base64 = Base64.encodeToString(outputStream.toByteArray(), Base64.NO_WRAP)
            val finalMime = if (mimeType == "image/png") "image/png" else "image/jpeg"
            val fileName = getFileName(uri)

            viewModel.addAttachment(Attachment(finalMime, base64, fileName))
        } catch (e: Exception) {
            Toast.makeText(this, "Failed to load image: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun getFileName(uri: Uri): String? {
        var name: String? = null
        contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            val idx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (cursor.moveToFirst() && idx >= 0) {
                name = cursor.getString(idx)
            }
        }
        return name
    }

    // ── Observe ─────────────────────────────────────────────────

    private fun observeViewModel() {
        viewModel.messages.observe(this) { messages ->
            chatAdapter.submitList(messages.toList()) {
                if (messages.isNotEmpty()) {
                    binding.recyclerChat.scrollToPosition(messages.size - 1)
                }
            }
            binding.tvEmptyState.visibility =
                if (messages.isEmpty()) View.VISIBLE else View.GONE
        }

        viewModel.isLoading.observe(this) { loading ->
            binding.progressBar.visibility = if (loading) View.VISIBLE else View.GONE
            binding.btnSend.setImageResource(
                if (loading) android.R.drawable.ic_media_pause
                else android.R.drawable.ic_menu_send
            )
        }

        viewModel.error.observe(this) { error ->
            error?.let {
                Toast.makeText(this, it, Toast.LENGTH_LONG).show()
            }
        }

        viewModel.currentSession.observe(this) { session ->
            binding.tvTitle.text = session?.title ?: "New Chat"
            chatListAdapter.selectedId = session?.id
        }

        viewModel.chatSessions.observe(this) { sessions ->
            chatListAdapter.submitList(sessions.toList())
        }

        viewModel.pendingAttachments.observe(this) { attachments ->
            if (attachments.isNullOrEmpty()) {
                binding.attachmentPreview.visibility = View.GONE
            } else {
                binding.attachmentPreview.visibility = View.VISIBLE
                binding.tvAttachmentInfo.text =
                    attachments.joinToString(", ") { "📎 ${it.fileName ?: "image"}" }
            }
        }
    }

    // ── Settings dialog ─────────────────────────────────────────

    private fun showSettingsDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_settings, null)
        val etApiKey = dialogView.findViewById<TextInputEditText>(R.id.etApiKey)
        val spinnerModel = dialogView.findViewById<Spinner>(R.id.spinnerModel)
        val switchThinking = dialogView.findViewById<SwitchMaterial>(R.id.switchThinking)
        val layoutBudget = dialogView.findViewById<View>(R.id.layoutBudget)
        val etBudgetTokens = dialogView.findViewById<TextInputEditText>(R.id.etBudgetTokens)
        val switchSearch = dialogView.findViewById<SwitchMaterial>(R.id.switchSearch)
        val etMaxTokens = dialogView.findViewById<TextInputEditText>(R.id.etMaxTokens)
        val etSystemPrompt = dialogView.findViewById<TextInputEditText>(R.id.etSystemPrompt)

        val prefs = viewModel.prefs

        etApiKey.setText(prefs.apiKey)
        etBudgetTokens.setText(prefs.thinkingBudget.toString())
        etMaxTokens.setText(prefs.maxTokens.toString())
        etSystemPrompt.setText(prefs.systemPrompt)
        switchThinking.isChecked = prefs.thinkingEnabled
        switchSearch.isChecked = prefs.searchEnabled
        layoutBudget.visibility = if (prefs.thinkingEnabled) View.VISIBLE else View.GONE

        switchThinking.setOnCheckedChangeListener { _, checked ->
            layoutBudget.visibility = if (checked) View.VISIBLE else View.GONE
        }

        setupModelSpinner(spinnerModel, prefs.selectedModel)

        val dialog = AlertDialog.Builder(this, R.style.Theme_JxClaude_Dialog)
            .setTitle("⚙️  Settings")
            .setView(dialogView)
            .setPositiveButton("Save") { _, _ ->
                val newKey = etApiKey.text.toString().trim()
                val oldKey = prefs.apiKey

                prefs.apiKey = newKey
                prefs.thinkingEnabled = switchThinking.isChecked
                prefs.thinkingBudget =
                    etBudgetTokens.text.toString().toIntOrNull() ?: 10000
                prefs.searchEnabled = switchSearch.isChecked
                prefs.maxTokens = etMaxTokens.text.toString().toIntOrNull() ?: 4096
                prefs.systemPrompt = etSystemPrompt.text.toString()

                val selectedPosition = spinnerModel.selectedItemPosition
                val models = viewModel.availableModels.value ?: emptyList()
                if (selectedPosition in models.indices) {
                    prefs.selectedModel = models[selectedPosition].id
                }

                if (newKey != oldKey && newKey.isNotBlank()) {
                    com.jx.claude.api.RetrofitClient.invalidate()
                    viewModel.fetchModels()
                }

                Toast.makeText(this, "Settings saved ✓", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Cancel", null)
            .create()

        dialog.show()
    }

    private fun setupModelSpinner(spinner: Spinner, currentModel: String) {
        val models = viewModel.availableModels.value ?: ChatViewModel.FALLBACK_MODELS

        val displayNames = models.map { it.displayName ?: it.id }
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, displayNames)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinner.adapter = adapter

        val currentIndex = models.indexOfFirst { it.id == currentModel }
        if (currentIndex >= 0) {
            spinner.setSelection(currentIndex)
        }

        viewModel.availableModels.observe(this) { updatedModels ->
            val names = updatedModels.map { it.displayName ?: it.id }
            val newAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, names)
            newAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            spinner.adapter = newAdapter
            val idx = updatedModels.indexOfFirst { it.id == currentModel }
            if (idx >= 0) spinner.setSelection(idx)
        }
    }
}