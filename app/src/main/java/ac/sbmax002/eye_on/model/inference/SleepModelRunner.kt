package ac.sbmax002.eye_on.model.inference

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import org.tensorflow.lite.Interpreter
import java.io.File
import java.io.FileOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.exp

class SleepModelRunner(context: Context) {
    private val sleepInterpreter = Interpreter(File(assetFilePath(context, SLEEP_MODEL_ASSET)))
    private val eyeInterpreter = Interpreter(File(assetFilePath(context, EYE_MODEL_ASSET)))

    init {
        sleepInterpreter.allocateTensors()
        eyeInterpreter.allocateTensors()
    }

    private val eyeResizeBitmap =
        Bitmap.createBitmap(EYE_INPUT_SIZE, EYE_INPUT_SIZE, Bitmap.Config.ARGB_8888)
    private val eyeResizeCanvas = Canvas(eyeResizeBitmap)
    private val eyeResizePaint = Paint(Paint.FILTER_BITMAP_FLAG)
    private val eyePixels = IntArray(EYE_INPUT_SIZE * EYE_INPUT_SIZE)
    private var eyeInputBuffer = newFloatBuffer(3 * EYE_INPUT_SIZE * EYE_INPUT_SIZE)

    @Synchronized
    fun runSleepGru(features: FloatArray, seqLen: Int): FloatArray {
        require(seqLen > 0) { "seqLen must be > 0" }
        require(features.size == seqLen * FEATURE_COUNT) {
            "features.size must be seqLen * $FEATURE_COUNT. got=${features.size}, seqLen=$seqLen"
        }

        val input = buildSleepInputBuffer(features, seqLen, sleepInterpreter.getInputTensor(0).shape())
        val output = runFloatModel(sleepInterpreter, input)
        return normalizeProbabilities(output)
    }

    @Synchronized
    fun describeSleepModel(): String {
        val inputTensor = sleepInterpreter.getInputTensor(0)
        val outputTensor = sleepInterpreter.getOutputTensor(0)
        return "inputShape=${inputTensor.shape().contentToString()}, " +
            "inputType=${inputTensor.dataType()}, " +
            "outputShape=${outputTensor.shape().contentToString()}, " +
            "outputType=${outputTensor.dataType()}"
    }

    fun runEyeModelBatch(sourceBitmap: Bitmap, eyeRects: List<Rect>): List<FloatArray> {
        require(eyeRects.isNotEmpty()) { "eyeRects must not be empty" }

        return eyeRects.map { rect ->
            val input = buildEyeTensor(sourceBitmap, rect)
            val outputValues = runFloatModel(eyeInterpreter, input)
            normalizeProbabilities(outputValues)
        }
    }

    private fun buildSleepInputBuffer(
        features: FloatArray,
        seqLen: Int,
        inputShape: IntArray
    ): ByteBuffer {
        require(inputShape.size == 3) {
            "sleep GRU input rank must be 3. got=${inputShape.joinToString(prefix = "[", postfix = "]")}"
        }

        val featureAxis = inputShape.indexOf(FEATURE_COUNT).takeIf { it >= 0 }
            ?: error(
                "sleep GRU input shape must contain feature count $FEATURE_COUNT. " +
                    "got=${inputShape.joinToString(prefix = "[", postfix = "]")}"
            )
        val seqAxis = listOf(1, 2).firstOrNull { it != featureAxis && inputShape[it] == seqLen }
            ?: listOf(1, 2).firstOrNull { it != featureAxis && inputShape[it] == -1 }
            ?: listOf(1, 2).firstOrNull { it != featureAxis && inputShape[it] > 0 }
            ?: listOf(1, 2).firstOrNull { it != featureAxis }
            ?: error(
                "sleep GRU input shape must contain seqLen $seqLen. " +
                    "got=${inputShape.joinToString(prefix = "[", postfix = "]")}"
            )
        val modelSeqLen = inputShape[seqAxis].takeIf { it > 0 } ?: seqLen
        require(seqLen >= modelSeqLen) {
            "sleep GRU needs at least $modelSeqLen feature rows. got=$seqLen"
        }

        if (inputShape[0] != 1 || inputShape[seqAxis] == -1 || inputShape[featureAxis] != FEATURE_COUNT) {
            val resizedShape = inputShape.copyOf()
            resizedShape[0] = 1
            resizedShape[seqAxis] = modelSeqLen
            resizedShape[featureAxis] = FEATURE_COUNT
            sleepInterpreter.resizeInput(0, resizedShape)
            sleepInterpreter.allocateTensors()
        }

        val firstFrameIndex = seqLen - modelSeqLen
        val buffer = newFloatBuffer(modelSeqLen * FEATURE_COUNT)
        if (seqAxis == 1 && featureAxis == 2) {
            for (frameIndex in firstFrameIndex until seqLen) {
                for (featureIndex in 0 until FEATURE_COUNT) {
                    buffer.putFloat(features[frameIndex * FEATURE_COUNT + featureIndex])
                }
            }
        } else if (seqAxis == 2 && featureAxis == 1) {
            for (featureIndex in 0 until FEATURE_COUNT) {
                for (frameIndex in firstFrameIndex until seqLen) {
                    buffer.putFloat(features[frameIndex * FEATURE_COUNT + featureIndex])
                }
            }
        } else {
            error("unsupported sleep GRU input shape ${inputShape.joinToString(prefix = "[", postfix = "]")}")
        }
        buffer.rewind()
        return buffer
    }

    private fun runFloatModel(interpreter: Interpreter, input: ByteBuffer): FloatArray {
        val outputTensor = interpreter.getOutputTensor(0)
        val output = newFloatBuffer(outputTensor.numElements())
        interpreter.run(input, output)
        output.rewind()
        return FloatArray(outputTensor.numElements()) {
            output.float
        }
    }

    private fun buildEyeTensor(sourceBitmap: Bitmap, eyeRect: Rect): ByteBuffer {
        val inputShape = eyeInterpreter.getInputTensor(0).shape()
        require(inputShape.size == 4) {
            "eye model input rank must be 4. got=${inputShape.joinToString(prefix = "[", postfix = "]")}"
        }

        val isNhwc = inputShape[1] == EYE_INPUT_SIZE && inputShape[2] == EYE_INPUT_SIZE && inputShape[3] == 3
        val isNchw = inputShape[1] == 3 && inputShape[2] == EYE_INPUT_SIZE && inputShape[3] == EYE_INPUT_SIZE
        require(isNhwc || isNchw) {
            "unsupported eye model input shape ${inputShape.joinToString(prefix = "[", postfix = "]")}"
        }
        if (inputShape[0] != 1) {
            val resizedShape = if (isNhwc) {
                intArrayOf(1, EYE_INPUT_SIZE, EYE_INPUT_SIZE, 3)
            } else {
                intArrayOf(1, 3, EYE_INPUT_SIZE, EYE_INPUT_SIZE)
            }
            eyeInterpreter.resizeInput(0, resizedShape)
            eyeInterpreter.allocateTensors()
        }

        val imageSize = EYE_INPUT_SIZE * EYE_INPUT_SIZE
        val requiredFloats = 3 * imageSize
        if (eyeInputBuffer.capacity() != requiredFloats * BYTES_PER_FLOAT) {
            eyeInputBuffer = newFloatBuffer(requiredFloats)
        }
        eyeInputBuffer.clear()

        eyeResizeCanvas.drawBitmap(
            sourceBitmap,
            eyeRect,
            RectF(0f, 0f, EYE_INPUT_SIZE.toFloat(), EYE_INPUT_SIZE.toFloat()),
            eyeResizePaint
        )
        eyeResizeBitmap.getPixels(eyePixels, 0, EYE_INPUT_SIZE, 0, 0, EYE_INPUT_SIZE, EYE_INPUT_SIZE)

        if (isNhwc) {
            var pixelIndex = 0
            while (pixelIndex < imageSize) {
                val color = eyePixels[pixelIndex]
                eyeInputBuffer.putFloat(normalizeRed(color))
                eyeInputBuffer.putFloat(normalizeGreen(color))
                eyeInputBuffer.putFloat(normalizeBlue(color))
                pixelIndex++
            }
        } else {
            putNchwChannel(imageSize, ::normalizeRed)
            putNchwChannel(imageSize, ::normalizeGreen)
            putNchwChannel(imageSize, ::normalizeBlue)
        }

        eyeInputBuffer.rewind()
        return eyeInputBuffer
    }

    private fun putNchwChannel(imageSize: Int, normalize: (Int) -> Float) {
        var pixelIndex = 0
        while (pixelIndex < imageSize) {
            eyeInputBuffer.putFloat(normalize(eyePixels[pixelIndex]))
            pixelIndex++
        }
    }

    private fun normalizeRed(color: Int): Float =
        ((((color shr 16) and 0xFF) / 255f) - EYE_MODEL_MEAN[0]) / EYE_MODEL_STD[0]

    private fun normalizeGreen(color: Int): Float =
        ((((color shr 8) and 0xFF) / 255f) - EYE_MODEL_MEAN[1]) / EYE_MODEL_STD[1]

    private fun normalizeBlue(color: Int): Float =
        (((color and 0xFF) / 255f) - EYE_MODEL_MEAN[2]) / EYE_MODEL_STD[2]

    private fun normalizeProbabilities(values: FloatArray): FloatArray {
        if (values.isEmpty()) return values
        val sum = values.sum()
        val alreadyProbabilities = values.all { it.isFinite() && it in 0f..1f } && sum in 0.98f..1.02f
        if (alreadyProbabilities) return values

        val maxValue = values.maxOrNull() ?: 0f
        var expSum = 0.0
        val expValues = DoubleArray(values.size)
        values.forEachIndexed { index, value ->
            val expValue = exp((value - maxValue).toDouble())
            expValues[index] = expValue
            expSum += expValue
        }
        return FloatArray(values.size) { index ->
            (expValues[index] / expSum).toFloat()
        }
    }

    companion object {
        const val FEATURE_COUNT = 13
        const val EYE_INPUT_SIZE = 224
        private const val SLEEP_MODEL_ASSET = "gru_fp32.tflite"
        private const val EYE_MODEL_ASSET = "eye_fp32.tflite"
        private const val BYTES_PER_FLOAT = 4
        private val EYE_MODEL_MEAN = floatArrayOf(0.485f, 0.456f, 0.406f)
        private val EYE_MODEL_STD = floatArrayOf(0.229f, 0.224f, 0.225f)

        private fun newFloatBuffer(floatCount: Int): ByteBuffer =
            ByteBuffer.allocateDirect(floatCount * BYTES_PER_FLOAT).order(ByteOrder.nativeOrder())

        fun assetFilePath(context: Context, assetName: String): String {
            val file = File(context.filesDir, assetName)
            if (file.exists() && file.length() > 0) return file.absolutePath

            context.assets.open(assetName).use { input ->
                FileOutputStream(file).use { output ->
                    val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                    while (true) {
                        val read = input.read(buffer)
                        if (read == -1) break
                        output.write(buffer, 0, read)
                    }
                    output.flush()
                }
            }
            return file.absolutePath
        }
    }
}
