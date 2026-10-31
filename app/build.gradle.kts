import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    id("org.jetbrains.kotlin.plugin.compose")
}

val apiBaseUrl = (findProperty("gradtrackApiBaseUrl") as String?) ?: "http://10.0.2.2:3000/api/v1/"
val useLocalApi = ((findProperty("gradtrackLocal") as String?) ?: "true").toBoolean()
val openLogin = ((findProperty("gradtrackOpenLogin") as String?) ?: "true").toBoolean()

// Supabase keys live in local.properties, which is git-ignored, so they never reach GitHub.
val localProperties = Properties().apply { rootProject.file("local.properties").takeIf { it.exists() }?.inputStream()?.use { load(it) } }
val supabaseUrl = localProperties.getProperty("supabase.url").orEmpty()
val supabaseKey = localProperties.getProperty("supabase.key").orEmpty()

// Firebase settings come from app/google-services.json (git-ignored). Without it the app keeps data on the device.
val firebase: Map<String, String> =
    file("google-services.json").takeIf { it.exists() }?.let { json ->
        @Suppress("UNCHECKED_CAST")
        val root = groovy.json.JsonSlurper().parse(json) as Map<String, Any?>
        val info = root["project_info"] as Map<String, Any?>
        val client = (root["client"] as List<Map<String, Any?>>).first { c ->
            ((c["client_info"] as Map<String, Any?>)["android_client_info"] as Map<String, Any?>)["package_name"] == "com.gradtrack.app"
        }
        mapOf(
            "projectId" to info["project_id"].toString(),
            "appId" to (client["client_info"] as Map<String, Any?>)["mobilesdk_app_id"].toString(),
            "apiKey" to ((client["api_key"] as List<Map<String, Any?>>).first()["current_key"]).toString(),
        )
    }.orEmpty()

android {
    namespace = "com.gradtrack.app"
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
        buildConfigField("String", "SUPABASE_URL", "\"$supabaseUrl\"")
        buildConfigField("String", "SUPABASE_KEY", "\"$supabaseKey\"")
        buildConfigField("String", "FIREBASE_PROJECT_ID", "\"${firebase["projectId"].orEmpty()}\"")
        buildConfigField("String", "FIREBASE_APP_ID", "\"${firebase["appId"].orEmpty()}\"")
        buildConfigField("String", "FIREBASE_API_KEY", "\"${firebase["apiKey"].orEmpty()}\"")
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
        compose = true
        buildConfig = true
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.navigation.compose)

    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.foundation)
    implementation(libs.compose.material3)
    implementation(libs.compose.ui.tooling.preview)
    debugImplementation(libs.compose.ui.tooling)

    implementation(libs.retrofit)
    implementation(libs.retrofit.gson)
    implementation(libs.gson)
    implementation(libs.okhttp)
    implementation(libs.okhttp.logging)

    implementation(libs.coil.compose)
    implementation(libs.coil.network.okhttp)

    implementation(libs.kotlinx.coroutines.android)

    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.firestore)

    implementation(libs.backdrop)
    implementation(libs.kyant.shapes)

    testImplementation(libs.junit4)
    testImplementation(libs.truth)
    testImplementation(libs.kotlinx.coroutines.test)
}
