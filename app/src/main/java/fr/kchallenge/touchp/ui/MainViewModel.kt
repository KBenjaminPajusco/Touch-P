package fr.kchallenge.touchp.ui

import android.app.Application
import android.graphics.Bitmap
import android.os.SystemClock
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import fr.kchallenge.touchp.camera.DebugMedia
import fr.kchallenge.touchp.tracking.HandFrame
import fr.kchallenge.touchp.tracking.HandTracker
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

    private val tracker = HandTracker(app)
    private val debugMedia = DebugMedia(app)

    private val _frame = MutableStateFlow(HandFrame())
    val frame: StateFlow<HandFrame> = _frame.asStateFlow()

    private val _stats = MutableStateFlow(Stats(delegate = tracker.delegateName))
    val stats: StateFlow<Stats> = _stats.asStateFlow()

    private val _debug = MutableStateFlow(false)
    val debug: StateFlow<Boolean> = _debug.asStateFlow()

    private val _shot = MutableStateFlow<DebugShot?>(null)
    val shot: StateFlow<DebugShot?> = _shot.asStateFlow()

    private var debugJob: Job? = null
    private var lastFrameNs = 0L
    private var fpsEma = 0f
    private var inferEma = 0f

    /** Appelé par la caméra sur son thread d'analyse (une image à la fois). */
    fun onCameraFrame(bitmap: Bitmap) {
        if (_debug.value) return
        process(bitmap)
    }

    @Synchronized
    private fun process(bitmap: Bitmap) {
        val result = tracker.detect(bitmap)
        val now = SystemClock.elapsedRealtimeNanos()
        if (lastFrameNs != 0L) {
            val fps = 1e9f / (now - lastFrameNs)
            fpsEma = if (fpsEma == 0f) fps else fpsEma * 0.9f + fps * 0.1f
        }
        lastFrameNs = now
        inferEma = if (inferEma == 0f) result.inferenceMs else inferEma * 0.9f + result.inferenceMs * 0.1f
        _frame.value = result
        _stats.value = Stats(fpsEma, inferEma, tracker.delegateName)
    }

    fun setDebug(enabled: Boolean) {
        if (_debug.value == enabled) return
        _debug.value = enabled
        debugJob?.cancel()
        _frame.value = HandFrame()
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
        _shot.value = DebugShot(bmp, item.name)
        val end = SystemClock.elapsedRealtime() + IMAGE_DURATION_MS
        // On ré-analyse en boucle pour que FPS / latence restent significatifs.
        while (SystemClock.elapsedRealtime() < end) {
            process(bmp)
            delay(30)
        }
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
        const val IMAGE_DURATION_MS = 3000L
    }
}
