package com.project.habithearth.ui.map

import android.content.Context
import android.content.pm.ActivityInfo
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.project.habithearth.HabitHearthApplication
import com.project.habithearth.model.canAfford
import com.project.habithearth.ui.components.LockScreenOrientation
import com.project.habithearth.ui.state.GameUiState
import com.project.habithearth.ui.state.resources
import com.project.habithearth.ui.tasks.TaskListViewModel
import com.project.habithearth.ui.tasks.TaskListViewModelFactory
import com.project.habithearth.ui.theme.HabitHearthTheme
import com.project.habithearth.ui.theme.HearthBackground
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

// configuaration: constants for the interactive map behavior
private val MapMinScale = 1f // minimum zoom (100%)
private val MapMaxScale = 4f // maximum zoom (400%)
private const val MapBackgroundAssetPath = "images/background_image.png" // memory safety: do not load images bigger than 2k px


private const val MapBackgroundMaxEdgePx = 2048
private const val MapBuildingMaxEdgePx = 384


private val BuildingMarkerWidth = 50.dp
private val BuildingMarkerHeight = 56.dp
private val LockedBadgeSize = 28.dp
private val LockedIconSize = 18.dp
private val PendingBadgeSize = 14.dp
private const val CottageBuildingId = "cottage"

private data class LoadedMapAssets(
    val background: ImageBitmap?,
    val markerByPath: Map<String, ImageBitmap?>,
)

@Composable
fun MapScreen(
    ownedBuildingIds: Set<String>, // list of IDs the player has already bought
    gameUiState: GameUiState, // current gold/gems counts
    onOpenBuilding: (VillageBuilding) -> Unit,
    onPurchaseBuilding: (String) -> Boolean,
    modifier: Modifier = Modifier,
    mapViewModel: MapViewModel = viewModel(),
) {
    // lock orientation: game maps are hard to navigate if the screen flips
    LockScreenOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT)

    // viewport state: manager where the camera is looking and how zoomed in it is
    val viewport by mapViewModel.viewportState.collectAsState()
    val density = LocalDensity.current

    // autofocus the camera on the main building when the screen first loads
    LaunchedEffect(Unit) {
        mapViewModel.seedInitialViewportIfDefault(
            MapViewModel.initialViewportForMainBuildings(density),
        )
    }

    // gesture handling: listen for multitouch (pinch and pan) and update the viewmodel
    val transformableState = rememberTransformableState { zoomChange, panChange, _ ->
        val nextScale = (viewport.scale * zoomChange).coerceIn(MapMinScale, MapMaxScale)
        val nextPan = viewport.pan + panChange
        mapViewModel.updateViewport(
            scale = nextScale,
            pan = nextPan,
        )
    }

    // task notifications: checks how many incomplete habits are in each building
    val app = LocalContext.current.applicationContext as HabitHearthApplication
    val taskListVm: TaskListViewModel = viewModel(
        factory = TaskListViewModelFactory(app.taskRepository),)
    val tasks by taskListVm.tasks.collectAsState()

    // creates a map (Building(D -> Number of pending tasks) to show red notification dots
    val pendingByBuilding = remember(tasks) {
        tasks
            .asSequence()
            .filter { !it.isCompleted }
            .map { task -> task.buildingId?.takeIf { it.isNotBlank() } ?: CottageBuildingId }
            .groupingBy { it }
            .eachCount()
    }

    val buildings = remember { defaultVillageBuildings() }
    val markerPaths = remember(buildings) {
        buildings.mapIndexed { index, building ->
            markerAssetPathForBuilding(building.id, index)
        }.distinct()
    }
    val loadedAssets by produceState<LoadedMapAssets?>(
        initialValue = null,
        key1 = markerPaths,
    ) {
        value = withContext(Dispatchers.IO) {
            val background = decodeMapBackgroundBitmap(
                context = app,
                assetPath = MapBackgroundAssetPath,
                maxEdgePx = MapBackgroundMaxEdgePx,
            )
            val markerByPath = markerPaths.associateWith { path ->
                decodeMapMarkerBitmap(
                    context = app,
                    assetPath = path,
                    maxEdgePx = MapBuildingMaxEdgePx,
                )
            }
            LoadedMapAssets(
                background = background,
                markerByPath = markerByPath,
            )
        }
    }

    // unlock dialog: logic for the do you want to buy this popup
    var pendingUnlock by remember { mutableStateOf<VillageBuilding?>(null) }

    pendingUnlock?.let { building ->
        val cost = building.unlockCost()
        val canAfford = gameUiState.resources.canAfford(cost)
        AlertDialog(
            onDismissRequest = { pendingUnlock = null },
            title = { Text(building.name) },
            // explains cost and if play can afford it
            text = {
                Text(
                    text = "Unlock this building for ${cost.displayLabel()}.\n\nYou currently " +
                        if (canAfford) "have enough to buy." else "don’t have enough.",
                    style = MaterialTheme.typography.bodyMedium,
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (onPurchaseBuilding(building.id)) {
                            pendingUnlock = null
                        }
                    },
                    enabled = canAfford,
                ) {
                    Text("Buy")
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingUnlock = null }) {
                    Text("Cancel")
                }
            },
        )
    }

    if (loadedAssets == null) {
        MapLoadingScreen(modifier = modifier)
        return
    }
    val mapAssets = loadedAssets ?: return

    // the map container
    Box(
        modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(0.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerLow),
    ) {
        Box(
            Modifier
                .fillMaxSize()
                // this layer applies the zoom and pan movements
                .graphicsLayer {
                    scaleX = viewport.scale
                    scaleY = viewport.scale
                    translationX = viewport.pan.x
                    translationY = viewport.pan.y
                }
                .transformable(transformableState),
        ) {
            // the actual map canvas
            BoxWithConstraints(
                Modifier
                    .size(1400.dp, 980.dp).align(Alignment.TopCenter),) {
                MapBaseBackground(
                    imageBitmap = mapAssets.background,
                    modifier = Modifier.fillMaxSize(),
                ) // the giant ground image
                buildings.forEachIndexed { index, building ->
                    val assetPath = markerAssetPathForBuilding(building.id, index)

                    BuildingMarker(
                        building = building,
                        markerBitmap = mapAssets.markerByPath[assetPath],
                        locked = building.id !in ownedBuildingIds,
                        pendingCount = pendingByBuilding[building.id] ?: 0,
                        modifier = Modifier.offset(
                            x = maxWidth * building.xFraction - BuildingMarkerWidth / 2,
                            y = maxHeight * building.yFraction - BuildingMarkerHeight / 2,
                        ),
                        onClick = {
                            if (building.id in ownedBuildingIds) {
                                onOpenBuilding(building)
                            } else {
                                pendingUnlock = building
                            }
                        },
                    )
                }
            }
        }

        Text(
            text = "Pinch to zoom · drag to pan · tap a building to explore or unlock",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(16.dp),
        )
    }
}

@Composable
private fun MapBaseBackground(
    imageBitmap: ImageBitmap?,
    modifier: Modifier = Modifier,
) {
    if (imageBitmap != null) {
        Image(
            bitmap = imageBitmap,
            contentDescription = null,
            modifier = modifier.fillMaxSize(),
            contentScale = ContentScale.Crop,
        )
    } else {
        Box(
            modifier
                .fillMaxSize()
                .background(HearthBackground),
        )
    }
}

private fun decodeMapBackgroundBitmap(
    context: Context,
    assetPath: String,
    maxEdgePx: Int,
): ImageBitmap? {
    return runCatching {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        context.assets.open(assetPath).use { BitmapFactory.decodeStream(it, null, bounds) }
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return@runCatching null

        val sample = sampleSizeForMaxEdge(bounds.outWidth, bounds.outHeight, maxEdgePx)
        val decode = BitmapFactory.Options().apply {
            inSampleSize = sample
            inPreferredConfig = Bitmap.Config.RGB_565
            inScaled = false
        }
        context.assets.open(assetPath).use { stream ->
            BitmapFactory.decodeStream(stream, null, decode)?.asImageBitmap()
        }
    }.getOrNull()
}


private fun decodeMapMarkerBitmap(
    context: Context,
    assetPath: String,
    maxEdgePx: Int,
): ImageBitmap? {
    return runCatching {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        context.assets.open(assetPath).use { BitmapFactory.decodeStream(it, null, bounds) }
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return@runCatching null

        val sample = sampleSizeForMaxEdge(bounds.outWidth, bounds.outHeight, maxEdgePx)
        val decode = BitmapFactory.Options().apply {
            inSampleSize = sample
            inScaled = false
        }
        context.assets.open(assetPath).use { stream ->
            BitmapFactory.decodeStream(stream, null, decode)?.asImageBitmap()
        }
    }.getOrNull()
}

private fun sampleSizeForMaxEdge(width: Int, height: Int, maxEdgePx: Int): Int {
    val longest = maxOf(width, height)
    if (longest <= maxEdgePx) return 1
    var sample = 1
    while (longest / sample > maxEdgePx) {
        sample *= 2
    }
    return sample
}


@Composable
private fun BuildingMarker(
    building: VillageBuilding,
    markerBitmap: ImageBitmap?,
    locked: Boolean,
    pendingCount: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .size(BuildingMarkerWidth, BuildingMarkerHeight)
            .clickable(onClick = onClick, onClickLabel = building.name),
        contentAlignment = Alignment.Center,
    ) {
        if (markerBitmap != null) {
            val colorFilter = if (locked) {
                ColorFilter.colorMatrix(ColorMatrix().apply { setToSaturation(0f) })
            } else {
                null
            }
            Image(
                bitmap = markerBitmap,
                contentDescription = building.name,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Fit,
                colorFilter = colorFilter,
            )
        }
        if (locked) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                Box(
                    modifier = Modifier
                        .size(LockedBadgeSize)
                        .background(Color.Black.copy(alpha = 0.5f), CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Filled.Lock,
                        contentDescription = "Locked",
                        modifier = Modifier.size(LockedIconSize),
                        tint = Color.White.copy(alpha = 0.95f),
                    )
                }
            }
        }
        if (pendingCount > 0) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .size(PendingBadgeSize)
                    .background(
                        color = MaterialTheme.colorScheme.errorContainer,
                        shape = CircleShape,
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = pendingCount.toString(),
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onErrorContainer,
                )
            }
        }
    }
}

@Composable
private fun MapLoadingScreen(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surfaceContainerLow),
        contentAlignment = Alignment.Center,
    ) {
        Box(contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
            Text(
                text = "Loading map...",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 72.dp),
            )
        }
    }
}


