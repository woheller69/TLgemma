package com.seemless

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.lifecycle.lifecycleScope
import com.google.android.material.floatingactionbutton.FloatingActionButton
import io.shubham0204.smollm.SmolLM
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

class MainActivity : AppCompatActivity() {

    private lateinit var etvResult: EditText
    private lateinit var etvInput: EditText
    private lateinit var progressBar: ProgressBar
    private lateinit var translateButton: FloatingActionButton
    private lateinit var spinnerSource: Spinner
    private lateinit var spinnerTarget: Spinner
    private lateinit var btnSwap: ImageButton
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
        btnSwap.isEnabled = false      // disabled while auto is active
        btnSwap.imageAlpha = if (btnSwap.isEnabled) 255 else 128

        // Show/hide custom input when "Other" is selected
        spinnerSource.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, pos: Int, id: Long) {
                etvCustomSource.visibility = if (LANGUAGES_SRC[pos] == "other-src") View.VISIBLE else View.GONE
                btnSwap.isEnabled = LANGUAGES_SRC[pos] != "auto"
                btnSwap.imageAlpha = if (btnSwap.isEnabled) 255 else 128
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        spinnerTarget.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, pos: Int, id: Long) {
                etvCustomTarget.visibility = if (LANGUAGES_TARGET[pos] == "other-tgt") View.VISIBLE else View.GONE
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        // Swap button — swaps values, not positions, to handle different list lengths
        btnSwap.setOnClickListener {
            val srcPos = spinnerSource.selectedItemPosition
            if (LANGUAGES_SRC[srcPos] == "auto") {
                Toast.makeText(this, "Cannot swap: source is set to Auto Detect", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Capture current values BEFORE changing anything
            val srcVal = getSourceLang()
            val tgtVal = getTargetLang()

            // Set target to the source value — find matching index in target list
            val newTgtPos = LANGUAGES_TARGET.indexOf(srcVal)
            if (newTgtPos >= 0) {
                spinnerTarget.setSelection(newTgtPos)
                etvCustomTarget.visibility = View.GONE
            } else {
                spinnerTarget.setSelection(LANGUAGES_TARGET.indexOf("other-tgt"))
                etvCustomTarget.setText(srcVal)
                etvCustomTarget.visibility = View.VISIBLE
            }

            // Set source to the target value — find matching index in source list
            val newSrcPos = LANGUAGES_SRC.indexOf(tgtVal)
            if (newSrcPos >= 0) {
                spinnerSource.setSelection(newSrcPos)
                etvCustomSource.visibility = View.GONE
            } else {
                spinnerSource.setSelection(LANGUAGES_SRC.indexOf("other-src"))
                etvCustomSource.setText(tgtVal)
                etvCustomSource.visibility = View.VISIBLE
            }

            btnSwap.isEnabled = LANGUAGES_SRC[spinnerSource.selectedItemPosition] != "auto"
            btnSwap.imageAlpha = if (btnSwap.isEnabled) 255 else 128
        }

        translateButton.setOnClickListener { processTranslationRequest() }

        // Init progress bar
        progressBar.isIndeterminate = true
        progressBar.visibility = ProgressBar.INVISIBLE

        val modelFile = File(getExternalFilesDir(null), "model.gguf")

        if (!modelFile.exists()) {
            startActivity(Intent(this, SetupActivity::class.java))
            return
        }

        loadModelWithProgress()
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

    private fun loadModelWithProgress() {
        val modelFile = File(getExternalFilesDir(null), "model.gguf")
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                withContext(Dispatchers.Main) {
                    progressBar.visibility = ProgressBar.VISIBLE
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
                    progressBar.visibility = ProgressBar.INVISIBLE
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    progressBar.visibility = ProgressBar.INVISIBLE
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

        etvResult.text.clear()
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                withContext(Dispatchers.Main) {
                    progressBar.visibility = ProgressBar.VISIBLE
                    progressBar.isIndeterminate = true
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
                    // Unescape JSON escape sequences in the FULL accumulated response
                    val decoded = response
                        .replace("\\\\", "\u0000")   // protect real backslashes
                        .replace("\\n", "\n")        // \n → actual newline
                        .replace("\\t", "\t")        // \t → tab
                        .replace("\\r", "\r")        // \r → carriage return
                        .replace("\u0000", "\\")     // restore real backslashes

                    withContext(Dispatchers.Main) {
                        etvResult.text = Editable.Factory.getInstance().newEditable(
                            decoded.ifEmpty { "⚠️ Empty response" }
                        )
                    }
                }

                withContext(Dispatchers.Main) {
                    progressBar.visibility = ProgressBar.INVISIBLE
                    translateButton.isEnabled = true
                }
                loadModelWithProgress()
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    progressBar.visibility = ProgressBar.INVISIBLE
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
