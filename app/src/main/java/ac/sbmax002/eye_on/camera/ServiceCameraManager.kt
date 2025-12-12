package ac.sbmax002.eye_on.camera

import android.content.Context
import android.util.Log
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.core.content.ContextCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

/**
 * Service에서 사용할 커스텀 LifecycleOwner
 * Service의 lifecycle을 수동으로 관리
 */
private class ServiceLifecycleOwner : LifecycleOwner {
    private val lifecycleRegistry = LifecycleRegistry(this)
    
    init {
        lifecycleRegistry.currentState = Lifecycle.State.CREATED
    }
    
    fun start() {
        lifecycleRegistry.currentState = Lifecycle.State.STARTED
    }
    
    fun resume() {
        lifecycleRegistry.currentState = Lifecycle.State.RESUMED
    }
    
    fun pause() {
        lifecycleRegistry.currentState = Lifecycle.State.STARTED
    }
    
    fun stop() {
        lifecycleRegistry.currentState = Lifecycle.State.CREATED
    }
    
    override val lifecycle: Lifecycle
        get() = lifecycleRegistry
}

/**
 * Service에서 사용하는 카메라 매니저
 * LifecycleOwner 없이 카메라를 실행할 수 있도록 구현
 * 
 * ProcessCameraProvider를 직접 바인딩하여 Activity lifecycle과 독립적으로 동작
 */
class ServiceCameraManager(
    private val context: Context,
    private val config: CameraConfig = CameraConfig(),
    private val onFrameAvailable: ((ImageProxy) -> Unit)? = null
) {

    // 🔹 노출 제어에 쓸 CameraX Camera 참조
    private var camera: Camera? = null
    
    private var cameraProvider: ProcessCameraProvider? = null
    private var imageAnalysis: ImageAnalysis? = null
    private var preview: Preview? = null
    private lateinit var cameraExecutor: ExecutorService
    private val lifecycleOwner = ServiceLifecycleOwner()
    
    /**
     * Service에서 카메라 시작 (ImageAnalysis만)
     * Preview는 Activity에서 필요할 때 추가
     */
    suspend fun startCamera() = withContext(Dispatchers.Main) {
        cameraExecutor = Executors.newSingleThreadExecutor()
        
        val provider = getCameraProviderSync()
        cameraProvider = provider
        
        // ImageAnalysis UseCase 생성
        // setTargetResolution은 deprecated되었으므로 제거
        // CameraX가 자동으로 적절한 해상도를 선택합니다
        imageAnalysis = ImageAnalysis.Builder()
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_RGBA_8888)
            .build()
            .also {
                it.setAnalyzer(cameraExecutor) { image ->
                    // 1) 프레임 밝기 계산
                    val avgLuma = estimateLuma(image)

                    // 2) 노출 보정
                    adjustExposure(avgLuma)

                    onFrameAvailable?.invoke(image)
                }
            }
        
        // 기존 바인딩 해제
        provider.unbindAll()
        
        // LifecycleOwner 시작
        lifecycleOwner.start()
        
        // Service LifecycleOwner로 바인딩
        camera = provider.bindToLifecycle(
            lifecycleOwner,
            config.cameraSelector,
            imageAnalysis
        )
    }
    
    /**
     * Activity에서 Preview UseCase 추가
     * Service에서 이미 실행 중인 카메라에 Preview만 추가
     */
    suspend fun attachPreview(previewView: PreviewView) = withContext(Dispatchers.Main) {
        val provider = cameraProvider ?: return@withContext
        
        preview = Preview.Builder()
            .build()
            .also {
                it.setSurfaceProvider(previewView.surfaceProvider)
            }
        
        // Preview와 ImageAnalysis 함께 바인딩
        provider.unbindAll()
        provider.bindToLifecycle(
            lifecycleOwner,
            config.cameraSelector,
            preview,
            imageAnalysis
        )
    }
    
    /**
     * Activity에서 Preview UseCase 제거
     * ImageAnalysis는 계속 실행
     */
    suspend fun detachPreview() = withContext(Dispatchers.Main) {
        val provider = cameraProvider ?: return@withContext
        
        preview = null
        
        // ImageAnalysis만 다시 바인딩
        provider.unbindAll()
        provider.bindToLifecycle(
            lifecycleOwner,
            config.cameraSelector,
            imageAnalysis
        )
    }
    
    /**
     * 카메라 중지
     */
    fun stopCamera() {
        lifecycleOwner.stop()
        cameraProvider?.unbindAll()
        imageAnalysis = null
        preview = null
        if (::cameraExecutor.isInitialized) {
            cameraExecutor.shutdown()
        }
    }
    
    /**
     * 카메라 Provider 인스턴스 가져오기 (동기)
     * 코루틴에서 사용하기 위해 suspend 함수로 변환
     */
    private suspend fun getCameraProviderSync(): ProcessCameraProvider {
        return suspendCancellableCoroutine { continuation ->
            val providerFuture = ProcessCameraProvider.getInstance(context)
            providerFuture.addListener({
                try {
                    val provider = providerFuture.get()
                    continuation.resume(provider)
                } catch (e: Exception) {
                    continuation.resumeWithException(e)
                }
            }, ContextCompat.getMainExecutor(context))
        }
    }
    
    /**
     * 카메라 Provider 인스턴스 가져오기
     * Activity에서 같은 Provider를 사용하기 위함
     */
    suspend fun getCameraProvider(): ProcessCameraProvider? {
        return cameraProvider ?: getCameraProviderSync().also {
            cameraProvider = it
        }
    }

    //밝기 계산 함수
    private fun estimateLuma(image: ImageProxy): Double {
        // Y-plane (밝기 정보)
        val yPlane = image.planes[0]

        // 🔴 원본 버퍼를 직접 쓰지 말고, 복제본을 사용
        val buffer = yPlane.buffer.duplicate()
        buffer.rewind()   // position = 0 으로 초기화

        val data = ByteArray(buffer.remaining())
        buffer.get(data)

        // 모든 픽셀 다 돌면 비싸니까, 샘플링 간격을 둔다
        var sum = 0L
        var count = 0
        val step = 8  // 8픽셀마다 하나씩 샘플

        var i = 0
        while (i < data.size) {
            sum += (data[i].toInt() and 0xFF)
            count++
            i += step
        }

        if (count == 0) return 0.0
        return sum.toDouble() / count.toDouble()  // 0 ~ 255
    }


    //설정한 노출 보정 인덱스 캐시
    private var lastExposureIndex: Int? = null

    private fun adjustExposure(avgLuma: Double) {
        val cam = camera ?: return
        val exposureState: ExposureState = cam.cameraInfo.exposureState
        if (!exposureState.isExposureCompensationSupported) return

        val currentIndex = exposureState.exposureCompensationIndex
        val range = exposureState.exposureCompensationRange

        // 목표 밝기(0~255 사이) + 허용 오차
        val targetLuma = 110.0
        val tolerance = 15.0

        var newIndex = currentIndex

        if (avgLuma < targetLuma - tolerance) {
            // 너무 어두우면 노출 ↑
            newIndex = currentIndex + 1
        } else if (avgLuma > targetLuma + tolerance) {
            // 너무 밝으면 노출 ↓
            newIndex = currentIndex - 1
        } else {
            // 적당히 밝으면 건드리지 않음
            return
        }

        // 카메라가 지원하는 범위 안으로 자르기
        newIndex = newIndex.coerceIn(range.lower, range.upper)

        // 같은 값이면 또 호출할 필요 없음
        if (newIndex == currentIndex || newIndex == lastExposureIndex) return

        lastExposureIndex = newIndex
        Log.d("CameraManager", "adjustExposure: luma=${avgLuma}, index=$currentIndex -> $newIndex")

        cam.cameraControl.setExposureCompensationIndex(newIndex)
    }

}

