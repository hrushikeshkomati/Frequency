package com.frequency.android.ui.media

import android.net.Uri
import android.widget.MediaController
import android.widget.VideoView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import java.io.File

@Composable
fun VideoMessageItem(path: String, modifier: Modifier = Modifier) {
    if (!File(path).exists()) return
    AndroidView(
        modifier = modifier.fillMaxWidth().aspectRatio(16f / 9f).clip(RoundedCornerShape(12.dp)),
        factory = { context ->
            VideoView(context).apply {
                val controller = MediaController(context)
                setMediaController(controller)
                controller.setAnchorView(this)
                setVideoURI(Uri.fromFile(File(path)))
            }
        },
        onRelease = { it.stopPlayback() }
    )
}
