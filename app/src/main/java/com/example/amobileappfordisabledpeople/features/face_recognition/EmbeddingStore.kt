package com.example.amobileappfordisabledpeople.features.face_recognition

import android.content.Context
import android.util.Log
import com.example.amobileappfordisabledpeople.R
import com.example.amobileappfordisabledpeople.features.FaceDetectorProvider.faceDetector
import com.google.gson.Gson
import com.google.mlkit.vision.common.InputImage
import org.tensorflow.lite.support.image.TensorImage
import java.io.File


object EmbeddingStore {
    private const val EMBEDDINGS_FILE = "embeddings.json"
    private var storedImageEmbeddings: List<FloatArray> = mutableListOf()

    fun initialize(context: Context, faceNetModel: FaceNetModel) {
        if (storedImageEmbeddings.isEmpty()) {
//            val embeddingsFile = File(context.filesDir, EMBEDDINGS_FILE)
            storedImageEmbeddings = embeddingStoredImages(context, faceNetModel)
//            if (embeddingsFile.exists()) {
//                Log.d("EmbeddingStore", "Loading embeddings from embeddings.json")
//                storedImageEmbeddings = loadEmbeddingsFromFile(embeddingsFile)
//            } else {
//                Log.d("EmbeddingStore", "embeddings.json does not exist. Creating a new file.")
//                storedImageEmbeddings = embeddingStoredImages(context, faceNetModel)
//                saveEmbeddingsToFile(embeddingsFile, storedImageEmbeddings)
//            }
        }
    }

    fun getEmbeddings(): List<FloatArray> {
        return storedImageEmbeddings
    }

    private fun embeddingStoredImages(context: Context, faceNetModel: FaceNetModel): List<FloatArray> {
        val famousImages = listOf(
//            R.drawable.khai,
//            R.drawable.quang1,
            R.drawable.tuananh2,
            R.drawable.quang,
            R.drawable.khai
        )

        val embeddings = mutableListOf<FloatArray>()

        famousImages.forEach { drawableResId ->
            val bitmap = loadDrawableAsBitmap(context, drawableResId)
            val inputImage = InputImage.fromBitmap(bitmap, 0)

            faceDetector.process(inputImage)
                .addOnSuccessListener { faces ->
                    faces.forEach { face ->
                        val faceBitmap = cropFace(bitmap, face.boundingBox, 0f)
                        if (faceBitmap != null) {
                            val tensorImage = TensorImage.fromBitmap(faceBitmap)
                            val preprocessedImage = preprocessImage(tensorImage)
                            val embedding = faceNetModel.getEmbedding(preprocessedImage)
                            embeddings.add(embedding)
                        }
                    }
                }
                .addOnFailureListener { e ->
                    e.printStackTrace()
                }
        }
        return embeddings
    }

    private fun saveEmbeddingsToFile(file: File, embeddings: List<FloatArray>) {
        val gson = Gson()
        val json = gson.toJson(embeddings)
        file.writeText(json)
    }

    private fun loadEmbeddingsFromFile(file: File): List<FloatArray> {
        val gson = Gson()
        val json = file.readText()
        val type = Array<FloatArray>::class.java
//        val type = object : TypeToken<List<FloatArray>>() {}.type
        return gson.fromJson(json, type).toList()
    }
}