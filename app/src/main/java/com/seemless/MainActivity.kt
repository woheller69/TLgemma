package com.seemless

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.view.View
import android.widget.Button
import android.widget.ProgressBar
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.lifecycle.lifecycleScope
import io.shubham0204.smollm.SmolLM
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

class MainActivity : AppCompatActivity() {

    private lateinit var etvResult: EditText
    private lateinit var etvInput: EditText
    private lateinit var progressBar: ProgressBar
    private lateinit var translateButton: Button
    private var smolLM: SmolLM? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        ThemeUtils.setStatusBarAppearance(this)
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
        etvResult = findViewById(R.id.etvResult)
        etvInput = findViewById(R.id.etvInput)
        progressBar = findViewById(R.id.progressBar)
        translateButton = findViewById(R.id.translateButton)

        translateButton.setOnClickListener(View.OnClickListener { view ->
            processTranslationRequest()
        })

        // Initialize progress bar state
        progressBar.isIndeterminate = true
        progressBar.visibility = ProgressBar.GONE

        val modelFile = File(getExternalFilesDir(null), "model.gguf")

        if (!modelFile.exists()) {
            val intent = Intent(this, SetupActivity::class.java)
            startActivity(intent)
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
                    etvResult.text = Editable.Factory.getInstance().newEditable("Loading model...")
                }

                val smolLMInstance = SmolLM()
                val params = SmolLM.InferenceParams(contextSize = 2048, storeChats = false, temperature = 0.01f)

                // Load the model (this may take 10-60 seconds depending on device)
                smolLMInstance.load(modelFile.absolutePath, params)

                // Store reference for later inference
                this@MainActivity.smolLM = smolLMInstance

                // Model loaded successfully - hide progress and trigger inference
                withContext(Dispatchers.Main) {
                    progressBar.visibility = ProgressBar.GONE
                    etvResult.text = Editable.Factory.getInstance().newEditable("✅ Model loaded.")
                    //processTranslationRequest()
                }

            } catch (e: Exception) {
                // Handle any error during loading
                withContext(Dispatchers.Main) {
                    progressBar.visibility = ProgressBar.GONE
                    etvResult.text = Editable.Factory.getInstance().newEditable("❌ Error loading model:\n${e.message ?: "Unknown error"}")
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
            etvResult.text = Editable.Factory.getInstance().newEditable("❌ Model not initialized")
            return
        }

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                // Show indeterminate progress during inference
                withContext(Dispatchers.Main) {
                    progressBar.visibility = ProgressBar.VISIBLE
                    progressBar.isIndeterminate = true
                    etvResult.text = Editable.Factory.getInstance().newEditable("🔄 Translating...")
                }

                val textToTranslate = etvInput.text.toString()

                val requestJson = """
                    {
                        "type": "text",
                        "source_lang_code": "en",
                        "target_lang_code": "de-DE",
                        "text": "$textToTranslate"
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
                                etvResult.text = Editable.Factory.getInstance().newEditable(response.ifEmpty { "⚠️ Empty response" })
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
                    etvResult.text = Editable.Factory.getInstance().newEditable("❌ Error during inference:\n${e.message}")
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