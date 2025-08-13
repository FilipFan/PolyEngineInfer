import java.net.URI
import java.nio.file.Files
import java.nio.file.StandardCopyOption

plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
}

android {
    namespace = "dev.filipfan.polyengineinfer.onnx"
    compileSdk = 36

    defaultConfig {
        minSdk = 34

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        consumerProguardFiles("consumer-rules.pro")
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
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
}

val onnxRuntimeGenAiVersion = "0.8.3"
val onnxRuntimeGenAiAarFileName = "onnxruntime-genai-android-$onnxRuntimeGenAiVersion.aar"
val onnxRuntimeGenAiAarUrl =
    "https://github.com/microsoft/onnxruntime-genai/releases/download/v$onnxRuntimeGenAiVersion/$onnxRuntimeGenAiAarFileName"

tasks.register("downloadOnnxRuntimeGenAiAar") {
    group = "ONNX build setup"
    description = "Downloads the onnxruntime-genai AAR if it's missing."

    inputs.property("onnxRuntimeGenAiAarUrl", onnxRuntimeGenAiAarUrl)

    val libsDir = project.layout.projectDirectory.dir("libs")
    val aarFile = libsDir.file(onnxRuntimeGenAiAarFileName)
    outputs
        .file(aarFile)
        .withPropertyName("outputFile")

    doLast {
        val destinationFile = aarFile.asFile
        if (!destinationFile.exists()) {
            println("AAR file not found. Downloading from $onnxRuntimeGenAiAarUrl...")
            destinationFile.parentFile.mkdirs()
            URI(onnxRuntimeGenAiAarUrl).toURL().openStream().use { input ->
                Files.copy(input, destinationFile.toPath(), StandardCopyOption.REPLACE_EXISTING)
            }
            println("Download complete: ${destinationFile.path}")
        }
    }
}

tasks.named("preBuild") {
    dependsOn("downloadOnnxRuntimeGenAiAar")
}

dependencies {
    api(project(":api"))
    implementation(libs.onnxruntime.android)
    compileOnly(files("libs/$onnxRuntimeGenAiAarFileName"))

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.test.runner)
    androidTestImplementation(files("libs/$onnxRuntimeGenAiAarFileName"))
}
