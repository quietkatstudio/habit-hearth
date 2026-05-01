package com.project.habithearth.ui.map

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.compose.foundation.background
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.project.habithearth.HabitHearthApplication
import com.project.habithearth.model.canAfford
import com.project.habithearth.ui.components.HabitTaskRowCard
import com.project.habithearth.ui.components.VerticalScrollIndicator
import com.project.habithearth.ui.state.GameStateViewModel
import com.project.habithearth.ui.state.resources
import com.project.habithearth.ui.tasks.TaskListViewModel
import com.project.habithearth.ui.tasks.TaskListViewModelFactory


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BuildingDetailScreen(
    buildingId: String, // unique ID of building to display
    onBack: () -> Unit, // navigation callback for the back button
    onAddHabitInBuilding: (String) -> Unit, // navigation to habit creation screen
    onEditTask: (String) -> Unit, // navigation to habit editor
    gameStateViewModel: GameStateViewModel, // global state for gold/gems/unlocked buildings
    modifier: Modifier = Modifier,
) {
    val app = LocalContext.current.applicationContext as HabitHearthApplication

    // initializing the task viewmodel using a custom factory to inject the database repository
    val taskListVm: TaskListViewModel = viewModel(
        factory = TaskListViewModelFactory(app.taskRepository),
    )

    // state: get the habits linked specifically to this building
    val tasksFlow = remember(buildingId) { taskListVm.tasksForBuilding(buildingId) }
    val tasksInBuilding by tasksFlow.collectAsState(initial = emptyList())

    // state: check if the player actually owns this building
    val game by gameStateViewModel.uiState.collectAsState()
    val building = villageBuildingById(buildingId)  // helper to get static building metadata
    val owned = buildingId in game.ownedBuildingIds
    val scrollState = rememberScrollState()

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = building?.name ?: "Building",
                        maxLines = 1,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                        )
                    }
                },
            )
        },
    ) { innerPadding ->
        // error handling: if the ID provided does not exist in our data
        if (building == null) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(24.dp),
                verticalArrangement = Arrangement.Center,
            ) {
                Text(
                    text = "This building isn’t on the map anymore.",
                    style = MaterialTheme.typography.bodyLarge,
                )
                Spacer(modifier = Modifier.height(16.dp))
                Button(onClick = onBack) {
                    Text("Go back")
                }
            }
            return@Scaffold
        }

        Column(
            modifier = Modifier
                .fillMaxSize()

                .padding(innerPadding)
                .padding(horizontal = 16.dp),
        ) {
            val context = LocalContext.current

            // asset loading: fetch the building image from the andriod assets folder
            val headerAssetPath = remember(buildingId) {
                markerAssetPathForBuilding(buildingId, 0)
            }
            val headerBitmap: ImageBitmap? = remember(headerAssetPath) {
                decodeBuildingMarkerBitmap(
                    context = context,
                    assetPath = headerAssetPath,
                    maxEdgePx = 256,
                )
            }

            if (headerBitmap != null) {
                // visual feedback: if the building is locked,
                // applies a grayscale filter
                val lockedFilter = if (!owned) {
                    ColorFilter.colorMatrix(ColorMatrix().apply { setToSaturation(0f) })
                } else {
                    null
                }
                Image(
                    bitmap = headerBitmap,
                    contentDescription = "${building.name} header",
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(130.dp),
                    contentScale = ContentScale.Fit,
                    colorFilter = lockedFilter,
                )
            } else {
                Spacer(modifier = Modifier.height(130.dp))
            }
            Spacer(modifier = Modifier.height(8.dp))

            // ui logic: toggle between shop view and habit list view
            if (!owned) {
                // locked view
                val cost = building.unlockCost()
                val canAfford = game.resources.canAfford(cost)
                Text(
                    text = "This building is locked. Purchase it for ${cost.displayLabel()} to open the full story and habits here.",
                    style = MaterialTheme.typography.bodyLarge,
                )
                Spacer(modifier = Modifier.height(12.dp))
                Button(
                    onClick = { gameStateViewModel.tryPurchaseBuilding(buildingId) },
                    enabled = canAfford,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("Buy (${cost.displayLabel()})")
                }
                Spacer(modifier = Modifier.weight(1f))
            } else {
                // unlocked view
                Text(
                    text = building.story,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))
                Text(
                    text = "Habits filed here",
                    style = MaterialTheme.typography.titleMedium,
                )
                Spacer(modifier = Modifier.height(8.dp))

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(end = 14.dp)
                            .verticalScroll(scrollState),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        if (tasksInBuilding.isEmpty()) {
                            Text(
                                text = "No habits in this building yet. Add one to file it here.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        } else {
                            tasksInBuilding.forEach { task ->
                                HabitTaskRowCard(
                                    task = task,
                                    onCompletedChange = { checked ->
                                        // update the habit database and trigger game rewards (coins/gems)
                                        taskListVm.setCompleted(task.id, checked) { before, delta ->
                                            gameStateViewModel.applyRewardDelta(
                                                category = before.category,
                                                delta = delta,
                                                buildingId = before.buildingId,
                                            )
                                        }
                                    },
                                    onOpenEdit = { onEditTask(task.id) },
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                    }

                    // custom scrollbar component to help users navigate the habit list
                    VerticalScrollIndicator(
                        scrollState = scrollState,
                        modifier = Modifier
                            .align(Alignment.CenterEnd)
                            .fillMaxHeight()
                            .padding(vertical = 4.dp),
                    )
                }

                Button(
                    onClick = { onAddHabitInBuilding(buildingId) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp, bottom = 8.dp),
                ) {
                    Text("Add habit to this building")
                }
            }
        }
    }
}

// optimized image loading:
//      reads an image from assets and scales it down to maxEdgePx to prevent
//      outOfMemory errors on high resolution graphics
private fun decodeBuildingMarkerBitmap(
    context: Context,
    assetPath: String,
    maxEdgePx: Int,
): ImageBitmap? {
    return runCatching {
        // step 1: read the image dimensions (fast, low memory)
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        context.assets.open(assetPath).use { BitmapFactory.decodeStream(it, null, bounds) }
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return@runCatching null

        // step 2: calculate how much we need to shrink it
        val sample = sampleSizeForMaxEdge(bounds.outWidth, bounds.outHeight, maxEdgePx)

        // step 3: decode the actual image with the calculated scale (inSampleSize)
        val decode = BitmapFactory.Options().apply {
            inSampleSize = sample
            inScaled = false
        }
        context.assets.open(assetPath).use { stream ->
            BitmapFactory.decodeStream(stream, null, decode)?.asImageBitmap()
        }
    }.getOrNull()
}

// calculated a power of two sample size for bitmapfactory
// if the image is 1000px and max is 256px, it returns 4 (decoding every 4th pixel)
private fun sampleSizeForMaxEdge(width: Int, height: Int, maxEdgePx: Int): Int {
    val longest = maxOf(width, height)
    if (longest <= maxEdgePx) return 1
    var sample = 1
    while (longest / sample > maxEdgePx) {
        sample *= 2
    }
    return sample
}

