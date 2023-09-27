plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("kotlin-kapt")
}

android {
    namespace = "com.dikascode.airtellivenessdetection"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.dikascode.airtellivenessdetection"
        minSdk = 29
        targetSdk = 29
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlinOptions {
        jvmTarget = "1.8"
    }
    buildFeatures {
        viewBinding = true
    }
}

dependencies {

    implementation("androidx.core:core-ktx:1.9.0")
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("com.google.android.material:material:1.9.0")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")
    implementation("androidx.camera:camera-view:1.2.3")
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")

    implementation ("com.google.mlkit:face-detection:16.1.5")
    implementation ("androidx.camera:camera-camera2:1.4.0-alpha01")
    implementation ("androidx.camera:camera-core:1.4.0-alpha01")
    implementation ("androidx.camera:camera-lifecycle:1.4.0-alpha01")
    implementation ("androidx.camera:camera-view:1.4.0-alpha01")
    implementation ("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.1")
    implementation ("org.jetbrains.kotlin:kotlin-stdlib:1.9.0")


    implementation ("com.google.dagger:hilt-android:2.42")
    kapt ("com.google.dagger:hilt-compiler:2.42")
    kapt ("org.jetbrains.kotlinx:kotlinx-metadata-jvm:0.4.2")


}