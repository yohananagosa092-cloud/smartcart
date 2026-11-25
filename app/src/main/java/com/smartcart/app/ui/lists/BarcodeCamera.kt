package com.smartcart.app.ui.lists

import androidx.camera.core.CameraSelector
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage

internal class BarcodeCamera(
    private val previewView: PreviewView,
    private val owner: LifecycleOwner,
    private val onBarcode: (String) -> Unit,
    private val onError: () -> Unit,
) {
    private val executor = ContextCompat.getMainExecutor(previewView.context)
    private val scanner = BarcodeScanning.getClient(
        BarcodeScannerOptions.Builder().setBarcodeFormats(
            Barcode.FORMAT_EAN_13, Barcode.FORMAT_EAN_8, Barcode.FORMAT_UPC_A,
            Barcode.FORMAT_UPC_E, Barcode.FORMAT_CODE_128, Barcode.FORMAT_ITF,
        ).build(),
    )
    private var provider: ProcessCameraProvider? = null
    private var preview: Preview? = null
    private var analysis: ImageAnalysis? = null
    private var active = false
    private var generation = 0

    @androidx.annotation.OptIn(ExperimentalGetImage::class)
    fun start() {
        if (active) return
        active = true
        val request = ++generation
        val future = ProcessCameraProvider.getInstance(previewView.context)
        future.addListener({
            if (!active || request != generation) return@addListener
            try {
                val cameraProvider = future.get()
                val cameraPreview = Preview.Builder().build().also {
                    it.setSurfaceProvider(previewView.surfaceProvider)
                }
                val cameraAnalysis = ImageAnalysis.Builder()
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST).build()
                cameraAnalysis.setAnalyzer(executor) { frame ->
                    val mediaImage = frame.image
                    if (!active || request != generation || mediaImage == null) {
                        frame.close()
                    } else {
                        val image = InputImage.fromMediaImage(mediaImage, frame.imageInfo.rotationDegrees)
                        scanner.process(image)
                            .addOnSuccessListener { barcodes ->
                                if (!active || request != generation) return@addOnSuccessListener
                                val barcode = barcodes.firstNotNullOfOrNull { detection ->
                                    detection.rawValue?.takeIf { code ->
                                        code.isNotEmpty() && code.all { it in '0'..'9' }
                                    }
                                }
                                if (barcode != null) {
                                    stop()
                                    onBarcode(barcode)
                                }
                            }
                            .addOnFailureListener {
                                if (active && request == generation) {
                                    stop()
                                    onError()
                                }
                            }
                            .addOnCompleteListener { frame.close() }
                    }
                }
                provider = cameraProvider
                preview = cameraPreview
                analysis = cameraAnalysis
                cameraProvider.bindToLifecycle(owner, CameraSelector.DEFAULT_BACK_CAMERA, cameraPreview, cameraAnalysis)
            } catch (_: Exception) {
                stop()
                onError()
            }
        }, executor)
    }

    fun stop() {
        active = false
        generation++
        analysis?.clearAnalyzer()
        val boundPreview = preview
        val boundAnalysis = analysis
        if (boundPreview != null && boundAnalysis != null) provider?.unbind(boundPreview, boundAnalysis)
        preview = null
        analysis = null
    }

    fun close() {
        stop()
        scanner.close()
    }
}
