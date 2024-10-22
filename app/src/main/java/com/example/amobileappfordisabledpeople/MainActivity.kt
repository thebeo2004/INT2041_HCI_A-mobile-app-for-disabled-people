package com.example.amobileappfordisabledpeople

import android.content.res.AssetFileDescriptor
import android.os.Bundle
import android.speech.tts.TextToSpeech
import java.util.Locale
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.ui.graphics.Color
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.google.accompanist.systemuicontroller.rememberSystemUiController
import com.example.amobileappfordisabledpeople.ui.views.DetectionScreen
import dagger.hilt.android.AndroidEntryPoint
import org.tensorflow.lite.Interpreter
import java.io.BufferedReader
import java.io.FileInputStream
import java.io.InputStream
import java.io.InputStreamReader
import java.nio.ByteBuffer
import java.nio.channels.FileChannel
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

@AndroidEntryPoint
class MainActivity : ComponentActivity(), TextToSpeech.OnInitListener {

    private lateinit var textToSpeech: TextToSpeech
    //------------------------  onCreate ----------------------------------
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Khởi tạo TextToSpeech
        textToSpeech = TextToSpeech(this, this)

        setContent {
            val systemUiController = rememberSystemUiController()
            val navController = rememberNavController()

            if(isSystemInDarkTheme()){
                systemUiController.setNavigationBarColor(Color.Black, darkIcons = false)
                systemUiController.setStatusBarColor(Color.Black, darkIcons = false)
            }

            cameraExecutor = Executors.newSingleThreadExecutor()

            NavHost(
                navController = navController,
                startDestination = "Detection"
            ) {
                composable(route = "Detection"){
                    DetectionScreen(
                        cameraExecutor = cameraExecutor,
                        yuvToRgbConverter = yuvToRgbConverter,
                        interpreter = interpreter,
                        labels = labels,
                        textToSpeech = textToSpeech
                    )
                }
            }
        }
    }
    //------------------------Fin  onCreate --------------------------------

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            // Thiết lập ngôn ngữ (US English)
            val result = textToSpeech.setLanguage(Locale.US)
            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                Toast.makeText(this, "Ngôn ngữ không hỗ trợ", Toast.LENGTH_SHORT).show()
            }
        } else {
            Toast.makeText(this, "Khởi tạo TTS thất bại", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
        if (::textToSpeech.isInitialized) {
            textToSpeech.stop()
            textToSpeech.shutdown()
        }
    }

    //-----------------------------------------------------------------------
    companion object{
        private const val MODEL_FILE_NAME = "ssd_mobilenet_v1_1_metadata_1.tflite"
        private const val LABEL_FILE_NAME = "coco_dataset_labels_v1.txt"
    }

    private lateinit var cameraExecutor: ExecutorService

    private val interpreter: Interpreter by lazy {
        Interpreter(loadModel())
    }

    private val labels: List<String> by lazy {
        loadLabels()
    }

    private val yuvToRgbConverter: YuvToRgbConverter by lazy {
        YuvToRgbConverter(this)
    }

    private fun loadModel(fileName: String = MODEL_FILE_NAME): ByteBuffer {
        lateinit var modelBuffer: ByteBuffer
        var file: AssetFileDescriptor? = null
        try {
            file = assets.openFd(fileName)
            val inputStream = FileInputStream(file.fileDescriptor)
            val fileChannel = inputStream.channel
            modelBuffer = fileChannel.map(FileChannel.MapMode.READ_ONLY, file.startOffset, file.declaredLength)
        } catch (e: Exception) {
            Toast.makeText(this, "Error loading model", Toast.LENGTH_SHORT).show()
            finish()
        } finally {
            file?.close()
        }
        return modelBuffer
    }

    private fun loadLabels(fileName: String = LABEL_FILE_NAME): List<String> {
        var labels = listOf<String>()
        var inputStream: InputStream? = null
        try {
            inputStream = assets.open(fileName)
            val reader = BufferedReader(InputStreamReader(inputStream))
            labels = reader.readLines()
        } catch (e: Exception) {
            Toast.makeText(this, "Error loading model", Toast.LENGTH_SHORT).show()
            finish()
        } finally {
            inputStream?.close()
        }
        return labels
    }

    // Hàm để đọc văn bản
    fun speakText(text: String) {
        textToSpeech.speak(text, TextToSpeech.QUEUE_FLUSH, null, null)
    }
}






