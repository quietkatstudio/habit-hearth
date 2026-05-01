package com.project.habithearth.ui.map

// this files acts as the switchboard for the maps graphics

// a single data wrapper to hold the file path string

// using a data class makes it easier to add more properties like shadow or icon later
private data class BuildingMarkerArt(val image: String)

// the registry:
//      this map links a unique string ID (the buildingID) to a specific image file path

// these paths refer to files inside the app/src/main/assets/
private val BuildingMarkerArtById: Map<String, BuildingMarkerArt> = mapOf(
    // format: Building_ID to BuildingMarkerArt(image = "path/to/file/.png")
    "library" to BuildingMarkerArt(
        image = "images/buildings/wisdom/observatory.PNG",
    ),
    "cottage" to BuildingMarkerArt(
        image = "images/buildings/cottage.png",
    ),
    "spa" to BuildingMarkerArt(
        image = "images/buildings/vitality/greenhouse.PNG",
    ),
    "guild" to BuildingMarkerArt(
        image = "images/buildings/strength/tower.PNG",
    ),
    "greenhouse" to BuildingMarkerArt(
        image = "images/buildings/spirit/bakery.PNG",
    ),
)

// fallback: if the building ID is requessted that isn't in the map above,
// the app uses a default image to prevent a crash
private val DefaultBuildingMarkerArt = BuildingMarkerArt(
    image = "images/buildings/wisdom/observatory.PNG",
)

//the getter function
// use this function to find out which image to display
fun markerAssetPathForBuilding(buildingId: String, buildingIndex: Int): String {
    return (BuildingMarkerArtById[buildingId] ?: DefaultBuildingMarkerArt).image
}
