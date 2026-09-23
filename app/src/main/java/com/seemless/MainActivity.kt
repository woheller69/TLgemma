package com.seemless

import android.os.Bundle
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import io.shubham0204.smollm.SmolLM
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

class MainActivity : AppCompatActivity() {

    private lateinit var tvResult: TextView
    private lateinit var progressBar: ProgressBar
    private var smolLM: SmolLM? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        tvResult = findViewById(R.id.tvResult)
        progressBar = findViewById(R.id.progressBar)

        // Initialize progress bar state
        progressBar.isIndeterminate = true
        progressBar.visibility = ProgressBar.GONE

        val modelFile = File(getExternalFilesDir(null), "translategemma-4b-it.Q4_K_M.gguf")

        if (!modelFile.exists()) {
            tvResult.text = "❌ Model file not found:\n${modelFile.absolutePath}"
            return
        }

        // Load model with progress indicator
        loadModelWithProgress(modelFile)
    }

    private fun loadModelWithProgress(modelFile: File) {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                // Show progress bar on main thread before starting
                withContext(Dispatchers.Main) {
                    progressBar.visibility = ProgressBar.VISIBLE
                    tvResult.text = "Loading model..."
                }

                val smolLMInstance = SmolLM()
                val params = SmolLM.InferenceParams(contextSize = 2048, storeChats = false, temperature = 0.2f)

                // Load the model (this may take 10-60 seconds depending on device)
                smolLMInstance.load(modelFile.absolutePath, params)

                // Store reference for later inference
                this@MainActivity.smolLM = smolLMInstance

                // Model loaded successfully - hide progress and trigger inference
                withContext(Dispatchers.Main) {
                    progressBar.visibility = ProgressBar.GONE
                    tvResult.text = "✅ Model loaded. Processing..."
                    processTranslationRequest()
                }

            } catch (e: Exception) {
                // Handle any error during loading
                withContext(Dispatchers.Main) {
                    progressBar.visibility = ProgressBar.GONE
                    tvResult.text = "❌ Error loading model:\n${e.message ?: "Unknown error"}"
                    Toast.makeText(
                        this@MainActivity,
                        "Failed to load model: ${e.message}",
                        Toast.LENGTH_LONG
                    ).show()
                }
                e.printStackTrace()
            }
        }
    }

    private fun processTranslationRequest() {
        val smolLM = this.smolLM ?: run {
            tvResult.text = "❌ Model not initialized"
            return
        }

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                // Show indeterminate progress during inference
                withContext(Dispatchers.Main) {
                    progressBar.visibility = ProgressBar.VISIBLE
                    progressBar.isIndeterminate = true
                    tvResult.text = "🔄 Translating..."
                }

                val requestJson = """
                    {
                        "type": "text",
                        "source_lang_code": "en",
                        "target_lang_code": "de-DE",
                        "text": "This is a test."
                    }
                """.trimIndent()

                // Get streaming response from model
                var response =""
                lifecycleScope.launch {
                    smolLM.getResponseAsFlow(requestJson)
                        .collect { token ->
                            response += token
                            withContext(Dispatchers.Main) {
                                progressBar.visibility = ProgressBar.GONE
                                tvResult.text = response.ifEmpty { "⚠️ Empty response" }
                            }

                        }
                }

                // Display result on main thread
                withContext(Dispatchers.Main) {
                    progressBar.visibility = ProgressBar.GONE
                }

            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    progressBar.visibility = ProgressBar.GONE
                    tvResult.text = "❌ Error during inference:\n${e.message}"
                    Toast.makeText(
                        this@MainActivity,
                        "Inference failed: ${e.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
                e.printStackTrace()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        // Clean up native resources when Activity is destroyed
        smolLM?.let {
            try {
                // If your library has an unload/close method, call it here:
                // it.unload() // or it.close()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        smolLM = null
    }
}