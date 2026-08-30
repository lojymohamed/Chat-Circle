# ChatCircle 💬

ChatCircle is a robust, real-time group chat Android application designed with modern Android development practices. It leverages Firebase for real-time data sync and Room for offline persistence, ensuring a seamless user experience.

## 🚀 Features

- **Secure Authentication**: Email/Password and Google Sign-in integration via Firebase Auth.
- **Dynamic Chat Rooms**: Create new rooms or join existing ones using unique 6-character room codes.
- **Real-time Messaging**: Instant message delivery and receipt using Firestore.
- **Offline Support**: Full offline capability with a local Room database acting as the single source of truth for the UI.
- **Presence Tracking**: Real-time online/offline status indicators for users.
- **Media Sharing**: Seamless image sharing using Firebase Storage.
- **User Profiles**: Customizable profiles including display names and profile pictures.
- **Unread Indicators**: Visual cues for unread messages in chat rooms.

## 🛠 Tech Stack

- **Language**: [Kotlin](https://kotlinlang.org/)
- **UI Framework**: [Jetpack Compose](https://developer.android.com/jetpack/compose) (Modern declarative UI)
- **Dependency Injection**: [Hilt](https://developer.android.com/training/dependency-injection/hilt-android)
- **Architecture**: MVVM (Model-View-ViewModel) + Clean Architecture principles.
- **Backend**:
    - **Firestore**: Real-time NoSQL database for messages and rooms.
    - **Firebase Auth**: User authentication.
    - **Firebase Storage**: Image and media hosting.
- **Local Database**: [Room](https://developer.android.com/training/data-storage/room) (SQLite abstraction for offline caching).
- **Image Loading**: [Coil](https://coil-kt.github.io/coil/)
- **Navigation**: Jetpack Navigation Component.
- **Concurrency**: Kotlin Coroutines & Flow.

## 🏗 Project Structure

The project follows a modular Clean Architecture approach, divided into three main layers:

### 1. Data Layer (`data/`)
- **Repository Impls**: Concrete implementations of domain repositories (e.g., `ChatRepositoryImpl`).
- **Local**: Room Database definitions, DAOs, and Entities.
- **Remote**: Firebase data sources.
- **Mappers**: Logic to convert between Data Entities and Domain Models.

### 2. Domain Layer (`domain/`)
- **Models**: Pure Kotlin data classes representing the core business logic (e.g., `ChatRoom`, `Message`).
- **Repositories**: Interfaces defining the contracts for data operations, keeping the domain layer independent of frameworks.

### 3. UI Layer (`ui/`)
- **Components**: Organized by feature (auth, chat, rooms, profile).
- **ViewModels**: Manage UI state and communicate with the domain layer.
- **Fragments/Compose**: The view layer responsible for rendering the UI.

### 4. Dependency Injection (`di/`)
- Hilt modules for providing Firebase, Database, and Repository instances.

## 🔑 Important Details

- **Single Source of Truth**: The app uses a "Write-through" cache strategy. Firestore updates are synced to the local Room DB in the background, and the UI only observes the local DB.
- **Multi-DB Support**: The local database is scoped per user (`chatcircle_$uid.db`), ensuring data privacy and integrity when multiple users share a device.
- **Room Codes**: Unique, collision-resistant codes are generated for every chat room to facilitate easy joining.

## 👥 Team

- **Lojain Khalil**
- **Rawda Waffaey**
