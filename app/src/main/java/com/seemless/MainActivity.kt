package com.seemless

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.view.View
import android.widget.*
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
    private lateinit var spinnerSource: Spinner
    private lateinit var spinnerTarget: Spinner
    private lateinit var btnSwap: Button
    private lateinit var etvCustomSource: EditText
    private lateinit var etvCustomTarget: EditText

    private var smolLM: SmolLM? = null

    // Language options matching the HTML frontend
    private val LANGUAGES_SRC = listOf(
        "auto",
        "en-US",
        "de-DE",
        "fr-FR",
        "es-ES",
        "it-IT",
        "ja-JP",
        "zh-CN",
        "ru-RU",
        "pt-PT",
        "pt-BR",
        "other-src"
    )

    private val LANGUAGES_TARGET = listOf(
        "en-US",
        "de-DE",
        "fr-FR",
        "es-ES",
        "it-IT",
        "ja-JP",
        "zh-CN",
        "ru-RU",
        "pt-PT",
        "pt-BR",
        "other-tgt"
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        ThemeUtils.setStatusBarAppearance(this)
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)

        etvResult = findViewById(R.id.etvResult)
        etvInput = findViewById(R.id.etvInput)
        progressBar = findViewById(R.id.progressBar)
        translateButton = findViewById(R.id.translateButton)
        spinnerSource = findViewById(R.id.spinnerSource)
        spinnerTarget = findViewById(R.id.spinnerTarget)
        btnSwap = findViewById(R.id.btnSwap)
        etvCustomSource = findViewById(R.id.etvCustomSource)
        etvCustomTarget = findViewById(R.id.etvCustomTarget)

        // Populate spinners
        val srcAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, LANGUAGES_SRC).apply {
            setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        }
        val tgtAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, LANGUAGES_TARGET).apply {
            setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        }
        spinnerSource.adapter = srcAdapter
        spinnerTarget.adapter = tgtAdapter

        // Default selections (auto / German)
        spinnerSource.setSelection(0)   // auto
        spinnerTarget.setSelection(1)  // de-DE

        // Show/hide custom input when "Other" is selected
        spinnerSource.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, pos: Int, id: Long) {
                etvCustomSource.visibility = if (LANGUAGES_SRC[pos] == "other-src") View.VISIBLE else View.GONE
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        spinnerTarget.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, pos: Int, id: Long) {
                etvCustomTarget.visibility = if (LANGUAGES_TARGET[pos] == "other-tgt") View.VISIBLE else View.GONE
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        // Swap button
        btnSwap.setOnClickListener {
            val srcPos = spinnerSource.selectedItemPosition
            val tgtPos = spinnerTarget.selectedItemPosition

            val tempLang = LANGUAGES_SRC[srcPos]
            spinnerSource.setSelection(tgtPos)
            spinnerTarget.setSelection(srcPos)

            // Swap custom values too
            if (tempLang == "other-src") {
                etvCustomTarget.setText(etvCustomSource.text.toString())
            }
            if (LANGUAGES_TARGET[tgtPos] == "other-tgt") {
                etvCustomTarget.visibility = View.VISIBLE
            } else {
                etvCustomTarget.visibility = View.GONE
            }
        }

        translateButton.setOnClickListener { processTranslationRequest() }

        // Init progress bar
        progressBar.isIndeterminate = true
        progressBar.visibility = ProgressBar.GONE

        val modelFile = File(getExternalFilesDir(null), "model.gguf")

        if (!modelFile.exists()) {
            startActivity(Intent(this, SetupActivity::class.java))
            return
        }

        loadModelWithProgress(modelFile)
    }

    /** Returns the resolved source language code. */
    private fun getSourceLang(): String {
        val selected = LANGUAGES_SRC[spinnerSource.selectedItemPosition]
        return if (selected == "other-src") etvCustomSource.text.toString().trim() else selected
    }

    /** Returns the resolved target language code. */
    private fun getTargetLang(): String {
        val selected = LANGUAGES_TARGET[spinnerTarget.selectedItemPosition]
        return if (selected == "other-tgt") etvCustomTarget.text.toString().trim() else selected
    }

    private fun loadModelWithProgress(modelFile: File) {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                withContext(Dispatchers.Main) {
                    progressBar.visibility = ProgressBar.VISIBLE
                    etvResult.text = Editable.Factory.getInstance().newEditable("Loading model…")
                }

                val smolLMInstance = SmolLM()
                val params = SmolLM.InferenceParams(
                    contextSize = 2048,
                    storeChats = false,
                    temperature = 0.01f
                )
                smolLMInstance.load(modelFile.absolutePath, params)

                this@MainActivity.smolLM = smolLMInstance

                withContext(Dispatchers.Main) {
                    progressBar.visibility = ProgressBar.GONE
                    etvResult.text = Editable.Factory.getInstance().newEditable("✅ Model loaded.")
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    progressBar.visibility = ProgressBar.GONE
                    etvResult.text = Editable.Factory.getInstance().newEditable("❌ Error loading model:\n${e.message ?: "Unknown"}")
                    Toast.makeText(this@MainActivity, "Failed to load model: ${e.message}", Toast.LENGTH_LONG).show()
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

        val srcLang = getSourceLang()
        val tgtLang = getTargetLang()

        // Validate
        if (srcLang.isEmpty()) {
            Toast.makeText(this, "Please enter a source language code.", Toast.LENGTH_SHORT).show()
            return
        }
        if (tgtLang.isEmpty()) {
            Toast.makeText(this, "Please enter a target language code.", Toast.LENGTH_SHORT).show()
            return
        }
        if (srcLang == tgtLang) {
            Toast.makeText(this, "Source and target languages must differ.", Toast.LENGTH_SHORT).show()
            return
        }

        val textToTranslate = escapeJson(etvInput.text.toString().trim())
        if (textToTranslate.isEmpty()) {
            Toast.makeText(this, "Please enter text to translate.", Toast.LENGTH_SHORT).show()
            return
        }

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                withContext(Dispatchers.Main) {
                    progressBar.visibility = ProgressBar.VISIBLE
                    progressBar.isIndeterminate = true
                    etvResult.text = Editable.Factory.getInstance().newEditable("🔄 Translating…")
                    translateButton.isEnabled = false
                }

                val requestJson = """
                    {
                        "type": "text",
                        "source_lang_code": "$srcLang",
                        "target_lang_code": "$tgtLang",
                        "text": $textToTranslate
                    }
                """.trimIndent()

                var response = ""
                smolLM.getResponseAsFlow(requestJson).collect { token ->
                    response += token
                    withContext(Dispatchers.Main) {
                        progressBar.visibility = ProgressBar.GONE
                        etvResult.text = Editable.Factory.getInstance().newEditable(
                            response.ifEmpty { "⚠️ Empty response" }
                        )
                    }
                }

                withContext(Dispatchers.Main) {
                    progressBar.visibility = ProgressBar.GONE
                    translateButton.isEnabled = true
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    progressBar.visibility = ProgressBar.GONE
                    etvResult.text = Editable.Factory.getInstance().newEditable("❌ Error:\n${e.message}")
                    Toast.makeText(this@MainActivity, "Inference failed: ${e.message}", Toast.LENGTH_SHORT).show()
                    translateButton.isEnabled = true
                }
                e.printStackTrace()
            }
        }
    }

    /** JSON-escape a string value for embedding inside a template literal. */
    private fun escapeJson(value: String): String {
        return value
            .replace("\\", "\\\\")
            .replace("\"", "\\\"")
            .replace("\n", "\\n")
            .replace("\r", "\\r")
            .replace("\t", "\\t")
    }

    override fun onDestroy() {
        super.onDestroy()
        smolLM = null
    }
}
