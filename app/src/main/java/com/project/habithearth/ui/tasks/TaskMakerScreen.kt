package com.project.habithearth.ui.tasks

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarColors
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.project.habithearth.HabitHearthApplication
import com.project.habithearth.model.TaskCategory
import com.project.habithearth.ui.map.defaultVillageBuildings
import com.project.habithearth.ui.state.GameStateViewModel



private const val CottageBuildingId = "cottage"

/**
 * Habit create/edit screen.
 *
 * The title is now composed via [BuildSentenceCard] (sentence builder) rather
 * than a plain text field. [DifficultyCard] replaces the old implicit
 * rewardAmount=1 default — the selected difficulty (1–5) maps directly to
 * [HabitTask.rewardAmount] and acts as the base per-completion reward before
 * the streak multiplier is applied.
 *
 * Note, category, and building dropdowns are preserved unchanged: category
 * routes gems to the correct pool; building places the habit in the village map.
 *
 * [gameStateViewModel] is still read for:
 *   - `ownedBuildingIds` to filter the building dropdown.
 *   - reward-pool rebalance when a completed task's category changes.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskMakerScreen(
    onBack: () -> Unit,
    gameStateViewModel: GameStateViewModel,
    modifier: Modifier = Modifier,
) {
    val app = LocalContext.current.applicationContext as HabitHearthApplication
    val editorVm: TaskEditorViewModel = viewModel(
        factory = TaskEditorViewModelFactory(app.taskRepository),
    )
    val sentenceVm: TaskSentenceViewModel = viewModel()

    val game by gameStateViewModel.uiState.collectAsState()
    val existingTask by editorVm.editingTask.collectAsState()
    val isSaving by editorVm.isSaving.collectAsState()
    val saveError by editorVm.saveError.collectAsState()
    val isEditMode = editorVm.isEditMode
    val initialBuildingId = editorVm.initialBuildingId

    var note by remember { mutableStateOf("") }
    var categoryExpanded by remember { mutableStateOf(false) }
    var selectedCategory by remember { mutableStateOf(TaskCategory.UNSORTED) }
    var buildingExpanded by remember { mutableStateOf(false) }
    var selectedBuildingId by remember { mutableStateOf<String?>(null) }
    // Prevents a late DataStore emission from clobbering text the user has typed.
    var seeded by remember { mutableStateOf(false) }

    val villageBuildings = remember { defaultVillageBuildings() }
    val ownedBuildings = remember(game.ownedBuildingIds) {
        villageBuildings.filter { it.id in game.ownedBuildingIds }
    }
    val buildingsForDropdown = remember(ownedBuildings, existingTask?.buildingId) {
        val orphanId = existingTask?.buildingId
        val orphan = orphanId?.let { id -> villageBuildings.find { it.id == id } }
        if (orphan != null && orphan.id !in game.ownedBuildingIds) {
            ownedBuildings + orphan
        } else {
            ownedBuildings
        }
    }

    val isEditingTaskLoaded by editorVm.isEditingTaskLoaded.collectAsState()

    // Navigate back if the task was deleted while the editor was open.
    LaunchedEffect(isEditMode, isEditingTaskLoaded, existingTask) {
        if (isEditMode && isEditingTaskLoaded && existingTask == null) {
            onBack()
        }
    }

    // Seed all form fields from the persisted task on first emission (edit mode)
    // or from route args (new-task mode).
    LaunchedEffect(existingTask, isEditMode, isEditingTaskLoaded) {
        if (!seeded) {
            val task = existingTask
            if (isEditMode) {
                if (task != null) {
                    note = task.note
                    selectedCategory = task.category
                    selectedBuildingId = task.buildingId ?: CottageBuildingId
                    sentenceVm.seedFromTask(task)
                    seeded = true
                }
            } else {
                selectedBuildingId =
                    initialBuildingId?.takeIf { it in game.ownedBuildingIds }
                        ?: CottageBuildingId
                seeded = true
            }
        }
    }

    LaunchedEffect(game.ownedBuildingIds, isEditMode) {
        if (!isEditMode) {
            val sid = selectedBuildingId
            if (sid != null && sid !in game.ownedBuildingIds) {
                selectedBuildingId = CottageBuildingId.takeIf { it in game.ownedBuildingIds }
            }
        }
    }

    Scaffold(
        //containerColor = MaterialTheme.colorScheme.background,
        //modifier = modifier.fillMaxSize(),
        topBar = {
            CenterAlignedTopAppBar(
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                ),
                title = {
                    Text(
                        text = if (isEditMode) "Task: ${sentenceVm.getFinalSentenceString()}" else "New habit",
                        textAlign = TextAlign.Center,
                        softWrap = true,

                    ) },

                navigationIcon = {
                    if (!isEditMode) {
                        IconButton(onClick = onBack) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                            )
                        }
                    }
                }
            )
        },
    ) { innerPadding ->
        val formEnabled = !isEditMode || isEditingTaskLoaded

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(innerPadding)
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            item {
                Text(
                    text = if (isEditMode) " " else "Create a habit",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 4.dp),
                )
            }

            // Sentence builder card
            item {
                BuildSentenceCard(
                    viewModel = sentenceVm,
                    startExpanded = true,
                )
            }

            // Difficulty card
            item {
                DifficultyCard(
                    viewModel = sentenceVm,
                    startExpanded = true,
                )
            }



            item {
                Card(){
                Column(modifier=Modifier. padding(15.dp)){
                // Category dropdown
                Text(
                    text = "Category",
                    style = MaterialTheme.typography.titleSmall,
                )
                ExposedDropdownMenuBox(
                    expanded = categoryExpanded,
                    onExpandedChange = { if (formEnabled) categoryExpanded = it },
                ) {
                    OutlinedTextField(
                        value = selectedCategory.displayName,
                        onValueChange = {},
                        readOnly = true,
                        singleLine = true,
                        enabled = formEnabled,
                        label = { Text("Habit category") },
                        trailingIcon = {
                            ExposedDropdownMenuDefaults.TrailingIcon(expanded = categoryExpanded)
                        },
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth(),
                        colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
                    )
                    ExposedDropdownMenu(
                        expanded = categoryExpanded,
                        onDismissRequest = { categoryExpanded = false },
                    ) {
                        TaskCategory.entries.forEach { category ->
                            DropdownMenuItem(
                                text = { Text(category.displayName) },
                                onClick = {
                                    selectedCategory = category
                                    categoryExpanded = false
                                },
                            )
                        }
                    }
                }
            }

            // Building dropdown
            Text(
                text = "File in building",
                style = MaterialTheme.typography.titleSmall,
            )
            ExposedDropdownMenuBox(
                expanded = buildingExpanded,
                onExpandedChange = { if (formEnabled) buildingExpanded = it },
            ) {
                val buildingLabel = selectedBuildingId?.let { id ->
                    villageBuildings.find { it.id == id }
                        ?.let { b -> "${b.shortLabel} — ${b.name}" }
                } ?: "Home (not on map)"
                OutlinedTextField(
                    value = buildingLabel,
                    onValueChange = {},
                    readOnly = true,
                    singleLine = true,
                    enabled = formEnabled,
                    label = { Text("Building") },
                    trailingIcon = {
                        ExposedDropdownMenuDefaults.TrailingIcon(expanded = buildingExpanded)
                    },
                    modifier = Modifier
                        .menuAnchor()
                        .fillMaxWidth(),
                    colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
                )
                ExposedDropdownMenu(
                    expanded = buildingExpanded,
                    onDismissRequest = { buildingExpanded = false },
                ) {
                    DropdownMenuItem(
                        text = { Text("Home (not filed to a building)") },
                        onClick = {
                            selectedBuildingId = null
                            buildingExpanded = false
                        },
                    )
                    buildingsForDropdown.forEach { building ->
                        if (building.category == selectedCategory) {
                            DropdownMenuItem(
                                text = { Text("${building.shortLabel} — ${building.name}") },
                                onClick = {
                                    selectedBuildingId = building.id
                                    buildingExpanded = false
                                },
                            )
                        }
                    }
                }
            }
        }
    }


            // Optional note
            item {
                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text("Note (optional)") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3,
                    enabled = formEnabled,
                )
            }

            // Save button
            item {
                val sentence = sentenceVm.getFinalSentenceString()
                val difficulty = sentenceVm.selectedDifficulty

                Button(
                    onClick = {
                        val before = existingTask
                        val targetCategory = selectedCategory
                        editorVm.saveAsync(
                            title = sentence,
                            note = note,
                            category = targetCategory,
                            buildingId = selectedBuildingId ?: CottageBuildingId,
                            rewardAmount = difficulty.coerceAtLeast(1),
                            onSaved = {
                                if (before != null && before.isCompleted &&
                                    before.category != targetCategory
                                ) {
                                    gameStateViewModel.applyRewardDelta(
                                        category = before.category,
                                        delta = -before.rewardAmount,
                                        buildingId = before.buildingId,
                                    )
                                    gameStateViewModel.applyRewardDelta(
                                        category = targetCategory,
                                        delta = before.rewardAmount,
                                        buildingId = selectedBuildingId ?: CottageBuildingId,
                                    )
                                }
                                onBack()
                            },
                        )
                    },
                    // Require verb text and a difficulty selection before allowing save
                    enabled = sentence.length > "I will .".length &&
                        difficulty > 0 &&
                        !isSaving &&
                        (!isEditMode || isEditingTaskLoaded),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        when {
                            isSaving -> "Saving..."
                            isEditMode -> "Save changes"
                            else -> "Save habit"
                        },
                    )
                }
                saveError?.let { errorMessage ->
                    Text(
                        text = "Couldn't save: $errorMessage",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }
        }
    }
}
