import java.util.Properties

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
        versionCode = getVersionCode() // Automatically set versionCode
        versionName =  getVersionName()

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


    implementation (libs.okhttp)



    implementation (libs.barcode.scanning)



    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)

}

fun getVersionCode(): Int {
    val versionFile = File("version.properties")

    // Create the file if it doesn't exist and initialize with default versions
    if (!versionFile.exists()) {
        versionFile.createNewFile()
        versionFile.writeText("MAJOR_VERSION=1\nMINOR_VERSION=0")
    }

    // Load properties from the file
    val properties = Properties()
    versionFile.inputStream().use { properties.load(it) }

    // Read the current major and minor version
    var majorVersion = properties.getProperty("MAJOR_VERSION").toInt()
    var minorVersion = properties.getProperty("MINOR_VERSION").toInt()

    // Increment minor version and check if it needs to roll over
    if (minorVersion < 10) {
        minorVersion += 1
    } else {
        minorVersion = 0
        majorVersion += 1
    }

    // Combine major and minor versions to create a unique versionCode
    val versionCode = majorVersion * 100 + minorVersion

    // Update properties in the file
    properties.setProperty("MAJOR_VERSION", majorVersion.toString())
    properties.setProperty("MINOR_VERSION", minorVersion.toString())
    versionFile.outputStream().use { properties.store(it, null) }

    return versionCode
}


fun getVersionName(): String {
    val versionFile = File("version.properties")
    val properties = Properties()
    versionFile.inputStream().use { properties.load(it) }

    // Retrieve the major and minor versions to construct version name
    val majorVersion = properties.getProperty("MAJOR_VERSION")
    val minorVersion = properties.getProperty("MINOR_VERSION")

    return "$majorVersion.$minorVersion"
}
