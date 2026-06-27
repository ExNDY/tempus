# Koin DI Setup and Usage

В проекте используется **Koin** для внедрения зависимостей (Dependency Injection). Основная конфигурация находится в модуле `mpp-library`.

## 1. Конфигурация в Shared модуле

Точка входа для инициализации DI на обеих платформах — функция `startDI`.

### Определение модулей (`mpp-library`)
Пример типичного модуля фичи (`featureAuthModule.kt`):

```kotlin
val featureAuthModule = module {
    // Регистрация ViewModel
    factoryOf(::SignInViewModel)
    factoryOf(::AuthCodeViewModel)
    
    // Регистрация репозиториев
    singleOf(::AuthRepositoryImpl) bind AuthRepository::class
}
```

### Расширения (Extensions) для платформ
Для удобного доступа к зависимостям (особенно на iOS) используются extension-функции к объекту `Koin`:

```kotlin
fun Koin.getSignInViewModel(): SignInViewModel {
    return get<SignInViewModel>()
}

fun Koin.getAuthCodeViewModel(phone: String?, email: String?): AuthCodeViewModel {
    return get<AuthCodeViewModel> {
        parametersOf(phone, email)
    }
}
```

### Инициализация в CommonMain
В файле `Koin.kt` происходит сборка всех модулей:

```kotlin
fun startDI(
    baseUrl: String,
    // ... другие параметры
    appDeclaration: KoinAppDeclaration? = null
): KoinApplication {
    return startKoin {
        modules(
            listOf(
                platformModule,
                networkModule,
                featureAuthModule,
                // ... другие модули
            )
        )
        appDeclaration?.invoke(this)
    }
}
```

---

## 2. Настройка на Android

### Инициализация (`MainApplication.kt`)
В Android-приложении `startDI` вызывается в методе `onCreate`:

```kotlin
class MainApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        
        startDI(
            baseUrl = BuildConfig.BASE_URL,
            // ...
        ) {
            androidLogger()
            androidContext(this@MainApplication)
        }
    }
}
```

### Использование в Compose
Для получения ViewModel используется кастомная функция `getViewModel`, которая корректно работает с `ViewModelStoreOwner` и жизненным циклом Compose:

```kotlin
@Composable
override fun Content(navController: NavController, args: Bundle?) {
    // Получение ViewModel через Koin extension
    val viewModel: SignInViewModel = getViewModel { getSignInViewModel().apply { onStart() } }
    
    // ... UI код
}
```

---

## 3. Настройка на iOS

### Инициализация (`KoinExtension.swift`)
На стороне iOS создается обертка над инстансом Koin:

```swift
extension Koin {
    static var instance: Koin {
        return koinInstance
    }

    static func setup() {
        let koinApp: KoinApplication = KoinKt.startDI(
            baseUrl: AppEnvironment.Keys.serverBaseUrl.value(),
            // ...
        )
        koinInstance = koinApp.koin
    }
}
```

### Использование в SwiftUI
Для интеграции с жизненным циклом SwiftUI используется Property Wrapper `@ViewModelWrapper`, который вызывает `onCleared()` при уничтожении View:

```swift
struct SignInScreen: View {
    // Использование враппера и расширения Koin
    @ViewModelWrapper private var viewModel: SignInViewModel = Koin.instance.getSignInViewModel()

    var body: some View {
        VStack {
            // ... UI код
        }
    }
}
```

---

## 4. Использование в Legacy компонентах

Если в проекте используются старые подходы (XML, Fragments на Android или ViewControllers на iOS), используйте следующие паттерны.

### Android (Fragments / Activities / XML)

Для получения зависимостей в классических Android-компонентах используйте стандартные делегаты Koin:

```kotlin
class MyFragment : Fragment(R.layout.fragment_my) {
    // Получение ViewModel (стандартный делегат Koin)
    private val viewModel: MyViewModel by viewModel()
    
    // Получение обычной зависимости
    private val analytics: AnalyticsAdapter by inject()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        // ... использование viewModel
    }
}
```

### iOS (ViewControllers)

В `UIViewController` зависимости инициализируются вручную через `Koin.instance`. При использовании ViewModels важно не забывать вручную управлять их жизненным циклом, если не используется специальный контейнер.

```swift
class MyViewController: UIViewController {
    // Получение зависимости через инстанс Koin
    private let viewModel: MyViewModel = Koin.instance.getMyViewModel()
    private let analytics: AnalyticsAdapter = Koin.instance.get()

    override func viewDidLoad() {
        super.viewDidLoad()
        // ... привязка данных (binding)
    }

    deinit {
        // ВАЖНО: вручную вызываем onCleared, если ViewModel не управляется контейнером
        viewModel.onCleared()
    }
}
```

---

## Ключевые компоненты
- **`startDI`**: Общая функция инициализации в Kotlin.
- **`Koin.get...ViewModel()`**: Extension-функции для типизированного доступа к зависимостям.
- **`getViewModel { ... }`**: Android-функция (Compose) для связи Koin и ViewModel Lifecycle.
- **`@ViewModelWrapper`**: iOS-обертка для управления жизненным циклом ViewModel.
