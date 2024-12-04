package com.example.amobileappfordisabledpeople.features

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Rect
import android.util.Log
import androidx.annotation.OptIn
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.example.amobileappfordisabledpeople.features.face_recognition.FaceNetModel
import com.example.amobileappfordisabledpeople.features.face_recognition.loadDrawableAsBitmap
import com.example.amobileappfordisabledpeople.features.face_recognition.preprocessImage
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.Face
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetectorOptions
import com.example.amobileappfordisabledpeople.R
import kotlin.math.sqrt

class FaceDetectionAnalyzer(
    private val context: Context,
    private val onFaceDetected: (faces: MutableList<Face>, width: Int, height: Int) -> Unit
) : ImageAnalysis.Analyzer {

    private val faceNetModel = FaceNetModel(context)

    private val options = FaceDetectorOptions.Builder()
        .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_ACCURATE)
        .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_NONE)
        .enableTracking()
        .build()

    private val faceDetector = FaceDetection.getClient(options)


    @OptIn(ExperimentalGetImage::class)
    override fun analyze(image: ImageProxy) {
        image.image?.let {

            Log.d("FaceRecognition", "Get image from ImageProxy and convert to bitmap")
            val bitmap = image.toBitmap()
            Log.d("FaceRecognition", "Convert bitmap to InputImage")
            val imageValue = InputImage.fromMediaImage(it, image.imageInfo.rotationDegrees)
            faceDetector.process(imageValue)
                .addOnCompleteListener { faces ->

                    faces.result.forEach { face ->

                        try {
                            Log.d(
                                "FaceRecognition",
                                "Bitmap dimensions: ${bitmap.width}x${bitmap.height}"
                            )
                            Log.d("FaceRecognition", "Face bounding box: ${face.boundingBox}")
                            val faceBitmap = cropFace(bitmap, face.boundingBox)
                            Log.d(
                                "FaceRecognition",
                                "Cropped face bitmap dimensions: ${faceBitmap?.width}x${faceBitmap?.height}"
                            )
                        } catch (e: Exception) {
                            Log.e("FaceRecognition", "Error cropping face bitmap", e)
                        }

                        val faceBitmap = cropFace(bitmap, face.boundingBox)
                        Log.d("FaceRecognition", "Convert face bitmap to InputImage")

                        val faceImage = InputImage.fromBitmap(faceBitmap!!, 0)
                        Log.d("FaceRecognition", "Preprocess the face image")

                        val preprocessedImage = preprocessImage(faceImage)

                        val faceEmbedding = faceNetModel.getEmbedding(preprocessedImage)

                        Log.d("FaceRecognition", "Get stored image as Bitmap")
                        val storedBitmap = loadDrawableAsBitmap(context, R.drawable.donald_trump)

                        Log.d("FaceRecognition", "Preprocess the stored image")
                        // Preprocess the stored image
                        val storedImage = InputImage.fromBitmap(storedBitmap, 0)
                        val preprocessedStoredImage = preprocessImage(storedImage)

                        val storedImageEmbedding =
                            faceNetModel.getEmbedding(preprocessedStoredImage)

                        val similarity = 1 - sqrt(
                            calculateEuclideanDistance(
                                faceEmbedding,
                                storedImageEmbedding
                            )
                        )
                        Log.d("FaceRecognitionDonal", "Similarity: $similarity")
                    }

                    onFaceDetected(faces.result, image.width, image.height)
                    image.image?.close()
                    image.close()

                } ?: image.close()
        }
    }

    fun ImageProxy.toBitmap(): Bitmap {
        val buffer = planes[0].buffer
        val bytes = ByteArray(buffer.capacity())
        buffer.get(bytes)
        return BitmapFactory.decodeByteArray(bytes, 0, bytes.size, null)
    }
}

    fun cropFace(image: Bitmap, boundingBox: Rect): Bitmap? {
        val shift = 0
        if (boundingBox.top >= 0 && boundingBox.bottom <= image.getWidth()
            && boundingBox.top + boundingBox.height() <= image.getHeight()
            && boundingBox.left >= 0
            && boundingBox.left + boundingBox.width() <= image.getWidth()
        ) {
            return Bitmap.createBitmap(
                image,
                boundingBox.left,
                boundingBox.top + shift,
                boundingBox.width(),
                boundingBox.height()
            )
        }
        return null
    }

    fun calculateCosineSimilarity(embedding1: FloatArray, embedding2: FloatArray): Float {
        var dotProduct = 0f
        var normA = 0f
        var normB = 0f
        for (i in embedding1.indices) {
            dotProduct += embedding1[i] * embedding2[i]
            normA += embedding1[i] * embedding1[i]
            normB += embedding2[i] * embedding2[i]
        }
        return dotProduct / (Math.sqrt(normA.toDouble()) * Math.sqrt(normB.toDouble())).toFloat()
    }

    fun calculateEuclideanDistance(embedding1: FloatArray, embedding2: FloatArray): Float {
        var sum = 0f
        for (i in embedding1.indices) {
            val diff = embedding1[i] - embedding2[i]
            sum += diff * diff
        }
        return Math.sqrt(sum.toDouble()).toFloat()
    }
