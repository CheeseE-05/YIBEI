package com.brewkin.beanbook.ui

import android.content.ClipData
import android.content.Intent
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.core.content.FileProvider
import com.brewkin.beanbook.MainViewModel
import com.brewkin.beanbook.ui.components.BeanIcon
import com.brewkin.beanbook.ui.components.BookIcon
import com.brewkin.beanbook.ui.components.ChartIcon
import com.brewkin.beanbook.ui.components.CupIcon
import com.brewkin.beanbook.ui.components.HyperTabBar
import com.brewkin.beanbook.ui.components.HyperTabItem
import com.brewkin.beanbook.ui.components.HomeIcon
import com.brewkin.beanbook.ui.screens.BeanDetailScreen
import com.brewkin.beanbook.ui.screens.BeanEditScreen
import com.brewkin.beanbook.ui.screens.BeansScreen
import com.brewkin.beanbook.ui.screens.BookletScreen
import com.brewkin.beanbook.ui.screens.PromoCardScreen
import com.brewkin.beanbook.ui.screens.SettingsScreen
import com.brewkin.beanbook.ui.screens.HomeScreen
import com.brewkin.beanbook.ui.screens.MakerScreen
import com.brewkin.beanbook.ui.screens.RecipeDetailScreen
import com.brewkin.beanbook.ui.screens.StatsScreen
import com.brewkin.beanbook.ui.theme.BrewColors
import kotlinx.coroutines.delay

private val rootRoutes = setOf("home", "beans", "maker", "booklet", "stats")

/** 悬浮胶囊里的 4 个 tab；「调一杯」做成胶囊右侧的绿色圆形动作按钮（参考 HyperOS 联系人 App）。 */
private val rootTabs = listOf(
    HyperTabItem("home", "首页") { modifier, color -> HomeIcon(modifier, color) },
    HyperTabItem("beans", "豆子") { modifier, color -> BeanIcon(modifier, color) },
    HyperTabItem("booklet", "菜单") { modifier, color -> BookIcon(modifier, color) },
    HyperTabItem("stats", "统计") { modifier, color -> ChartIcon(modifier, color) }
)

@Composable
fun BeanBookApp(viewModel: MainViewModel = viewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val draft by viewModel.draft.collectAsStateWithLifecycle()
    val editingRecipe by viewModel.editingRecipe.collectAsStateWithLifecycle()
    val navController = rememberNavController()
    val context = LocalContext.current
    val backStack by navController.currentBackStackEntryAsState()
    val route = backStack?.destination?.route
    val showBottomBar = route in rootRoutes
    val imageFile: (com.brewkin.beanbook.data.Bean) -> java.io.File? = remember(viewModel) {
        { bean -> viewModel.beanImageFile(bean.imagePath) }
    }

    // 截图/验证用：携带 promoDebug intent extra 启动时直接进入第一杯的卡片编辑器。
    val debugOpenCard = remember {
        ((context as? android.app.Activity)?.intent)?.getStringExtra("promoDebug") != null
    }
    LaunchedEffect(state.recipes.size) {
        if (!debugOpenCard) return@LaunchedEffect
        val first = state.recipes.firstOrNull() ?: return@LaunchedEffect
        delay(400)
        val route = navController.currentDestination?.route
        if (route == null || route == "home") {
            navController.navigate("card/${first.id}")
        }
    }

    fun switchTab(target: String) {
        navController.navigate(target) {
            popUpTo(navController.graph.findStartDestination().id) {
                saveState = true
            }
            launchSingleTop = true
            restoreState = true
        }
    }

    Box(
        Modifier
            .fillMaxSize()
            .background(BrewColors.Paper)
    ) {
        NavHost(
            navController = navController,
            startDestination = "home",
            modifier = Modifier.fillMaxSize(),
            enterTransition = {
                fadeIn(tween(220)) + slideInHorizontally(tween(260)) { it / 14 }
            },
            exitTransition = { fadeOut(tween(140)) },
            popEnterTransition = {
                fadeIn(tween(220)) + slideInHorizontally(tween(260)) { -it / 14 }
            },
            popExitTransition = { fadeOut(tween(140)) }
        ) {
            composable("home") {
                HomeScreen(
                    state = state,
                    imageFile = imageFile,
                    onOpenBean = { navController.navigate("bean/$it") },
                    onOpenRecipe = { navController.navigate("recipe/$it") },
                    onSeeBeans = { switchTab("beans") },
                    onSeeBooklet = { switchTab("booklet") },
                    onAddBean = { navController.navigate("beanEdit") },
                    onMake = { switchTab("maker") },
                    onStats = { switchTab("stats") }
                )
            }
            composable("beans") {
                BeansScreen(
                    state = state,
                    imageFile = imageFile,
                    onAddBean = { navController.navigate("beanEdit") },
                    onOpenBean = { navController.navigate("bean/$it") }
                )
            }
            composable("maker") {
                MakerScreen(
                    state = state,
                    draft = draft,
                    editingRecipe = editingRecipe,
                    imageFile = imageFile,
                    onCancelEdit = viewModel::cancelEditRecipe,
                    onPour = { spec, amount -> viewModel.pour(spec, amount) },
                    onUndo = viewModel::undo,
                    onClear = viewModel::clearCup,
                    onStir = viewModel::toggleStir,
                    onSelectBean = viewModel::selectBean,
                    onAddCustom = viewModel::addCustomIngredient,
                    onDeleteCustom = viewModel::deleteCustomIngredient,
                    onEditLayer = viewModel::editLayer,
                    onRemoveLayer = viewModel::removeLayer,
                    onSave = { name, beanId, rating, notes, mouthfeel, sweetness, grams, price, onSaved ->
                        if (editingRecipe != null) {
                            // 编辑态：覆盖原配方，不新增一杯。
                            viewModel.saveEditedRecipe(
                                name = name,
                                beanId = beanId,
                                rating = rating,
                                notes = notes,
                                mouthfeel = mouthfeel,
                                sweetness = sweetness,
                                beanGrams = grams,
                                price = price,
                                onSaved = onSaved
                            )
                        } else {
                            viewModel.saveRecipe(
                                name = name,
                                beanId = beanId,
                                rating = rating,
                                notes = notes,
                                mouthfeel = mouthfeel,
                                sweetness = sweetness,
                                beanGrams = grams,
                                price = price,
                                onSaved = onSaved
                            )
                        }
                    },
                    onOpenRecipe = { navController.navigate("recipe/$it") }
                )
            }
            composable("booklet") {
                BookletScreen(
                    state = state,
                    onOpenSettings = { navController.navigate("settings") },
                    onOpenRecipe = { navController.navigate("recipe/$it") },
                    onOpenCard = { navController.navigate("card/$it") },
                    onMake = { switchTab("maker") }
                )
            }
            composable("settings") {
                SettingsScreen(
                    state = state,
                    settings = viewModel.settings,
                    onBack = { navController.popBackStack() },
                    onExportDatabase = { uri, done -> viewModel.exportDatabase(uri, done) },
                    onImportDatabase = { uri, mode, done -> viewModel.importDatabase(uri, mode, done) },
                    onImportRecipe = { uri, done -> viewModel.importRecipeFile(uri, done) }
                )
            }
            composable("stats") {
                StatsScreen(
                    state = state,
                    onMake = { switchTab("maker") },
                    onSeeBeans = { switchTab("beans") },
                    onOpenSettings = { navController.navigate("settings") }
                )
            }
            composable("beanEdit") {
                BeanEditScreen(
                    existing = null,
                    existingImage = null,
                    onBack = { navController.popBackStack() },
                    onSave = { bean, bitmap ->
                        viewModel.saveBean(bean, bitmap) { navController.popBackStack() }
                    }
                )
            }
            composable(
                route = "beanEdit/{beanId}",
                arguments = listOf(navArgument("beanId") { type = NavType.StringType })
            ) { entry ->
                val bean = state.beanById(entry.arguments?.getString("beanId"))
                if (bean == null) {
                    MissingScreen(onBack = { navController.popBackStack() })
                } else {
                    BeanEditScreen(
                        existing = bean,
                        existingImage = imageFile(bean),
                        onBack = { navController.popBackStack() },
                        onSave = { updated, bitmap ->
                            viewModel.saveBean(updated, bitmap) { navController.popBackStack() }
                        }
                    )
                }
            }
            composable(
                route = "bean/{beanId}",
                arguments = listOf(navArgument("beanId") { type = NavType.StringType })
            ) { entry ->
                val bean = state.beanById(entry.arguments?.getString("beanId"))
                if (bean == null) {
                    MissingScreen(onBack = { navController.popBackStack() })
                } else {
                    BeanDetailScreen(
                        bean = bean,
                        recipes = state.recipes,
                        imageFile = imageFile(bean),
                        onBack = { navController.popBackStack() },
                        onEdit = { navController.navigate("beanEdit/${bean.id}") },
                        onMake = {
                            viewModel.previewBeanForMaker(bean)
                            switchTab("maker")
                        },
                        onOpenRecipe = { navController.navigate("recipe/$it") },
                        onDelete = {
                            viewModel.deleteBean(bean.id)
                            navController.popBackStack()
                        }
                    )
                }
            }
            composable(
                route = "recipe/{recipeId}",
                arguments = listOf(navArgument("recipeId") { type = NavType.StringType })
            ) { entry ->
                val recipe = state.recipeById(entry.arguments?.getString("recipeId"))
                if (recipe == null) {
                    MissingScreen(onBack = { navController.popBackStack() })
                } else {
                    RecipeDetailScreen(
                        recipe = recipe,
                        bean = state.beanById(recipe.beanId),
                        onBack = { navController.popBackStack() },
                        onEdit = {
                            viewModel.beginEditRecipe(recipe)
                            switchTab("maker")
                        },
                        onMakeCard = { navController.navigate("card/${recipe.id}") },
                        onRepeat = {
                            viewModel.loadIntoMaker(recipe)
                            switchTab("maker")
                        },
                        onOpenBean = { navController.navigate("bean/$it") },
                        onShareFile = {
                            viewModel.createRecipeShareFile(recipe.id) { result ->
                                result.onSuccess { file ->
                                    runCatching {
                                        val uri = FileProvider.getUriForFile(
                                            context,
                                            "${context.packageName}.fileprovider",
                                            file
                                        )
                                        val share = Intent(Intent.ACTION_SEND).apply {
                                            type = "application/json"
                                            putExtra(Intent.EXTRA_STREAM, uri)
                                            clipData = ClipData.newRawUri("YIBEI recipe", uri)
                                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                        }
                                        context.startActivity(Intent.createChooser(share, "发送配方文件"))
                                    }.onFailure {
                                        Toast.makeText(context, "发送失败：${it.message}", Toast.LENGTH_LONG).show()
                                    }
                                }.onFailure {
                                    Toast.makeText(context, it.message ?: "配方导出失败", Toast.LENGTH_LONG).show()
                                }
                            }
                        },
                        onDelete = {
                            viewModel.deleteRecipe(recipe.id)
                            navController.popBackStack()
                        }
                    )
                }
            }
            composable(
                route = "card/{recipeId}",
                arguments = listOf(navArgument("recipeId") { type = NavType.StringType })
            ) { entry ->
                val recipe = state.recipeById(entry.arguments?.getString("recipeId"))
                if (recipe == null) {
                    MissingScreen(onBack = { navController.popBackStack() })
                } else {
                    val cardBean = state.beanById(recipe.beanId)
                    PromoCardScreen(
                        recipe = recipe,
                        bean = cardBean,
                        beanImageFile = cardBean?.let { imageFile(it) },
                        settings = viewModel.settings,
                        onBack = { navController.popBackStack() },
                        onSaveConfig = { card -> viewModel.updateRecipeCard(recipe.id, card) },
                        onUpdatePrice = { price -> viewModel.updateRecipePrice(recipe.id, price) },
                        onSaveCoverImage = { bitmap, done -> viewModel.saveCardCoverImage(bitmap, done) },
                        onLoadCoverFile = { path -> viewModel.cardCoverImageFile(path) },
                        onOpenSettings = { navController.navigate("settings") }
                    )
                }
            }
        }

        AnimatedVisibility(
            visible = showBottomBar,
            enter = fadeIn(tween(160)) + slideInVertically(tween(200)) { it / 4 },
            exit = fadeOut(tween(120)),
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            // 澎湃 OS 4 风格悬浮胶囊导航 + 绿色「调一杯」FAB（参考 HyperOS 联系人 App）。
            HyperTabBar(
                tabs = rootTabs,
                currentRoute = route,
                onNavigate = { switchTab(it) },
                onMake = { switchTab("maker") }
            )
        }
    }
}

@Composable
private fun MissingScreen(onBack: () -> Unit) {
    Box(
        Modifier.fillMaxSize().background(BrewColors.Paper),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("这一页已经不在手边了", style = MaterialTheme.typography.titleLarge, color = BrewColors.Ink)
            Spacer(Modifier.height(8.dp))
            Text(
                "它可能已经被删除。",
                style = MaterialTheme.typography.bodyMedium,
                color = BrewColors.Muted
            )
            Spacer(Modifier.height(18.dp))
            Surface(
                onClick = onBack,
                shape = CircleShape,
                color = BrewColors.Lime
            ) {
                Text(
                    "回到上一页",
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 11.dp),
                    style = MaterialTheme.typography.labelLarge,
                    color = BrewColors.Ink
                )
            }
        }
    }
}
