package com.phlox.tvwebbrowser.service.ai
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import com.google.mediapipe.framework.image.BitmapImageBuilder
import com.google.mediapipe.tasks.core.BaseOptions
import com.google.mediapipe.tasks.vision.objectdetector.ObjectDetector
import com.google.mediapipe.tasks.vision.objectdetector.ObjectDetector.ObjectDetectorOptions

class ObjectDetectionHelper(context: Context, modelAssetPath: String) {

    private var objectDetector: ObjectDetector

    init {
        // Configure the ObjectDetector using BaseOptions and ObjectDetectorOptions
        val baseOptions = BaseOptions.builder()
            .setModelAssetPath(modelAssetPath)
            .build()

        val options = ObjectDetectorOptions.builder()
            .setBaseOptions(baseOptions)
            .setMaxResults(5) // Limit the number of detected objects
            .setScoreThreshold(0.5f) // Minimum confidence score (0.0 to 1.0)
            .build()

        objectDetector = ObjectDetector.createFromOptions(context, options)
    }

    /**
     * Takes an input bitmap, detects objects, and draws bounding boxes around them.
     * Returns a new bitmap with the drawn annotations.
     */
    fun detectAndDraw(inputBitmap: Bitmap): Bitmap {
        // 1. Convert Android Bitmap to MediaPipe MPImage
        val mpImage = BitmapImageBuilder(inputBitmap).build()

        // 2. Run object detection synchronously (for IMAGE mode)
        val detectionResult = objectDetector.detect(mpImage)

        // 3. Create a mutable copy of the bitmap to draw on
        val resultBitmap = inputBitmap.copy(Bitmap.Config.ARGB_8888, true)
        val canvas = Canvas(resultBitmap)

        // 4. Set up Paint objects for the bounding box and text
        val boxPaint = Paint().apply {
            color = Color.RED
            style = Paint.Style.STROKE
            strokeWidth = 8f
        }
        val textPaint = Paint().apply {
            color = Color.RED
            textSize = 50f
            style = Paint.Style.FILL
            isAntiAlias = true
        }

        // 5. Iterate through detections and draw them
        detectionResult.detections().forEach { detection ->
            val boundingBox = detection.boundingBox() // Returns an Android RectF

            // Draw the bounding box
            canvas.drawRect(boundingBox, boxPaint)

            // Get the highest scoring category and draw the label above the box
            val category = detection.categories().firstOrNull()
            if (category != null) {
                // Format: "Person (0.85)"
                val labelText = "${category.categoryName()} (${String.format("%.2f", category.score())})"

                // Draw text just above the bounding box
                canvas.drawText(labelText, boundingBox.left, boundingBox.top - 10f, textPaint)
            }
        }

        return resultBitmap
    }

    // Remember to close the detector when you are done to free up resources
    fun close() {
        objectDetector.close()
    }
}