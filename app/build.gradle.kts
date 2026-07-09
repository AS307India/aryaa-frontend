import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
    alias(libs.plugins.google.services)
}

// Read local.properties for release-only config that must never be committed to source control.
val localProps = Properties()
val localPropsFile = rootProject.file("local.properties")
if (localPropsFile.exists()) {
    localPropsFile.inputStream().use { localProps.load(it) }
}

android {
    namespace = "com.as307.aryaa"
    compileSdk = 36
    defaultConfig {
        applicationId = "com.as307.aryaa"
        minSdk = 26
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        // API_BASE_URL is set per build type below — not in defaultConfig.
        // debug → emulator localhost; release → read from local.properties (fail-fast if absent)
    }

    buildTypes {
        debug {
            // Emulator localhost — hardcoded, safe for development
            buildConfigField("String", "API_BASE_URL", "\"https://aryaa-backend.onrender.com/\"")
        }
        release {
            // Fail-fast: refuse to produce a release APK without a real API URL.
            // To build a release, add to local.properties (never commit this file):
            //   release.api.base.url=https://your-production-api.com/
            //
            // The check is deferred to task-execution time so debug builds are unaffected.
            val releaseApiUrl: String = localProps.getProperty("release.api.base.url")
                ?: "RELEASE_URL_NOT_SET"
            if (releaseApiUrl == "RELEASE_URL_NOT_SET") {
                // Register a task that fires only when a release task is actually requested
                tasks.whenTaskAdded {
                    if (name.contains("Release", ignoreCase = true)) {
                        doFirst {
                            throw GradleException(
                                "\n\nrelease.api.base.url is not set in local.properties.\n" +
                                "Add it before building a release APK:\n" +
                                "  release.api.base.url=https://your-production-api.com/\n"
                            )
                        }
                    }
                }
            }
            buildConfigField("String", "API_BASE_URL", "\"$releaseApiUrl\"")

            // R8 code shrinking and obfuscation
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    buildFeatures {
        compose = true
        aidl = false
        buildConfig = true
        shaders = false
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
            // mockk-android pulls in JUnit Jupiter transitively; exclude its duplicate licence files
            excludes += "META-INF/LICENSE.md"
            excludes += "META-INF/LICENSE-notice.md"
        }
    }
}

kotlin {
    jvmToolchain(17)
}

dependencies {
    val composeBom = platform(libs.androidx.compose.bom)
    implementation(composeBom)
    androidTestImplementation(composeBom)

    // Firebase
    implementation(platform("com.google.firebase:firebase-bom:33.7.0"))
    implementation("com.google.firebase:firebase-messaging-ktx")

    // Core Android dependencies
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)

    // Splashscreen API
    implementation(libs.androidx.core.splashscreen)

    // Compose Navigation & Google Fonts
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.compose.ui.text.google.fonts)

    // Dagger Hilt DI
    // compileOnly errorprone-annotations: needed on classpath for Hilt 2.60 + Room 2.7.x codegen
    compileOnly("com.google.errorprone:error_prone_annotations:2.36.0")
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(libs.androidx.hilt.navigation.compose)

    // Retrofit + OkHttp + Serialization (Declared, unused in this unit)
    implementation(libs.retrofit)
    implementation(libs.retrofit.converter.kotlinx.serialization)
    implementation(libs.okhttp.logging.interceptor)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.androidx.security.crypto)
    implementation(libs.play.services.location)
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.androidx.browser)
    implementation("androidx.work:work-runtime-ktx:2.9.1")

    // Room DB (Declared, unused in this unit)
    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    ksp(libs.room.compiler)

    // Arch Components
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)

    // Jetpack Compose
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.core)
    implementation(libs.androidx.compose.material.icons.extended)
    debugImplementation(libs.androidx.compose.ui.tooling)

    // Testing
    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.okhttp.mockwebserver)
    testImplementation(libs.mockk)

    // Instrumented Testing
    androidTestImplementation(libs.androidx.test.core)
    androidTestImplementation(libs.androidx.test.ext.junit)
    androidTestImplementation(libs.androidx.test.runner)
    androidTestImplementation(libs.androidx.test.espresso.core)
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    androidTestImplementation(libs.androidx.navigation.testing)
    androidTestImplementation(libs.okhttp.logging.interceptor) // for E2E network calls in SosE2EWithServiceTest
    androidTestImplementation(libs.mockk.android) // for mockk() in instrumented test fakes
}
