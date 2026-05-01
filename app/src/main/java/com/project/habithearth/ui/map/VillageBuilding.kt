package com.project.habithearth.ui.map

import com.project.habithearth.model.TaskCategory

// data model: represents a single location on the map
data class VillageBuilding(
    val id: String, // unique identifier used for logic and asset lookup
    val name: String, // the full name (The Healing Spa)
    val story: String, // descriptive text shown in the building detail screen

    // coordinates:
    //      these use a fraction 0.0 to 1.0 rather than fixed pixels
    // allows the map to scale to any screen size without buildings moving
    val xFraction: Float, // 0.5 is exactly the horizontal middle
    val yFraction: Float, // 0.5 is exactly the vertical middle


    val shortLabel: String = name, // smaller name for ui labels
    val category: TaskCategory, // links the buildings to a specific habit type
)

// helper: finds a building in the list below by its ID
fun villageBuildingById(id: String): VillageBuilding? =
    defaultVillageBuildings().find { it.id == id }

// set: identifies the 5 core buildings the user starts with
// these are used by the MapViewModel to calculate the initial camera zoom
val MainHubBuildingIds: Set<String> = setOf(
    "library",
    "cottage",
    "spa",
    "guild",
    "greenhouse",
)


fun defaultVillageBuildings(): List<VillageBuilding> = listOf(

// --------the main 5 ---------------------------------------
    VillageBuilding(
        id = "cottage",
        name = "Your Cottage",
        story = "Smoke curls from the chimney. This is home base—tasks pinned by the door, a chair by the fire for when the day is done.",

        xFraction = 0.513f, // near center
        yFraction = 0.62f, // lower middle
        shortLabel = "Home",
        category = TaskCategory.UNSORTED,
    ),
    VillageBuilding(
        id = "library",
        name = "Quiet Stacks",
        story = "Dust motes float in sunbeams. Here you sketch tomorrow’s routines in the margins of borrowed books. No one rushes you.",

        xFraction = 0.326f,
        yFraction = 0.62f,
        shortLabel = "Library",
        category = TaskCategory.WISDOM,
    ),
    VillageBuilding(
        id = "greenhouse",
        name = "The Hearth Bakery",
        story = "Locals swap tales of streaks kept and habits broken. The keeper nods when you pass—your name is already on the board for tonight’s round.",


        xFraction = 0.6f,
        yFraction = 0.53f,
        shortLabel = "Tavern",
        category = TaskCategory.VITALITY,
    ),
    VillageBuilding(
        id = "guild",
        name = "Habit Guild Hall",
        story = "Charts cover the walls: who watered the plants, who walked at dawn. Join a pledge, or start your own—every small win echoes in this hall.",
        xFraction = 0.4f,
        yFraction = 0.53f,
        shortLabel = "Guild",
        category = TaskCategory.STRENGTH,
    ),
    VillageBuilding(
        id = "spa",
        name = "The Healing Spa",
        story = "They say wishes dropped here return as reminders at just the right hour. You hear water far below—steady, patient.",

        xFraction = 0.687f,
        yFraction = 0.62f,
        shortLabel = "Well",
        category = TaskCategory.SPIRIT,
    ),


    //------------- exploration buildings, locked at start ---------------

    // northern buildings (y = 0.08 to 0.17)
    VillageBuilding(
        id = "lookout_post",
        name = "North Lookout",
        story = "A crooked ladder and a bell that only rings when someone finishes what they started. The view reminds you how far the path goes.",
        xFraction = 0.326f, // left side
        yFraction = 0.08f, // very top of map
        shortLabel = "Lookout",
        category = TaskCategory.STRENGTH,
    ),

    VillageBuilding(
        id = "apiary",
        name = "Sunny Apiary",
        story = "Hives hum like a deadline you’re not afraid of. The keeper trades honey for honest check-ins on your daily rhythm.",
        xFraction = 0.687f,
        yFraction = 0.08f,
        shortLabel = "Apiary",
        category = TaskCategory.VITALITY,
    ),
    VillageBuilding(
        id = "windmill",
        name = "Old Windmill",
        story = "Blades turn whether you notice or not—same as small habits. Grain dust glitters in the light; you leave with flour and a lighter head.",
        xFraction = 0.4f,
        yFraction = 0.17f,
        shortLabel = "Mill",
        category = TaskCategory.WISDOM,
    ),
    VillageBuilding(
        id = "shrine",
        name = "Stone Shrine",
        story = "Offerings are pebbles, leaves, and quiet promises. No sermon—just space to remember why you began.",
        xFraction = 0.6f,
        yFraction = 0.17f,
        shortLabel = "Shrine",
        category = TaskCategory.SPIRIT,
    ),

    // mid north buildings ( y= 0.26 to 0.35)
    VillageBuilding(
        id = "fishery",
        name = "Cold Dock",
        story = "Nets dry on posts; the water doesn’t hurry. Locals say the best ideas surface when you stop thrashing.",
        xFraction = 0.326f,
        yFraction = 0.26f,
        shortLabel = "Dock",
        category = TaskCategory.VITALITY,
    ),
    VillageBuilding(
        id = "observatory",
        name = "Starloft",
        story = "A leaky dome and a decent telescope. Charts on the wall map constellations to bedtime routines—silly until it works.",
        xFraction = 0.5f,
        yFraction = 0.26f,
        shortLabel = "Stars",
        category = TaskCategory.WISDOM,
    ),
    VillageBuilding(
        id = "stables",
        name = "Moss Stables",
        story = "Beasts rest; riders compare streaks. The farrier hammers rhythm into iron—you tap your foot without thinking.",
        xFraction = 0.687f,
        yFraction = 0.26f,
        shortLabel = "Stables",
        category = TaskCategory.STRENGTH,
    ),

    VillageBuilding(
        id = "bakery",
        name = "Dawn Oven",
        story = "Bread rises on the same schedule as the village. The smell alone counts as a morning win.",
        xFraction = 0.4f,
        yFraction = 0.35f,
        shortLabel = "Bakery",
        category = TaskCategory.UNSORTED,
    ),
    VillageBuilding(
        id = "forge",
        name = "Ember Forge",
        story = "Heat and hammer—nothing vague here. Blacksmiths joke that discipline is just metal that stopped arguing.",
        xFraction = 0.6f,
        yFraction = 0.35f,
        shortLabel = "Forge",
        category = TaskCategory.STRENGTH,
    ),

    // mid south buildings (y = 0.44 to 0.53)

    VillageBuilding(
        id = "guild2",
        name = "Habit Annex",
        story = "Spillover from the main hall—quiet desks for planning the week. Someone always leaves a spare quill.",

        xFraction = 0.5f,
        yFraction = 0.44f,
        shortLabel = "Annex",
        category = TaskCategory.STRENGTH,
    ),

    VillageBuilding(
        id = "market_stall",
        name = "Ribbon Market",
        story = "Stalls trade ribbons for completed chores—colors by category. Gaudy, cheerful, impossible to ignore on a good day.",

        xFraction = 0.326f,
        yFraction = 0.44f,
        shortLabel = "Market",
        category = TaskCategory.UNSORTED,
    ),

    VillageBuilding(
        id = "workshop",
        name = "Tinker Workshop",
        story = "Gears, glue, and half-finished projects. The motto carved over the door: good enough today beats perfect never.",

        xFraction = 0.687f,
        yFraction = 0.44f,
        shortLabel = "Shop",
        category = TaskCategory.WISDOM,
    ),

    // southern buildings (y = 0.71 to 0.80)
    VillageBuilding(
        id = "vineyard",
        name = "Slope Vineyard",
        story = "Rows follow the hill; so does patience. Tasting notes on the board read like habit reviews—earthy, bright, still improving.",
        xFraction = 0.4f,
        yFraction = 0.71f,
        shortLabel = "Vines",
        category = TaskCategory.VITALITY,
    ),
    VillageBuilding(
        id = "gatehouse",
        name = "South Gate",
        story = "The guard only asks one question: did you show up for yourself today? Answer honestly; the gate opens either way.",
        xFraction = 0.6f,
        yFraction = 0.71f,
        shortLabel = "Gate",
        category = TaskCategory.SPIRIT,
    ),

    VillageBuilding(
        id = "orchard",
        name = "Late Orchard",
        story = "Fruit drops when it’s ready—so do old excuses here. Baskets wait; the trees don’t judge partial effort.",
        xFraction = 0.326f,
        yFraction = 0.80f,
        shortLabel = "Orchard",
        category = TaskCategory.VITALITY,
    ),
    VillageBuilding(
        id = "lodge",
        name = "Travelers’ Lodge",
        story = "Beds for strangers, hearth for regulars. The ledger by the door is all arrivals and departures—like habits, in and out.",
        xFraction = 0.5f,
        yFraction = 0.80f,
        shortLabel = "Lodge",
        category = TaskCategory.UNSORTED,
    ),
    VillageBuilding(
        id = "ruins",
        name = "Moss Ruins",
        story = "Broken arches, soft green reclaiming stone. Kids dare each other to sit still for one minute—hardest quest in the village.",
        xFraction = 0.687f,
        yFraction = 0.80f,
        shortLabel = "Ruins",
        category = TaskCategory.SPIRIT,
    ),
)
