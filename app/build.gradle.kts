plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
}

android {
    namespace = "com.agent.s25"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.agent.s25"
        minSdk = 28
        targetSdk = 35
        versionCode = 30
        versionName = "1.0.30"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        
        ndk {
            // Target Snapdragon 8 Elite (arm64-v8a)
            abiFilters.addAll(listOf("arm64-v8a"))
        }

        externalNativeBuild {
            cmake {
                cppFlags += "-std=c++20"
                arguments += listOf(
                    "-DANDROID_STL=c++_shared",
                    "-DGGML_VULKAN=ON",
                    "-DANDROID_SUPPORT_FLEXIBLE_PAGE_SIZES=ON"
                )
            }
        }
    }

    signingConfigs {
        create("release") {
            storeFile = file("${rootDir}/s25-agent.keystore")
            storePassword = "s25agentpass"
            keyAlias = "s25agent"
            keyPassword = "s25agentpass"
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            signingConfig = signingConfigs.getByName("release")
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
        debug {
            isMinifyEnabled = false
            isDebuggable = true
            signingConfig = signingConfigs.getByName("release")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
        freeCompilerArgs += listOf(
            "-opt-in=androidx.compose.foundation.layout.ExperimentalLayoutApi",
            "-opt-in=androidx.compose.material3.ExperimentalMaterial3Api"
        )
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    externalNativeBuild {
        cmake {
            path = file("src/main/cpp/CMakeLists.txt")
            version = "3.22.1"
        }
    }

    packaging {
        jniLibs {
            useLegacyPackaging = true
        }
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.material.icons.extended)
    implementation(libs.kotlinx.coroutines.android)

    // Storage & Encryption (Knox Keystore AES-256 GCM + Standard Room SQLite)
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)

    // MCP Client (SSE Transport & JSON)
    implementation(libs.okhttp)
    implementation(libs.okhttp.sse)
    implementation(libs.gson)

    // Location & Spatial
    implementation(libs.play.services.location)

    // Background Workers
    implementation(libs.androidx.work.runtime.ktx)

    debugImplementation(libs.androidx.ui.tooling)
}
