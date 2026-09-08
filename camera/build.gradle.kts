import java.net.URI

plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
}

android {
    namespace = "com.mulkallah.aircontrole.camera"
    compileSdk = 35

    defaultConfig {
        minSdk = 26
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
}

val handLandmarkerAsset = layout.projectDirectory.file("src/main/assets/hand_landmarker.task")
val downloadHandLandmarker by tasks.registering {
    description = "Downloads Google MediaPipe hand_landmarker.task into camera assets."
    val dest = handLandmarkerAsset.asFile
    outputs.file(dest)
    doLast {
        dest.parentFile.mkdirs()
        if (dest.exists() && dest.length() > 1_000_000L) {
            logger.lifecycle("Hand landmarker model already present (${dest.length()} bytes)")
            return@doLast
        }
        val url = URI(
            "https://storage.googleapis.com/mediapipe-models/hand_landmarker/hand_landmarker/float16/1/hand_landmarker.task",
        ).toURL()
        logger.lifecycle("Downloading MediaPipe hand_landmarker.task…")
        url.openStream().use { input ->
            dest.outputStream().use { output -> input.copyTo(output) }
        }
        check(dest.length() > 1_000_000L) {
            "Downloaded hand_landmarker.task is too small (${dest.length()} bytes)"
        }
        logger.lifecycle("Saved ${dest.length()} bytes to ${dest.path}")
    }
}

tasks.named("preBuild").configure { dependsOn(downloadHandLandmarker) }

dependencies {
    implementation(project(":core"))
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.camera.core)
    implementation(libs.androidx.camera.camera2)
    implementation(libs.androidx.camera.lifecycle)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.mediapipe.tasks.vision)
}
