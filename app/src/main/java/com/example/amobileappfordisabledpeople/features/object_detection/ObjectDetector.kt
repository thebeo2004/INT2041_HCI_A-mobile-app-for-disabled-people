package com.example.amobileappfordisabledpeople.features.object_detection

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.graphics.RectF
import android.media.Image
import android.util.Log
import android.util.Size
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.example.amobileappfordisabledpeople.Data.DetectionObject
import com.example.amobileappfordisabledpeople.ObjectDetectorCallback
import org.tensorflow.lite.DataType
import org.tensorflow.lite.Interpreter
import org.tensorflow.lite.support.common.ops.NormalizeOp
import org.tensorflow.lite.support.image.ImageProcessor
import org.tensorflow.lite.support.image.TensorImage
import org.tensorflow.lite.support.image.ops.ResizeOp
import org.tensorflow.lite.support.image.ops.Rot90Op
import androidx.compose.runtime.Composable
import kotlinx.coroutines.*
import kotlin.system.measureTimeMillis

class ObjectDetector(
    private val yuvToRgbConverter: YuvToRgbConverter,
    private val interpreter: Interpreter,
    private val labels: List<String>,
    private val resultViewSize: Size,
    private val listener: ObjectDetectorCallback
) : ImageAnalysis.Analyzer {

    companion object {
        // Tamaño de entrada y salida del modelo
        private const val IMG_SIZE_X = 640 //300
        private const val IMG_SIZE_Y = 640 //300
        private const val MAX_DETECTION_NUM = 10//10

        // Dado que el modelo tflite utilizado esta vez ha sido cuantificado, la normalización relacionada no es 127.5f sino la siguiente
        private const val NORMALIZE_MEAN = 0f//0f
        private const val NORMALIZE_STD = 1f//1f

        // Umbral de puntuación de resultado de detección
        private const val SCORE_THRESHOLD = 0.6f

        private const val OUTPUT_TENSOR_SIZE = 84 * 8400
    }

    private var imageRotationDegrees: Int = 0
    private val tfImageProcessor by lazy {
        ImageProcessor.Builder()
            .add(ResizeOp(IMG_SIZE_X, IMG_SIZE_Y, ResizeOp.ResizeMethod.BILINEAR)) // Cambiar el tamaño de la imagen para que se ajuste a la entrada del modelo
            //.add(Rot90Op(-imageRotationDegrees / 90)) // yolo thi khong can
            //.add(NormalizeOp(NORMALIZE_MEAN, NORMALIZE_STD)) // yolo thi khong can
            .add(NormalizeOp(0f, 255f))
            .build()
    }

    //private val tfImageBuffer = TensorImage(DataType.UINT8)
    private val tfImageBuffer = TensorImage(DataType.FLOAT32) //yolo dung cai nay

    // Cuadro delimitador de resultado de detección [1:10:4]
    // El cuadro delimitador tiene la forma de [arriba, izquierda, abajo, derecha]

    // Định nghĩa các mảng đầu ra phù hợp với YOLOv8 Nano
    private val outputTensor = Array(1) { Array(84) { FloatArray(8400) } } // Mảng dữ liệu thô từ YOLOv8 Nano

    private val outputBoundingBoxes: Array<FloatArray> = Array(MAX_DETECTION_NUM) { FloatArray(4) } // Lưu tọa độ hộp
    private val outputLabels: IntArray = IntArray(MAX_DETECTION_NUM) // Lưu ID lớp
    private val outputScores: FloatArray = FloatArray(MAX_DETECTION_NUM) // Lưu xác suất cho mỗi dự đoán
    private val outputDetectionNum: IntArray = IntArray(1) // Tổng số lượng phát hiện vượt ngưỡng

    // Đưa vào outputMap
    private val outputMap = mapOf(
        0 to outputTensor
    )

    // Infiera la imagen de vista previa que fluye desde cameraX colocándola en el modelo de detección de objetos.
    @SuppressLint("UnsafeExperimentalUsageError", "UnsafeOptInUsageError")
    override fun analyze(image: ImageProxy) {
        if (image.image == null) return
        imageRotationDegrees = image.imageInfo.rotationDegrees
        val detectedObjectList = detect(image.image!!)
        listener(detectedObjectList)
        image.close()
    }

    // Convierta la imagen a YUV-> mapa de bits RGB-> tensorflowImage-> tensorflowBuffer,
    // infiera y envíe el resultado como una lista

    fun flattenTensorParallel(outputTensor: Array<Array<FloatArray>>): FloatArray {
        val flattenedArray = outputTensor.flatMapIndexed { _, batch ->
            batch[0].indices.flatMap { i ->
                batch.indices.map { j ->
                    batch[j][i]
                }
            }
        }.toFloatArray()
        return flattenedArray
    }

    // Hàm xử lý đầu ra
    private fun detect(targetImage: Image): List<DetectionObject> {
        val targetBitmap = Bitmap.createBitmap(targetImage.width, targetImage.height, Bitmap.Config.ARGB_8888)
        yuvToRgbConverter.yuvToRgb(targetImage, targetBitmap) // Chuyển sang RGB
        tfImageBuffer.load(targetBitmap)
        val tensorImage = tfImageProcessor.process(tfImageBuffer)

        // Chạy inference với model YOLOv8 Nano
        interpreter.runForMultipleInputsOutputs(arrayOf(tensorImage.buffer), outputMap)

        val flattenedArray = FloatArray(1 * 84 * 8400) // = runBlocking{flattenTensorParallel(outputTensor)}
        var index = 0

        for (batch in outputTensor) {
            for (i in batch[0].indices) {
                for (j in batch.indices) {
                    flattenedArray[index++] = batch[j][i]
                }
            }
        }
        // Xử lý outputTensor để lấy bounding boxes, labels, và scores
        val numAnchors = 8400
        val numClasses = 80

        var detectionCount = 0
        for (i in 0 until numAnchors) {
            // Lấy tọa độ bounding box
            val xCenter = flattenedArray[i * 84] * resultViewSize.width
            val yCenter = flattenedArray[i * 84 + 1] * resultViewSize.height
            val width = flattenedArray[i * 84 + 2] * resultViewSize.width
            val height = flattenedArray[i * 84 + 3] * resultViewSize.height

            // Xác định xác suất cao nhất và class ID
            var maxScore = -Float.MAX_VALUE
            var classId = -1
            for (j in 0 until numClasses) {
                val score = flattenedArray[i * 84 + 4 + j]
                if (score > maxScore) {
                    maxScore = score
                    classId = j
                }
            }

            Log.d("3336", "xCenter:$maxScore, yCenter:0, w:0, h:0")
            // Kiểm tra nếu xác suất vượt qua ngưỡng
            if (maxScore >= SCORE_THRESHOLD && detectionCount < MAX_DETECTION_NUM) {
                outputBoundingBoxes[detectionCount][0] = (xCenter - width / 2)  // left
                outputBoundingBoxes[detectionCount][1] = (yCenter - height / 2) // top
                outputBoundingBoxes[detectionCount][2] = (xCenter + width / 2)  // right
                outputBoundingBoxes[detectionCount][3] = (yCenter + height / 2) // bottom
                val left = (xCenter - width / 2)
                val top = (yCenter - height / 2)
                val right = (xCenter + width / 2)
                val bottom = (yCenter + height / 2)
                Log.d("l,t,r,b", "left:$left, top:$top, right:$right, bottom:$bottom")
                outputLabels[detectionCount] = classId
                outputScores[detectionCount] = maxScore
                detectionCount++
            }
        }
        outputDetectionNum[0] = detectionCount

        // Lấy danh sách các đối tượng phát hiện
        val detectedObjectList = arrayListOf<DetectionObject>()
        for (i in 0 until outputDetectionNum[0]) {
            val score = outputScores[i]
            val label = labels[outputLabels[i]]
            val boundingBox = RectF(
                outputBoundingBoxes[i][0],
                outputBoundingBoxes[i][1],
                outputBoundingBoxes[i][2],
                outputBoundingBoxes[i][3]
            )

            // Tính toán vị trí trung tâm của bounding box
            val objectCenterX = boundingBox.centerX()
            val objectCenterY = boundingBox.centerY()

            // Tính toán vị trí ngang
            val horizontalPosition = when {
                objectCenterX < resultViewSize.width * 0.33f -> "Left"
                objectCenterX > resultViewSize.width * 0.67f -> "Right"
                else -> "Center"
            }

            // Tính toán vị trí dọc
            val verticalPosition = when {
                objectCenterY < resultViewSize.height * 0.33f -> "Top"
                objectCenterY > resultViewSize.height * 0.67f -> "Bottom"
                else -> "Center"
            }

            // Thêm đối tượng vào danh sách nếu xác suất đạt ngưỡng
            detectedObjectList.add(
                DetectionObject(
                    score = score,
                    label = label,
                    boundingBox = boundingBox,
                    horizontalPosition = horizontalPosition,
                    verticalPosition = verticalPosition
                )
            )
        }
        return detectedObjectList// Lấy tối đa 4 đối tượng đầu tiên
    }
}