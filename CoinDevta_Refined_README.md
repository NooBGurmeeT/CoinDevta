# 🚀 CoinDevta

CoinDevta is a modern Android cryptocurrency tracking application built
using Kotlin and Jetpack Compose.\
It provides real-time cryptocurrency price updates, favorites
management, pinned live notifications, interactive charts, and a
Glance-based home screen widget.

The project follows Clean Architecture principles within a single-module
Android app and is designed for scalability, performance, and
responsiveness (phone, tablet, and foldable devices).

------------------------------------------------------------------------

# 📌 Core Features

## 🔴 Real-Time Crypto Price Streaming

-   Live WebSocket integration (Binance stream)
-   Batched updates using Kotlin Flow to prevent UI over-rendering
-   Symbol-wise latest price tracking
-   24h price direction detection

## ⭐ Favorites Management

-   Mark/unmark coins as favorites
-   Favorites appear in:
    -   Home screen section
    -   Android home screen widget
-   Automatic widget synchronization via DataStore

## 📌 Pinned Coin Notification Service

-   Long press to pin a coin
-   Persistent foreground notification
-   Real-time price updates in notification
-   Automatic stop when unpinned

## 📊 Chart Screen

-   Line chart built with Compose Canvas
-   Dynamic time formatting based on selected interval
-   High/Low indicators
-   Responsive layout for tablets and foldables

## 🔍 Search & Sorting

-   Real-time search filtering
-   Sort options:
    -   Market Cap (Ascending / Descending)
    -   24h Change (Ascending / Descending)
    -   Alphabetical (A--Z / Z--A)

## 📈 Analytics & Logging

-   Screen view tracking
-   Coin click tracking
-   Favorite toggle tracking
-   Pin/unpin tracking
-   Centralized error logging system

------------------------------------------------------------------------

# 🏗 Architecture Overview

CoinDevta follows Clean Architecture inside a single Android app module.

## 1️⃣ Presentation Layer

Responsible for UI and state management.

-   Jetpack Compose UI
-   ViewModels
-   StateFlow & SharedFlow for reactive state
-   Event-based architecture (HomeAction / HomeEffect)

## 2️⃣ Domain Layer

Contains business logic.

-   Data models (Coin, TickerUpdate, ChartPoint)
-   UseCases (LoadInitialData, ToggleFavorite, PinCoin, etc.)
-   Repository interfaces

## 3️⃣ Data Layer

Handles data sources.

-   REST API (initial data)
-   WebSocket stream (live prices)
-   Room Database
-   DataStore (widget preferences)
-   Repository implementations

------------------------------------------------------------------------

# 📂 Complete Project Structure

    CoinDevta/
    │
    ├── app/
    │   ├── src/main/
    │   │   ├── java/com/gurmeet/coindevta/
    │   │   │
    │   │   │   ├── MainActivity.kt
    │   │   │
    │   │   │   ├── analytics/
    │   │   │   │   ├── AnalyticsConstants.kt
    │   │   │   │   ├── AnalyticsEvent.kt
    │   │   │   │   └── AnalyticsLogger.kt
    │   │   │
    │   │   │   ├── data/
    │   │   │   │   ├── local/
    │   │   │   │   │   ├── dao/
    │   │   │   │   │   ├── entity/
    │   │   │   │   │   └── database/
    │   │   │   │   │
    │   │   │   │   ├── remote/
    │   │   │   │   │   ├── api/
    │   │   │   │   │   └── websocket/
    │   │   │   │   │       └── BinanceSocketManager.kt
    │   │   │   │   │
    │   │   │   │   └── repository/
    │   │   │   │
    │   │   │   ├── domain/
    │   │   │   │   ├── model/
    │   │   │   │   │   ├── Coin.kt
    │   │   │   │   │   ├── TickerUpdate.kt
    │   │   │   │   │   └── ChartPoint.kt
    │   │   │   │   │
    │   │   │   │   ├── repository/
    │   │   │   │   └── usecase/
    │   │   │   │       ├── LoadInitialDataUseCase.kt
    │   │   │   │       ├── ObserveCoinsUseCase.kt
    │   │   │   │       ├── ObserveLivePricesUseCase.kt
    │   │   │   │       ├── ToggleFavoriteUseCase.kt
    │   │   │   │       ├── PinCoinUseCase.kt
    │   │   │   │       └── UnPinCoinUseCase.kt
    │   │   │
    │   │   │   ├── presentation/
    │   │   │   │   ├── home/
    │   │   │   │   │   ├── HomeViewModel.kt
    │   │   │   │   │   ├── HomeScreenUi.kt
    │   │   │   │   │   ├── HomeAction.kt
    │   │   │   │   │   ├── HomeEffect.kt
    │   │   │   │   │   └── HomeScreenState.kt
    │   │   │   │   │
    │   │   │   │   └── chart/
    │   │   │   │       ├── ChartActivity.kt
    │   │   │   │       ├── ChartViewModel.kt
    │   │   │   │       └── ChartScreenUi.kt
    │   │   │
    │   │   │   ├── service/
    │   │   │   │   └── PinnedPriceService.kt
    │   │   │
    │   │   │   ├── widget/
    │   │   │   │   ├── CoinWidget.kt
    │   │   │   │   ├── CoinWidgetService.kt
    │   │   │   │   ├── WidgetSyncManager.kt
    │   │   │   │   └── WidgetKeys.kt
    │   │   │
    │   │   │   ├── logger/
    │   │   │   │   ├── ErrorLogger.kt
    │   │   │   │   └── LogLevel.kt
    │   │   │
    │   │   │   └── util/
    │   │   │       ├── Extensions.kt
    │   │   │       └── FlowExtensions.kt
    │   │
    │   │   └── res/
    │   │       ├── layout/
    │   │       ├── values/
    │   │       └── drawable/
    │
    ├── build.gradle.kts
    ├── settings.gradle.kts
    └── gradle/

------------------------------------------------------------------------

# ⚙ Detailed Working Flow

## 🟢 App Launch Flow

1.  MainActivity launches.
2.  HomeViewModel initializes.
3.  Initial REST data loaded.
4.  WebSocket stream starts.
5.  Periodic sync every 60 seconds.

## 🟢 Live Price Update Flow

1.  BinanceSocketManager emits TickerUpdate.
2.  HomeViewModel batches updates (1-second window).
3.  Latest symbol prices merged into tickerMap.
4.  UI recomposes via StateFlow.

## 🟢 Favorite Toggle Flow

1.  User taps favorite icon.
2.  ToggleFavoriteUseCase updates database.
3.  WidgetSyncManager.syncFavorites() updates DataStore.
4.  WidgetService reacts and updates Glance widget.

## 🟢 Pin Coin Flow

1.  Long press → confirmation bottom sheet.
2.  PinCoinUseCase updates DB.
3.  PinnedPriceService starts foreground notification.
4.  WebSocket updates reflected in notification.

## 🟢 Widget Update Flow

1.  CoinWidgetService observes favorites.
2.  WebSocket collects live prices.
3.  Every \~5 seconds, JSON pushed to Glance state.
4.  Widget UI refreshes safely.

------------------------------------------------------------------------

# 🛠 Tech Stack

-   Kotlin
-   Jetpack Compose (Material3)
-   Hilt (Dependency Injection)
-   Room (Local Database)
-   DataStore (Preferences)
-   Retrofit (REST API)
-   WebSocket (Real-time streaming)
-   Kotlin Coroutines & Flow
-   Android Glance AppWidget
-   Foreground Services

------------------------------------------------------------------------

# 🚀 Getting Started

``` bash
git clone https://github.com/NooBGurmeeT/CoinDevta.git
```

Open in Android Studio (minSdk 26), build and run.

------------------------------------------------------------------------

# 👨‍💻 Author

Gurmeet Singh (NooBGurmeeT)

------------------------------------------------------------------------

# 📄 License

Add your preferred license here.
