package com.ml.shubham0204.clipandroid

import android.clip.cpp.CLIPAndroid
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory.decodeStream
import android.graphics.Matrix
import android.net.Uri
import android.util.Size
import androidx.compose.runtime.mutableStateOf
import androidx.exifinterface.media.ExifInterface
import androidx.lifecycle.ViewModel
import com.ml.shubham0204.clipandroid.data.ImagesDB
import com.ml.shubham0204.clipandroid.data.VectorSearchResults
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.buffer
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.nio.ByteBuffer
import com.ml.shubham0204.clipandroid.data.ImageReader
import android.util.Log


class MainActivityViewModel : ViewModel() {
    private val translator = Translator()
    val selectedImagesUriListState = mutableStateOf<List<Uri>>(emptyList())
    val queryTextState = mutableStateOf("")
    val isLoadingModelState = mutableStateOf(true)
    val isInsertingImagesState = mutableStateOf(false)
    val insertedImagesCountState = mutableStateOf(0)
    val isShowingModelInfoDialogState = mutableStateOf(false)
    val isInferenceRunningState = mutableStateOf(false)

    val isShowingResultsState = mutableStateOf(false)
    val vectorSearchResultsState = mutableStateOf<VectorSearchResults?>(null)

    private val clipAndroid = CLIPAndroid()
    var visionHyperParameters: CLIPAndroid.CLIPVisionHyperParameters? = null
    var textHyperParameters: CLIPAndroid.CLIPTextHyperParameters? = null

    private val MODEL_PATH = "/data/local/tmp/clip_model_fp16.gguf"
    private val NUM_THREADS = 4
    private val VERBOSITY = 1
    private val embeddingDim = 512
    private val threshold = 0.8

    private val imagesDB = ImagesDB()

    init {
        CoroutineScope(Dispatchers.IO).launch {
            mainScope { isLoadingModelState.value = true }
            clipAndroid.load(MODEL_PATH, VERBOSITY)
            visionHyperParameters = clipAndroid.visionHyperParameters
            textHyperParameters = clipAndroid.textHyperParameters
            mainScope { isLoadingModelState.value = false }
        }
    }

    fun processQuery(context: Context) {
        CoroutineScope(Dispatchers.IO).launch {
            mainScope { isInferenceRunningState.value = true }

            try {
                // Dịch văn bản từ tiếng Việt sang tiếng Anh
                val translatedText = withContext(Dispatchers.IO) {
                    translator.translateText(queryTextState.value.lowercase())
                }
                Log.d("Translator", "Văn bản đã dịch: $translatedText")

                // Encode văn bản đã dịch
                val textEmbedding = clipAndroid.encodeText(
                    translatedText,
                    NUM_THREADS,
                    embeddingDim,
                    true
                )
                val vectorSearchResults = imagesDB.nearestNeighbors(textEmbedding)
                mainScope {
                    vectorSearchResultsState.value = vectorSearchResults
                    selectedImagesUriListState.value =
                        vectorSearchResults.imageEntities
                            .filterIndexed { index, imageEntity ->
                                vectorSearchResults.scores[index] <= threshold
                            }
                            .map { Uri.parse(it.uri) }
                    isInferenceRunningState.value = false
                    isShowingResultsState.value = true
                }
            } catch (e: Exception) {
                Log.e("ProcessQuery", "Lỗi khi xử lý truy vấn: ${e.message}")
                mainScope {
                    isInferenceRunningState.value = false
                }
            }
        }
    }

    fun removeAllImages() {
        CoroutineScope(Dispatchers.IO).launch {
            imagesDB.removeAll()
            mainScope { selectedImagesUriListState.value = emptyList() }
        }
    }

    fun addImagesToDB(context: Context) {
        CoroutineScope(Dispatchers.IO).launch {
            mainScope { isInsertingImagesState.value = true }
            flow {
                    selectedImagesUriListState.value.forEach { uri ->
                        val bitmap = getFixedBitmap(context, uri)
                        val resizedBitmap =
                            Bitmap.createScaledBitmap(
                                bitmap,
                                visionHyperParameters?.imageSize ?: 224,
                                visionHyperParameters?.imageSize ?: 224,
                                true
                            )
                        val imageBuffer = bitmapToByteBuffer(resizedBitmap)
                        emit(
                            Pair(
                                imageBuffer,
                                Pair(uri, Size(resizedBitmap.width, resizedBitmap.height))
                            )
                        )
                    }
                }
                .buffer()
                .collect { (imageBuffer, uriAndSize) ->
                    val imageEmbedding =
                        clipAndroid.encodeImageNoResize(
                            imageBuffer,
                            uriAndSize.second.width,
                            uriAndSize.second.height,
                            NUM_THREADS,
                            embeddingDim,
                            true
                        )
                    imagesDB.add(uriAndSize.first.toString(), imageEmbedding)
                    mainScope { insertedImagesCountState.value += 1 }
                }
            mainScope {
                isInsertingImagesState.value = false
                insertedImagesCountState.value = 0
            }
        }
    }

    fun loadImages() {
        CoroutineScope(Dispatchers.IO).launch {
            val images = imagesDB.getAll()
            Log.d("ImageDebug", "Loaded ${images.size} images from DB")
            mainScope {
                selectedImagesUriListState.value = images.map { Uri.parse(it.uri) }
            }
        }
    }

    // In MainActivityViewModel.kt
    fun loadAllImagesFromMediaStore(context: Context) {
        CoroutineScope(Dispatchers.IO).launch {
            mainScope {
                isInsertingImagesState.value = true
                insertedImagesCountState.value = 0
            }

            val imageReader = ImageReader(context, clipAndroid, imagesDB)
            // Pass a suspend lambda to update the progress
            imageReader.getAllImagesFiles {
                mainScope {
                    insertedImagesCountState.value += 1
                }
            }

            // Refresh UI images after processing is complete.
            loadImages()

            mainScope {
                isInsertingImagesState.value = false
                insertedImagesCountState.value = 0
            }
        }
    }





    fun showModelInfo() {
        isShowingModelInfoDialogState.value = true
    }

    fun closeResults() {
        isShowingResultsState.value = false
        loadImages()
        queryTextState.value = ""
    }

    private suspend fun mainScope(action: () -> Unit) {
        withContext(Dispatchers.Main) { action() }
    }

    private fun bitmapToByteBuffer(bitmap: Bitmap): ByteBuffer {
        val width = bitmap.width
        val height = bitmap.height
        val imageBuffer = ByteBuffer.allocateDirect(width * height * 3)
        for (y in 0 until height) {
            for (x in 0 until width) {
                val pixel = bitmap.getPixel(x, y)
                imageBuffer.put((pixel shr 16 and 0xFF).toByte())
                imageBuffer.put((pixel shr 8 and 0xFF).toByte())
                imageBuffer.put((pixel and 0xFF).toByte())
            }
        }
        return imageBuffer
    }

    private fun getFixedBitmap(context: Context, imageFileUri: Uri): Bitmap {
        var imageBitmap = decodeStream(context.contentResolver.openInputStream(imageFileUri))
        val exifInterface = ExifInterface(context.contentResolver.openInputStream(imageFileUri)!!)
        imageBitmap =
            when (
                exifInterface.getAttributeInt(
                    ExifInterface.TAG_ORIENTATION,
                    ExifInterface.ORIENTATION_UNDEFINED
                )
            ) {
                ExifInterface.ORIENTATION_ROTATE_90 -> rotateBitmap(imageBitmap, 90f)
                ExifInterface.ORIENTATION_ROTATE_180 -> rotateBitmap(imageBitmap, 180f)
                ExifInterface.ORIENTATION_ROTATE_270 -> rotateBitmap(imageBitmap, 270f)
                else -> imageBitmap
            }
        return imageBitmap
    }

    private fun rotateBitmap(source: Bitmap, degrees: Float): Bitmap {
        val matrix = Matrix()
        matrix.postRotate(degrees)
        return Bitmap.createBitmap(source, 0, 0, source.width, source.height, matrix, false)
    }
}
