package com.project.habithearth.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.dp
import android.widget.Toast
import androidx.compose.material3.NavigationBarItemDefaults
import com.project.habithearth.BuildConfig
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.project.habithearth.HabitHearthApplication
import com.project.habithearth.data.AccountSettings
import com.project.habithearth.ui.home.BuildingDirectoryDialog
import com.project.habithearth.ui.home.HomeScreen
import com.project.habithearth.ui.map.BuildingDetailScreen
import com.project.habithearth.ui.map.MapScreen
import com.project.habithearth.ui.navigation.AppDestination
import com.project.habithearth.ui.navigation.TopChromeWithMenu
import com.project.habithearth.ui.navigation.TopResourceBar
import com.project.habithearth.notifications.TaskReminderScheduler
import com.project.habithearth.ui.profile.ProfileScreen
import com.project.habithearth.ui.shop.ShopScreen
import com.project.habithearth.ui.state.GameStateViewModel
import com.project.habithearth.ui.state.GameStateViewModelFactory
import com.project.habithearth.ui.story.StoryScreen
import com.project.habithearth.ui.tasks.TaskMakerScreen

private const val TaskMakerRoute = "task_maker"
private const val TaskMakerNewInBuildingRoute = "task_maker/building/{buildingId}"
private const val TaskMakerEditRoute = "task_maker/{taskId}"
private const val BuildingDetailRoute = "building_detail/{buildingId}"
private const val ShopRoute = "shop"
private const val ShopBuildingId = "workshop"

@Composable
fun HabitHearthApp(modifier: Modifier = Modifier) {
    val app = LocalContext.current.applicationContext as HabitHearthApplication
    val userProgressRepository = app.userProgressRepository

    val gameVm: GameStateViewModel = viewModel(
        factory = GameStateViewModelFactory(
            repository = userProgressRepository,
            chapter1ProgressRepository = app.chapter1ProgressRepository,
            debugResetEmitter = app.debugResetEvents,
        ),
    )
    val game by gameVm.uiState.collectAsState()
    val account by userProgressRepository.accountSettings.collectAsState(initial = AccountSettings.DEFAULT)

    var showBuildingDirectory by remember { mutableStateOf(false) }

    // Hidden debug panel: 7 consecutive Profile-tab taps unlocks it for the
    // current process lifetime. State is session-only by design; persisting
    // would surface "Debug" on every cold boot and defeat the easter egg.
    // BuildConfig.DEBUG gates this so release builds can never reach the
    // unlock path at all.
    val unlockTapsRequired = 7
    var debugUnlocked by remember { mutableStateOf(false) }
    var profileTapCount by remember { mutableStateOf(0) }
    val context = LocalContext.current

    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val current = backStackEntry?.destination
    val route = current?.route.orEmpty()
    val isTaskMaker =
        route == TaskMakerRoute ||
            route.startsWith("task_maker/building/") ||
            (route.startsWith("task_maker/") && !route.startsWith("task_maker/building/"))
    val isBuildingDetail = route.startsWith("building_detail/")
    // Keep resource bar + bottom navigation visible on building detail screens.
    val hideMainChrome = isTaskMaker
    val isHome = current?.route == AppDestination.Home.route

    LaunchedEffect(isHome) {
        if (!isHome) showBuildingDirectory = false
    }

    LaunchedEffect(account.pushNotifications, account.notificationHour, account.notificationMinute) {
        if (account.pushNotifications) {
            TaskReminderScheduler.scheduleDaily(
                context = context,
                hour = account.notificationHour,
                minute = account.notificationMinute,
            )
        } else {
            TaskReminderScheduler.cancel(context)
        }
    }

    val welcomeName = account.displayName.ifBlank { "Traveler" }

    Box(Modifier.fillMaxSize()) {
        Scaffold(
            modifier = modifier.fillMaxSize(),
            topBar = {
                if (!hideMainChrome) {
                    if (isHome) {
                        TopChromeWithMenu(
                            onMenuClick = { showBuildingDirectory = true },
                            strengthGems = game.strengthGems,
                            wisdomGems = game.wisdomGems,
                            vitalityGems = game.vitalityGems,
                            spiritGems = game.spiritGems,
                            coins = game.coins,
                            totalXp = game.totalXp,
                        )
                    } else {
                        TopResourceBar(
                            strengthGems = game.strengthGems,
                            wisdomGems = game.wisdomGems,
                            vitalityGems = game.vitalityGems,
                            spiritGems = game.spiritGems,
                            coins = game.coins,
                            totalXp = game.totalXp,
                        )
                    }
                }
            },
            bottomBar = {
                if (!hideMainChrome) {
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surfaceContainer,
                    contentColor = MaterialTheme.colorScheme.onSurface,
                ) {
                        AppDestination.entries.forEach { destination ->
                            val selected =
                                current?.hierarchy?.any { it.route == destination.route } == true
                            NavigationBarItem(
                                icon = {
                                    Icon(
                                        imageVector = destination.icon,
                                        contentDescription = destination.label,
                                    )
                                },
                                label = { Text(destination.label) },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = MaterialTheme.colorScheme.onSecondaryContainer,
                                    selectedTextColor = MaterialTheme.colorScheme.onSecondaryContainer,
                                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                    indicatorColor = MaterialTheme.colorScheme.secondaryContainer,
                                ),
                                selected = selected,
                                onClick = {
                                    if (BuildConfig.DEBUG && !debugUnlocked) {
                                        if (destination == AppDestination.Profile) {
                                            val next = profileTapCount + 1
                                            if (next >= unlockTapsRequired) {
                                                debugUnlocked = true
                                                profileTapCount = 0
                                                Toast.makeText(
                                                    context,
                                                    "Debug panel unlocked",
                                                    Toast.LENGTH_SHORT,
                                                ).show()
                                            } else {
                                                profileTapCount = next
                                            }
                                        } else {
                                            profileTapCount = 0
                                        }
                                    }
                                    navController.navigate(destination.route) {
                                        popUpTo(navController.graph.findStartDestination().id) {
                                            saveState = true
                                        }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                },
                            )
                        }
                    }
                }
            },
        ) { innerPadding ->
            val layoutDirection = LocalLayoutDirection.current
            val navPadding =
                if (route == AppDestination.Map.route) {
                    PaddingValues(
                        start = innerPadding.calculateStartPadding(layoutDirection),
                        top = 0.dp,
                        end = innerPadding.calculateEndPadding(layoutDirection),
                        bottom = innerPadding.calculateBottomPadding(),
                    )
                } else {
                    innerPadding
                }
            NavHost(
                navController = navController,
                startDestination = AppDestination.Home.route,
                modifier = Modifier.padding(navPadding),
                enterTransition = {
                    val toRoute = targetState.destination.route
                    if (toRoute == AppDestination.Map.route) {
                        fadeIn(animationSpec = tween(250))
                    } else {
                        slideInHorizontally(
                            initialOffsetX = { fullWidth -> fullWidth / 4 },
                            animationSpec = tween(240),
                        ) + fadeIn(animationSpec = tween(180))
                    }
                },
                exitTransition = {
                    val fromRoute = initialState.destination.route
                    if (fromRoute == AppDestination.Map.route) {
                        fadeOut(animationSpec = tween(180))
                    } else {
                        slideOutHorizontally(
                            targetOffsetX = { fullWidth -> -fullWidth / 4 },
                            animationSpec = tween(200),
                        ) + fadeOut(animationSpec = tween(140))
                    }
                },
                popEnterTransition = {
                    val toRoute = targetState.destination.route
                    if (toRoute == AppDestination.Map.route) {
                        fadeIn(animationSpec = tween(250))
                    } else {
                        slideInHorizontally(
                            initialOffsetX = { fullWidth -> -fullWidth / 4 },
                            animationSpec = tween(240),
                        ) + fadeIn(animationSpec = tween(180))
                    }
                },
                popExitTransition = {
                    val fromRoute = initialState.destination.route
                    if (fromRoute == AppDestination.Map.route) {
                        fadeOut(animationSpec = tween(180))
                    } else {
                        slideOutHorizontally(
                            targetOffsetX = { fullWidth -> fullWidth / 4 },
                            animationSpec = tween(200),
                        ) + fadeOut(animationSpec = tween(140))
                    }
                },
            ) {
                composable(
                    route = BuildingDetailRoute,
                    arguments = listOf(
                        navArgument("buildingId") { type = NavType.StringType },
                    ),
                ) { entry ->
                    val buildingId = entry.arguments?.getString("buildingId")
                    if (buildingId != null) {
                        BuildingDetailScreen(
                            buildingId = buildingId,
                            onBack = { navController.popBackStack() },
                            onAddHabitInBuilding = { bid ->
                                navController.navigate("task_maker/building/$bid")
                            },
                            onEditTask = { taskId ->
                                navController.navigate("task_maker/$taskId")
                            },
                            gameStateViewModel = gameVm,
                        )
                    }
                }
                composable(AppDestination.Map.route) {
                    MapScreen(
                        ownedBuildingIds = game.ownedBuildingIds,
                        gameUiState = game,
                        onOpenBuilding = { building ->
                            if (building.id == ShopBuildingId) {
                                navController.navigate(ShopRoute)
                            } else {
                                navController.navigate("building_detail/${building.id}")
                            }
                        },
                        onPurchaseBuilding = { buildingId ->
                            gameVm.tryPurchaseBuilding(buildingId)
                        },
                    )
                }
                composable(ShopRoute) {
                    ShopScreen(
                        gameUiState = game,
                        onBack = { navController.popBackStack() },
                        onHireWorker = { buildingId, workerCost ->
                            gameVm.tryHireWorkerForBuilding(buildingId, workerCost)
                        },
                        onBuyItem = { itemId, costCoins ->
                            gameVm.tryBuyShopItem(itemId, costCoins)
                        },
                    )
                }
                composable(AppDestination.Home.route) {
                    HomeScreen(
                        welcomeDisplayName = welcomeName,
                        onOpenTasks = { navController.navigate(TaskMakerRoute) },
                        onEditTask = { taskId ->
                            navController.navigate("task_maker/$taskId")
                        },
                        gameStateViewModel = gameVm,
                    )
                }
                composable(AppDestination.Story.route) { StoryScreen(gameState = game) }
                composable(AppDestination.Profile.route) {
                    ProfileScreen(
                        gameUiState = game,
                        userProgressRepository = userProgressRepository,
                        gameStateViewModel = gameVm,
                        debugPanelVisible = debugUnlocked,
                        onHideDebugPanel = { debugUnlocked = false },
                    )
                }
                composable(
                    route = TaskMakerNewInBuildingRoute,
                    arguments = listOf(
                        navArgument("buildingId") { type = NavType.StringType },
                    ),
                ) {
                    // TaskEditorViewModel reads buildingId off the
                    // back-stack entry's SavedStateHandle, so this
                    // composable doesn't need to forward route args.
                    TaskMakerScreen(
                        onBack = { navController.popBackStack() },
                        gameStateViewModel = gameVm,
                    )
                }
                composable(TaskMakerRoute) {
                    TaskMakerScreen(
                        onBack = { navController.popBackStack() },
                        gameStateViewModel = gameVm,
                    )
                }
                composable(
                    route = TaskMakerEditRoute,
                    arguments = listOf(
                        navArgument("taskId") { type = NavType.StringType },
                    ),
                ) {
                    TaskMakerScreen(
                        onBack = { navController.popBackStack() },
                        gameStateViewModel = gameVm,
                    )
                }
            }
        }

        if (showBuildingDirectory) {
            BuildingDirectoryDialog(
                onDismiss = { showBuildingDirectory = false },
            )
        }
    }
}
