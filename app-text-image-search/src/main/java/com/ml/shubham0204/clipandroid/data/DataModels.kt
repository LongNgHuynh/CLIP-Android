package com.ml.shubham0204.clipandroid.data

import io.objectbox.annotation.Entity
import io.objectbox.annotation.HnswIndex
import io.objectbox.annotation.Id
import io.objectbox.annotation.VectorDistanceType

@Entity
data class ImageEntity(
    @Id var id: Long = 0,
    var uri: String = "",
    var date: Long = 0,
    var album: String? = null,
    @HnswIndex(dimensions = 512, distanceType = VectorDistanceType.DOT_PRODUCT)
    var embedding: FloatArray = floatArrayOf()
)

data class VectorSearchResults(
    val imageEntities: List<ImageEntity>,
    val scores: List<Double>,
    val numVectorsSearched: Int,
    val timeTakenMillis: Long
)
