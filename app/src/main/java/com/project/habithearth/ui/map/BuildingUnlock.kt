package com.project.habithearth.ui.map

import com.project.habithearth.model.BuildingUnlockCost
import com.project.habithearth.model.TaskCategory


// maps a ui building to its specific price type
// logic:
//      -if the building belongs to a specific attribute (strength,wisdom,...),
//      it costs specialized gems of that type

//      -If its unsorted, its costs standard coins
fun VillageBuilding.unlockCost(): BuildingUnlockCost =
    when (category) {
        // these categories represent the rpg stats of your habits
        TaskCategory.STRENGTH -> BuildingUnlockCost.Gems(TaskCategory.STRENGTH)
        TaskCategory.WISDOM -> BuildingUnlockCost.Gems(TaskCategory.WISDOM)
        TaskCategory.VITALITY -> BuildingUnlockCost.Gems(TaskCategory.VITALITY)
        TaskCategory.SPIRIT -> BuildingUnlockCost.Gems(TaskCategory.SPIRIT)

        // default currency for generic buildings
        TaskCategory.UNSORTED -> BuildingUnlockCost.Coins()
    }

// makes the data readable
// 500 strength gems
// 1000 coins
fun BuildingUnlockCost.displayLabel(): String =
    when (this) {
        // 'is' checks for the specific subclass of the building unlock cost sealed class
        is BuildingUnlockCost.Gems -> "$amount ${category.displayName.lowercase()} gems"
        is BuildingUnlockCost.Coins -> "$amount coins"
    }
