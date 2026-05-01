package com.project.habithearth.ui.components

import androidx.activity.compose.LocalActivity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.saveable.rememberSaveable

// component that forces the device's screen orientation
@Composable
fun LockScreenOrientation(orientation: Int) {
    // access the current activity context; if it doesn't exist, we cannot change orientation
    val activity = LocalActivity.current ?: return

    // rememberSaveable stores the original orientation the device was in before we changed it
    // ensures that even if the activity is recreated, we remember how to undo our change later
    val previousOrientation = rememberSaveable { activity.requestedOrientation }


    // used for side effects that need to be cleaned up
    // triggers when this function enters the composition and cleans up when it leaves
    DisposableEffect(activity, orientation, previousOrientation) {
        // force the screen into the new orientation
        activity.requestedOrientation = orientation

        // cleanup: block runs when the composable is removed from the screen
        onDispose {
            // only reset the orientation if the app is not currently undergoing a
            // configuration change
            if (!activity.isChangingConfigurations) {
                activity.requestedOrientation = previousOrientation
            }
        }
    }
}
