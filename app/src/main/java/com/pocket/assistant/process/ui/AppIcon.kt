package com.pocket.assistant.process.ui

import android.graphics.drawable.Drawable
import android.widget.ImageView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView

/** 用 AndroidView 承载应用图标 Drawable，避免手动转 Bitmap。 */
@Composable
fun AppIcon(drawable: Drawable?, modifier: Modifier = Modifier) {
    if (drawable == null) {
        Box(
            modifier
                .clip(RoundedCornerShape(8.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant),
        )
        return
    }
    AndroidView(
        modifier = modifier,
        factory = { ctx -> ImageView(ctx).apply { setImageDrawable(drawable) } },
    )
}
