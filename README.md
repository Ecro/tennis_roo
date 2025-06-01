# Tennis Roo - Tennis Point Detection Watch App

A Wear OS application that detects tennis point events via on-device IMU, shows the current score, and logs the match locally.

## Project Structure

This project is organized into multiple modules to ensure separation of concerns and maintainability:

### watch_app

The main application module containing UI components and application lifecycle management.

**Responsibilities:**
- User interface using Jetpack Compose for Wear OS
- Application lifecycle management
- Navigation between screens
- Coordination between other modules

### core_sensors

Handles raw sensor data collection from the watch's IMU sensors.

**Responsibilities:**
- Sensor registration and data collection (accelerometer, gyroscope, heart rate)
- Circular buffer implementation for storing 10 seconds of sensor data
- Sensor data preprocessing
- Sensor event callbacks

### ml_gesture

TensorFlow Lite wrapper for gesture recognition (tennis point detection).

**Responsibilities:**
- Interface for gesture classification
- Mock implementation for testing
- TensorFlow Lite model integration (placeholder)
- Point event detection

### data_store

Room database implementation for local match logging.

**Responsibilities:**
- Database schema definition
- Data access objects (DAOs)
- Repository pattern implementation
- Match data export functionality

## Key Features

- **Tennis Point Detection**: Uses on-device IMU sensors to detect tennis point events
- **Real-time Scoring**: Shows the current score with a glanceable layout
- **Match Logging**: Stores match data locally for later review
- **Undo Functionality**: Allows correcting misdetected points
- **Match Export**: Exports match data for sharing or analysis

## Tennis Scoring Rules

The application implements standard tennis scoring rules:

- Standard game flow (0-15-30-40-Game)
- Deuce and advantage rules
- Tie break at 6-6 games (first to 7 points with margin of 2)
- Two set format options: best of 3 sets or pro set first to 8

## Sensor Detection

The application uses the following sensor thresholds for point detection:

- 300ms prefilter window
- Acceleration peak magnitude threshold: 8 m/s²
- Gyroscope Z-axis peak threshold: 500 degrees per second
- Detection rule: both acceleration AND gyroscope peaks within window indicate a candidate stroke

## UI Features

- Glanceable layout with big digits and high contrast
- Auto theme following system dark/light
- Haptic feedback: short for points, long for errors
- Swipe left to open log
- Accessibility support for font scaling and color-blind palette

## Getting Started

1. Clone the repository
2. Open the project in Android Studio
3. Build and run on a Wear OS device or emulator

## Development

This project uses:
- Kotlin for all code
- Jetpack Compose for UI
- Room for local database
- Coroutines and Flow for asynchronous operations
- TensorFlow Lite for gesture recognition
