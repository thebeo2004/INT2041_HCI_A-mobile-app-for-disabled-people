package com.example.amobileappfordisabledpeople.features

import android.annotation.SuppressLint
import androidx.annotation.OptIn
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.Face
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetectorOptions
import org.tensorflow.lite.support.common.ops.NormalizeOp
import org.tensorflow.lite.support.image.ImageProcessor
import org.tensorflow.lite.support.image.TensorImage
import org.tensorflow.lite.support.image.ops.ResizeOp

@SuppressLint("UnsafeOptInUsageError")
class FaceDetectionAnalyzer(
    private val onFaceDetected: (faces: MutableList<Face>, width: Int, height: Int) -> Unit
) : ImageAnalysis.Analyzer {

    private companion object {
        private const val FACENET_INPUT_IMAGE_SIZE = 224
    }


    private val options = FaceDetectorOptions.Builder()
        .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_ACCURATE)
        .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_NONE)
        .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_ALL)
        .setContourMode(FaceDetectorOptions.CONTOUR_MODE_ALL)
        .enableTracking()
        .build()

    private val faceDetector = FaceDetection.getClient(options)

//    private val faceNetImageProcessor = ImageProcessor.Builder()
//        .add(
//            ResizeOp(
//                FACENET_INPUT_IMAGE_SIZE,
//                FACENET_INPUT_IMAGE_SIZE,
//                ResizeOp.ResizeMethod.BILINEAR
//            )
//        )
//        .add(NormalizeOp(0f, 255f))
//        .build()

    override fun analyze(image: ImageProxy) {
        image.image?.let {
            val imageValue = InputImage.fromMediaImage(it, image.imageInfo.rotationDegrees)
            faceDetector.process(imageValue)
                .addOnCompleteListener { faces ->
                    onFaceDetected(faces.result, image.width, image.height)
                    image.image?.close()
                    image.close()
                }
        } ?: image.close()
    }
}