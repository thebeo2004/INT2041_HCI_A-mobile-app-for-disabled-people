package com.example.amobileappfordisabledpeople.features.face_recognition

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
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
    private val faceNetModel: FaceNetModel,
    private val onFaceDetected: (faces: MutableList<Face>, width: Int, height: Int) -> Unit,
) : ImageAnalysis.Analyzer {

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

                        var person = "None"

                        if (mostProminentFace != null) {
                            val faceBitmap = cropFace(bitmap, mostProminentFace.boundingBox, image.imageInfo.rotationDegrees.toFloat())

                            if (faceBitmap != null) {
                                Log.d("FaceRecognition", "Face bitmap: $faceBitmap")

                                val tensorImage = TensorImage.fromBitmap(faceBitmap)
                                Log.d("FaceRecognition", "Tensor image: $tensorImage")

                                val preprocessedImage = preprocessImage(tensorImage)
                                val faceEmbedding = faceNetModel.getEmbedding(preprocessedImage)

                                val famous = listOf(
                                    "Billie Eilish",
                                    "David Beckham",
                                    "Donald Trump",
                                    "MTP",
                                    "Rihanna"
                                )

                                val storedImageEmbeddings = embeddingStoredImages()

                                storedImageEmbeddings.forEachIndexed { index, embedding ->
                                    val distance = calculateEuclideanDistance(faceEmbedding, embedding)
                                    Log.d("Famous", "Distance from $person to ${famous[index]}: $distance")
                                    if (distance < 1.1f) {
                                        person = famous[index]
                                    }
                                }

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

    fun embeddingStoredImages(): List<FloatArray> {

        val famousImages = listOf(
            R.drawable.billie_eilish,
            R.drawable.david_beckham,
            R.drawable.donal_trump,
            R.drawable.mtp,
            R.drawable.rihanna
        )

        val embeddings = mutableListOf<FloatArray>()

        val storedBitmaps = famousImages.map { drawableResId ->
            loadDrawableAsBitmap(context, drawableResId)
        }

        val storedImages = storedBitmaps.map { bitmap ->
            TensorImage.fromBitmap(bitmap)
        }

        val preprocessedImages = storedImages.map { tensorImage ->
            preprocessImage(tensorImage)
        }

        preprocessedImages.map { preprocessedImage ->
            embeddings.add(faceNetModel.getEmbedding(preprocessedImage))
        }

        return embeddings
    }
}

fun cropFace(image: Bitmap?, boundingBox: Rect, rotationDegrees: Float): Bitmap? {
    val shift = 0
    if (image != null) {

        val matrix = Matrix().apply {
            postRotate(rotationDegrees)
        }

        val rotatedImage = Bitmap.createBitmap(image, 0, 0, image.width, image.height, matrix, true)
        // Ensure the bounding box is within the image dimensions
        val left = boundingBox.left.coerceAtLeast(0)
        val top = boundingBox.top.coerceAtLeast(0)
        val right = boundingBox.right.coerceAtMost(image.width)
        val bottom = boundingBox.bottom.coerceAtMost(image.height)

        val width = right - left
        val height = bottom - top

        return Bitmap.createBitmap(rotatedImage, left, top, width, height)
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
