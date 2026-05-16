package com.example.muse.ui.screen.edit.component.image

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.muse.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * 图片模块：在网格中展示多张图片（每行3张），单击放大，左右滑动切换。
 */
@Composable
fun ImageModule(
    imageUris: List<Uri>,
    modifier: Modifier = Modifier,
) {
    if (imageUris.isEmpty()) return

    var lightboxIndex by remember { mutableIntStateOf(-1) }

    ImageGrid(
        imageUris = imageUris,
        onImageClick = { lightboxIndex = it },
        modifier = modifier,
    )

    if (lightboxIndex >= 0) {
        ImageLightbox(
            imageUris = imageUris,
            initialIndex = lightboxIndex,
            onDismiss = { lightboxIndex = -1 },
        )
    }
}

@Composable
private fun ImageGrid(
    imageUris: List<Uri>,
    onImageClick: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    BoxWithConstraints(modifier = modifier.fillMaxWidth()) {
        val spacing = 4.dp
        val itemWidth = (maxWidth - spacing * 2) / 3f

        Column(verticalArrangement = Arrangement.spacedBy(spacing)) {
            imageUris.chunked(3).forEachIndexed { rowIdx, rowUris ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(spacing),
                ) {
                    rowUris.forEachIndexed { colIdx, uri ->
                        val index = rowIdx * 3 + colIdx
                        ImageThumbnail(
                            uri = uri,
                            modifier = Modifier
                                .width(itemWidth)
                                .aspectRatio(1f),
                            onClick = { onImageClick(index) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ImageThumbnail(
    uri: Uri,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val bitmap by rememberBitmapFromUri(uri, targetSizePx = 300)

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0xFF333333))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        if (bitmap != null) {
            Image(
                bitmap = bitmap!!.asImageBitmap(),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        } else {
            Icon(
                painter = painterResource(R.drawable.ic_image),
                contentDescription = null,
                tint = Color(0xFF666666),
                modifier = Modifier.size(24.dp),
            )
        }
    }
}

@Composable
private fun ImageLightbox(
    imageUris: List<Uri>,
    initialIndex: Int,
    onDismiss: () -> Unit,
) {
    val pagerState = rememberPagerState(
        initialPage = initialIndex,
        pageCount = { imageUris.size },
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.95f))
    ) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize(),
            beyondViewportPageCount = 1,
        ) { page ->
            val uri = imageUris[page]
            val bitmap by rememberBitmapFromUri(uri, targetSizePx = 1200)

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clickable(onClick = onDismiss),
                contentAlignment = Alignment.Center,
            ) {
                if (bitmap != null) {
                    Image(
                        bitmap = bitmap!!.asImageBitmap(),
                        contentDescription = null,
                        contentScale = ContentScale.Fit,
                        modifier = Modifier.fillMaxSize(),
                    )
                } else {
                    Text(
                        "加载中...",
                        color = Color.White.copy(alpha = 0.5f),
                        fontSize = 14.sp,
                    )
                }
            }
        }

        IconButton(
            onClick = onDismiss,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(16.dp),
        ) {
            Icon(
                imageVector = Icons.Filled.Close,
                contentDescription = "关闭",
                tint = Color.White,
            )
        }

        Text(
            text = "${pagerState.currentPage + 1} / ${imageUris.size}",
            color = Color.White.copy(alpha = 0.8f),
            fontSize = 14.sp,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(16.dp),
        )
    }
}

@Composable
fun rememberBitmapFromUri(uri: Uri, targetSizePx: Int = 400): State<Bitmap?> {
    val context = LocalContext.current
    val bitmapState = remember(uri) { mutableStateOf<Bitmap?>(null) }

    LaunchedEffect(uri) {
        bitmapState.value = withContext(Dispatchers.IO) {
            loadBitmap(context, uri, targetSizePx)
        }
    }

    return bitmapState
}

private fun loadBitmap(context: Context, uri: Uri, targetSizePx: Int): Bitmap? {
    return try {
        val resolver = context.contentResolver

        val boundsOpts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        resolver.openInputStream(uri)?.use { input ->
            BitmapFactory.decodeStream(input, null, boundsOpts)
        } ?: return null

        val sampleSize = calcSampleSize(boundsOpts.outWidth, boundsOpts.outHeight, targetSizePx)

        val decodeOpts = BitmapFactory.Options().apply { inSampleSize = sampleSize }
        resolver.openInputStream(uri)?.use { input ->
            BitmapFactory.decodeStream(input, null, decodeOpts)
        }
    } catch (_: Exception) {
        null
    }
}

private fun calcSampleSize(width: Int, height: Int, targetMax: Int): Int {
    var sampleSize = 1
    while (width / (sampleSize * 2) > targetMax && height / (sampleSize * 2) > targetMax) {
        sampleSize *= 2
    }
    return sampleSize
}
