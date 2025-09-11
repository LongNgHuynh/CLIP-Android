# CLIP-Android

Run OpenAI’s CLIP on Android — fully on-device — with two demo apps:

* **Text ↔ Image Search** (`app-text-image-search`)
* **Zero-shot Image Classification** (`app-zero-shot-classiify`)

> CLIP (Contrastive Language–Image Pre-training) learns joint text–image embeddings so you can compare images and natural-language prompts in the same vector space.

---

## ✨ What’s inside

* **Two sample apps** showcasing the two most common CLIP use-cases on mobile.
* **Kotlin, Gradle (KTS)** project structure for modern Android development.
* **Apache-2.0 licensed** — use it in personal or commercial projects.

---

## 📦 Project layout

```
.
├─ app-text-image-search/      # Demo: search images with natural-language text
├─ app-zero-shot-classiify/    # Demo: zero-shot classification with custom labels
├─ resources/                  # (Optional) notes/assets for models/export
├─ build.gradle.kts
├─ settings.gradle.kts
└─ LICENSE (Apache-2.0)
```

---

## 🚀 Quick start

1. **Clone & open**

   ```bash
   git clone https://github.com/LongNgHuynh/CLIP-Android.git
   ```

   Open the project in **Android Studio** (latest stable).

2. **Pick a demo app to run**

   * `app-text-image-search`
   * `app-zero-shot-classiify`
     Select the module’s run configuration and press **Run ▶**.

3. **Add models (required)**
   Place the CLIP **image encoder** and **text encoder** model files into the target module’s `src/main/assets/` folder.
   Typical file names you might use:

   ```
   assets/
     vision_model.onnx  (or .ort)
     text_model.onnx    (or .ort)
     tokenizer.json     (if your runtime needs it)
   ```

> **Heads-up:** If you’re exporting models yourself, export both encoders and (optionally) quantize for mobile performance. Any on-device runtime that can run ONNX/ORT or TorchScript should work as long as inputs/outputs match the app’s pre/post-processing.

---

## 🧪 How to use the demos

### Text ↔ Image Search

* Provide a set of images (from gallery or sample set).
* Type a natural-language query like “a yellow car at night”.
* The app embeds both text and images and returns the **most similar images**.

### Zero-shot Classification

* Enter a **comma-separated** list of labels, e.g. `cat, dog, airplane, pizza`.
* Capture or pick an image.
* The app computes similarities between the image embedding and text label embeddings and shows **top-k classes**.

---

## ⚙️ Model export (example workflow)

If you don’t already have ready-to-run CLIP encoders:

1. Start from OpenAI’s CLIP repository to load a pretrained model.
2. Export **image** and **text** encoders to ONNX (batch size 1).
3. (Optional) Quantize to INT8 for speed.
4. Copy the two model files into `src/main/assets/` of the module you’re running.

> Exact export commands depend on the runtime you choose (ONNX Runtime Mobile, PyTorch Lite, etc.). Keep tensor shapes, normalization, and tokenization aligned with the mobile code.

---

## 🧩 Implementation notes

* Inputs typically use CLIP normalization (mean/std) and a 224×224 center-crop; text is tokenized to CLIP’s vocab and padded/truncated to the model’s max sequence length.
* Similarity is cosine between **L2-normalized** embeddings; higher is more relevant.

---

## 📱 Requirements

* Android Studio (latest)
* Android Gradle Plugin (KTS build scripts)
* A compatible on-device inference runtime and the two CLIP encoder models

---

## 🗺️ Roadmap (ideas)

* Add pre-built sample models (if licenses allow)
* Toggle quantization levels and measure latency
* Add camera streaming for real-time search
* Support multilingual text encoders

---

## 🤝 Contributing

Issues and PRs are welcome! Please open an issue to discuss large changes first.

---


## 🙏 Acknowledgements

* [OpenAI CLIP](https://github.com/openai/CLIP) for the original model and paper.
* Special thanks to **shubham0204/CLIP-Android** — this project is a **fork** and further development based on that work.

---

### Short Description

**Android demos for OpenAI CLIP — text↔image search & zero-shot classification, fully on-device.**
