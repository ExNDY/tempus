# Moko MVVM and Flow migration notes

This document is the handoff from stage 1. Continue the migration by following the patterns below instead of redesigning the approach.

## Current baseline

- The project is Android-only right now. Do not create an `mpp-library` module during the next stage.
- `kotlin-parcelize` is intentionally absent. Do not bring it back.
- Koin starts through `startDI { ... }` in `App.kt`.
- Moko dependencies are connected in `app/build.gradle`.
- `UiText` uses moko-resources `StringDesc` through `String.desc()`.
- `UiEvent` is the first shared event contract for messages and one-shot UI effects.

## ViewModel pattern

- New or migrated ViewModels should extend `dev.icerock.moko.mvvm.viewmodel.ViewModel`.
- Prefer constructor DI for repositories.
- Temporary no-arg constructors are allowed only as a bridge for legacy `ViewModelProvider(...)` call sites.
- Do not add `Application`, `Context`, `Activity`, `Fragment`, `Dialog`, or `LifecycleOwner` to ViewModel constructors or public APIs.
- Long-lived screen state should be exposed as `StateFlow`.
- One-shot UI effects should be exposed as `SharedFlow<UiEvent>`.

## Koin pattern

- Keep module assembly inside `startDI(...)`.
- Register dependencies in Koin modules, then inject them through constructors.
- Add typed Koin extensions for migrated ViewModels when useful for future platform entry points.
- Existing AndroidX `viewModel { ... }` registrations are still present as a transition bridge. Move to the newer Koin constructor DSL during the next cleanup wave.

## Repository pattern

- One-shot operations should become `suspend fun`.
- Observed data should become `Flow<T>`.
- Repository public APIs should not return `MutableLiveData`.
- Repository public APIs should not accept `LifecycleOwner`, `Fragment`, `Activity`, `Dialog`, or `Context`.
- Temporary `LiveData` bridges can stay close to legacy Java UI, but the main repository API should be suspend/Flow-first.

## Stage 1 pilots

- `LoginViewModel` now extends moko `ViewModel` and receives `ServerRepository`.
- `RadioEditorViewModel` now extends moko `ViewModel`, receives `RadioRepository`, and emits `UiEvent.ShowMessage(UiText)`.
- `SearchViewModel` is now Kotlin/moko and no longer exposes a `SearchFragment` parameter.
- `SearchingRepository` has suspend-first search APIs and no longer depends on `SearchFragment`.
- `SearchFragment` still uses LiveData bridges, but it no longer passes itself into the ViewModel/repository.

## Continue with stage 2

- Replace remaining `ViewModelProvider(...)` call sites with Koin accessors/delegates or keep no-arg bridge constructors only until each screen is migrated.
- Convert Java ViewModels to Kotlin in small batches.
- Remove `AndroidViewModel` from every ViewModel.
- Move repository `MutableLiveData` APIs to `suspend fun` or `Flow`.
- Replace UI `observe(...)` usage with Flow collection after the corresponding ViewModel state is converted.
- Remove `mvvm-livedata` only after all temporary LiveData bridges are gone.

## Verification commands

- `./gradlew :app:compileTempusDebugKotlin`
- `./gradlew :app:assembleTempusDebug`
- `./gradlew :app:testTempusDebugUnitTest`

## Stage 1 verification status

- `./gradlew :app:compileTempusDebugKotlin` passes.
- `./gradlew :app:assembleTempusDebug` passes.
- `./gradlew :app:testTempusDebugUnitTest` currently fails in `BaseSessionCallbackTest` because `BaseSessionCallback` calls `App.get(...)` before Koin is started in the unit test process. This is outside the stage 1 ViewModel pilot and should be fixed by either injecting that dependency into `BaseSessionCallback` or starting a minimal Koin context in the test setup.
