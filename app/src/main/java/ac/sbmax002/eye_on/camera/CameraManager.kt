package ac.sbmax002.eye_on.camera

import android.content.Context
import android.util.Log
import android.util.Size
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.lifecycle.LifecycleOwner
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class CameraManager(
    private val context: Context,
    private val lifecycleOwner: LifecycleOwner,
    private val config: CameraConfig = CameraConfig(),
    private val onFrameAvailable: ((ImageProxy) -> Unit)? = null
) {
    /**
     * @param context 앱 컨텍스트 (카메라 Provider 생성 등에 사용)
     * @param lifecycleOwner CameraX lifecycle 바인딩에 사용
     *  @param config 카메라 설정(CameraConfig)
     * @param onFrameAvailable 매 프레임(ImageProxy) 분석 콜백. 후처리 로직 삽입.
     */

    private var cameraProvider: ProcessCameraProvider? = null //카메라 바인딩
    private var preview: Preview? = null // 화면에 비디오를 띄우는 뷰
    private var imageAnalysis: ImageAnalysis? = null //프레임 분석용 useCase
    private lateinit var cameraExecutor: ExecutorService //ImageAnalysis가 프레임을 처리할 백그라운드 스레드 풀
    // 🔹 노출 제어에 쓸 CameraX Camera 참조
    private var camera: Camera? = null

    // 초기화 (카메라 시작)
    suspend fun startCamera(previewView: PreviewView) = withContext(Dispatchers.Main) { //Camera 바인딩, view 작업은 메인 스레드에서 해야함
        cameraExecutor = Executors.newSingleThreadExecutor() //카메라 프레임 분석용 스레드 생성

        val provider = ProcessCameraProvider.getInstance(context).get()
        cameraProvider = provider

        // Preview UseCase
        preview = Preview.Builder() //화면에 보여줄 프리뷰 useCase builder
            .build()
            .also { // 생성된 객체에 대해서 추가 작업을 함
                it.setSurfaceProvider(previewView.surfaceProvider) //화면을 PreveiwView에 연결
            }

        val targetSize: Size = config.resolutionPreset.size

        // ImageAnalysis UseCase
        imageAnalysis = ImageAnalysis.Builder() // 카메라 프레임을 받아서 분석하는 usecae builder
            .setTargetResolution(targetSize) //분석용 프레임 해상도 지정
            .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_RGBA_8888) // 모델이 RGBA_8888로 포멧을 사용
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)//프레임이 많이 들어 왔을 떄 오래된 프레임은 버림
            .build()
            .also {
                it.setAnalyzer(cameraExecutor) { image -> // 스레드 풀에서 프레임을 분석하기 위함. image는 한 프레임
                    // 1) 프레임 밝기 계산
                    val avgLuma = estimateLuma(image)

                    // 2) 노출 보정
                    adjustExposure(avgLuma)
                    onFrameAvailable?.invoke(image)  // 프레임 콜백(외부에 넘겨준 콜백이 있으면 image를 넘겨줌)
                }
            }

        provider.unbindAll() //기존 바인딩된 카메라 해제
        provider.bindToLifecycle( //카메라를 lifecle에 맞춰 동작하게 함
            lifecycleOwner,
            config.cameraSelector,
            preview,
            imageAnalysis
        )
    }

    fun stopCamera() {
        cameraProvider?.unbindAll()
        cameraExecutor.shutdown()
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