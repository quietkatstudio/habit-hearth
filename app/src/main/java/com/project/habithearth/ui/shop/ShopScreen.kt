package com.project.habithearth.ui.shop

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.project.habithearth.ui.map.defaultVillageBuildings
import com.project.habithearth.ui.state.GameUiState

data class ShopItem(
    val id: String,
    val name: String,
    val description: String,
    val costCoins: Int,
)

private val DefaultShopItems = listOf(
    ShopItem(
        id = "fertilizer_bundle",
        name = "Fertilizer Bundle",
        description = "Boost your village plots with premium compost.",
        costCoins = 20,
    ),
    ShopItem(
        id = "focus_lantern",
        name = "Focus Lantern",
        description = "A warm lamp to keep evening habit sessions on track.",
        costCoins = 35,
    ),
    ShopItem(
        id = "task_chalk",
        name = "Task Chalk Set",
        description = "Write quick goals and check them off in style.",
        costCoins = 12,
    ),
)

fun nextWorkerCostCoins(currentWorkers: Int): Int =
    25 + (currentWorkers.coerceAtLeast(0) * 15)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShopScreen(
    gameUiState: GameUiState,
    onBack: () -> Unit,
    onHireWorker: (buildingId: String, costCoins: Int) -> Unit,
    onBuyItem: (itemId: String, costCoins: Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val ownedBuildings = defaultVillageBuildings().filter { it.id in gameUiState.ownedBuildingIds }
    val totalWorkers = gameUiState.workersByBuildingId.values.sum()
    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text("Village Shop") },
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
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "Hire workers and stock up on supplies. Workers boost rewards for tasks in their assigned building.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Text(
                            text = "Worker roster",
                            style = MaterialTheme.typography.titleMedium,
                        )
                        Text(
                            text = "Total workers: $totalWorkers",
                            style = MaterialTheme.typography.bodyMedium,
                        )
                        Text(
                            text = "Each worker adds +1 reward to completed tasks in their building.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }

            item {
                Text(
                    text = "Assign workers to buildings",
                    style = MaterialTheme.typography.titleMedium,
                )
            }

            items(ownedBuildings, key = { it.id }) { building ->
                val workersAtBuilding = gameUiState.workersByBuildingId[building.id] ?: 0
                val workerCost = nextWorkerCostCoins(workersAtBuilding)
                val canHire = gameUiState.coins >= workerCost
                Card(
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Text(
                            text = building.name,
                            style = MaterialTheme.typography.titleSmall,
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Text(
                                text = "Workers: $workersAtBuilding",
                                style = MaterialTheme.typography.bodySmall,
                            )
                            Text(
                                text = "Hire cost: $workerCost coins",
                                style = MaterialTheme.typography.bodySmall,
                            )
                        }
                        Button(
                            onClick = { onHireWorker(building.id, workerCost) },
                            enabled = canHire,
                        ) {
                            Text("Hire for ${building.shortLabel}")
                        }
                    }
                }
            }

            item {
                Text(
                    text = "Shop inventory",
                    style = MaterialTheme.typography.titleMedium,
                )
            }

            items(DefaultShopItems, key = { it.id }) { item ->
                val ownedCount = gameUiState.purchasedItemCounts[item.id] ?: 0
                val canAfford = gameUiState.coins >= item.costCoins
                Card(
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Text(
                            text = item.name,
                            style = MaterialTheme.typography.titleSmall,
                        )
                        Text(
                            text = item.description,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Text(
                                text = "Owned: $ownedCount",
                                style = MaterialTheme.typography.bodySmall,
                            )
                            Text(
                                text = "${item.costCoins} coins",
                                style = MaterialTheme.typography.bodySmall,
                            )
                        }
                        Button(
                            onClick = { onBuyItem(item.id, item.costCoins) },
                            enabled = canAfford,
                        ) {
                            Text("Buy")
                        }
                    }
                }
            }

            item { Spacer(Modifier.height(12.dp)) }
        }
    }
}
