package com.example.amobileappfordisabledpeople.features.face_recognition

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Rect
import android.util.Log
import androidx.annotation.OptIn
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.google.android.gms.tasks.Task
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.Face
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetectorOptions
import org.tensorflow.lite.support.image.TensorImage
import kotlin.collections.List
import com.example.amobileappfordisabledpeople.R

class FaceRecognitionAnalyzer(
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

            Log.d("FaceRecognition", "Image width: ${image.width}, Image height: ${image.height}")

            val bitmap = image.toBitmap()
            val imageValue = InputImage.fromMediaImage(it, image.imageInfo.rotationDegrees)

            faceDetector.process(imageValue)
                .addOnCompleteListener { task: Task<List<Face>> ->

                    if (task.isSuccessful) {
                        val faces = task.result
                        val imageWidth = image.width
                        val imageHeight = image.height

                        // Find the most prominent face (largest bounding box)
                        val mostProminentFace = faces.maxByOrNull { it.boundingBox.width() * it.boundingBox.height() }

                        Log.d("FaceRecognition", "Most prominent face: $mostProminentFace")

                        if (mostProminentFace != null) {
                            val faceBitmap = cropFace(bitmap, mostProminentFace.boundingBox)

                            if (faceBitmap != null) {
                                Log.d("FaceRecognition", "Face bitmap: $faceBitmap")

                                val tensorImage = TensorImage.fromBitmap(faceBitmap)
                                Log.d("FaceRecognition", "Tensor image: $tensorImage")

                                val preprocessedImage = preprocessImage(tensorImage)
                                val faceEmbedding = faceNetModel.getEmbedding(preprocessedImage)

                                val storedImageEmbedding = embeddingStoraedImages()

                                Log.d("FaceRecognition", "${calculateEuclideanDistance(faceEmbedding, storedImageEmbedding)}")

                            }
                        }
                        onFaceDetected(mostProminentFace?.let { mutableListOf(it) } ?: mutableListOf(), imageWidth, imageHeight)
                    }

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

    fun embeddingStoraedImages(): FloatArray {
        val storedBitmap = loadDrawableAsBitmap(context, R.drawable.donal_trump)
        val storedImage = TensorImage.fromBitmap(storedBitmap)
        val preprocessedImage = preprocessImage(storedImage)
        val storedImageEmbedding = faceNetModel.getEmbedding(preprocessedImage)
        return storedImageEmbedding
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

fun calculateEuclideanDistance(embedding1: FloatArray, embedding2: FloatArray): Float {
    var sum = 0f
    for (i in embedding1.indices) {
        val diff = embedding1[i] - embedding2[i]
        sum += diff * diff
    }
    return Math.sqrt(sum.toDouble()).toFloat()
}
