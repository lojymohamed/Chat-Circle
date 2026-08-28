# AGENTS.md — ChatCircle Project Guide

## Project Overview

ChatCircle is a real-time **room-based group chat** Android application built with Kotlin. Users can create or join chat rooms and exchange messages in real time. The app features email/password and Google Sign-In authentication, all backed by Firebase services.

## Tech Stack

| Layer              | Technology                                                        |
| ------------------ | ----------------------------------------------------------------- |
| Language           | Kotlin                                                            |
| UI Framework       | XML Layouts + ViewBinding                                         |
| UI Components      | Material Components for Android (`material:1.12.0`)               |
| Architecture       | 3-Layer Clean Architecture (UI → Domain → Data) + MVVM            |
| DI Framework       | Dagger Hilt 2.56.2 (with KSP compiler)                           |
| Auth               | Firebase Authentication (email/password + Google via CredentialManager) |
| Database           | Cloud Firestore                                                   |
| Navigation         | AndroidX Navigation Component (Fragments + SafeArgs)              |
| Async              | Kotlin Coroutines + Flow / StateFlow / SharedFlow                 |
| Build System       | Gradle 8.13 (Kotlin DSL) + Version Catalog (`libs.versions.toml`) |
| Java/JVM Target    | 17                                                                |
| Kotlin Version     | 2.2.21                                                            |
| AGP Version        | 8.13.2                                                            |
| Min SDK            | 24 (Android 7.0)                                                  |
| Target/Compile SDK | 34 (Android 14)                                                   |

## Project Structure

```
com.example.chatcircle/
├── ChatCircleApp.kt                       # @HiltAndroidApp Application class
├── MainActivity.kt                        # Single @AndroidEntryPoint Activity hosting NavHostFragment
│
├── domain/                                # Domain Layer (pure Kotlin, no Android framework deps)
│   ├── model/
│   │   ├── User.kt                        # uid, displayName, email, photoUrl, isOnline
│   │   ├── ChatRoom.kt                    # id, name, memberIds, lastMessage, timestamp
│   │   └── Message.kt                     # id, senderId, senderName, text, imageUrl, timestamp
│   ├── repository/                        # Repository interfaces (contracts)
│   │   ├── AuthRepository.kt
│   │   ├── ChatRepository.kt
│   │   ├── ChatRoomRepository.kt
│   │   ├── MessageRepository.kt
│   │   └── UserRepository.kt
│   └── usecase/auth/
│       ├── SignInUseCase.kt               # Input validation + sign-in execution
│       ├── SignUpUseCase.kt               # Input validation (password >= 6) + sign-up
│       └── SignInWithGoogleUseCase.kt     # Google ID token sign-in
│
├── data/                                  # Data Layer (Firebase implementations)
│   ├── mapper/
│   │   └── UserMapper.kt                 # FirebaseUser → domain User mapping
│   ├── remote/
│   │   └── FirebaseAuthDataSource.kt      # FirebaseAuth wrapper with coroutines (.await())
│   └── repository/
│       ├── AuthRepositoryImpl.kt          # Auth contract implementation
│       ├── ChatRepositoryImpl.kt          # Firestore messaging (subcollection pattern)
│       └── ChatRoomRepositoryImpl.kt      # Firestore room management
│
├── di/                                    # Hilt Dependency Injection Modules
│   ├── FirebaseModule.kt                  # Provides FirebaseAuth, FirebaseAuthDataSource
│   ├── RepositoryModule.kt                # Provides FirebaseFirestore, all repository bindings
│   └── UseCaseModule.kt                   # Provides auth use cases
│
└── ui/                                    # Presentation Layer (MVVM + ViewBinding)
    ├── auth/
    │   ├── AuthUiState.kt                 # Sealed class: Idle, Loading, Success(User), Error(msg)
    │   ├── login/
    │   │   ├── LoginFragment.kt           # Login UI + Google Sign-In via CredentialManager
    │   │   └── LoginViewModel.kt          # @HiltViewModel for email/pwd & Google auth
    │   └── register/
    │       ├── RegisterFragment.kt        # Registration UI
    │       └── RegisterViewModel.kt       # @HiltViewModel for sign-up + password confirmation
    ├── rooms/
    │   ├── ChatRoomFragment.kt            # Room creation & joining UI
    │   └── ChatRoomViewModel.kt           # @HiltViewModel with ChatRoomUiState + navigation events
    └── chat/
        ├── ChatFragment.kt                # Real-time room chat with RecyclerView
        ├── ChatViewModel.kt               # @HiltViewModel using SavedStateHandle (roomId, roomName)
        └── MessageAdapter.kt             # ListAdapter with sent/received ViewHolders + DiffUtil
```

## Navigation Graph

```
loginFragment (startDestination)
    ├── → registerFragment (sign up)
    └── → chatRoomFragment (on auth success)

registerFragment
    └── ← pop back to loginFragment

chatRoomFragment
    └── → chatFragment (SafeArgs: roomId, roomName)

chatFragment
    └── ← navigateUp() to chatRoomFragment
```

| Route                     | Fragment           | SafeArgs              |
| ------------------------- | ------------------ | --------------------- |
| `loginFragment`           | LoginFragment      | —                     |
| `registerFragment`        | RegisterFragment   | —                     |
| `chatRoomFragment`        | ChatRoomFragment   | —                     |
| `chatFragment`            | ChatFragment       | `roomId`, `roomName`  |

## Architecture Diagram

```
┌─────────────────────────────────────────────────────┐
│  UI Layer (Fragments + ViewBinding + ViewModels)     │
│  • Collects StateFlow via repeatOnLifecycle          │
│  • ViewModels annotated with @HiltViewModel          │
└───────────────────────┬─────────────────────────────┘
                        │
┌───────────────────────▼─────────────────────────────┐
│  Domain Layer (Pure Kotlin)                          │
│  • Models: User, ChatRoom, Message                   │
│  • Repository interfaces (contracts)                 │
│  • Use Cases: SignIn, SignUp, SignInWithGoogle         │
└───────────────────────┬─────────────────────────────┘
                        │
┌───────────────────────▼─────────────────────────────┐
│  Data Layer (Firebase implementations)               │
│  • AuthRepositoryImpl → FirebaseAuthDataSource       │
│  • ChatRepositoryImpl → Firestore subcollections     │
│  • ChatRoomRepositoryImpl → Firestore collection     │
│  • UserMapper (FirebaseUser → domain model)          │
└─────────────────────────────────────────────────────┘
```

## Firestore Data Model

```
chatRooms/{roomId}
├── id: String
├── name: String
├── memberIds: List<String>         # UIDs of members (arrayUnion on join)
├── lastMessage: String
├── timestamp: Timestamp
└── messages/{messageId}            # Subcollection
    ├── id: String
    ├── senderId: String
    ├── senderName: String
    ├── text: String
    ├── imageUrl: String?           # Defined but not yet implemented
    └── timestamp: Timestamp
```

## Build & Run

```bash
# Build debug APK
./gradlew assembleDebug

# Install on connected device/emulator
./gradlew installDebug

# Run unit tests
./gradlew test

# Run instrumented tests
./gradlew connectedAndroidTest

# Clean build
./gradlew clean assembleDebug
```

> **Required:** A `google-services.json` file must be present in `app/` for Firebase to function. This file is excluded from version control.

## Key Conventions

- **Kotlin only** — no Java code in this project.
- **XML layouts + ViewBinding** — all UI is defined in XML with ViewBinding for type-safe view access. **No Jetpack Compose.**
- **Single Activity, multiple Fragments** — `MainActivity` hosts a `NavHostFragment`; all screens are Fragments.
- **Clean Architecture** — strict 3-layer separation: `ui/` → `domain/` → `data/`. Domain layer has zero Android framework dependencies.
- **Dagger Hilt for DI** — `@HiltAndroidApp` on Application, `@AndroidEntryPoint` on Activity/Fragments, `@HiltViewModel` on ViewModels, `@Module`/`@Provides`/`@Singleton` in `di/` package.
- **SafeArgs for navigation** — type-safe argument passing between Fragments via the Navigation SafeArgs plugin.
- **StateFlow + SharedFlow** — ViewModels expose `StateFlow` for UI state and `SharedFlow` for one-time navigation events.
- **repeatOnLifecycle** — Fragments collect flows using `viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED)`.
- **callbackFlow for Firestore** — real-time Firestore listeners are wrapped in `callbackFlow` for reactive streaming.
- **Material Components** — follow Material Design guidelines using `com.google.android.material` components.
- **CredentialManager** — Google Sign-In uses the modern `androidx.credentials` CredentialManager API (not deprecated `GoogleSignInClient`).

## Permissions

The app declares only one permission in `AndroidManifest.xml`:
- `INTERNET` — required for Firebase communication

## Common Tasks

### Adding a new screen
1. Create an XML layout in `res/layout/`.
2. Create a Fragment in `ui/` that uses ViewBinding and is annotated with `@AndroidEntryPoint`.
3. If state is needed, create a `@HiltViewModel` ViewModel with `StateFlow`.
4. Add the Fragment as a destination in `res/navigation/nav_graph.xml`.
5. Define SafeArgs arguments if the screen requires parameters.
6. Add navigation actions from source Fragments.

### Adding a new Firebase feature
1. Define a domain model in `domain/model/`.
2. Define a repository interface in `domain/repository/`.
3. Implement the repository in `data/repository/` using Firestore or other Firebase SDKs.
4. Bind the implementation in `di/RepositoryModule.kt`.
5. Optionally create a use case in `domain/usecase/` for business logic.
6. If use case is created, provide it in `di/UseCaseModule.kt`.
7. Inject into the relevant ViewModel via constructor injection.

### Adding a new Hilt module
1. Create an `@Module` `@InstallIn(SingletonComponent::class)` object in `di/`.
2. Add `@Provides` `@Singleton` functions for each dependency.

## Planned / Partially-Defined Features

These are defined in interfaces or models but not yet fully implemented:

- **User presence / online status** — `isOnline` field exists in `User.kt` and `UserRepository` interface is defined, but real-time presence sync is not wired.
- **Image sharing in chat** — `imageUrl` field exists in `Message.kt`, but Firebase Storage upload and media pickers are not implemented.
- **Push notifications** — not yet implemented.
- **User profile editing** — `UserRepository` interface exists but no profile UI screen is built.
