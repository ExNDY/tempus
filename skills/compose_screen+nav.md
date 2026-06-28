# Инструкция: Навигация и структура экранов в Compose

Данный документ является стандартом проекта для построения навигации и организации кода экранов на Jetpack Compose.

## 1. Системные реализации (Core)

Эти компоненты должны быть добавлены в проект в пакет `org.example.android.utils.navigation`.

### Интерфейс Screen
```kotlin
interface Screen {
    val screenName: String
    val navArgs: List<NamedNavArgument> get() = emptyList()

    @Composable
    fun Content(navController: NavController, args: Bundle?)
}
```

### ScreenNameExtension
```kotlin
package org.example.android.utils.navigation

import org.example.android.utils.navigation.DefaultScreenNameExtension.defaultScreenName

object ScreenNameExtension {
    lateinit var allScreens: List<Screen>

    /**
     * Return screen name from screen list of application
     * @return The string path of default screen name
     */
    @Suppress("UnsafeCallOnNullableType")
    inline fun <reified S : Screen> getScreenName(): String {
        return allScreens.find { it is S }!!.screenName
    }

    /**
     * Return navigation path for screen with required parameters
     *
     * @param params The navigation arguments for screen path
     * @return The string path of default screen name with required parameters of navigation
     */
    fun Screen.screenNameWithParams(vararg params: Any): String {
        return defaultScreenName() + params.joinToString(separator = "/", prefix = "/")
    }

    /**
     * Return navigation path for screen with required and optional parameters
     *
     * @param params The navigation arguments for screen path
     * @param optionalParams The optional arguments that will be used for screen path
     * @return The string path of default screen name with required parameters of navigation,
     * path can contains optional parameters
     */
    fun Screen.screenNameWithOptionalParams(
        params: List<Any>,
        optionalParams: List<Pair<String, Any?>>
    ): String {
        val mandatoryPart: String = if (params.isNotEmpty()) {
            params.joinToString(prefix = "/", separator = "/") { it.toString() }
        } else {
            ""
        }
        val validOptionals: List<Pair<String, Any?>> = optionalParams.filter { it.second != null }
        val optionalPart: String = if (validOptionals.isNotEmpty()) {
            validOptionals.joinToString(prefix = "?", separator = "&") {
                "${it.first}=${it.second}"
            }
        } else {
            ""
        }

        return defaultScreenName() + mandatoryPart + optionalPart
    }

    /**
     * Return navigation path for screen only with optional parameters
     *
     * @param optionalParams The optional arguments that will be used for screen path
     * @return The string path of default screen name with optional parameters of navigation,
     */
    fun Screen.screenNameWithOptionalParams(optionalParams: List<Pair<String, Any>>): String {
        if (optionalParams.isEmpty()) return defaultScreenName()

        val optionalPart: String = optionalParams.joinToString(prefix = "?", separator = "&") {
            "${it.first}=${it.second}"
        }

        return defaultScreenName() + optionalPart
    }
}
```

### NavController Extensions
```kotlin
package org.example.android.utils.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.navigation.NavController
import androidx.navigation.NavGraph.Companion.findStartDestination

/**
 * Заменить весь стек навигации на другой экран, вне
 * зависимости от содержимого текущего бекстека.
 *
 * То есть после такого перехода если юзер нажмет назад - приложение закроется.
 * История обнуляется данным переходом.
 */
fun NavController.replace(route: String) {
    navigate(route) {
        // Очищаем стек до корневого графа. Это надежнее, чем magic number 0.
        popUpTo(graph.id) { inclusive = true }

        // Избегаем создания дубликатов экрана, если случайно вызвали навигацию дважды
        launchSingleTop = true
    }
}

fun NavController.navigateSingleTop(
    screenName: String,
    saveState: Boolean = true,
) {
    navigate(screenName) {
        // Pop up to the start destination of the graph to
        // avoid building up a large stack of destinations
        // on the back stack as users select items
        popUpTo(graph.findStartDestination().id) {
            this.saveState = saveState
        }
        // Avoid multiple copies of the same destination when
        // reselecting the same item
        launchSingleTop = true
        // Restore state when reselecting a previously selected item
        restoreState = true
    }
}

@Suppress("ComposableNaming")
@Composable
fun <T> NavController.observeForResult(key: String, onResult: (T) -> Unit) {
    val savedStateHandle = currentBackStackEntry?.savedStateHandle ?: return

    LaunchedEffect(key, savedStateHandle) {
        // Subscribe for changes of the specified key
        savedStateHandle.getStateFlow<T?>(key, null).collect { result ->
            if (result != null) {
                onResult(result)
                // Remove result to avoid repeating its handling during recomposition
                savedStateHandle.remove<T>(key)
            }
        }
    }
}

fun <T> NavController.saveResultForPreviousScreen(key: String, value: T) {
    this.previousBackStackEntry
        ?.savedStateHandle
        ?.set(key, value)
}

fun <T> NavController.saveResultToCurrentScreen(key: String, value: T) {
    this.currentBackStackEntry
        ?.savedStateHandle
        ?.set(key, value)
}
```

### DefaultScreenNameExtension
```kotlin
package org.example.android.utils.navigation

import org.example.android.utils.navigation.DefaultScreenNameExtension.defaultScreenName

object DefaultScreenNameExtension {
    /**
     * Default realisation of screen name for compose navigation, without parameters
     *
     * @return the simple name of the underlying class
     */
    fun Screen.defaultScreenName(): String = this::class.java.simpleName

    /**
     * Default realisation of screen name for compose navigation, with required parameters
     *
     * @return the string with parameters of navigation like in web: "screenName/argument"
     */
    fun Screen.defaultScreenNameWithParams(vararg params: String): String {
        return defaultScreenName() + params.joinToString(separator = "/", prefix = "/") { "{$it}" }
    }

    fun String.additionParams(vararg params: String): String {
        return this + params.joinToString(separator = "/", prefix = "/") { "{$it}" }
    }

    /**
     * Default realisation of screen name for compose navigation,
     * with required and optional parameters
     *
     * @return the string with parameters of navigation like in web: "screenName/argument" or
     * with optional parameters: "screenName/requiredArgument?optionalArgument={optionalValue}"
     */
    fun Screen.defaultScreenNameWithOptionalParams(
        params: List<String>,
        optionalParams: List<String>,
    ): String {
        val optionals = optionalParams.joinToString { "?$it={$it}" }
        return defaultScreenName() + params.joinToString { "/{$it}" } + optionals
    }

    /**
     * Default realisation of screen name for compose navigation,
     * with optional parameters
     *
     * @return the string with parameters of navigation like in web:
     * "screenName/?optionalArgument={optionalValue}"
     */
    fun Screen.defaultScreenNameWithOptionalParams(vararg optionalParams: String): String {
        val optionals = optionalParams.joinToString { "?$it={$it}" }
        return defaultScreenName() + optionals
    }
}
```

---

## 2. Шаблоны реализации экранов

### А. Обычный экран (без параметров)
```kotlin
object MyScreen : Screen {
    override val screenName: String = defaultScreenName()

    @Composable
    override fun Content(navController: NavController, args: Bundle?) {
        // Получение ViewModel через Koin (см. koin_setup.md)
        val viewModel: MyViewModel = getViewModel { getMyViewModel() }
        
        MyScreenContent(
            state = viewModel.state.collectAsState().value,
            onEvent = viewModel::onEvent
        )
    }
}
```

### Б. Экран с обязательными параметрами
```kotlin
object DetailsScreen : Screen {
    private const val ID = "ID"
    
    override val navArgs = listOf(
        navArgument(ID) { type = NavType.StringType }
    )
    
    override val screenName = defaultScreenNameWithParams(ID)

    // Функция для вызова навигации: navController.navigate(DetailsScreen.route("123"))
    fun route(id: String) = screenNameWithParams(id)

    @Composable
    override fun Content(navController: NavController, args: Bundle?) {
        val id = args?.getString(ID) ?: return
        
        val viewModel: DetailsViewModel = getViewModel { getDetailsViewModel(id) }
        DetailsContent(state = viewModel.state.collectAsState().value)
    }
}
```

### В. Экран с опциональными параметрами
```kotlin
object SearchScreen : Screen {
    private const val QUERY = "query"
    
    override val navArgs = listOf(
        navArgument(QUERY) { 
            type = NavType.StringType
            nullable = true 
        }
    )
    
    override val screenName = defaultScreenNameWithOptionalParams(QUERY)

    fun route(query: String? = null) = screenNameWithOptionalParams(
        optionalParams = listOf(QUERY to query)
    )

    @Composable
    override fun Content(navController: NavController, args: Bundle?) {
        val query = args?.getString(QUERY)
        val viewModel: SearchViewModel = getViewModel { getSearchViewModel(query) }
        SearchContent(state = viewModel.state.collectAsState().value)
    }
}
```

---

## 3. Правила организации кода (UI Structure)

При создании или переводе экрана необходимо придерживаться структуры **пакета фичи**:

1.  **`FeatureScreen.kt` (объект)**:
    *   Реализует интерфейс `Screen`.
    *   Содержит ключи аргументов (`const val`).
    *   Описывает `navArgs` и `screenName`.
    *   В функции `Content`: получает аргументы из `Bundle`, инициализирует `ViewModel` (через Koin `getViewModel`), вызывает `FeatureContent`.

2.  **`FeatureContent.kt` (Composable функции)**:
    *   Основная функция `FeatureContent(state, onEvent)`.
    *   Не должна напрямую работать с `ViewModel`.
    *   Только отрисовка UI на основе переданного состояния.

3.  **`FeatureViewModel.kt`**:
    *   Бизнес-логика, хранение `StateFlow`.

---

## 4. Настройка RootContainer

Все самостоятельные экраны ОБЯЗАТЕЛЬНО должны быть добавлены в список экранов своего host-графа.
Вложенные части экрана в эти списки не добавляются: они остаются обычными `@Composable`
функциями внутри родительского экрана.

```kotlin
val authScreens: List<Screen> = listOf(
    WelcomeScreen,
    LoginScreen,
)

val mainScreens: List<Screen> = listOf(
    HomeScreen,
    LibraryScreen,
    DownloadScreen,
    DetailsScreen,
    SearchScreen,
)

enum class Hosts(val route: String) {
    Auth("auth"),
    Main("main"),
}

@Suppress("LongMethod")
@Composable
fun RootContainer() {
    val navController: NavHostController = rememberNavController()

    var bottomMenuConfig: BottomMenuConfig by remember {
        mutableStateOf(BottomMenuConfig.Hidden)
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        bottomBar = {
            (bottomMenuConfig as? BottomMenuConfig.Visible)?.let { config ->
                BottomBar(
                    navController = navController,
                    selectedItem = config.bottomItem,
                )
            }
        },
    ) { padding ->
        NavHost(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .consumeWindowInsets(padding)
                .windowInsetsPadding(
                    WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal)
                ),
            navController = navController,
            startDestination = Hosts.Auth.route,
            enterTransition = { fadeIn(animationSpec = tween(TRANSITION_ANIMATION_DURATION)) },
            exitTransition = { fadeOut(animationSpec = tween(TRANSITION_ANIMATION_DURATION)) },
        ) {
            navigation(
                route = Hosts.Auth.route,
                startDestination = WelcomeScreen.screenName,
            ) {
                authScreens.forEach { screen ->
                    composableScreen(
                        screen = screen,
                        navController = navController,
                        updateBottomMenuConfig = { bottomMenuConfig = it },
                    )
                }
            }

            navigation(
                route = Hosts.Main.route,
                startDestination = HomeScreen.screenName,
            ) {
                mainScreens.forEach { screen ->
                    composableScreen(
                        screen = screen,
                        navController = navController,
                        updateBottomMenuConfig = { bottomMenuConfig = it },
                    )
                }
            }
        }
    }
}
```

---

## 5. Чек-лист при переводе экрана на Compose

1. [ ] Понять тип элемента: самостоятельный экран, bottom sheet/dialog или вложенный компонент.
2. [ ] Для самостоятельного экрана создать `object ScreenName : Screen.DefaultScreen` в пакете фичи.
3. [ ] Для bottom sheet/dialog создать `object ScreenName : Screen.BottomSheetScreen`.
4. [ ] Для вложенного компонента НЕ создавать `Screen`, а оставить обычный `@Composable` внутри родительского экрана.
5. [ ] Определить `bottomMenuConfig()`: `Visible(...)` только для экранов нижнего меню, иначе `Hidden`.
6. [ ] Определить константы для ключей аргументов.
7. [ ] Настроить `navArgs` (указать типы `NavType`, `nullable`).
8. [ ] Выбрать правильную функцию формирования `screenName` (`defaultScreenNameWithParams` и т.д.).
9. [ ] Написать статическую функцию `screenName(...)` или `route(...)` для вызова из других мест.
10. [ ] В `Content`: получить аргументы из `Bundle`, создать `ViewModel`, подписаться на actions, вызвать `FeatureContent`.
11. [ ] Реализовать UI в отдельной composable-функции `FeatureContent(...)` без прямой зависимости от `NavController`.
12. [ ] Добавить объект экрана в список нужного host-графа (`authScreens`, `mainScreens` и т.д.).
13. [ ] Для переходов использовать `navController.navigate(TargetScreen.screenName(args))` или `navigateSingleTop(...)`.

---

## 6. План миграции проекта на чистый Compose

### Цель

Убрать XML navigation и Fragment-обертки из основного пользовательского flow.
Самостоятельные экраны становятся объектами `Screen.DefaultScreen` / `Screen.BottomSheetScreen`,
а вложенные части остаются composable-компонентами внутри родительского экрана.

### Правило классификации

**Самостоятельный экран**:

*   Может быть открыт из другого экрана, bottom bar, drawer, deep link или back stack.
*   Имеет собственный route и, при необходимости, nav arguments.
*   Реализуется как `object FeatureScreen : Screen.DefaultScreen`.

**Bottom sheet/dialog destination**:

*   Открывается поверх текущего экрана через navigation.
*   Имеет собственный route и аргументы.
*   Реализуется как `object FeatureBottomSheetScreen : Screen.BottomSheetScreen`.

**Вложенный компонент**:

*   Не должен жить отдельно в back stack.
*   Не открывается напрямую как route.
*   Используется только внутри основного экрана.
*   Реализуется как обычная `@Composable` функция.

Примеры вложенных компонентов:

*   `HomeTabMusicScreen`, `HomeTabPodcastScreen`, `HomeTabRadioScreen` внутри `HomeScreen`.
*   `PlayerCoverScreen`, `PlayerLyricsScreen`, `PlayerQueueScreen`, `PlayerControllerScreen` внутри `PlayerScreen`.
*   `SettingsScreenContent`, `SettingsSectionContent`, `SettingsDialogContent` внутри `SettingsScreen`.
*   Общие компоненты из `ui/components`.

### Порядок миграции

1.  **RootContainer**
    *   Создать Compose root с `rememberNavController`, `Scaffold`, `NavHost` и host-графами.
    *   Вынести списки экранов в `authScreens`, `mainScreens` и отдельные списки при необходимости.
    *   Инициализировать `ScreenNameExtension.allScreens` объединенным списком всех route-экранов.
    *   Оставить нижнюю навигацию глобальной на уровне root через `BottomMenuConfig`: это нормальная точка управления для широкого приложения.

2.  **Top-level flow**
    *   Перевести `Landing`, `Login`, `Home`, `Library`, `Download`.
    *   Для `Home`, `Library`, `Download` вернуть `BottomMenuConfig.Visible(...)`.
    *   Для auth-экранов вернуть `BottomMenuConfig.Hidden`.
    *   Заменить `goToLogin` / `goFromLogin` на переходы между `Hosts.Auth` и `Hosts.Main` через `replace(...)`.

3.  **Основные самостоятельные страницы**
    *   Перевести catalogue/page/list экраны: album, artist, genre, playlist, podcast, search, settings, equalizer, index, directory.
    *   Все аргументы описывать через `navArgs` и route helper.
    *   В `Content` доставать аргументы из `Bundle`; если обязательный аргумент отсутствует, падать через `error(...)` или уходить назад по принятому для проекта правилу.

4.  **Bottom sheets и dialogs**
    *   Перевести bottom sheet dialogs на `Screen.BottomSheetScreen`.
    *   Использовать `onClose` из `Content(navController, args, onClose)`.
    *   Старые `FragmentResultListener` заменить на `saveResultForPreviousScreen` / `observeForResult`.

5.  **Удаление старого слоя**
    *   После переноса flow удалить соответствующие Fragment-обертки.
    *   Удалить destinations из `nav_graph.xml`.
    *   После полного переноса заменить `FragmentContainerView` в `activity_main.xml` на Compose root или убрать layout целиком через `setContent`.
    *   Удалить `NavigationHelper` / `NavigationController`, когда не останется вызовов.

### Шаблон самостоятельного экрана

```kotlin
object HomeScreen : Screen.DefaultScreen {

    override val screenName: String = defaultScreenName()

    override fun bottomMenuConfig(): BottomMenuConfig {
        return BottomMenuConfig.Visible(BottomBarItems.home)
    }

    @Composable
    override fun Content(navController: NavController, args: Bundle?) {
        val viewModel: HomeViewModel = getViewModel {
            getHomeViewModel().apply { onStart() }
        }

        viewModel.actions.observeAsActions { action ->
            when (action) {
                is HomeViewModel.Action.RouteToDetails -> {
                    navController.navigate(
                        DetailsScreen.screenName(action.id)
                    )
                }
            }
        }

        HomeContent(
            screenState = viewModel.screenState.collectValue(),
            onDetailsClick = viewModel::routeToDetails,
        )
    }
}
```

### Шаблон экрана с аргументами

```kotlin
object DetailsScreen : Screen.DefaultScreen {
    private const val ID = "id"

    override val navArgs: List<NamedNavArgument> = listOf(
        navArgument(ID) { type = NavType.StringType }
    )

    override val screenName: String = defaultScreenNameWithParams(ID)

    fun screenName(id: String): String = screenNameWithParams(id)

    override fun bottomMenuConfig(): BottomMenuConfig = BottomMenuConfig.Hidden

    @Composable
    override fun Content(navController: NavController, args: Bundle?) {
        val id: String = args?.getString(ID)
            ?: error("Invalid navigation argument: id is NULL")

        val viewModel: DetailsViewModel = getViewModel {
            getDetailsViewModel(id).apply { onStart() }
        }

        DetailsContent(
            screenState = viewModel.screenState.collectValue(),
            onBackClick = navController::navigateUp,
        )
    }
}
```

### Что не делать

*   Не создавать отдельный `Screen` для табов, pager pages, секций, toolbar/content/dialog-content composables.
*   Не прокидывать `NavController` в `FeatureContent`: навигация остается в route-level `Content`.
*   Не держать одновременно Fragment navigation и Compose navigation для одного и того же экрана после завершения миграции этого экрана.
