package com.example.muse.ui.screen.edit.component.image

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Log
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
    onImageClick: (Int) -> Unit = {},
    isDeleting: Boolean = false,
    onDeleteImage: (Int) -> Unit = {},
    onDeleteModule: () -> Unit = {},
) {
    if (imageUris.isEmpty()) return

    Column(modifier = modifier) {
        ImageGrid(
            imageUris = imageUris,
            onImageClick = onImageClick,
            isDeleting = isDeleting,
            onDeleteImage = onDeleteImage,
            modifier = Modifier.fillMaxWidth(),
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(22.dp),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(
                onClick = onDeleteModule,
                modifier = Modifier.size(22.dp),
            ) {
                Icon(
                    imageVector = Icons.Filled.Delete,
                    contentDescription = "删除",
                    tint = Color(0xFFB3B3B3),
                    modifier = Modifier.size(18.dp),
                )
            }
        }
    }
}

@Composable
private fun ImageGrid(
    imageUris: List<Uri>,
    onImageClick: (Int) -> Unit,
    modifier: Modifier = Modifier,
    isDeleting: Boolean = false,
    onDeleteImage: (Int) -> Unit = {},
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
                        Box(
                            modifier = Modifier
                                .width(itemWidth)
                                .aspectRatio(1f)
                        ) {
                            ImageThumbnail(
                                uri = uri,
                                modifier = Modifier.fillMaxSize(),
                                onClick = { onImageClick(index) },
                            )

                            if (isDeleting) {
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .padding(2.dp)
                                        .size(22.dp)
                                        .clip(CircleShape)
                                        .background(Color.Red)
                                        .clickable { onDeleteImage(index) },
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Close,
                                        contentDescription = "删除",
                                        tint = Color.White,
                                        modifier = Modifier.size(14.dp),
                                    )
                                }
                            }
                        }
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
fun ImageLightbox(
    imageUris: List<Uri>,
    initialIndex: Int,
    onDismiss: () -> Unit,
) {
    val pagerState = rememberPagerState(
        initialPage = initialIndex,
        pageCount = { imageUris.size },
    )

    Box(modifier = Modifier.fillMaxSize()) {
        // 半透明遮罩 — 点外部关闭
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.4f))
                .clickable(onClick = onDismiss)
        )

        // 浮动卡片 — 径向菜单上方
        Card(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(5.dp)
                .fillMaxWidth()
                .fillMaxHeight(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF2A2A2A)),
        ) {
            Box(Modifier.fillMaxSize()) {
                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier.fillMaxSize(),
                    beyondViewportPageCount = 1,
                ) { page ->
                    val uri = imageUris[page]
                    val bitmap by rememberBitmapFromUri(uri, targetSizePx = 800)

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
                        .padding(4.dp),
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
                    fontSize = 13.sp,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(8.dp),
                )
            }
        }
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
        val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
            ?: return null.also { Log.w("ImageModule", "openInputStream returned null: $uri") }

        val boundsOpts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, boundsOpts)

        if (boundsOpts.outWidth <= 0 || boundsOpts.outHeight <= 0) {
            Log.w("ImageModule", "Invalid bounds for $uri: ${boundsOpts.outWidth}x${boundsOpts.outHeight}")
            return null
        }

        val sampleSize = calcSampleSize(boundsOpts.outWidth, boundsOpts.outHeight, targetSizePx)
        val opts = BitmapFactory.Options().apply { inSampleSize = sampleSize }
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, opts)
    } catch (e: Exception) {
        Log.e("ImageModule", "Failed to load bitmap from $uri", e)
        null
    }
}

private fun calcSampleSize(width: Int, height: Int, targetMax: Int): Int {
    var sampleSize = 1
    val maxDim = maxOf(width, height)
    while (maxDim / (sampleSize * 2) >= targetMax) {
        sampleSize *= 2
    }
    return sampleSize
}
