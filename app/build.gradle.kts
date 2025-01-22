import org.jetbrains.kotlin.fir.declarations.builder.buildField
import java.util.Properties


//fun getLocalProperty(key: String, project: Project): String {
//    val properties = Properties()
//    val localPropertiesFile = project.rootProject.file("local.properties")
//    if (localPropertiesFile.exists()) {
//        properties.load(localPropertiesFile.inputStream())
//    }
//    return properties.getProperty(key, "")
//}
plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

android {

    namespace = "com.example.samplepolaapi"
    compileSdk = 35
    buildFeatures {
        buildConfig = true // BuildConfig 機能を有効にする
        // ...
    }

    defaultConfig {
        applicationId = "com.example.samplepolaapi"
        minSdk = 29
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        buildConfigField("String", "DEVICE_ID", "\"${project.properties["DEVICE_ID"]}\"")
//        buildConfigField "String", "DEVICE_ID", "\"${project.properties['DEVICE_ID']}\""
//        buildConfigField("String", "DEVICE_ID","\"${getLocalProperty("DEVICE_ID", project)}\"")
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
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }
    buildFeatures {
        compose = true
    }
}

dependencies {
    implementation ("com.github.polarofficial:polar-ble-sdk:5.12.0")
    implementation ("io.reactivex.rxjava3:rxjava:3.1.6")
    implementation ("io.reactivex.rxjava3:rxandroid:3.0.2")

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
}