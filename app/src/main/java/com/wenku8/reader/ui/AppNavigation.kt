package com.wenku8.reader.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Book
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.wenku8.reader.feature.debug.DevLogScreen
import com.wenku8.reader.feature.detail.DetailScreen
import com.wenku8.reader.feature.home.HomeScreen
import com.wenku8.reader.feature.library.LibraryScreen
import com.wenku8.reader.feature.reader.ReaderScreen
import com.wenku8.reader.feature.search.SearchScreen

object Routes {
    const val HOME = "home"
    const val LIBRARY = "library"
    const val SEARCH = "search"
    const val DEV_LOG = "devlog"
    const val DETAIL = "detail/{aid}"
    const val READER = "reader/{aid}?cid={cid}"

    fun detail(aid: Int) = "detail/$aid"

    /** cid == -1 → continue from saved progress; else open that chapter from its start. */
    fun reader(aid: Int, cid: Int?) = "reader/$aid?cid=${cid ?: -1}"
}

private data class BottomTab(val route: String, val label: String, val icon: ImageVector)

private val bottomTabs = listOf(
    BottomTab(Routes.HOME, "首页", Icons.Outlined.Home),
    BottomTab(Routes.LIBRARY, "我的小说", Icons.Outlined.Book),
)

@Composable
fun AppBottomBar(currentRoute: String?, onNavigate: (String) -> Unit) {
    NavigationBar {
        bottomTabs.forEach { tab ->
            val selected = currentRoute == tab.route
            NavigationBarItem(
                selected = selected,
                onClick = { onNavigate(tab.route) },
                icon = { Icon(tab.icon, contentDescription = tab.label) },
                label = { Text(tab.label) },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = MaterialTheme.colorScheme.primary,
                    selectedTextColor = MaterialTheme.colorScheme.primary,
                    indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                ),
            )
        }
    }
}

@Composable
fun AppNavigation() {
    val navController = rememberNavController()
    AppNavHost(navController)
}

@Composable
private fun AppNavHost(navController: NavHostController) {
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route

    // Tab navigation: single-top + save/restore so switching tabs keeps each screen's state.
    val onNavigateTab: (String) -> Unit = { route ->
        navController.navigate(route) {
            popUpTo(navController.graph.startDestinationId) { saveState = true }
            launchSingleTop = true
            restoreState = true
        }
    }

    NavHost(navController = navController, startDestination = Routes.HOME) {

        composable(Routes.HOME) {
            HomeScreen(
                currentRoute = currentRoute,
                onNavigateTab = onNavigateTab,
                onSearchClick = { navController.navigate(Routes.SEARCH) },
                onOpenDetail = { aid -> navController.navigate(Routes.detail(aid)) },
                onOpenDevLog = { navController.navigate(Routes.DEV_LOG) },
            )
        }

        composable(Routes.LIBRARY) {
            LibraryScreen(
                currentRoute = currentRoute,
                onNavigateTab = onNavigateTab,
                onOpenDetail = { aid -> navController.navigate(Routes.detail(aid)) },
            )
        }

        composable(Routes.SEARCH) {
            SearchScreen(
                onBack = { navController.popBackStack() },
                onOpenNovel = { aid -> navController.navigate(Routes.detail(aid)) },
            )
        }

        composable(Routes.DEV_LOG) {
            DevLogScreen(onBack = { navController.popBackStack() })
        }

        composable(
            Routes.DETAIL,
            arguments = listOf(navArgument("aid") { type = NavType.IntType }),
        ) { entry ->
            val aid = entry.arguments?.getInt("aid") ?: 0
            DetailScreen(
                aid = aid,
                onBack = { navController.popBackStack() },
                onOpenNovel = { id, cid -> navController.navigate(Routes.reader(id, cid)) },
            )
        }

        composable(
            Routes.READER,
            arguments = listOf(
                navArgument("aid") { type = NavType.IntType },
                navArgument("cid") { type = NavType.IntType; defaultValue = -1 },
            ),
        ) { entry ->
            val aid = entry.arguments?.getInt("aid") ?: 0
            val cid = entry.arguments?.getInt("cid") ?: -1
            ReaderScreen(
                aid = aid,
                targetCid = cid.takeIf { it > 0 },
                onExit = { navController.popBackStack() },
            )
        }
    }
}
