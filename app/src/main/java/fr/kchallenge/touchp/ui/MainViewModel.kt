package fr.kchallenge.touchp.ui

import android.app.Application
import android.graphics.Bitmap
import android.util.Log
import android.os.SystemClock
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import fr.kchallenge.touchp.camera.DebugMedia
import fr.kchallenge.touchp.gesture.GestureEngine
import fr.kchallenge.touchp.gesture.HandReading
import fr.kchallenge.touchp.tracking.HandFrame
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

/** Image debug affichée à la place de la caméra. */
data class DebugShot(val bitmap: Bitmap, val label: String)

class MainViewModel(app: Application) : AndroidViewModel(app) {

    private var replay = false
    // Créé au premier usage : MediaPipe charge sa lib native à ce moment-là.
    private val tracker: HandDetector by lazy { if (replay) ReplayDetector(getApplication()) else HandTracker(getApplication()) }
    private val debugMedia = DebugMedia(app)

    private val _frame = MutableStateFlow(HandFrame())
    val frame: StateFlow<HandFrame> = _frame.asStateFlow()

    private val gestures = GestureEngine()
    private val _readings = MutableStateFlow<List<HandReading>>(emptyList())
    val readings: StateFlow<List<HandReading>> = _readings.asStateFlow()

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
        val result = tracker.detect(bitmap, rotationDegrees)
        val now = SystemClock.elapsedRealtimeNanos()
        if (lastFrameNs != 0L) {
            val fps = 1e9f / (now - lastFrameNs)
            fpsEma = if (fpsEma == 0f) fps else fpsEma * 0.9f + fps * 0.1f
        }
        lastFrameNs = now
        inferEma = if (inferEma == 0f) result.inferenceMs else inferEma * 0.9f + result.inferenceMs * 0.1f
        _frame.value = result
        _readings.value = gestures.update(result)
        _stats.value = Stats(fpsEma, inferEma, tracker.delegateName)
        if (now - lastPerfLogNs > 2_000_000_000L) {
            lastPerfLogNs = now
            Log.i(TAG, "PERF fps=%.1f inferMs=%.1f delegate=%s debug=%b".format(fpsEma, inferEma, tracker.delegateName, _debug.value))
        }
    }

    /** Doit être appelé avant tout usage du détecteur. */
    fun useReplay(enabled: Boolean) {
        replay = enabled
    }

    fun setDebug(enabled: Boolean) {
        if (_debug.value == enabled) return
        _debug.value = enabled
        debugJob?.cancel()
        _frame.value = HandFrame()
        _readings.value = emptyList()
        gestures.reset()
        lastFrameNs = 0L
        fpsEma = 0f
        inferEma = 0f
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
            tracker.reset()
            tracker.onScene(item.name)
            gestures.reset()
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
        tracker.close()
    }

    private companion object {
        const val TAG = "TouchP"
        const val IMAGE_DURATION_MS = 3000L
    }
}
