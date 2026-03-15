# FitTrack - Complete Fitness & Nutrition Tracker

A comprehensive Android app combining meal tracking and workout logging with wearable integration.

## Features

### Nutrition Tracking
- Log meals with calorie and macro tracking (protein, carbs, fat)
- Food database search (Open Food Facts API)
- Barcode scanning for packaged foods
- Daily/weekly nutrition summaries
- Custom food entries

### Workout Tracking
- Log exercises with sets, reps, and weight
- Pre-built exercise library
- Custom exercises
- Workout templates/routines
- Progress tracking over time

### Calorie Balance
- Net calories (intake - burned)
- Daily goals and targets
- Visual progress charts

### Wearable Integration (via Health Connect)
- Fitbit
- Garmin
- Samsung Health
- Wear OS
- Google Fit
- And more...

## Tech Stack
- Kotlin
- Jetpack Compose (UI)
- Health Connect API (wearables)
- Room (local database)
- Retrofit (API calls)
- Open Food Facts API (food database)
- Material Design 3

## Project Structure
```
app/
├── src/main/java/com/fittrack/
│   ├── data/
│   │   ├── local/          # Room database
│   │   ├── remote/         # API services
│   │   └── repository/     # Data repositories
│   ├── domain/
│   │   ├── model/          # Domain models
│   │   └── usecase/        # Business logic
│   ├── ui/
│   │   ├── screens/        # Compose screens
│   │   ├── components/     # Reusable components
│   │   └── theme/          # App theming
│   └── health/             # Health Connect integration
└── src/main/res/
```

## Build
```bash
./gradlew assembleDebug
```
