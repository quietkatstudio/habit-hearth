package com.project.habithearth.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.SemanticsActions.OnClick
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.project.habithearth.model.HabitTask
import com.project.habithearth.model.TaskCategory
import com.project.habithearth.model.toEpochMs
import com.project.habithearth.ui.theme.outlineColor

@Composable
fun HabitTaskRowCard(
    task: HabitTask, // the data model containing title, category, logs, etc.
    onCompletedChange: (Boolean) -> Unit, // callback when the collect button is pressed
    onOpenEdit: () -> Unit, // callback when the text area is clicked to edit the task
    modifier: Modifier = Modifier,
) {
    // ***explain this comment pls***
    // defines the rounded corners for the card one here to reuse if needed
    val shape = RoundedCornerShape(12.dp)

    // logic sections : cooldown check
    //      remember ensures this calculation doesn't run unnecessarily on every recomposition
    //      unless the task's collectingLog actually changes
    val canCollect = remember(task.collectionLog) {
        // gets the timestamp of the last time this was collected (default to 0)
        val lastMs = task.collectionLog.lastOrNull()?.timestamp?.toEpochMs() ?: 0L

        // checks if current time is less than 24 hours (in milliseconds) since the last log
        System.currentTimeMillis() - lastMs > 24L * 60 * 60 * 1000
    }
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = shape,

        // uses a custom extension property outlineColor based on the task category
        border = BorderStroke(2.dp, task.category.outlineColor),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        ),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {

            // left side: task details (Title, note, rewards)
            Column(
                modifier = Modifier
                    .weight(1f) // takes up all available horizontal space, pushing the button to the right
                    .clickable(onClick = onOpenEdit), // clicking the text triggers the edit action
            ) {
                Text(
                    text = task.title,
                    style = MaterialTheme.typography.titleMedium,
                )

                // only renders the note text if it isn't empty
                if (task.note.isNotBlank()) {
                    Text(
                        text = task.note,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                // subtitle: shows category and reward type (coins vs gems)
                Text(
                    text = "${task.category.displayName} · +${task.rewardAmount} " +
                        when (task.category) {
                            TaskCategory.UNSORTED -> "coins"
                            else -> "gems"
                        },
                    style = MaterialTheme.typography.labelSmall,

                    //colors text to match the card border
                    color = task.category.outlineColor,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }

            // right side: the action button
            Button(
                onClick = { onCompletedChange(true) },
                // button is greyed out/disabled if 24 hours haven't passed
                enabled = canCollect,
            ) {
                Text("Collect")
            }
        }
    }
}
