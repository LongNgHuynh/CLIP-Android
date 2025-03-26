package com.ml.shubham0204.clipandroid.data

import android.util.Log
import io.objectbox.Box
import kotlin.time.DurationUnit
import kotlin.time.measureTimedValue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale


class ImagesDB {

    private val imagesBox: Box<ImageEntity> = ObjectBoxStore.store.boxFor(ImageEntity::class.java)

    suspend fun add(uri: String, embedding: FloatArray, date: Long, album: String) =
        withContext(Dispatchers.IO) {
            val dateFormat = SimpleDateFormat("dd_MM_yyyy", Locale.getDefault())
            val dateString = dateFormat.format(Date(date)) // Ensure date is in milliseconds
            Log.d("ImagesDB", "Adding image with URI: $uri, date: $dateString, album: $album")
            imagesBox.put(ImageEntity(uri = uri, date = date, album = album, embedding = embedding))
        }


    suspend fun getAll(): List<ImageEntity> = withContext(Dispatchers.IO) {
        val images = imagesBox.all
        images.forEach { image ->
            Log.d("ImagesDB", "Image URI: ${image.uri}, Date: ${image.date}, Album: ${image.album}")
        }
        images
    }

    suspend fun removeAll() = withContext(Dispatchers.IO) {
        Log.d("ImagesDB", "Removing all images")
        imagesBox.removeAll()
    }

    suspend fun remove(id: Long) = withContext(Dispatchers.IO) {
        Log.d("ImagesDB", "Removing image with ID: $id")
        imagesBox.remove(id)
    }

    suspend fun nearestNeighbors(embedding: FloatArray): VectorSearchResults =
        withContext(Dispatchers.IO) {
            val query = imagesBox.query(ImageEntity_.embedding.nearestNeighbors(embedding, 50)).build()
            val (results, time) = measureTimedValue { query.findWithScores() }
            val scores = results.map { it.score }
            val entities = results.map { it.get() }
            Log.d("ImagesDB", "Nearest neighbors search results: $entities with scores: $scores")
            VectorSearchResults(
                imageEntities = entities,
                scores = scores,
                numVectorsSearched = results.size,
                timeTakenMillis = time.toLong(DurationUnit.MILLISECONDS)
            )
        }
}
