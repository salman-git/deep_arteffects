package com.meancoder.meanarteffect

import android.content.Context
import android.graphics.Bitmap
import android.os.SystemClock
import android.util.Log
import org.tensorflow.lite.DataType
import org.tensorflow.lite.Interpreter
import org.tensorflow.lite.gpu.CompatibilityList
import org.tensorflow.lite.gpu.GpuDelegate
import org.tensorflow.lite.nnapi.NnApiDelegate
import org.tensorflow.lite.support.common.FileUtil
import org.tensorflow.lite.support.common.ops.DequantizeOp
import org.tensorflow.lite.support.common.ops.NormalizeOp
import org.tensorflow.lite.support.image.ImageProcessor
import org.tensorflow.lite.support.image.TensorImage
import org.tensorflow.lite.support.image.ops.ResizeOp
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer
import java.lang.Integer.min

class StyleTransferHelper(
    var numThreads: Int = 2,
    var currentDelegate: Int = 0,
    var currentModel: Int = 0,
    val context: Context,
    val styleTransferListener: StyleTransferListener?
) {
    private var interpreterPredict: Interpreter? = null
    private var interpreterTransform: Interpreter? = null
    private var styleImage: Bitmap? = null
    private var inputPredictTargetWidth = 0
    private var inputPredictTargetHeight = 0
    private var inputTransformTargetWidth = 0
    private var inputTransformTargetHeight = 0
    private var outputPredictShape = intArrayOf()
    private var outputTransformShape = intArrayOf()

    init {
        if (setupStyleTransfer()) {
            initInputOutputDims()
        } else {
            styleTransferListener?.onError("TFLite failed to init.")
        }
    }
    private fun initInputOutputDims() {
        inputPredictTargetHeight = interpreterPredict!!.getInputTensor(0)
            .shape()[1]
        inputPredictTargetWidth = interpreterPredict!!.getInputTensor(0)
            .shape()[2]
        outputPredictShape = interpreterPredict!!.getOutputTensor(0).shape()

        inputTransformTargetHeight =
            interpreterTransform!!.getInputTensor(0)
                .shape()[1]
        inputTransformTargetWidth = interpreterTransform!!.getInputTensor(0)
            .shape()[2]
        outputTransformShape =
            interpreterTransform!!.getOutputTensor(0).shape()
    }
    private fun setupStyleTransfer(): Boolean {
        val tfliteOption = Interpreter.Options()
        tfliteOption.numThreads = numThreads

        when (currentDelegate) {
            DELEGATE_CPU -> {
                // Default
            }
            DELEGATE_GPU -> {
                if (CompatibilityList().isDelegateSupportedOnThisDevice) {
                    tfliteOption.addDelegate(GpuDelegate())
                } else {
                    styleTransferListener?.onError("GPU is not supported on this device")
                }
            }
            DELEGATE_NNAPI -> {
                tfliteOption.addDelegate(NnApiDelegate())
            }
        }
        val modelPredict: String
        val modelTransfer: String
        if (currentModel == MODEL_INT8) {
            modelPredict = "predict_int8.tflite"
            modelTransfer = "transfer_int8.tflite"
        } else {
            modelPredict = "predict_float16.tflite"
            modelTransfer = "transfer_float16.tflite"
        }

        try {
            interpreterPredict = Interpreter(
                FileUtil.loadMappedFile(
                    context,
                    modelPredict,
                ), tfliteOption
            )

            interpreterTransform = Interpreter(
                FileUtil.loadMappedFile(
                    context,
                    modelTransfer
                ), tfliteOption
            )

            return true
        } catch (e: Exception) {
            styleTransferListener?.onError(
                "Style transfer failed to initialize. See error logs for " +
                        "details"
            )
            Log.e(TAG, "TFLite failed to load model with error: " + e.message)
            return false
        }

    }

    fun transfer(bitmap: Bitmap, intensity:Float=1f) {
        styleTransferListener?.onStartTransfer()
        if (interpreterPredict == null || interpreterTransform == null) {
            setupStyleTransfer()
        }

        if (styleImage == null) {
            styleTransferListener?.onError(
                "Please select the style before run the transforming"
            )
            return
        }
        // Inference time is the difference between the system time at the start and finish of the
        // process
        var inferenceTime = SystemClock.uptimeMillis()
        interpreterPredict!!.resizeInput(0, intArrayOf(1,
            (styleImage!!.width*intensity).toInt(), (styleImage!!.height*intensity).toInt(), 3))
        interpreterPredict!!.allocateTensors()
        initInputOutputDims()
        processInputImage(
            bitmap,
            inputTransformTargetWidth,
            inputTransformTargetHeight
        )?.let { inputImage ->
            processInputImage(
                styleImage!!,
                inputPredictTargetWidth,
                inputPredictTargetHeight
            )?.let { styleImage ->
                val predictOutput = TensorBuffer.createFixedSize(
                    outputPredictShape, DataType.FLOAT32
                )
                // The results of this inference could be reused given the style does not change
                // That would be a good practice in case this was applied to a video stream.
                interpreterPredict?.run(styleImage.buffer, predictOutput.buffer)
                val mappedArray = predictOutput.floatArray.map {it}.toFloatArray()
                val mappedBuffer = TensorBuffer.createFixedSize(outputPredictShape, DataType.FLOAT32)
                mappedBuffer.loadArray(mappedArray)
                val transformInput =
                    arrayOf(inputImage.buffer, mappedBuffer.buffer)
                val outputImage = TensorBuffer.createFixedSize(
                    outputTransformShape, DataType.FLOAT32
                )
                interpreterTransform?.runForMultipleInputsOutputs(
                    transformInput,
                    mapOf(Pair(0, outputImage.buffer))
                )
                getOutputImage(outputImage, bitmap.height, bitmap.width)?.let { outputBitmap ->
                    inferenceTime = SystemClock.uptimeMillis() - inferenceTime

                    styleTransferListener?.onResult(outputBitmap, inferenceTime, bitmap.height, bitmap.width)
                }
            }
        }
    }
    fun setStyleImage(bitmap: Bitmap) {
        styleImage = bitmap
    }

    fun clearStyleTransferHelper() {
        interpreterPredict = null
        interpreterTransform = null
    }

    // Preprocess the image and convert it into a TensorImage for
    // transformation.
    private fun processInputImage(
        image: Bitmap,
        targetWidth: Int,
        targetHeight: Int
    ): TensorImage? {
        val height = image.height
        val width = image.width
        val cropSize = min(height, width)
        val imageProcessor = ImageProcessor.Builder()
//            .add(ResizeWithCropOrPadOp(cropSize, cropSize))
            .add(
                ResizeOp(
                    targetHeight,
                    targetWidth,
                    ResizeOp.ResizeMethod.BILINEAR
                )
            )
            .add(NormalizeOp(0f, 255f))
            .build()
        val tensorImage = TensorImage(DataType.FLOAT32)
        tensorImage.load(image)
        return imageProcessor.process(tensorImage)
    }

    // Convert output bytebuffer to bitmap image.
    private fun getOutputImage(output: TensorBuffer, height: Int, width: Int): Bitmap? {
        val imagePostProcessor = ImageProcessor.Builder()
            .add(DequantizeOp(0f, 255f))
            .add(ResizeOp(height, width, ResizeOp.ResizeMethod.BILINEAR))

            .build()
        val tensorImage = TensorImage(DataType.FLOAT32)
        tensorImage.load(output)
        return imagePostProcessor.process(tensorImage).bitmap
    }

    interface StyleTransferListener {
        fun onError(error: String)
        fun onResult(bitmap: Bitmap, inferenceTime: Long, sourceHeight: Int, sourceWidth: Int)
        fun onStartTransfer()
    }

    companion object {
        const val DELEGATE_CPU = 0
        const val DELEGATE_GPU = 1
        const val DELEGATE_NNAPI = 2
        const val MODEL_INT8 = 0

        private const val TAG = "Style Transfer Helper"
    }
}
