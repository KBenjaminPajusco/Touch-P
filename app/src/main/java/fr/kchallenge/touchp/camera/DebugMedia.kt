package fr.kchallenge.touchp.camera

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever

/**
 * Médias de test embarqués dans `assets/debug/` (jpg/png/mp4/webm), qui remplacent la caméra.
 * Les images sont affichées quelques secondes chacune ; les vidéos sont lues image par image.
 */
class DebugMedia(private val context: Context) {

    data class Item(val name: String, val isVideo: Boolean)

    val items: List<Item> = (context.assets.list(DIR) ?: emptyArray())
        .sorted()
        .mapNotNull {
            when (it.substringAfterLast('.', "").lowercase()) {
                "jpg", "jpeg", "png", "webp" -> Item(it, isVideo = false)
                "mp4", "webm", "3gp" -> Item(it, isVideo = true)
                else -> null
            }
        }

    fun loadImage(item: Item): Bitmap? =
        context.assets.open("$DIR/${item.name}").use { BitmapFactory.decodeStream(it) }
            ?.copy(Bitmap.Config.ARGB_8888, false)

    /** Ouvre une vidéo des assets ; l'appelant doit appeler [VideoReader.close]. */
    fun openVideo(item: Item): VideoReader = VideoReader(context, "$DIR/${item.name}")

    class VideoReader(context: Context, assetPath: String) : AutoCloseable {
        private val retriever = MediaMetadataRetriever()
        val durationMs: Long

        init {
            context.assets.openFd(assetPath).use { retriever.setDataSource(it.fileDescriptor, it.startOffset, it.length) }
            durationMs = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull() ?: 0L
        }

        fun frameAt(positionMs: Long): Bitmap? =
            retriever.getFrameAtTime(positionMs * 1000, MediaMetadataRetriever.OPTION_CLOSEST)

        override fun close() = retriever.release()
    }

    companion object {
        const val DIR = "debug"
    }
}
