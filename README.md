# TLgemma — On-Device Language Translation

> Translate any supported language pair directly on your Android device — no internet required, no data ever leaves your phone.

Powered by **[TranslateGemma](https://huggingface.co/collections/google/translategemma)** and the **[SmolLM](https://github.com/shubham0204/SmolChat-Android)** inference engine (based on **[llama.cpp](https://github.com/ggml-org/llama.cpp)**).

---

## ✨ Features

| Feature | Description |
|---|---|
| 📱 **100% offline** | All inference runs locally via GGUF on-device model execution |
| 🔒 **Privacy-first** | Zero network calls — your text never leaves the device |
| 🌍 **Auto-detect source** | Choose "auto" to let the model guess the input language, or pick a specific one manually |
| 🔄 **Swap languages** | One tap to reverse source ↔ target with full custom-language support |
| 🎨 **Custom language codes** | Select "Other" on either side and type any IETF BCP-47 code |
| 🌙 **System theme** | Follows your device's light/dark setting automatically |

### Supported Languages

Source: `auto`, English (US), German, French, Spanish, Italian, Japanese, Chinese (Simplified), Russian, Portuguese (PT & BR), or **custom**.

Target: English (US), German, French, Spanish, Italian, Japanese, Chinese (Simplified), Russian, Portuguese (PT & BR), or **custom**.

---

## 🤖 Model Setup

The app requires a TranslateGemma GGUF model file at runtime. On first launch, you are redirected to the **Setup Activity** where you can:

### Download model

Tap **Download**, which opens Hugging Face in your browser:

[translategemma-4b-it.Q4_K_M.gguf](https://huggingface.co/mradermacher/translategemma-4b-it-GGUF/blob/main/translategemma-4b-it.Q4_K_M.gguf)

> ⚠️ The model file is ~2.5 GB. Ensure a stable Wi-Fi connection.


### Install

Tap **Install Model**, select the `.gguf` file from your device using the system document picker, and the app copies it into its private storage.

Once the model is in place, tap **Start** to launch the translator.

---

## 🚀 Using the App

1. **Enter or paste** text in the *Input* field.
2. Choose a **Source language** (or leave on *Auto Detect*).
3. Choose a **Target language**.
4. Tap the **Swap** button (↔) to reverse languages.
5. Tap **Translate** and wait for results — output streams token by token in real time.

---

## 🐛 Troubleshooting

| Issue | Possible Fix |
|---|---|
| Model fails to load | Ensure the `.gguf` file is fully copied and not corrupted. Re-download if needed. |
| Very slow translations | Q4 quantization requires a mid-to-high-end device. Consider a less-quantized model (Q5/K_M) on powerful hardware, or accept slower speeds. |
| "Auto Detect" cannot be swapped | Intentional — swapping would make auto-detection ambiguous. Change source to a specific language first. |

---

## 🙏 Acknowledgements

- **[Google Research](https://huggingface.co/collections/google/translategemma)** — TranslateGemma models (Gemma license)
- **[shubham0204](https://github.com/shubham0204/SmolChat-Android)** — SmolLM Android inference engine (Apache 2.0 license)
- **[mradermacher](https://huggingface.co/mradermacher)** — Quantized TranslateGemma GGUF model weights (Gemma license)
- **[llama.cpp](https://github.com/ggml-org/llama.cpp)** — LLM inference in C/C++ (MIT license)

---
