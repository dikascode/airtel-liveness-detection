# Airtel Liveness Detection

This project is an Android application, named `AirtelLivenessDetection` done for Airtel recruitment process, that demonstrates face liveness detection using Android's CameraX library and Google ML Kit's face detection API. The application detects faces in real-time and enables capturing and saving of images.

## Features

- Real-time face detection using Google ML Kit.
- Switching between front and back camera.
- Capture and save images.
- Display of the captured image.

## Tech Stack

- **Kotlin**: The project is fully written in Kotlin.
- **CameraX**: A camera library for Android for capturing images.
- **Google ML Kit**: Utilized for face detection.
- **Dagger Hilt**: For dependency injection.
- **Coroutines**: For handling asynchronous tasks.
- **View Binding**: For efficiently binding views.
- **AndroidX**: For the latest Android components.

## Dependencies

```gradle
implementation("androidx.core:core-ktx:1.9.0")
implementation("androidx.appcompat:appcompat:1.6.1")
implementation("com.google.android.material:material:1.9.0")
implementation("androidx.constraintlayout:constraintlayout:2.1.4")
implementation("androidx.camera:camera-view:1.2.3")
implementation("com.google.mlkit:face-detection:16.1.5")
implementation("androidx.camera:camera-camera2:1.4.0-alpha01")
implementation("androidx.camera:camera-core:1.4.0-alpha01")
implementation("androidx.camera:camera-lifecycle:1.4.0-alpha01")
implementation("androidx.camera:camera-view:1.4.0-alpha01")
implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.1")
implementation("org.jetbrains.kotlin:kotlin-stdlib:1.9.0")
implementation("com.google.dagger:hilt-android:2.42")
kapt("com.google.dagger:hilt-compiler:2.42")
kapt("org.jetbrains.kotlinx:kotlinx-metadata-jvm:0.4.2")
```


## Getting Started
** Prerequisites
1. Android Studio
2. Android Device or Emulator (minSdk: 29, targetSdk: 34)

** Setup and Installation
1. Clone this repository.
2. Open the project in Android Studio.
3. Sync the Gradle files and build the project.
4. Run the application on an Android device or emulator.
   
** Permissions
The application requires the CAMERA permission to access the device's camera.
