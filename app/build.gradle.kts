plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.compose.compiler)
}

android {
    namespace = "dev.filipfan.polyengineinfer"
    compileSdk = 36

    defaultConfig {
        applicationId = "dev.filipfan.polyengineinfer"
        minSdk = 34
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        ndk {
            abiFilters += listOf("arm64-v8a", "x86_64")
        }
        ndkVersion = "28.2.13676358"
    }
    signingConfigs {
        create("peiSigningConfig") {
            storeFile = file("../keystore/pei_keystore.jks")
            storePassword = "pei_test"
            keyAlias = "pei_signing_key"
            keyPassword = "pei_test"
        }
    }
    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
            signingConfig = signingConfigs.getByName("peiSigningConfig")
        }

        debug {
            signingConfig = signingConfigs.getByName("peiSigningConfig")
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

tasks.whenTaskAdded {
    if (name == "collectReleaseDependencies") {
        dependsOn(":onnx:downloadOnnxRuntimeGenAiAar")
    }
}

dependencies {
    implementation(project(":onnx"))
    implementation(fileTree(mapOf("dir" to "../onnx/libs", "include" to listOf("*.aar"))))
    implementation(project(":llamacpp"))
    implementation(project(":mediapipe"))
    implementation(project(":executorch"))

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.material.icons.extended)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    debugImplementation(libs.androidx.ui.tooling)
}
