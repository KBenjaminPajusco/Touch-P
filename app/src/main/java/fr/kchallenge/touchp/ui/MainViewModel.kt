package fr.kchallenge.touchp.ui

import android.app.Application
import android.graphics.Bitmap
import android.util.Log
import android.os.SystemClock
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import fr.kchallenge.touchp.camera.DebugMedia
import fr.kchallenge.touchp.figure.Figure
import fr.kchallenge.touchp.figure.FigureClassifier
import fr.kchallenge.touchp.gesture.Debouncer
import fr.kchallenge.touchp.gesture.GestureEngine
import fr.kchallenge.touchp.gesture.HandReading
import fr.kchallenge.touchp.tracking.HandFrame
import fr.kchallenge.touchp.tracking.BodyFrame
import fr.kchallenge.touchp.tracking.BodyTracker
import fr.kchallenge.touchp.tracking.HandDetector
import fr.kchallenge.touchp.tracking.HandTracker
import fr.kchallenge.touchp.tracking.ReplayDetector
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class Stats(val fps: Float = 0f, val inferenceMs: Float = 0f, val delegate: String = "")

enum class Mode(val label: String) { HANDS("Mains"), BODY("Corps") }

/** Image debug affichée à la place de la caméra. */
data class DebugShot(val bitmap: Bitmap, val label: String)

class MainViewModel(app: Application) : AndroidViewModel(app) {

    private var replay = false
    // Créés au premier usage : MediaPipe charge sa lib native à ce moment-là.
    private val handLazy = lazy { if (replay) ReplayDetector(getApplication()) else HandTracker(getApplication()) }
    private val tracker: HandDetector by handLazy
    private val bodyLazy = lazy { BodyTracker(getApplication()) }
    private val bodyTracker: BodyTracker by bodyLazy
    private val debugMedia = DebugMedia(app)

    private val _frame = MutableStateFlow(HandFrame())
    val frame: StateFlow<HandFrame> = _frame.asStateFlow()

    private val gestures = GestureEngine()
    private val _readings = MutableStateFlow<List<HandReading>>(emptyList())
    val readings: StateFlow<List<HandReading>> = _readings.asStateFlow()

    private val _mode = MutableStateFlow(Mode.HANDS)
    val mode: StateFlow<Mode> = _mode.asStateFlow()

    private val _body = MutableStateFlow(BodyFrame())
    val body: StateFlow<BodyFrame> = _body.asStateFlow()

    private val figureFilter = Debouncer(FIGURE_CONFIRM_FRAMES, Figure.NONE)
    private val _figure = MutableStateFlow(Figure.NONE)
    val figure: StateFlow<Figure> = _figure.asStateFlow()

    private val _stats = MutableStateFlow(Stats())
    val stats: StateFlow<Stats> = _stats.asStateFlow()

    private val _debug = MutableStateFlow(false)
    val debug: StateFlow<Boolean> = _debug.asStateFlow()

    private val _shot = MutableStateFlow<DebugShot?>(null)
    val shot: StateFlow<DebugShot?> = _shot.asStateFlow()

    private var debugJob: Job? = null
    private var lastFrameNs = 0L
    private var fpsEma = 0f
    private var inferEma = 0f
    private var lastPerfLogNs = 0L

    /** Appelé par la caméra sur son thread d'analyse (une image à la fois). */
    fun onCameraFrame(bitmap: Bitmap, rotationDegrees: Int) {
        if (_debug.value) return
        process(bitmap, rotationDegrees)
    }

    @Synchronized
    private fun process(bitmap: Bitmap, rotationDegrees: Int = 0) {
        val (inferenceMs, delegate) = when (_mode.value) {
            Mode.HANDS -> processHands(bitmap, rotationDegrees)
            Mode.BODY -> processBody(bitmap, rotationDegrees)
        }
        val now = SystemClock.elapsedRealtimeNanos()
        if (lastFrameNs != 0L) {
            val fps = 1e9f / (now - lastFrameNs)
            fpsEma = if (fpsEma == 0f) fps else fpsEma * 0.9f + fps * 0.1f
        }
        lastFrameNs = now
        inferEma = if (inferEma == 0f) inferenceMs else inferEma * 0.9f + inferenceMs * 0.1f
        _stats.value = Stats(fpsEma, inferEma, delegate)
        if (now - lastPerfLogNs > 2_000_000_000L) {
            lastPerfLogNs = now
            Log.i(TAG, "PERF mode=%s fps=%.1f inferMs=%.1f delegate=%s debug=%b".format(_mode.value, fpsEma, inferEma, delegate, _debug.value))
        }
    }

    private fun processHands(bitmap: Bitmap, rotationDegrees: Int): Pair<Float, String> {
        val result = tracker.detect(bitmap, rotationDegrees)
        _frame.value = result
        _readings.value = gestures.update(result)
        return result.inferenceMs to tracker.delegateName
    }

    private fun processBody(bitmap: Bitmap, rotationDegrees: Int): Pair<Float, String> {
        val result = bodyTracker.detect(bitmap, rotationDegrees)
        _body.value = result
        val raw = result.landmarks?.let { FigureClassifier.classify(it, result.imageWidth, result.imageHeight) } ?: Figure.NONE
        _figure.value = figureFilter.update(raw)
        return result.inferenceMs to bodyTracker.delegateName
    }

    @Synchronized
    fun setMode(mode: Mode) {
        if (_mode.value == mode) return
        _mode.value = mode
        resetResults()
    }

    /** Efface résultats et statistiques (changement de mode ou de source). */
    private fun resetResults() {
        _frame.value = HandFrame()
        clearGestures()
        clearBody()
        lastFrameNs = 0L
        fpsEma = 0f
        inferEma = 0f
    }

    private fun clearBody() {
        _body.value = BodyFrame()
        figureFilter.reset(Figure.NONE)
        _figure.value = Figure.NONE
    }

    private fun clearGestures() {
        gestures.reset()
        _readings.value = emptyList()
    }

    /** Doit être appelé avant tout usage du détecteur. */
    fun useReplay(enabled: Boolean) {
        replay = enabled
    }

    fun setDebug(enabled: Boolean) {
        if (_debug.value == enabled) return
        _debug.value = enabled
        debugJob?.cancel()
        resetResults()
        if (!enabled) {
            _shot.value = null
            return
        }
        debugJob = viewModelScope.launch(Dispatchers.Default) { runDebugLoop() }
    }

    private suspend fun runDebugLoop() {
        val items = debugMedia.items
        if (items.isEmpty()) return
        while (currentCoroutineContext().isActive) {
            for (item in items) {
                if (item.isVideo) playVideo(item) else showImage(item)
            }
        }
    }

    private suspend fun showImage(item: DebugMedia.Item) {
        val bmp = debugMedia.loadImage(item) ?: return
        synchronized(this) { // pas de suivi résiduel de l'image précédente
            when (_mode.value) {
                Mode.HANDS -> {
                    tracker.reset()
                    tracker.onScene(item.name)
                }
                Mode.BODY -> bodyTracker.reset()
            }
            clearGestures()
            clearBody()
        }
        _shot.value = DebugShot(bmp, item.name)
        val end = SystemClock.elapsedRealtime() + IMAGE_DURATION_MS
        // On ré-analyse en boucle pour que FPS / latence restent significatifs.
        while (SystemClock.elapsedRealtime() < end) {
            process(bmp)
            delay(30)
        }
        val gestureList = _readings.value.joinToString(",") { "${it.side.name}:${it.gesture.name}:${it.fingerCount}" }
        Log.i(TAG, "HANDS image=${item.name} n=${_frame.value.hands.size} inferMs=%.1f gestures=%s".format(inferEma, gestureList))
    }

    private suspend fun playVideo(item: DebugMedia.Item) {
        debugMedia.openVideo(item).use { video ->
            var pos = 0L
            while (pos < video.durationMs) {
                val t0 = SystemClock.elapsedRealtime()
                val bmp = video.frameAt(pos) ?: break
                _shot.value = DebugShot(bmp, item.name)
                process(bmp)
                pos += maxOf(33L, SystemClock.elapsedRealtime() - t0)
            }
        }
    }

    override fun onCleared() {
        debugJob?.cancel()
        if (handLazy.isInitialized()) tracker.close()
        if (bodyLazy.isInitialized()) bodyTracker.close()
    }

    private companion object {
        const val TAG = "TouchP"
        const val FIGURE_CONFIRM_FRAMES = 6
        const val IMAGE_DURATION_MS = 3000L
    }
}
