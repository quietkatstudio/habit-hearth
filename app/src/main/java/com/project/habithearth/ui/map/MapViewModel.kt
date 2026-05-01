package com.project.habithearth.ui.map

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

//data model: represents the camera position
// scale: the zoom level (1.0 is normal, 4.0 is zoomed in)
// pan: the x and y coordinates (offset) of where the map is positioned
data class MapViewportState(
    val scale: Float,
    val pan: Offset,
)

class MapViewModel(
    //savedstatehandle allows the app to remember the camera position
    // even if the andriod system kills the app process to say memory
    private val savedStateHandle: SavedStateHandle,
) : ViewModel() {
    //  internal state: a flow that the ui observes to know where to draw the map
    private val _viewportState = MutableStateFlow(
        MapViewportState(
            // try to load the last position for memory; otherwise, use defaults (0,0, at scale 1)
            scale = savedStateHandle[KEY_SCALE] ?: DEFAULT_SCALE,
            pan = Offset(
                x = savedStateHandle[KEY_PAN_X] ?: DEFAULT_PAN_X,
                y = savedStateHandle[KEY_PAN_Y] ?: DEFAULT_PAN_Y,
            ),
        ),
    )
    // public state: the ui subscribes to this read only version
    val viewportState: StateFlow<MapViewportState> = _viewportState.asStateFlow()

    // called whenever the user pinches or drags the map
    // updates the current view and saves the new coordinates to persistent storage
    fun updateViewport(scale: Float, pan: Offset) {
        _viewportState.value = MapViewportState(scale = scale, pan = pan)
        savedStateHandle[KEY_SCALE] = scale
        savedStateHandle[KEY_PAN_X] = pan.x
        savedStateHandle[KEY_PAN_Y] = pan.y
    }

    // auto-center logic:
    //      if the user is opening the map for the first time (default settings),
    //      this snaps the camera focus to the main village buildings
    fun seedInitialViewportIfDefault(hub: MapViewportState) {
        val current = _viewportState.value

        // only trigger if the camera hasn't moved from the factory default
        if (current.scale != DEFAULT_SCALE || current.pan != Offset(DEFAULT_PAN_X, DEFAULT_PAN_Y)) {
            return
        }
        updateViewport(hub.scale, hub.pan)
    }

    companion object {
        // keys for storage
        private const val KEY_SCALE = "map_scale"
        private const val KEY_PAN_X = "map_pan_x"
        private const val KEY_PAN_Y = "map_pan_y"
        const val DEFAULT_SCALE = 1f
        const val DEFAULT_PAN_X = 0f
        const val DEFAULT_PAN_Y = 0f

        // the actual dimensions of map image file in DP
        private val MapContentWidthDp = 1400.dp
        private val MapContentHeightDp = 980.dp

        // calculates where the camera needs to go to center on the map hub

        // finds the min/max coordinates of the specific hub buildings

        // calculates the center point (cx, cy)

        // converts the 0.0-1.0 fraction into actual screen pixels based on device density

        // applies a default 2.35x zoom
        fun initialViewportForMainBuildings(density: Density): MapViewportState {
            val buildings = defaultVillageBuildings().filter { it.id in MainHubBuildingIds }
            check(buildings.size == MainHubBuildingIds.size) {
                "Main hub building ids must all exist in defaultVillageBuildings()"
            }

            // calculate the bounding box around the hub buildings
            val minX = buildings.minOf { it.xFraction }
            val maxX = buildings.maxOf { it.xFraction }
            val minY = buildings.minOf { it.yFraction }
            val maxY = buildings.maxOf { it.yFraction }
            val pad = 0.04f

            // find the center of that box
            val cx = ((minX + maxX) / 2f).coerceIn(pad, 1f - pad)
            val cy = ((minY + maxY) / 2f).coerceIn(pad, 1f - pad)

            val scale = 2.35f

            // convert dp to pixels for the math
            val mapW = with(density) { MapContentWidthDp.toPx() }
            val mapH = with(density) { MapContentHeightDp.toPx() }

            // offset logic: (current center - screen middle) * zoom
            val dx = (cx - 0.5f) * mapW
            val dy = (cy - 0.5f) * mapH
            val panX = -dx * scale
            val panY = -dy * scale

            return MapViewportState(scale = scale, pan = Offset(panX, panY))
        }
    }
}
