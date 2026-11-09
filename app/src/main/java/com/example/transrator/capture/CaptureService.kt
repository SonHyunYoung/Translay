package com.example.transrator.capture

import android.R
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.japanese.JapaneseTextRecognizerOptions

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.drop

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.RectF
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.ImageReader
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.IBinder
import android.util.DisplayMetrics
import android.util.Log
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.LinearLayout
import com.example.transrator.data.local.AppDatabase
import com.example.transrator.data.local.SettingsManager
import com.example.transrator.data.local.TogglePos
import com.example.transrator.data.local.TokenManager
import com.example.transrator.data.local.TranslationCacheEntity
import com.example.transrator.data.remote.RetrofitClient
import com.example.transrator.data.remote.TranslateRequest
import java.io.File
import java.io.FileOutputStream

class CaptureService : Service() {

    private var mediaProjection: MediaProjection? = null
    private lateinit var windowManager: WindowManager
    private var captureButton: View? = null

    private var imageReader: ImageReader? = null
    private var virtualDisplay: VirtualDisplay? = null

    private var screenWidth = 0
    private var screenHeight = 0
    private var screenDensity = 0

    @Volatile private var ocrPaused = false

    // 마지막 프레임의 이름 영역 크롭 (대사 확정 시 한 번만 OCR)
    // 최근 이름 영역 프레임(최신이 앞). 한 프레임에서 못 읽어도 다른 프레임에서 읽히도록 보관
    private val nameFrames = ArrayDeque<Bitmap>()
    private val nameLock = Any()

    private val db by lazy { AppDatabase.getInstance(applicationContext) }
    private val tokenManager by lazy { TokenManager(applicationContext) }

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var captureJob: Job? = null

    private val recognizer by lazy {
        TextRecognition.getClient(JapaneseTextRecognizerOptions.Builder().build())
    }

    private val detector = ChangeDetector(stableRequired = 2)

    @Volatile private var requestId = 0
    @Volatile private var generation = 0

    companion object {
        val DIALOG_REGION = RectF(0f, 0.54f, 1f, 0.62f)
        const val CAPTURE_INTERVAL_MS = 500L

        // 이름 OCR은 아래 모드를 순서대로 시도하고, 글자가 읽히는 첫 결과를 쓴다.
        //  "gray"        = 2배 확대 + 그레이스케일 + 대비 늘리기
        //  "white"       = 2배 확대 + min(R,G,B)(흰색 정도)만 사용, 검은 글씨로 반전. 색 있는 이름표에 유리
        //  "gray3"       = 3배 확대 + 그레이스케일 + 대비 + 여백 (작은 입력 보완)
        //  "none"        = 원본 그대로
        //  "binary_thinN" = 3배 확대 + 이진화 + N픽셀 깎기 (예: binary_thin3). 경험상 2~3회가 가장 잘 맞아 먼저 시도
        // 한자마다 맞는 전처리가 달라서(획이 많으면 깎기가 해롭고, 적당한 횟수도 글자마다 다름) 여러 개를 순서대로 시도
        // 이름으로 인정할 문자: 히라가나·가타카나·한자와 ー ・ 々 등. 영문/숫자/기호가 섞이면 오인식으로 보고 제외
        val NAME_VALID = Regex("^[\\p{IsHiragana}\\p{IsKatakana}\\p{IsHan}ー・゠々〆〇]+$")
        const val NAME_RETRY_DELAY_MS = 700L   // 모든 시도가 실패하면 이만큼 기다린 뒤 새 프레임으로 한 번 더
        const val NAME_FRAME_COUNT = 3   // 보관해 두고 차례로 읽어 볼 최근 이름 프레임 수
        // 빠른 모드: 모든 프레임에 시도 / 깎기 모드: 최신 프레임에만 마지막 수단으로 시도
        val NAME_FAST_MODES = listOf("white", "gray", "none")
        val NAME_THIN_MODES = listOf("binary_thin2", "binary_thin3", "binary_thin1", "binary_thin4")
        const val NAME_SCALE = 3        // binary 계열 확대 배율
        const val NAME_GRAY_SCALE = 2   // gray 확대 배율
        const val NAME_THIN_ITER = 1    // 모드 이름에 숫자가 없을 때(binary_thin)의 기본 깎기 횟수
        const val NAME_PAD = 24
    }

    override fun onCreate() {
        super.onCreate()
        startForegroundNotification()

        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager

        val metrics = DisplayMetrics()
        windowManager.defaultDisplay.getRealMetrics(metrics)
        screenDensity = metrics.densityDpi
        screenWidth = metrics.widthPixels
        screenHeight = metrics.heightPixels

        serviceScope.launch {
            CaptureResult.resumeSignal.drop(1).collectLatest {
                requestId++
                generation++
                detector.reset()
                clearNameFrames()
                ocrPaused = false
                CaptureResult.translating.value = false
                Log.d("CAPTURE", "유저 탭 감지 → 진행 중 결과 무효화, OCR 재개")
            }
        }
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        try {
            syncDisplaySize()
            relocateToggle()
        } catch (e: Exception) {
            Log.e("CAPTURE", "회전 처리 실패: $e")
        }
    }

    @Synchronized
    private fun syncDisplaySize() {
        val m = DisplayMetrics()
        windowManager.defaultDisplay.getRealMetrics(m)
        if (m.widthPixels == screenWidth && m.heightPixels == screenHeight) return

        screenWidth = m.widthPixels
        screenHeight = m.heightPixels
        try {
            val vd = virtualDisplay ?: return
            imageReader?.close()
            imageReader = ImageReader.newInstance(
                screenWidth, screenHeight, PixelFormat.RGBA_8888, 2
            )
            vd.resize(screenWidth, screenHeight, screenDensity)
            vd.surface = imageReader?.surface
            Log.d("CAPTURE", "화면 크기 변경 감지: ${screenWidth}x${screenHeight}")
        } catch (e: Exception) {
            Log.e("CAPTURE", "크기 동기화 실패: $e")
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val resultCode = intent?.getIntExtra("resultCode", 0) ?: 0
        val data = intent?.getParcelableExtra<Intent>("data")

        if (data != null) {
            val projectionManager =
                getSystemService(MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
            mediaProjection = projectionManager.getMediaProjection(resultCode, data)

            mediaProjection?.registerCallback(object : MediaProjection.Callback() {
                override fun onStop() {
                    Log.d("CAPTURE", "MediaProjection 중지됨")
                }
            }, null)

            setupVirtualDisplay()
            showCaptureButton()
        }
        return START_NOT_STICKY
    }

    private fun setupVirtualDisplay() {
        CaptureResult.running.value = true

        imageReader = ImageReader.newInstance(
            screenWidth, screenHeight, PixelFormat.RGBA_8888, 2
        )

        virtualDisplay = mediaProjection?.createVirtualDisplay(
            "ScreenCapture",
            screenWidth, screenHeight, screenDensity,
            DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
            imageReader?.surface,
            null, null
        )
    }

    private fun showCaptureButton() {
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
        }

        val toggleBtn = Button(this).apply {
            text = "시작"
            setOnClickListener {
                if (captureJob?.isActive == true) {
                    stopCaptureLoop()
                    text = "시작"
                } else {
                    startCaptureLoop()
                    text = "정지"
                }
            }
        }

        val stopBtn = Button(this).apply {
            text = "종료"
            setOnClickListener { stopSelf() }
        }

        layout.addView(toggleBtn)
        layout.addView(stopBtn)
        captureButton = layout

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        )
        applyTogglePosition(params)

        windowManager.addView(captureButton, params)
    }

    private fun applyTogglePosition(params: WindowManager.LayoutParams) {
        val layout = captureButton ?: return
        val pos = TogglePos.from(SettingsManager(this).load().togglePos)

        layout.measure(
            View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED),
            View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
        )
        val w = layout.measuredWidth
        val h = layout.measuredHeight
        val margin = (24 * resources.displayMetrics.density).toInt()

        params.gravity = Gravity.START or Gravity.TOP
        params.x = ((screenWidth * pos.x).toInt() - w / 2)
            .coerceIn(0, (screenWidth - w).coerceAtLeast(0))
        params.y = if (pos.name.startsWith("TOP")) margin
        else (screenHeight - h - margin).coerceAtLeast(0)
    }

    private fun relocateToggle() {
        val v = captureButton ?: return
        val params = v.layoutParams as? WindowManager.LayoutParams ?: return
        applyTogglePosition(params)
        windowManager.updateViewLayout(v, params)
    }

    private fun startCaptureLoop() {
        if (captureJob?.isActive == true) return
        Log.d("CAPTURE", "캡처 루프 시작")

        captureJob = serviceScope.launch {
            while (isActive) {
                captureAndProcess()
                delay(CAPTURE_INTERVAL_MS)
            }
        }
    }

    private fun stopCaptureLoop() {
        Log.d("CAPTURE", "캡처 루프 정지")
        captureJob?.cancel()
        captureJob = null
        ocrPaused = false
        clearNameFrames()
        CaptureResult.translating.value = false

        val profileId = tokenManager.profileId()
        serviceScope.launch(Dispatchers.IO) {
            db.translationCacheDao().clearByProfile(profileId)
            Log.d("CACHE", "캐시 정리됨 (세션 종료)")
        }
    }

    private fun captureAndProcess() {
        if (ocrPaused) return

        syncDisplaySize()
        val cropped = captureCropped() ?: return
        runOcr(cropped)
    }

    private fun captureCropped(): Bitmap? {
        val image = imageReader?.acquireLatestImage() ?: return null

        try {
            val w0 = image.width
            val h0 = image.height
            val plane = image.planes[0]
            val rowPadding = plane.rowStride - plane.pixelStride * w0

            val full = Bitmap.createBitmap(
                w0 + rowPadding / plane.pixelStride, h0, Bitmap.Config.ARGB_8888
            )
            full.copyPixelsFromBuffer(plane.buffer)

            val settings = SettingsManager(this).load()

            val region = settings.dialogRegion ?: DIALOG_REGION

            val x = (w0 * region.left).toInt().coerceIn(0, w0 - 1)
            val y = (h0 * region.top).toInt().coerceIn(0, h0 - 1)
            val w = (w0 * (region.right - region.left)).toInt().coerceIn(1, w0 - x)
            val h = (h0 * (region.bottom - region.top)).toInt().coerceIn(1, h0 - y)

            val cropped = Bitmap.createBitmap(full, x, y, w, h)

            settings.nameRegion?.let { r ->
                val nx = (w0 * r.left).toInt().coerceIn(0, w0 - 1)
                val ny = (h0 * r.top).toInt().coerceIn(0, h0 - 1)
                val nw = (w0 * (r.right - r.left)).toInt().coerceIn(1, w0 - nx)
                val nh = (h0 * (r.bottom - r.top)).toInt().coerceIn(1, h0 - ny)
                val nameBmp = Bitmap.createBitmap(full, nx, ny, nw, nh)
                synchronized(nameLock) {
                    nameFrames.addFirst(nameBmp)
                    while (nameFrames.size > NAME_FRAME_COUNT) nameFrames.removeLast().recycle()
                }
            }

            full.recycle()
            return cropped
        } finally {
            image.close()
        }
    }

    private fun runOcr(bitmap: Bitmap) {
        val myGeneration = generation
        val inputImage = InputImage.fromBitmap(bitmap, 0)

        recognizer.process(inputImage)
            .addOnSuccessListener { visionText ->
                if (myGeneration != generation) return@addOnSuccessListener

                val text = visionText.text
                Log.d("OCR", "인식: ${text.replace("\n", " / ")}")
                CaptureResult.ocrText = text

                val toTranslate = detector.onOcr(text)
                if (toTranslate != null) {
                    Log.d("DETECT", "완성 감지: $toTranslate")
                    ocrPaused = true
                    recognizeSpeaker { speaker ->
                        Log.d("SPEAKER", "이름: $speaker")
                        handleConfirmedText(toTranslate, speaker)
                    }
                }
            }
            .addOnFailureListener { e ->
                Log.e("OCR", "OCR 실패: $e")
            }
            .addOnCompleteListener {
                bitmap.recycle()
            }
    }

    /** 소수 쪽 색(글씨)을 검정, 배경을 흰색으로. 흰 글씨/검은 글씨 모두 대응 */
    private fun toDarkOnLight(src: Bitmap): Bitmap {
        val n = src.width * src.height
        val px = IntArray(n)
        src.getPixels(px, 0, src.width, 0, 0, src.width, src.height)

        val lum = IntArray(n) {
            val c = px[it]
            (0.299 * Color.red(c) + 0.587 * Color.green(c) + 0.114 * Color.blue(c)).toInt()
        }
        val mean = lum.average().toInt()
        val brightCount = lum.count { it > mean }
        val textIsBright = brightCount < n - brightCount

        val out = IntArray(n) {
            val isText = if (textIsBright) lum[it] > mean else lum[it] <= mean
            if (isText) Color.BLACK else Color.WHITE
        }
        val bmp = Bitmap.createBitmap(src.width, src.height, Bitmap.Config.ARGB_8888)
        bmp.setPixels(out, 0, src.width, 0, 0, src.width, src.height)
        return bmp
    }

    /** 글씨를 얇게: 검은 글씨/흰 배경 이미지에서 흰색을 3x3으로 팽창, iter번 반복 */
    private fun thinDarkText(src: Bitmap, iter: Int): Bitmap {
        val w = src.width
        val h = src.height
        var cur = IntArray(w * h)
        src.getPixels(cur, 0, w, 0, 0, w, h)

        repeat(iter) {
            val next = IntArray(w * h) { Color.BLACK }
            for (y in 0 until h) for (x in 0 until w) {
                var white = false
                loop@ for (dy in -1..1) for (dx in -1..1) {
                    val nx = x + dx
                    val ny = y + dy
                    if (nx < 0 || ny < 0 || nx >= w || ny >= h) continue
                    if (cur[ny * w + nx] == Color.WHITE) { white = true; break@loop }
                }
                if (white) next[y * w + x] = Color.WHITE
            }
            cur = next
        }
        val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        bmp.setPixels(cur, 0, w, 0, 0, w, h)
        return bmp
    }

    /** 그레이스케일 + 대비 늘리기 (흑백으로 자르지 않아 획이 뭉개지지 않음. 극성은 유지) */
    private fun grayContrast(src: Bitmap): Bitmap {
        val w = src.width
        val h = src.height
        val n = w * h
        val px = IntArray(n)
        src.getPixels(px, 0, w, 0, 0, w, h)

        val lum = IntArray(n) {
            val c = px[it]
            (0.299 * Color.red(c) + 0.587 * Color.green(c) + 0.114 * Color.blue(c)).toInt()
        }
        // 양끝 2%를 잘라낸 범위로 늘려서 극단값(잡티)에 휘둘리지 않게 함
        val sorted = lum.sortedArray()
        val lo = sorted[(n * 0.02).toInt().coerceIn(0, n - 1)]
        val hi = sorted[(n * 0.98).toInt().coerceIn(0, n - 1)]
        val range = (hi - lo).coerceAtLeast(1)

        val out = IntArray(n) {
            val v = ((lum[it] - lo) * 255 / range).coerceIn(0, 255)
            Color.rgb(v, v, v)
        }
        val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        bmp.setPixels(out, 0, w, 0, 0, w, h)
        return bmp
    }

    /**
     * "흰색 정도"만 남기기: min(R,G,B)가 높을수록 흰색.
     * 흰 글씨는 높게, 주황/검정 등 색 있는 이름표 배경은 낮게 떨어져서 밝기 변환보다 대비가 크다.
     * 결과는 검은 글씨/밝은 배경으로 뒤집어 반환한다.
     */
    private fun whitenessDarkOnLight(src: Bitmap): Bitmap {
        val w = src.width
        val h = src.height
        val n = w * h
        val px = IntArray(n)
        src.getPixels(px, 0, w, 0, 0, w, h)

        val m = IntArray(n) {
            val c = px[it]
            minOf(Color.red(c), Color.green(c), Color.blue(c))
        }
        val sorted = m.sortedArray()
        val lo = sorted[(n * 0.02).toInt().coerceIn(0, n - 1)]
        val hi = sorted[(n * 0.98).toInt().coerceIn(0, n - 1)]
        val range = (hi - lo).coerceAtLeast(1)

        val out = IntArray(n) {
            val v = ((m[it] - lo) * 255 / range).coerceIn(0, 255)
            val inv = 255 - v            // 흰 글씨 → 검정, 배경 → 밝게
            Color.rgb(inv, inv, inv)
        }
        val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        bmp.setPixels(out, 0, w, 0, 0, w, h)
        return bmp
    }

    /** 주변을 배경색으로 둘러싸 여백을 붙인다 (글씨가 가장자리에 붙으면 인식이 떨어짐) */
    private fun padWithBorderColor(src: Bitmap, pad: Int): Bitmap {
        val out = Bitmap.createBitmap(
            src.width + pad * 2, src.height + pad * 2, Bitmap.Config.ARGB_8888
        )
        Canvas(out).apply {
            drawColor(src.getPixel(0, 0))   // 좌상단 픽셀색 = 배경색으로 가정
            drawBitmap(src, pad.toFloat(), pad.toFloat(), null)
        }
        return out
    }

    /** 모드별 전처리. 실패하면 null */
    private fun preprocessName(mode: String, src: Bitmap): Bitmap? = try {
        when (mode) {
            "gray" -> {
                val scaled = Bitmap.createScaledBitmap(
                    src, src.width * NAME_GRAY_SCALE, src.height * NAME_GRAY_SCALE, true
                )
                val g = grayContrast(scaled)
                scaled.recycle()
                g
            }
            "white" -> {
                val scaled = Bitmap.createScaledBitmap(
                    src, src.width * NAME_GRAY_SCALE, src.height * NAME_GRAY_SCALE, true
                )
                val g = whitenessDarkOnLight(scaled)
                scaled.recycle()
                g
            }
            "gray3" -> {
                val scaled = Bitmap.createScaledBitmap(
                    src, src.width * NAME_SCALE, src.height * NAME_SCALE, true
                )
                val g = grayContrast(scaled)
                scaled.recycle()
                val padded = padWithBorderColor(g, NAME_PAD)
                g.recycle()
                padded
            }
            else -> if (mode.startsWith("binary_thin")) {
                val iter = mode.removePrefix("binary_thin").toIntOrNull() ?: NAME_THIN_ITER
                val scaled = Bitmap.createScaledBitmap(
                    src, src.width * NAME_SCALE, src.height * NAME_SCALE, true
                )
                val padded = Bitmap.createBitmap(
                    scaled.width + NAME_PAD * 2, scaled.height + NAME_PAD * 2,
                    Bitmap.Config.ARGB_8888
                )
                Canvas(padded).apply {
                    drawColor(Color.WHITE)
                    drawBitmap(scaled, NAME_PAD.toFloat(), NAME_PAD.toFloat(), null)
                }
                scaled.recycle()
                val binary = toDarkOnLight(padded)
                padded.recycle()
                val thin = thinDarkText(binary, iter)
                binary.recycle()
                thin
            } else {
                src.copy(Bitmap.Config.ARGB_8888, false)   // "none"
            }
        }
    } catch (e: Exception) {
        Log.e("SPEAKER", "전처리 실패($mode): $e")
        null
    }

    /**
     * 이름 영역을 OCR (영역 미지정/전부 실패 시 null).
     * 최근 프레임을 최신부터 차례로 빠른 모드(FAST)로 읽어 보고,
     * 그래도 못 읽으면 최신 프레임에 깎기(thin) 모드를 시도한다.
     * 글자가 읽히는 첫 결과를 사용한다.
     */
    private fun recognizeSpeaker(done: (String?) -> Unit) {
        val frames = synchronized(nameLock) {
            nameFrames.mapNotNull { f ->
                if (f.isRecycled) null
                else try { f.copy(Bitmap.Config.ARGB_8888, false) } catch (e: Exception) { null }
            }
        }
        if (frames.isEmpty()) {
            Log.d("SPEAKER", "이름 영역 없음 (미지정이거나 아직 캡처 전)")
            done(null); return
        }

        // 디버그: 전처리 전 원본 프레임 저장 (확인 후 이 블록을 지우세요)
        frames.forEachIndexed { i, f ->
            try {
                FileOutputStream(File(getExternalFilesDir(null), "debug_name_raw_f$i.png")).use {
                    f.compress(Bitmap.CompressFormat.PNG, 100, it)
                }
            } catch (e: Exception) {
                Log.e("SPEAKER", "원본 저장 실패: $e")
            }
        }
        Log.d("SPEAKER", "이름 프레임 ${frames.size}장 저장 (debug_name_raw_f0~)")

        // 시도 계획: (프레임 번호, 모드)
        val plan = ArrayList<Pair<Int, String>>()
        for (f in frames.indices) for (m in NAME_FAST_MODES) plan.add(f to m)
        for (m in NAME_THIN_MODES) plan.add(0 to m)

        tryNamePlan(frames, plan, 0) { result ->
            frames.forEach { it.recycle() }
            if (result != null) {
                done(result)
                return@tryNamePlan
            }
            // 전부 실패: 이름표가 아직 자리를 못 잡았을 수 있으니 잠시 기다린 뒤 새 프레임으로 재시도
            serviceScope.launch {
                delay(NAME_RETRY_DELAY_MS)
                val fresh = captureNameNow()
                if (fresh == null) {
                    Log.d("SPEAKER", "재시도용 새 프레임 없음 → null")
                    done(null)
                    return@launch
                }
                Log.d("SPEAKER", "${NAME_RETRY_DELAY_MS}ms 후 새 프레임으로 재시도")
                val retryPlan = (NAME_FAST_MODES + NAME_THIN_MODES).map { 0 to it }
                tryNamePlan(listOf(fresh), retryPlan, 0) { r ->
                    fresh.recycle()
                    done(r)
                }
            }
        }
    }

    private fun tryNamePlan(
        frames: List<Bitmap>,
        plan: List<Pair<Int, String>>,
        index: Int,
        done: (String?) -> Unit
    ) {
        if (index >= plan.size) {
            Log.d("SPEAKER", "모든 프레임/모드에서 이름을 못 읽음 → null")
            done(null); return
        }
        val (frameIdx, mode) = plan[index]
        val input = preprocessName(mode, frames[frameIdx])
        if (input == null) { tryNamePlan(frames, plan, index + 1, done); return }

        // 디버그: 모드별 입력 이미지 (확인 후 이 블록을 지우세요)
        try {
            FileOutputStream(File(getExternalFilesDir(null), "debug_name_$mode.png")).use {
                input.compress(Bitmap.CompressFormat.PNG, 100, it)
            }
        } catch (e: Exception) {
            Log.e("SPEAKER", "디버그 저장 실패: $e")
        }

        recognizer.process(InputImage.fromBitmap(input, 0))
            .addOnSuccessListener { r ->
                val lines = r.textBlocks.flatMap { it.lines }
                // 줄이 여럿이면(후리가나 + 이름) 글씨가 가장 큰 줄을 이름으로 사용
                val best = lines.maxByOrNull { it.boundingBox?.height() ?: 0 }
                val raw = best?.text ?: r.text
                // 공백 제거 + 하이픈/대시류는 ー로 통일 (一 이 -, – 등으로 읽히는 경우가 많음)
                val sent = raw.replace(Regex("\\s+"), "")
                    .replace(Regex("[-‐‑‒–—―−─－ｰ]"), "ー")
                    .ifBlank { null }
                Log.d(
                    "SPEAKER",
                    "프레임=$frameIdx 모드=$mode 입력=${input.width}x${input.height} / " +
                            "OCR 전체='${r.text.replace("\n", " / ")}' / " +
                            "줄 ${lines.size}개 [${lines.joinToString(", ") { it.text }}] → 전송='$sent'"
                )
                if (sent != null && !NAME_VALID.matches(sent)) {
                    Log.d("SPEAKER", "형식 불일치로 제외: '$sent' → 다음 시도")
                    tryNamePlan(frames, plan, index + 1, done)
                } else if (sent != null) {
                    done(sent)
                } else {
                    tryNamePlan(frames, plan, index + 1, done)
                }
            }
            .addOnFailureListener { e ->
                Log.e("SPEAKER", "이름 OCR 실패($mode): $e")
                tryNamePlan(frames, plan, index + 1, done)
            }
            .addOnCompleteListener { input.recycle() }
    }

    /** 지금 화면에서 이름 영역만 새로 잘라 온다 (새 프레임이 없거나 영역 미지정이면 null) */
    private fun captureNameNow(): Bitmap? {
        val region = SettingsManager(this).load().nameRegion ?: return null
        val image = imageReader?.acquireLatestImage() ?: return null
        try {
            val w0 = image.width
            val h0 = image.height
            val plane = image.planes[0]
            val rowPadding = plane.rowStride - plane.pixelStride * w0
            val full = Bitmap.createBitmap(
                w0 + rowPadding / plane.pixelStride, h0, Bitmap.Config.ARGB_8888
            )
            full.copyPixelsFromBuffer(plane.buffer)

            val nx = (w0 * region.left).toInt().coerceIn(0, w0 - 1)
            val ny = (h0 * region.top).toInt().coerceIn(0, h0 - 1)
            val nw = (w0 * (region.right - region.left)).toInt().coerceIn(1, w0 - nx)
            val nh = (h0 * (region.bottom - region.top)).toInt().coerceIn(1, h0 - ny)
            val out = Bitmap.createBitmap(full, nx, ny, nw, nh)
            full.recycle()
            return out
        } catch (e: Exception) {
            Log.e("SPEAKER", "새 이름 프레임 캡처 실패: $e")
            return null
        } finally {
            image.close()
        }
    }

    /** 이전 대사의 이름 프레임이 다음 대사에 섞이지 않도록 비운다 */
    private fun clearNameFrames() {
        synchronized(nameLock) {
            nameFrames.forEach { it.recycle() }
            nameFrames.clear()
        }
    }

    private fun handleConfirmedText(text: String, speaker: String?) {
        val profileId = tokenManager.profileId()
        if (profileId == -1) {
            Log.e("CACHE", "선택된 프로필이 없습니다.")
            clearNameFrames()
            ocrPaused = false
            return
        }
        val key = ChangeDetector.normalize(text) + "#" + (speaker ?: "")

        serviceScope.launch(Dispatchers.IO) {
            val cached = db.translationCacheDao().findBySourceText(profileId, key)

            if (cached != null) {
                Log.d("CACHE", "캐시 히트 → 즉시 표시: ${cached.translatedText}")
                CaptureResult.translatedText.value = cached.translatedText
            } else {
                Log.d("CACHE", "캐시 미스 → 서버 요청")
                requestTranslate(text, profileId, key, speaker)
            }
        }
    }

    private fun requestTranslate(text: String, profileId: Int, cacheKey: String, speaker: String?) {
        val token = "Bearer ${tokenManager.accessToken()}"
        val myRequestId = ++requestId
        CaptureResult.translating.value = true

        serviceScope.launch(Dispatchers.IO) {
            try {
                val response = RetrofitClient.api.translate(
                    token = token,
                    request = TranslateRequest(
                        profileId = profileId,
                        text = text,
                        speaker = speaker
                    )
                )

                val translated = response.translatedText ?: ""
                val name = response.speakerName
                val shown = if (name.isNullOrBlank()) translated else "【$name】\n$translated"

                if (translated.isNotBlank()) {
                    db.translationCacheDao().insert(
                        TranslationCacheEntity(
                            profileId = profileId,
                            sourceText = cacheKey,
                            translatedText = shown
                        )
                    )
                }

                if (myRequestId != requestId) {
                    Log.d("TRANSLATE", "오래된 요청 결과 표시 생략: $shown")
                    return@launch
                }

                Log.d("TRANSLATE", "번역 결과: $shown")
                CaptureResult.translatedText.value = shown

            } catch (e: Exception) {
                Log.e("TRANSLATE", "번역 실패: $e")
                if (myRequestId == requestId) {
                    clearNameFrames()
                    ocrPaused = false
                }
            } finally {
                // 최신 요청일 때만 "번역 중" 해제 (이전 요청이 늦게 끝나도 새 요청 표시 유지)
                if (myRequestId == requestId) CaptureResult.translating.value = false
            }
        }
    }

    private fun startForegroundNotification() {
        val channelId = "capture_channel"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId, "화면 캡처",
                NotificationManager.IMPORTANCE_LOW
            )
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }

        val notification: Notification = Notification.Builder(this, channelId)
            .setContentTitle("화면 캡처 중")
            .setContentText("게임 화면을 캡처하고 있습니다")
            .setSmallIcon(R.drawable.ic_menu_camera)
            .build()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                1, notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION
            )
        } else {
            startForeground(1, notification)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
        captureButton?.let { windowManager.removeView(it) }
        clearNameFrames()
        virtualDisplay?.release()
        imageReader?.close()
        mediaProjection?.stop()
        recognizer.close()
        CaptureResult.running.value = false
        CaptureResult.translating.value = false
    }

    override fun onBind(intent: Intent?): IBinder? = null
}

object CaptureResult {
    var bitmap: Bitmap? = null
    var ocrText: String? = null

    val running = MutableStateFlow(false)
    val translatedText = MutableStateFlow("")

    val resumeSignal = MutableStateFlow(0)

    // 서버에 번역 요청 중인지 (캐시 히트에서는 켜지 않음)
    val translating = MutableStateFlow(false)

    val pickRegionSignal = MutableStateFlow(0)
}