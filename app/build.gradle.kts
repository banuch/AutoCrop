plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "com.app.autocrop"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.app.autocrop"
        minSdk = 24
        targetSdk = 34
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

    aaptOptions{
        noCompress ("tflite")
    }

}

dependencies {

    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)

    // CameraX core library
    implementation(libs.camera.core)

    // CameraX Camera2 extensions
    implementation(libs.camera.camera2)

    // CameraX Lifecycle Library
    implementation(libs.camera.lifecycle)

    // CameraX View class
    implementation(libs.camera.view)

    implementation (libs.tasks.vision)


    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)

}