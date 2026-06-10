import java.util.Properties

val localProps = Properties().apply {
    val propsFile = rootProject.file("local.properties")
    if (propsFile.exists()) load(propsFile.inputStream())
}

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.hilt.plugin)
    alias(libs.plugins.ksp)
}

android {
    signingConfigs {
        create("release") {
            storeFile = file(localProps.getProperty("KEYSTORE_PATH"))
            storePassword = localProps.getProperty("KEYSTORE_PASSWORD")
            keyAlias = localProps.getProperty("KEY_ALIAS")
            keyPassword = localProps.getProperty("KEY_PASSWORD")
        }
    }
    namespace = "com.myhebnu"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.myhebnu"
        minSdk = 31
        targetSdk = 35
        versionCode = 3
        versionName = "1.2.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            signingConfig = signingConfigs.getByName("release")
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
        debug {
            applicationIdSuffix = ".debug"
            isMinifyEnabled = false
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlin {
        compilerOptions {
            jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
        }
    }

    packaging {
        resources.excludes.add("META-INF/versions/9/OSGI-INF/**")
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    applicationVariants.all {
        outputs.all {
            (this as com.android.build.gradle.internal.api.BaseVariantOutputImpl).outputFileName =
                "MyHEBNU-v${versionName}-${name}.apk"
        }
    }
}

dependencies {
    // Compose BOM + UI
    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.graphics)
    implementation(libs.compose.ui.tooling.preview)
    implementation(libs.compose.material3)
    implementation(libs.compose.material.icons)
    implementation(libs.compose.material.window)
    debugImplementation(libs.compose.ui.tooling)

    // Activity & Core
    implementation(libs.activity.compose)
    implementation(libs.core.ktx)

    // Lifecycle
    implementation(libs.lifecycle.runtime.compose)
    implementation(libs.lifecycle.viewmodel.compose)

    // Navigation
    implementation(libs.navigation.compose)

    // Hilt DI
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(libs.hilt.navigation.compose)

    // Room Database
    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    ksp(libs.room.compiler)

    // Network
    implementation(libs.retrofit.core)
    implementation(libs.retrofit.gson)
    implementation(libs.okhttp.core)
    implementation(libs.okhttp.logging)

    // Image Loading
    implementation(libs.coil.compose)

    // Widget (Glance)
    implementation(libs.glance.appwidget)

    // DataStore
    implementation(libs.datastore.preferences)

    // Coroutines
    implementation(libs.coroutines.android)

    // HTML Parser
    implementation(libs.jsoup)

    // WorkManager
    implementation(libs.workmanager.ktx)

    // Security
    implementation(libs.security.crypto)

    // Testing (Phase 8)
    testImplementation(libs.junit)
}
