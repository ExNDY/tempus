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

Все созданные экраны ОБЯЗАТЕЛЬНО должны быть добавлены в список `allScreens`.

```kotlin
val allScreens: List<Screen> = listOf(
    MainScreen,
    DetailsScreen,
    SearchScreen
)

@Composable
fun RootContainer() {
    val navController: NavHostController = rememberNavController()

    Scaffold(
        modifier = Modifier.fillMaxSize(),
    ) { padding ->
        NavHost(
            modifier = Modifier.padding(padding),
            navController = navController,
            startDestination = getScreenName<MainScreen>()
        ) {
            allScreens.forEach { screen ->
                composable(
                    route = screen.screenName,
                    arguments = screen.navArgs
                ) { backStackEntry ->
                    screen.Content(
                        navController = navController,
                        args = backStackEntry.arguments
                    )
                }
            }
        }
    }
}
```

---

## 5. Чек-лист при переводе экрана на Compose

1. [ ] Создать `object ScreenName : Screen` в пакете фичи.
2. [ ] Определить константы для ключей аргументов.
3. [ ] Настроить `navArgs` (указать типы `NavType`, `nullable`).
4. [ ] Выбрать правильную функцию формирования `screenName` (`defaultScreenNameWithParams` и т.д.).
5. [ ] Написать статическую функцию `route(...)` для вызова из других мест.
6. [ ] Реализовать UI в отдельном файле `...Content.kt`.
7. [ ] Добавить объект экрана в глобальный список `allScreens`.
8. [ ] Вызвать `navController.navigate(TargetScreen.route(args))` для перехода.
