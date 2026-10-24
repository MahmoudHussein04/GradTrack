plugins {
    alias(libs.plugins.android.application)
    id("org.jetbrains.kotlin.plugin.compose")
}

/**
 * `-PgradtrackApiBaseUrl=https://host/api/v1/` points the app at the real backend;
 * `-PgradtrackLocal=false` turns the on-device API off (default: on, the backend does not exist yet).
 * `-PgradtrackOpenLogin=false` makes the on-device API check passwords (default: off while the UI is being tested).
 */
val apiBaseUrl = (findProperty("gradtrackApiBaseUrl") as String?) ?: "http://10.0.2.2:3000/api/v1/"
val useLocalApi = ((findProperty("gradtrackLocal") as String?) ?: "true").toBoolean()
val openLogin = ((findProperty("gradtrackOpenLogin") as String?) ?: "true").toBoolean()

android {
    namespace = "com.gradtrack.app"
    // 37 because the Liquid Glass library (backdrop 2.0.1) declares minCompileSdk 37; the app still targets 36.
    compileSdk = 37

    defaultConfig {
        applicationId = "com.gradtrack.app"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "0.1.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        buildConfigField("String", "API_BASE_URL", "\"$apiBaseUrl\"")
        buildConfigField("boolean", "USE_LOCAL_API", "$useLocalApi")
        buildConfigField("boolean", "LOCAL_OPEN_LOGIN", "$openLogin")
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        viewBinding = true
        compose = true
        buildConfig = true
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.activity.ktx)
    implementation(libs.androidx.fragment.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.navigation.fragment.ktx)
    implementation(libs.androidx.navigation.ui.ktx)
    implementation(libs.androidx.recyclerview)
    implementation(libs.androidx.swiperefreshlayout)
    implementation(libs.material)

    implementation(libs.retrofit)
    implementation(libs.retrofit.gson)
    implementation(libs.gson)
    implementation(libs.okhttp)
    implementation(libs.okhttp.logging)

    implementation(libs.coil)
    implementation(libs.coil.network.okhttp)

    implementation(libs.kotlinx.coroutines.android)

    implementation(libs.backdrop)
    implementation(libs.kyant.shapes)
    implementation(libs.compose.foundation)
    implementation(libs.compose.ui)

    testImplementation(libs.junit4)
    testImplementation(libs.truth)
    testImplementation(libs.kotlinx.coroutines.test)
}
