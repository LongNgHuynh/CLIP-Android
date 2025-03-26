package com.ml.shubham0204.clipandroid.data

import android.Manifest
import android.content.ContentUris
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.core.content.ContextCompat
import android.clip.cpp.CLIPAndroid
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.buffer
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.withContext
import java.nio.ByteBuffer

class ImageReader(
    private val context: Context,
    private val clipAndroid: CLIPAndroid,
    private val imagesDB: ImagesDB
) {
    // Updated: onImageProcessed is now a suspend lambda.
    suspend fun getAllImagesFiles(onImageProcessed: (suspend () -> Unit)? = null) {
        withContext(Dispatchers.IO) {
            // Check if we should skip querying based on permission.
            val skipQuery = if (Build.VERSION.SDK_INT <= 32) {
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.READ_EXTERNAL_STORAGE
                ) != PackageManager.PERMISSION_GRANTED
            } else false

            if (skipQuery) {
                return@withContext
            }

            // Prepare the query.
            val queryUri = if (Build.VERSION.SDK_INT >= 29) {
                MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
            } else {
                MediaStore.Images.Media.getContentUri("external")
            }

            val projection = arrayOf(
                MediaStore.Images.Media._ID,
                MediaStore.Images.Media.DISPLAY_NAME,
                MediaStore.Images.Media.DATE_ADDED,
                MediaStore.Images.Media.BUCKET_DISPLAY_NAME
            )

            val imageFiles = mutableListOf<ImageEntity>() // (URI, Date)
            context.contentResolver.query(
                queryUri,
                projection,
                null,
                null,
                "${MediaStore.Images.Media.DATE_ADDED} DESC"
            )?.use { cursor ->
                val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
                val dateColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_ADDED)
                val albumColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.BUCKET_DISPLAY_NAME)
                while (cursor.moveToNext()) {
                    val id = cursor.getLong(idColumn)
                    val date = cursor.getLong(dateColumn)
                    val album = cursor.getString(albumColumn) ?: "Unknown"
                    val contentUri = ContentUris.withAppendedId(queryUri, id)
                    imageFiles.add(
                        ImageEntity(
                            uri = contentUri.toString(),
                            date = date,
                            album = album
                        )
                    )
                }
            }

            // Process images via a flow.
            flow {
                imageFiles.forEach { imageEntity ->
                    val bitmap = getFixedBitmap(context, Uri.parse(imageEntity.uri))
                    val resizedBitmap = Bitmap.createScaledBitmap(
                        bitmap,
                        clipAndroid.visionHyperParameters?.imageSize ?: 224,
                        clipAndroid.visionHyperParameters?.imageSize ?: 224,
                        true
                    )
                    val imageBuffer = bitmapToByteBuffer(resizedBitmap)
                    emit(Triple(imageBuffer, Uri.parse(imageEntity.uri), imageEntity))
                }
            }
                .buffer()
                .collect { (imageBuffer, uri, imageEntity) ->
                    val imageEmbedding = clipAndroid.encodeImageNoResize(
                        imageBuffer,
                        clipAndroid.visionHyperParameters?.imageSize ?: 224,
                        clipAndroid.visionHyperParameters?.imageSize ?: 224,
                        4, // NUM_THREADS
                        512, // embeddingDim
                        true
                    )
                    imageEntity.album?.let {
                        imagesDB.add(uri.toString(), imageEmbedding, imageEntity.date,
                            it
                        )
                    }
                    // Call the suspend callback after processing each image.
                    onImageProcessed?.invoke()
                }
        }
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

    private fun getFixedBitmap(context: Context, imageUri: Uri): Bitmap {
        return BitmapFactory.decodeStream(context.contentResolver.openInputStream(imageUri))!!
    }
}
