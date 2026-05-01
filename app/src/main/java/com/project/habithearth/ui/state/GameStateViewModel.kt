package com.project.habithearth.ui.state

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.project.habithearth.data.UserProgressRepository
import com.project.habithearth.data.story.Chapter1ProgressRepository
import com.project.habithearth.model.ResourceProgress
import com.project.habithearth.model.TaskCategory
import com.project.habithearth.model.canAfford
import com.project.habithearth.model.withUnlockCostPaid
import com.project.habithearth.ui.map.MainHubBuildingIds
import com.project.habithearth.ui.map.unlockCost
import com.project.habithearth.ui.map.villageBuildingById
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class GameUiState(
    val strengthGems: Int = 0,
    val wisdomGems: Int = 0,
    val vitalityGems: Int = 0,
    val spiritGems: Int = 0,
    val coins: Int = 0,
    val totalXp: Int = 0,
    val workersByBuildingId: Map<String, Int> = emptyMap(),
    val purchasedItemCounts: Map<String, Int> = emptyMap(),
    // Starter hubs (library/cottage/spa/guild/greenhouse) own at fresh-install
    // time so the home tile of the map isn't a wall of locks. Saved games
    // re-merge MainHubBuildingIds in loadGameState when the persisted set is
    // empty, so this default and that load-time fixup stay in sync.
    val ownedBuildingIds: Set<String> = MainHubBuildingIds,
)

class GameStateViewModel(
    private val userProgressRepository: UserProgressRepository,
    // Optional debug-only collaborators; nullable so non-debug surface area and
    // tests can construct the VM without setting up the full Application graph.
    // debugResetProgress wipes both pieces in addition to resetting in-memory
    // game state, then signals other VMs (StoryViewModel) via the SharedFlow.
    private val chapter1ProgressRepository: Chapter1ProgressRepository? = null,
    private val debugResetEmitter: MutableSharedFlow<Unit>? = null,
) : ViewModel() {

    private val _uiState = MutableStateFlow(GameUiState())
    val uiState: StateFlow<GameUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            reloadFromRepositoryNow()
        }
    }

    /** Loads from disk, or default [GameUiState] if none. */
    suspend fun reloadFromRepositoryNow() {
        _uiState.value = userProgressRepository.loadGameState() ?: GameUiState()
    }

    private fun persist() {
        viewModelScope.launch {
            userProgressRepository.saveGameState(_uiState.value)
        }
    }

    /**
     * Applies a gem/coin delta for a task category without touching the task
     * list. Phase 5 of the DataStore refactor (see PLAN.md): tasks now live in
     * [com.project.habithearth.data.task.TaskRepository], but reward-pool
     * bookkeeping stays here until `ProgressRepository` lands. Screens call
     * this after [com.project.habithearth.ui.tasks.TaskListViewModel.setCompleted]
     * reports a real flip, so we don't double-credit on no-op writes.
     *
     * Pass a positive [delta] when crediting a fresh completion, negative when
     * un-completing or re-categorizing a completed task.
     */
    fun applyRewardDelta(category: TaskCategory, delta: Int, buildingId: String? = null) {
        if (delta == 0) return
        val workersAtBuilding = buildingId?.let { _uiState.value.workersByBuildingId[it] ?: 0 } ?: 0
        val effectiveDelta = if (delta > 0) delta + workersAtBuilding else delta
        _uiState.update { state ->
            val withGems = state.withResourceDelta(category, effectiveDelta)
            // XP is monotonic: a fresh completion grants flat XP_PER_TASK,
            // but uncompleting (delta < 0) does NOT refund XP. Keeps story
            // gates from yo-yoing as users toggle tasks, and matches the
            // "reading the chapter is permanent" feel discussed in design.
            if (delta > 0) {
                withGems.copy(totalXp = withGems.totalXp + XP_PER_TASK)
            } else {
                withGems
            }
        }
        persist()
    }

    private fun GameUiState.withResourceDelta(category: TaskCategory, delta: Int): GameUiState {
        return when (category) {
            TaskCategory.STRENGTH -> copy(strengthGems = (strengthGems + delta).coerceAtLeast(0))
            TaskCategory.WISDOM -> copy(wisdomGems = (wisdomGems + delta).coerceAtLeast(0))
            TaskCategory.VITALITY -> copy(vitalityGems = (vitalityGems + delta).coerceAtLeast(0))
            TaskCategory.SPIRIT -> copy(spiritGems = (spiritGems + delta).coerceAtLeast(0))
            TaskCategory.UNSORTED -> copy(coins = (coins + delta).coerceAtLeast(0))
        }
    }

    /**
     * Debug-only resource minter. Applies signed deltas to each pool and the
     * XP bar, then persists. Wired through Profile's debug panel; gated by
     * BuildConfig.DEBUG at the call site so release builds never touch this.
     * Pools clamp at 0; xp clamps to [0, 1].
     */
    fun debugAdjustResources(
        strength: Int = 0,
        wisdom: Int = 0,
        vitality: Int = 0,
        spirit: Int = 0,
        coins: Int = 0,
        xpDelta: Int = 0,
    ) {
        _uiState.update { s ->
            s.copy(
                strengthGems = (s.strengthGems + strength).coerceAtLeast(0),
                wisdomGems = (s.wisdomGems + wisdom).coerceAtLeast(0),
                vitalityGems = (s.vitalityGems + vitality).coerceAtLeast(0),
                spiritGems = (s.spiritGems + spirit).coerceAtLeast(0),
                coins = (s.coins + coins).coerceAtLeast(0),
                totalXp = (s.totalXp + xpDelta).coerceAtLeast(0),
            )
        }
        persist()
    }

    /** Debug-only: own every building in the village. */
    fun debugUnlockAllBuildings() {
        _uiState.update { s ->
            val all = com.project.habithearth.ui.map.defaultVillageBuildings()
                .map { it.id }
                .toSet()
            s.copy(ownedBuildingIds = s.ownedBuildingIds + all)
        }
        persist()
    }

    /** Debug-only: zero pools, re-seed starter hubs, clear xp, wipe story. */
    fun debugResetProgress() {
        _uiState.value = GameUiState()
        persist()
        viewModelScope.launch {
            chapter1ProgressRepository?.clear()
            // Emit after the disk wipe so a collector that loads on signal
            // sees an already-empty repo.
            debugResetEmitter?.emit(Unit)
        }
    }

    /**
     * Unlocks a map building if the player can pay its category cost (see `VillageBuildingUnlockCost`).
     * Starter hub buildings are always owned (no charge).
     */
    fun tryPurchaseBuilding(buildingId: String): Boolean {
        val building = villageBuildingById(buildingId) ?: return false
        val current = _uiState.value
        if (building.id in current.ownedBuildingIds) return true
        val cost = building.unlockCost()
        if (!current.resources.canAfford(cost)) return false
        _uiState.update { s ->
            if (building.id in s.ownedBuildingIds) return@update s
            // Recheck affordability inside the atomic update: resource pools
            // can shift between the read above and the write here (e.g. a
            // concurrent task completion), and canAfford on a stale snapshot
            // is not enough to guarantee non-negative balances.
            if (!s.resources.canAfford(cost)) return@update s
            val paid = s.resources.withUnlockCostPaid(cost)
            s.copyResources(paid).copy(ownedBuildingIds = s.ownedBuildingIds + building.id)
        }
        persist()
        return true
    }

    fun tryHireWorkerForBuilding(buildingId: String, workerCostCoins: Int): Boolean {
        if (buildingId.isBlank() || workerCostCoins <= 0) return false
        val building = villageBuildingById(buildingId) ?: return false
        val current = _uiState.value
        if (building.id !in current.ownedBuildingIds) return false
        if (current.coins < workerCostCoins) return false
        var hired = false
        _uiState.update { s ->
            if (s.coins < workerCostCoins) return@update s
            if (building.id !in s.ownedBuildingIds) return@update s
            val currentWorkers = s.workersByBuildingId[building.id] ?: 0
            hired = true
            s.copy(
                coins = s.coins - workerCostCoins,
                workersByBuildingId = s.workersByBuildingId + (building.id to (currentWorkers + 1)),
            )
        }
        if (hired) persist()
        return hired
    }

    fun tryBuyShopItem(itemId: String, costCoins: Int): Boolean {
        if (itemId.isBlank() || costCoins <= 0) return false
        val current = _uiState.value
        if (current.coins < costCoins) return false
        var bought = false
        _uiState.update { s ->
            if (s.coins < costCoins) return@update s
            val nextCount = (s.purchasedItemCounts[itemId] ?: 0) + 1
            bought = true
            s.copy(
                coins = s.coins - costCoins,
                purchasedItemCounts = s.purchasedItemCounts + (itemId to nextCount),
            )
        }
        if (bought) persist()
        return bought
    }
}

/**
 * Bridge view of the player's wallet/XP/owned buildings as a [ResourceProgress].
 *
 * Phase 2 of the DataStore refactor moves unlock-cost math onto
 * [ResourceProgress] so it no longer depends on the UI state class. Until
 * Phase 4 collapses these duplicated fields, [GameUiState] continues to own
 * the actual storage and exposes this read-only view for the math.
 */
val GameUiState.resources: ResourceProgress
    get() = ResourceProgress(
        strengthGems = strengthGems,
        wisdomGems = wisdomGems,
        vitalityGems = vitalityGems,
        spiritGems = spiritGems,
        coins = coins,
        totalXp = totalXp,
        ownedBuildingIds = ownedBuildingIds,
    )

/** Inverse of [resources]: write a [ResourceProgress] back into a [GameUiState]. */
fun GameUiState.copyResources(rp: ResourceProgress): GameUiState =
    copy(
        strengthGems = rp.strengthGems,
        wisdomGems = rp.wisdomGems,
        vitalityGems = rp.vitalityGems,
        spiritGems = rp.spiritGems,
        coins = rp.coins,
        totalXp = rp.totalXp,
        ownedBuildingIds = rp.ownedBuildingIds,
    )

@Suppress("UNCHECKED_CAST")
class GameStateViewModelFactory(
    private val repository: UserProgressRepository,
    private val chapter1ProgressRepository: Chapter1ProgressRepository? = null,
    private val debugResetEmitter: MutableSharedFlow<Unit>? = null,
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return GameStateViewModel(
            userProgressRepository = repository,
            chapter1ProgressRepository = chapter1ProgressRepository,
            debugResetEmitter = debugResetEmitter,
        ) as T
    }
}

