package ac.sbmax002.eye_on.model.vision

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Matrix
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.SystemClock
import android.util.Log
import androidx.annotation.VisibleForTesting
import androidx.camera.core.ImageProxy
import com.google.mediapipe.framework.image.BitmapImageBuilder
import com.google.mediapipe.framework.image.MPImage
import com.google.mediapipe.tasks.core.BaseOptions
import com.google.mediapipe.tasks.core.Delegate
import com.google.mediapipe.tasks.vision.core.RunningMode
import com.google.mediapipe.tasks.vision.facelandmarker.FaceLandmarker
import com.google.mediapipe.tasks.vision.facelandmarker.FaceLandmarkerResult

class FaceLandmarkerHelper(

    var minFaceDetectionConfidence: Float = DEFAULT_FACE_DETECTION_CONFIDENCE, // 얼굴을 탐지했다고 판단할 최소 확신도 0~1
    var minFaceTrackingConfidence: Float = DEFAULT_FACE_TRACKING_CONFIDENCE, //얼굴이 움직일 때 같은 사람인지 추적할 신뢰도 기준, 값이 높으면 새로운 얼굴 잡기 어려움
    var minFacePresenceConfidence: Float = DEFAULT_FACE_PRESENCE_CONFIDENCE, //프레임 안에 얼굴이 남아있다고 판단할 신뢰도 기준 값
    var maxNumFaces: Int = DEFAULT_NUM_FACES,
    var currentDelegate: Int = DELEGATE_CPU,
    var runningMode: RunningMode = RunningMode.LIVE_STREAM,
    val context: Context, //안드로드의 context : 앱의 상태, 리소스, 시스템 서비스, 파일 접근 등을 관리하는 환경객체, 모델파일이나, 리소스를 로드할 때 필요
    // this listener is only used when running in RunningMode.LIVE_STREAM
    val faceLandmarkerHelperListener: LandmarkerListener? = null //얼굴 인식 결과를 전달할 콜백
) {

    companion object { // java의 static과 같은 개념 클래스 전체에서 공통으로 쓰는 상수, 설정값
        const val TAG = "FaceLandmarkerHelper" //에러 로그 찍을 때
        private const val MP_FACE_LANDMARKER_TASK = "face_landmarker.task"

        const val DELEGATE_CPU = 0
        const val DELEGATE_GPU = 1
        const val DEFAULT_FACE_DETECTION_CONFIDENCE = 0.5F
        const val DEFAULT_FACE_TRACKING_CONFIDENCE = 0.5F
        const val DEFAULT_FACE_PRESENCE_CONFIDENCE = 0.5F
        const val DEFAULT_NUM_FACES = 1
        const val OTHER_ERROR = 0
        const val GPU_ERROR = 1
    }

    // For this example this needs to be a var so it can be reset on changes.
    // If the Face Landmarker will not change, a lazy val would be preferable.
    private var faceLandmarker: FaceLandmarker? = null // 모델 인스턴스 보관

    init {
        setupFaceLandmarker() // 생성 시 모델/옵션 초기화
    }

    fun clearFaceLandmarker() { //리소스 해제 및 참조 제거
        faceLandmarker?.close()
        faceLandmarker = null
    }

    // Return running status of FaceLandmarkerHelper
    fun isClose(): Boolean {
        return faceLandmarker == null
    }

    // 현재 설정을 사용해서 해당 스레드에서 얼굴 랜드마커를 초기화
    // CPU 모드는, 메인 스레드에서 생성된 landmarker를 백그라운드 스레드에서 사용하는 것이 가능(CPU 연산은 스레드 간 이동이 자유롭다.)
    // GPU를 사용하는 경우 Landmarker를 초기화한 스레드에서만 사용해야 한다.(다른 스레드로 옭겨 사용할 수 없음)
    // Landmarker ; faceLandmarker를 현재 설정값으로 초기화하는 함수
    fun setupFaceLandmarker() {
        // Set general face landmarker options
        val baseOptionBuilder = BaseOptions.builder() // 모델 옵션을 담는 빈 BaseOptions 빌더 생성

        // Use the specified hardware for running the model. Default to CPU
        when (currentDelegate) { // 하드웨어 옵션 빌더에 셋팅
            DELEGATE_CPU -> {
                baseOptionBuilder.setDelegate(Delegate.CPU)
            }
            DELEGATE_GPU -> {
                baseOptionBuilder.setDelegate(Delegate.GPU)
            }
        }

        baseOptionBuilder.setModelAssetPath("face_landmarker.task") //사용할 모델의 에셋 경로 지정

        // 실행모드 검증 ( 비동기 모드에서 결과를 받을 콜백이 없으면 예외 처리)
        when (runningMode) {
            RunningMode.LIVE_STREAM -> {
                if (faceLandmarkerHelperListener == null) {
                    throw IllegalStateException(
                        "faceLandmarkerHelperListener must be set when runningMode is LIVE_STREAM."
                    )
                }
            }
            else -> {
                // no-op
            }
        }
        //지금까지 설정한 BaseOptions를 실제 객체로 빌드
        try {
            val baseOptions = baseOptionBuilder.build()
            // Create an option builder with base options and specific
            // options only use for Face Landmarker.
            val optionsBuilder =
                FaceLandmarker.FaceLandmarkerOptions.builder()
                    .setBaseOptions(baseOptions)
                    .setMinFaceDetectionConfidence(minFaceDetectionConfidence)
                    .setMinTrackingConfidence(minFaceTrackingConfidence)
                    .setMinFacePresenceConfidence(minFacePresenceConfidence)
                    .setNumFaces(maxNumFaces)
                    .setOutputFaceBlendshapes(false) //얼굴 표정 벡터값
                    .setRunningMode(runningMode)

            // The ResultListener and ErrorListener only use for LIVE_STREAM mode.
            if (runningMode == RunningMode.LIVE_STREAM) {
                optionsBuilder
                    .setResultListener(this::returnLivestreamResult) //결과가 나왔을 때 콜백함수 지정, :: 함수참조 연산자
                    .setErrorListener(this::returnLivestreamError) // 에러 발생시 실행할 함수
            }

            val options = optionsBuilder.build() // 옵션 빌드
            faceLandmarker =
                FaceLandmarker.createFromOptions(context, options) // 실제 landmarker 인스턴스 생성
        } catch (e: IllegalStateException) {
            faceLandmarkerHelperListener?.onError(
                "Face Landmarker failed to initialize. See error logs for " +
                        "details"
            )
            Log.e(
                TAG, "MediaPipe failed to load the task with error: " + e
                    .message
            )
        } catch (e: RuntimeException) {
            // This occurs if the model being used does not support GPU
            faceLandmarkerHelperListener?.onError(
                "Face Landmarker failed to initialize. See error logs for " +
                        "details", GPU_ERROR
            )
            Log.e(
                TAG,
                "Face Landmarker failed to load model with error: " + e.message
            )
        }
    }

    // Convert the ImageProxy to MP Image and feed it to FacelandmakerHelper. LIVE_STERAM 모드 전용 함수
    fun detectLiveStream(
        imageProxy: ImageProxy, //CameraX가 넘겨준 한 프레임(ImageProxy)
        isFrontCamera: Boolean // 전면카메라 여부
    ) {
        if (runningMode != RunningMode.LIVE_STREAM) { // 다른 모드가 들어오는 것 방지
            throw IllegalArgumentException(
                "Attempting to call detectLiveStream" +
                        " while not using RunningMode.LIVE_STREAM"
            )
        }
        val frameTime = SystemClock.uptimeMillis() //현재 프레임의 타임스탬프를 가져옴

        // 프레임 크기와 같은 ARGB_8888 포맷의 비트맵 버퍼를 하나 생성 후 카메라 프레임 픽셀을 복사해 담을 예정
        val bitmapBuffer =
            Bitmap.createBitmap(
                imageProxy.width,
                imageProxy.height,
                Bitmap.Config.ARGB_8888
            )
        //imageProxy,planes[0].buffer의 데이터를 bitmapBuffer로 복사; (ImageAnalysis가 RGBA_8888로 설정되어 있을 때만 정상. YUV_420이라면 색이 틀어짐 따로 RGB로 변환시켜야함)
        imageProxy.use { bitmapBuffer.copyPixelsFromBuffer(imageProxy.planes[0].buffer) } // use블록은 끝날 때 자동으로 close()호출함
        //imageProxy.close() //중복

        //화면 표시 방향과 일치하도록 회적을 적용
        //전면 카메라일 경우 좌우 미러 플립 적용 (x축 스케일 -1)
        val matrix = Matrix().apply {
            // Rotate the frame received from the camera to be in the same direction as it'll be shown
            postRotate(imageProxy.imageInfo.rotationDegrees.toFloat())

            // flip image if user use front camera
            if (isFrontCamera) {
                postScale(
                    -1f,
                    1f,
                    imageProxy.width.toFloat(),
                    imageProxy.height.toFloat()
                )
            }
        }
        //위에서 만든 변환 행렬을 실제로 적용해 새 비트맵 생성
        val rotatedBitmap = Bitmap.createBitmap(
            bitmapBuffer, 0, 0, bitmapBuffer.width, bitmapBuffer.height,
            matrix, true
        )

        // 모델이 요구하는 MPImage로 래핑, 바로 추론에 사용 가능
        val mpImage = BitmapImageBuilder(rotatedBitmap).build()

        //LIVE_STREAM 모드의 비동기 추론 실행(모델 실행), 결과는 등록한 ResultListener 콜백으로 돌아옴
        detectAsync(mpImage, frameTime)
    }

    //11-07
    // 비동기 추론 호출용 함수
    @VisibleForTesting // 테스트 코드에서도 접근할 수 있도록 공개
    fun detectAsync(mpImage: MPImage, frameTime: Long) {
        faceLandmarker?.detectAsync(mpImage, frameTime)
        // As we're using running mode LIVE_STREAM, the landmark result will
        // be returned in returnLivestreamResult function
    }

    // Accepts the URI for a video file loaded from the user's gallery and attempts to run
    // face landmarker inference on the video. This process will evaluate every
    // frame in the video and attach the results to a bundle that will be
    // returned.
    fun detectVideoFile(
        videoUri: Uri,
        inferenceIntervalMs: Long
    ): VideoResultBundle? {
        if (runningMode != RunningMode.VIDEO) {
            throw IllegalArgumentException(
                "Attempting to call detectVideoFile" +
                        " while not using RunningMode.VIDEO"
            )
        }

        // Inference time is the difference between the system time at the start and finish of the
        // process
        val startTime = SystemClock.uptimeMillis()

        var didErrorOccurred = false

        // Load frames from the video and run the face landmarker.
        val retriever = MediaMetadataRetriever()
        retriever.setDataSource(context, videoUri)
        val videoLengthMs =
            retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
                ?.toLong()

        // Note: We need to read width/height from frame instead of getting the width/height
        // of the video directly because MediaRetriever returns frames that are smaller than the
        // actual dimension of the video file.
        val firstFrame = retriever.getFrameAtTime(0)
        val width = firstFrame?.width
        val height = firstFrame?.height

        // If the video is invalid, returns a null detection result
        if ((videoLengthMs == null) || (width == null) || (height == null)) return null

        // Next, we'll get one frame every frameInterval ms, then run detection on these frames.
        val resultList = mutableListOf<FaceLandmarkerResult>()
        val numberOfFrameToRead = videoLengthMs.div(inferenceIntervalMs)

        for (i in 0..numberOfFrameToRead) {
            val timestampMs = i * inferenceIntervalMs // ms

            retriever
                .getFrameAtTime(
                    timestampMs * 1000, // convert from ms to micro-s
                    MediaMetadataRetriever.OPTION_CLOSEST
                )
                ?.let { frame ->
                    // Convert the video frame to ARGB_8888 which is required by the MediaPipe
                    val argb8888Frame =
                        if (frame.config == Bitmap.Config.ARGB_8888) frame
                        else frame.copy(Bitmap.Config.ARGB_8888, false)

                    // Convert the input Bitmap object to an MPImage object to run inference
                    val mpImage = BitmapImageBuilder(argb8888Frame).build()

                    // Run face landmarker using MediaPipe Face Landmarker API
                    faceLandmarker?.detectForVideo(mpImage, timestampMs)
                        ?.let { detectionResult ->
                            resultList.add(detectionResult)
                        } ?: {
                        didErrorOccurred = true
                        faceLandmarkerHelperListener?.onError(
                            "ResultBundle could not be returned" +
                                    " in detectVideoFile"
                        )
                    }
                }
                ?: run {
                    didErrorOccurred = true
                    faceLandmarkerHelperListener?.onError(
                        "Frame at specified time could not be" +
                                " retrieved when detecting in video."
                    )
                }
        }

        retriever.release()

        val inferenceTimePerFrameMs =
            (SystemClock.uptimeMillis() - startTime).div(numberOfFrameToRead)

        return if (didErrorOccurred) {
            null
        } else {
            VideoResultBundle(resultList, inferenceTimePerFrameMs, height, width)
        }
    }

    // Accepted a Bitmap and runs face landmarker inference on it to return
    // results back to the caller
    fun detectImage(image: Bitmap): ResultBundle? {
        if (runningMode != RunningMode.IMAGE) {
            throw IllegalArgumentException(
                "Attempting to call detectImage" +
                        " while not using RunningMode.IMAGE"
            )
        }


        // Inference time is the difference between the system time at the
        // start and finish of the process
        val startTime = SystemClock.uptimeMillis()

        // Convert the input Bitmap object to an MPImage object to run inference
        val mpImage = BitmapImageBuilder(image).build()

        // Run face landmarker using MediaPipe Face Landmarker API
        faceLandmarker?.detect(mpImage)?.also { landmarkResult ->
            val inferenceTimeMs = SystemClock.uptimeMillis() - startTime
            return ResultBundle(
                landmarkResult,
                inferenceTimeMs,
                image.height,
                image.width
            )
        }

        // If faceLandmarker?.detect() returns null, this is likely an error. Returning null
        // to indicate this.
        faceLandmarkerHelperListener?.onError(
            "Face Landmarker failed to detect."
        )
        return null
    }

    // LIVE_STREAM 결과 콜백으로 호출되는 내부 함수.
    private fun returnLivestreamResult(
        result: FaceLandmarkerResult, // 모델이 방금 산출한 얼굴 랜드마크 결과 묶음
        input: MPImage // 이 결과가 계산될 때 입력으로 쓰인 MPImage
    ) {
        if( result.faceLandmarks().size > 0 ) { //얼굴이 하나라도 검출 됐는지 확인
            val finishTimeMs = SystemClock.uptimeMillis() //지금 시각(ms) 추론이 끝나 콜백이 불린 순간
            val inferenceTime = finishTimeMs - result.timestampMs() //추론 지연시간 계산

            faceLandmarkerHelperListener?.onResults( //외부(호출자)에게 정상 결과 전달
                ResultBundle( // 데이터를 하나로 묶음
                    result, //원본 결과 객체(랜드마크, 블렌드셰입 등)
                    inferenceTime,
                    input.height,
                    input.width
                )
            )
        }
        else {
            faceLandmarkerHelperListener?.onEmpty()
        }
    }

    // LIVE_STREAM 추론 중 발생한 런타임 예외를 상위로 전달하는 에러 콜백
    // caller
    private fun returnLivestreamError(error: RuntimeException) {
        faceLandmarkerHelperListener?.onError(
            error.message ?: "An unknown error has occurred"
        )
    }


    data class ResultBundle( // 데이터 모음
        val result: FaceLandmarkerResult,
        val inferenceTime: Long,
        val inputImageHeight: Int,
        val inputImageWidth: Int,
    )

    data class VideoResultBundle(
        val results: List<FaceLandmarkerResult>,
        val inferenceTime: Long,
        val inputImageHeight: Int,
        val inputImageWidth: Int,
    )

    interface LandmarkerListener {
        fun onError(error: String, errorCode: Int = OTHER_ERROR)
        fun onResults(resultBundle: ResultBundle)

        fun onEmpty() {}
    }
}