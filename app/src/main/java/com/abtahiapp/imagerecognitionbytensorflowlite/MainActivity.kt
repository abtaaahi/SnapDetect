package com.abtahiapp.imagerecognitionbytensorflowlite

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import android.app.Activity
import android.content.ContentValues.TAG
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Environment
import android.widget.ImageView
import android.widget.TextView
import org.tensorflow.lite.Interpreter
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.channels.FileChannel
import android.provider.MediaStore
import android.util.Log
import android.view.View
import android.widget.EditText
import android.widget.ImageButton
import android.widget.Toast
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.cardview.widget.CardView
import androidx.core.content.ContextCompat
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import androidx.core.content.FileProvider
import com.bumptech.glide.Glide
import com.bumptech.glide.request.target.CustomTarget
import com.google.common.util.concurrent.ListenableFuture
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.io.FileOutputStream

class MainActivity : AppCompatActivity() {

    private lateinit var imageView: ImageView
    private lateinit var selectImageButton: ImageButton
    private lateinit var captureImageButton: ImageButton
    private lateinit var shareButton: ImageButton
    private lateinit var layoutCameraView: CardView
    private lateinit var resultTextView: TextView
    private lateinit var detailTextView: TextView
    private lateinit var tflite: Interpreter
    private lateinit var labels: List<String>
    private lateinit var currentPhotoPath: String
    private lateinit var imageUrlEditText: EditText
    private lateinit var recognizeFromUrlButton: ImageButton
    private lateinit var previewView: PreviewView
    private lateinit var cameraProviderFuture: ListenableFuture<ProcessCameraProvider>
    private lateinit var imageCapture: ImageCapture
    private lateinit var knowledgeGraphApiService: KnowledgeGraphApiService
    private val apiKey = Secret.APIKEY

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val retrofit = Retrofit.Builder()
            .baseUrl("https://kgsearch.googleapis.com/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        knowledgeGraphApiService = retrofit.create(KnowledgeGraphApiService::class.java)

        imageView = findViewById(R.id.imageView)
        selectImageButton = findViewById(R.id.selectImageButton)
        resultTextView = findViewById(R.id.resultTextView)
        detailTextView = findViewById(R.id.detailTextView)
        captureImageButton = findViewById(R.id.selectCameraButton)
        shareButton = findViewById(R.id.shareButton)
        imageUrlEditText = findViewById(R.id.imageUrlEditText)
        recognizeFromUrlButton = findViewById(R.id.recognizeFromUrlButton)
        previewView = findViewById(R.id.cameraView)
        layoutCameraView = findViewById(R.id.layoutCameraView)

        cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()
            startCamera(cameraProvider)
        }, ContextCompat.getMainExecutor(this))

        recognizeFromUrlButton.setOnLongClickListener {
            Toast.makeText(this, "Recognize from URL", Toast.LENGTH_SHORT).show()
            true
        }

        selectImageButton.setOnLongClickListener {
            Toast.makeText(this, "Select Image", Toast.LENGTH_SHORT).show()
            true
        }

        captureImageButton.setOnLongClickListener {
            Toast.makeText(this, "Capture Image", Toast.LENGTH_SHORT).show()
            true
        }

        shareButton.setOnLongClickListener {
            Toast.makeText(this, "Share", Toast.LENGTH_SHORT).show()
            true
        }
        selectImageButton.setOnClickListener {
            val intent = Intent(Intent.ACTION_PICK)
            intent.type = "image/*"
            startActivityForResult(intent, 1)
        }

        captureImageButton.setOnClickListener {
            if (imageView.visibility == View.VISIBLE) {
                imageView.visibility = View.GONE
                layoutCameraView.visibility = View.VISIBLE
                startCamera(cameraProviderFuture.get())
            } else {
                val photoFile: File? = try {
                    createImageFile()
                } catch (ex: IOException) {
                    Log.e(TAG, "Error creating photo file: ${ex.message}")
                    null
                }

                photoFile?.let {
                    val outputOptions = ImageCapture.OutputFileOptions.Builder(it).build()

                    imageCapture.takePicture(
                        outputOptions,
                        ContextCompat.getMainExecutor(this),
                        object : ImageCapture.OnImageSavedCallback {
                            override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                                val bitmap = BitmapFactory.decodeFile(it.absolutePath)
                                imageView.setImageBitmap(bitmap)

                                imageView.visibility = View.VISIBLE
                                layoutCameraView.visibility = View.GONE

                                val result = recognizeImage(bitmap)
                                resultTextView.text = result
                                fetchEntityDetails(result)
                            }

                            override fun onError(exception: ImageCaptureException) {
                                Log.e(TAG, "Photo capture failed: ${exception.message}", exception)
                                Toast.makeText(this@MainActivity, "Capture failed", Toast.LENGTH_SHORT).show()
                            }
                        }
                    )
                }
            }
        }


        val tfliteModel = loadModelFile()
        tflite = Interpreter(tfliteModel)

        labels = loadLabels("labels.txt")

        shareButton.setOnClickListener {
            shareImageAndText()
        }

        recognizeFromUrlButton.setOnClickListener {
            val imageUrl = imageUrlEditText.text.toString().trim()
            if (imageUrl.isNotEmpty()) {
                Glide.with(this)
                    .asBitmap()
                    .load(imageUrl)
                    .into(object : CustomTarget<Bitmap>() {
                        override fun onResourceReady(resource: Bitmap, transition: com.bumptech.glide.request.transition.Transition<in Bitmap>?) {
                            imageView.setImageBitmap(resource)
                            imageView.visibility = View.VISIBLE
                            layoutCameraView.visibility = View.GONE
                            val result = recognizeImage(resource)
                            resultTextView.text = result
                            fetchEntityDetails(result)
                        }
                        override fun onLoadFailed(errorDrawable: Drawable?) {
                            Toast.makeText(this@MainActivity, "Failed to load image from URL", Toast.LENGTH_SHORT).show()
                        }
                        override fun onLoadCleared(placeholder: Drawable?) {
                        }
                    })
            } else {
                Toast.makeText(this, "Please enter an image URL", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun startCamera(cameraProvider: ProcessCameraProvider) {
        val preview = Preview.Builder()
            .build()
            .also {
                it.setSurfaceProvider(previewView.surfaceProvider)
            }

        imageCapture = ImageCapture.Builder()
            .build()
        val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

        try {
            cameraProvider.unbindAll()
            cameraProvider.bindToLifecycle(
                this, cameraSelector, preview, imageCapture
            )
        } catch (e: Exception) {
            Log.e("CameraX", "Use case binding failed", e)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (resultCode == Activity.RESULT_OK) {
            when (requestCode) {
                1 -> { // From gallery selection
                    val uri = data?.data
                    uri?.let {
                        val inputStream = contentResolver.openInputStream(it)
                        val bitmap = BitmapFactory.decodeStream(inputStream)
                        imageView.setImageBitmap(bitmap)
                        imageView.visibility = View.VISIBLE
                        layoutCameraView.visibility = View.GONE
                        val result = recognizeImage(bitmap)
                        resultTextView.text = result
                        fetchEntityDetails(result)
                    }
                }
                2 -> { // From camera capture
                    val bitmap = BitmapFactory.decodeFile(currentPhotoPath)
                    imageView.setImageBitmap(bitmap)
                    val result = recognizeImage(bitmap)
                    resultTextView.text = result
                    fetchEntityDetails(result)
                }
            }
        }
    }

    @Throws(IOException::class)
    private fun createImageFile(): File {
        val timeStamp: String = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val storageDir: File? = getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        return File.createTempFile(
            "JPEG_${timeStamp}_",
            ".jpg",
            storageDir
        ).apply {
            currentPhotoPath = absolutePath
        }
    }

    private fun loadModelFile(): ByteBuffer {
        val assetFileDescriptor = assets.openFd("1.tflite")
        val inputStream = assetFileDescriptor.createInputStream()
        val fileChannel = inputStream.channel
        val startOffset = assetFileDescriptor.startOffset
        val declaredLength = assetFileDescriptor.declaredLength
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength).order(ByteOrder.nativeOrder())
    }

    private fun loadLabels(filename: String): List<String> {
        val labels = mutableListOf<String>()
        assets.open(filename).bufferedReader().useLines { lines ->
            lines.forEach { labels.add(it) }
        }
        return labels
    }

    private fun recognizeImage(bitmap: Bitmap): String {
        val resizedBitmap = Bitmap.createScaledBitmap(bitmap, 224, 224, true)
        val byteBuffer = ByteBuffer.allocateDirect(4 * 224 * 224 * 3).order(ByteOrder.nativeOrder())
        val intValues = IntArray(224 * 224)
        resizedBitmap.getPixels(intValues, 0, 224, 0, 0, 224, 224)

        for (pixel in intValues) {
            byteBuffer.putFloat((pixel shr 16 and 0xFF) / 255.0f)
            byteBuffer.putFloat((pixel shr 8 and 0xFF) / 255.0f)
            byteBuffer.putFloat((pixel and 0xFF) / 255.0f)
        }
        val resultArray = Array(1) { FloatArray(1001) }
        tflite.run(byteBuffer, resultArray)
        return getMaxResult(resultArray[0])
    }

    private fun fetchEntityDetails(query: String) {
        val call = knowledgeGraphApiService.getEntityDetails(query, apiKey)
        call.enqueue(object : Callback<KnowledgeGraphResponse> {
            override fun onResponse(call: Call<KnowledgeGraphResponse>, response: Response<KnowledgeGraphResponse>) {
                if (response.isSuccessful) {
                    val result = response.body()?.itemListElement?.firstOrNull()?.result
                    val description = result?.detailedDescription?.articleBody ?: result?.description ?: "No information found"
                    detailTextView.text = description
                } else {
                    detailTextView.text = "Failed to fetch details: ${response.code()}"
                }
            }

            override fun onFailure(call: Call<KnowledgeGraphResponse>, t: Throwable) {
                detailTextView.text = "Failed to fetch details: ${t.message}"
                t.printStackTrace()
            }
        })
    }

    private fun getMaxResult(resultArray: FloatArray): String {
        val maxIndex = resultArray.indices.maxByOrNull { resultArray[it] } ?: -1
        val label = if (maxIndex != -1) labels[maxIndex] else "Unknown"
        return label
    }

    private fun shareImageAndText() {
        if (imageView.drawable == null || resultTextView.text.isEmpty() || detailTextView.text.isEmpty()) {
            Toast.makeText(this, "Please select an image and ensure details are filled", Toast.LENGTH_SHORT).show()
            return
        }

        val bitmapDrawable = imageView.drawable as BitmapDrawable
        val bitmap = bitmapDrawable.bitmap

        val cachePath = File(cacheDir, "images")
        cachePath.mkdirs()
        val stream = FileOutputStream("$cachePath/image.png")
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
        stream.close()

        val imagePath = File(cachePath, "image.png")
        val imageUri = FileProvider.getUriForFile(this, "${packageName}.fileprovider", imagePath)

        val shareIntent = Intent().apply {
            action = Intent.ACTION_SEND_MULTIPLE
            putParcelableArrayListExtra(Intent.EXTRA_STREAM, arrayListOf(imageUri))
            putExtra(Intent.EXTRA_TEXT, "${resultTextView.text}\n\n${detailTextView.text}")
            type = "image/*"
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        startActivity(Intent.createChooser(shareIntent, "Share Image and Text"))
    }
}