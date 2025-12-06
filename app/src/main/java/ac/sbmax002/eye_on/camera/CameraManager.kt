package ac.sbmax002.eye_on.camera

import android.content.Context
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
}