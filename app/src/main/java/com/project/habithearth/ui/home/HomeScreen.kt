package com.project.habithearth.ui.home

import androidx.compose.foundation.background
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
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.project.habithearth.HabitHearthApplication
import com.project.habithearth.ui.components.HabitTaskRowCard
import com.project.habithearth.ui.components.VerticalScrollIndicator
import com.project.habithearth.ui.state.GameStateViewModel
import com.project.habithearth.ui.tasks.TaskListViewModel
import com.project.habithearth.ui.tasks.TaskListViewModelFactory

@Composable
fun HomeScreen(
    welcomeDisplayName: String,
    onOpenTasks: () -> Unit,
    onEditTask: (String) -> Unit,
    gameStateViewModel: GameStateViewModel,
    modifier: Modifier = Modifier,
) {
    val app = LocalContext.current.applicationContext as HabitHearthApplication
    val taskListVm: TaskListViewModel = viewModel(
        factory = TaskListViewModelFactory(app.taskRepository),
    )
    val tasks by taskListVm.tasks.collectAsState()
    val scrollState = rememberScrollState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 16.dp),
    ) {
        Text(
            text = "Welcome, $welcomeDisplayName",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(top = 8.dp, bottom = 4.dp),
        )
        Text(
            text = "Today's habits — all tasks, including those filed in map buildings",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 8.dp),
        )

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
                tasks.forEach { task ->
                    HabitTaskRowCard(
                        task = task,
                        onCompletedChange = { checked ->
                            // TaskListViewModel persists the flip; the
                            // onTransition callback only fires when the
                            // persisted state actually changed, so we don't
                            // double-credit gems on a no-op tap. Reward-pool
                            // bookkeeping stays on the legacy
                            // GameStateViewModel until ProgressRepository
                            // lands (PLAN.md second milestone).
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
                Spacer(modifier = Modifier.height(8.dp))
            }
            VerticalScrollIndicator(
                scrollState = scrollState,
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .fillMaxHeight()
                    .padding(vertical = 4.dp),
            )
        }

        Button(
            onClick = onOpenTasks,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 12.dp, bottom = 8.dp),
        ) {
            Text("Create habit")
        }
    }
}
