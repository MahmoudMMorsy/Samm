import java.util.Base64
import java.net.URL
import java.net.HttpURLConnection

plugins {
  alias(libs.plugins.android.application)
  alias(libs.plugins.kotlin.compose)
  alias(libs.plugins.google.devtools.ksp)
  alias(libs.plugins.roborazzi)
  alias(libs.plugins.secrets)
}

val base64File = layout.projectDirectory.dir("..").asFile.resolve("debug.keystore.base64")
val keystoreFile = layout.projectDirectory.dir("..").asFile.resolve("debug.keystore")
println("DIAGNOSTIC: base64File path = ${base64File.absolutePath}, exists = ${base64File.exists()}")
println("DIAGNOSTIC: keystoreFile path = ${keystoreFile.absolutePath}, exists = ${keystoreFile.exists()}")
println("DIAGNOSTIC: rootDir = ${rootDir.absolutePath}")
println("DIAGNOSTIC: signingConfig keystore = ${rootDir.resolve("debug.keystore").absolutePath}, exists = ${rootDir.resolve("debug.keystore").exists()}")
if (base64File.exists() && !keystoreFile.exists()) {
    try {
        val base64Text = base64File.readText().trim()
        val decoded = Base64.getDecoder().decode(base64Text)
        keystoreFile.writeBytes(decoded)
        println("RESTORE_LOGS: Decoded debug.keystore successfully to ${keystoreFile.absolutePath}!")
    } catch (e: Exception) {
        println("RESTORE_LOGS ERROR: Failed to decode debug.keystore: ${e.message}")
    }
}

android {
  namespace = "com.example"
  compileSdk = 35

  defaultConfig {
    applicationId = "com.aistudio.retroplay.wmpkqa"
    minSdk = 21
    targetSdk = 35
    versionCode = 1
    versionName = "1.0"

    testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

    externalNativeBuild {
      cmake {
        cppFlags("-std=c++17")
      }
    }
    ndk {
      abiFilters += listOf("arm64-v8a")
    }
  }

  externalNativeBuild {
    cmake {
      path = file("src/main/cpp/CMakeLists.txt")
      version = "3.22.1"
    }
  }

  signingConfigs {
    create("release") {
      val keystorePath = System.getenv("KEYSTORE_PATH") ?: "${rootDir}/my-upload-key.jks"
      storeFile = file(keystorePath)
      storePassword = System.getenv("STORE_PASSWORD")
      keyAlias = "upload"
      keyPassword = System.getenv("KEY_PASSWORD")
    }
    create("debugConfig") {
      storeFile = file("${rootDir}/debug.keystore")
      storePassword = "android"
      keyAlias = "androiddebugkey"
      keyPassword = "android"
    }
  }

  buildTypes {
    release {
      isCrunchPngs = false
      isMinifyEnabled = false
      proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
      val keystorePath = System.getenv("KEYSTORE_PATH") ?: "${rootDir}/my-upload-key.jks"
      if (file(keystorePath).exists()) {
        signingConfig = signingConfigs.getByName("release")
      } else {
        signingConfig = signingConfigs.getByName("debugConfig")
      }
    }
    debug {
      signingConfig = signingConfigs.getByName("debugConfig")
    }
  }
  compileOptions {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
  }
  buildFeatures {
    compose = true
    buildConfig = true
  }
  testOptions { unitTests { isIncludeAndroidResources = true } }
}

// Configure the Secrets Gradle Plugin to use .env and .env.example files
// to match the convention used in Web projects.
secrets {
  propertiesFileName = ".env"
  defaultPropertiesFileName = ".env.example"
}

// Some unused dependencies are commented out below instead of being removed.
// This makes it easy to add them back in the future if needed.
dependencies {
  implementation(platform(libs.androidx.compose.bom))
  implementation(platform(libs.firebase.bom))
  // implementation(libs.accompanist.permissions)
  implementation(libs.androidx.activity.compose)
  // implementation(libs.androidx.camera.camera2)
  // implementation(libs.androidx.camera.core)
  // implementation(libs.androidx.camera.lifecycle)
  // implementation(libs.androidx.camera.view)
  implementation(libs.androidx.compose.material.icons.core)
  implementation(libs.androidx.compose.material.icons.extended)
  implementation(libs.androidx.compose.material3)
  implementation(libs.androidx.compose.ui)
  implementation(libs.androidx.compose.ui.graphics)
  implementation(libs.androidx.compose.ui.tooling.preview)
  implementation(libs.androidx.core.ktx)
  implementation(libs.androidx.datastore.preferences)
  implementation(libs.androidx.lifecycle.runtime.compose)
  implementation(libs.androidx.lifecycle.runtime.ktx)
  implementation(libs.androidx.lifecycle.viewmodel.compose)
  implementation(libs.androidx.navigation.compose)
  implementation(libs.androidx.room.ktx)
  implementation(libs.androidx.room.runtime)
  implementation(libs.coil.compose)
  // implementation(libs.converter.moshi)
  // implementation(libs.firebase.ai)
  implementation(libs.kotlinx.coroutines.android)
  implementation(libs.kotlinx.coroutines.core)
  // implementation(libs.logging.interceptor)
  // implementation(libs.moshi.kotlin)
  // implementation(libs.okhttp)
  // implementation(libs.play.services.location)
  // implementation(libs.retrofit)
  testImplementation(libs.androidx.compose.ui.test.junit4)
  testImplementation(libs.androidx.core)
  testImplementation(libs.androidx.junit)
  testImplementation(libs.junit)
  testImplementation(libs.kotlinx.coroutines.test)
  testImplementation(libs.robolectric)
  testImplementation(libs.roborazzi)
  testImplementation(libs.roborazzi.compose)
  testImplementation(libs.roborazzi.junit.rule)
  androidTestImplementation(platform(libs.androidx.compose.bom))
  androidTestImplementation(libs.androidx.compose.ui.test.junit4)
  androidTestImplementation(libs.androidx.espresso.core)
  androidTestImplementation(libs.androidx.junit)
  androidTestImplementation(libs.androidx.runner)
  debugImplementation(libs.androidx.compose.ui.test.manifest)
  debugImplementation(libs.androidx.compose.ui.tooling)
  "ksp"(libs.androidx.room.compiler)
  // "ksp"(libs.moshi.kotlin.codegen)
}

tasks.register("copyApk") {
    dependsOn("assembleDebug")
    val sourceFile = layout.buildDirectory.file("outputs/apk/debug/app-debug.apk")
    val destRootFile = rootDir.resolve("RetroPlay_GBA.apk")
    val destApksFile = rootDir.resolve("apks/RetroPlay_GBA.apk")
    doLast {
        val src = sourceFile.get().asFile
        if (src.exists()) {
            src.copyTo(destRootFile, overwrite = true)
            src.copyTo(destApksFile, overwrite = true)
            println("APK successfully copied! Source size: ${src.length()} bytes, Dest root size: ${destRootFile.length()} bytes, Dest apks size: ${destApksFile.length()} bytes")
        } else {
            println("Source APK does not exist at ${src.absolutePath}")
        }
    }
}

tasks.register("downloadGbaCore") {
    val dest = file("src/main/assets/gba_core.js")
    doLast {
        val files = listOf(
            "util.js",
            "keypad.js",
            "gpio.js",
            "sio.js",
            "video.js",
            "audio.js",
            "irq.js",
            "savedata.js",
            "mmu.js",
            "io.js",
            "core.js",
            "arm.js",
            "thumb.js",
            "gba.js"
        )
        val sb = StringBuilder()
        files.forEach { fileName ->
            val urlStr = "https://cdn.jsdelivr.net/gh/endrift/gbajs/js/$fileName"
            println("Downloading from $urlStr...")
            try {
                val conn = URL(urlStr).openConnection() as HttpURLConnection
                conn.connectTimeout = 10000
                conn.readTimeout = 10000
                val respCode = conn.responseCode
                if (respCode == HttpURLConnection.HTTP_OK) {
                    val text = conn.inputStream.bufferedReader().use { it.readText() }
                    sb.append("// --- BEGIN FILE: $fileName ---\n")
                    sb.append(text)
                    sb.append("\n// --- END FILE: $fileName ---\n\n")
                    println("Successfully downloaded $fileName (${text.length} bytes)")
                } else {
                    println("ERROR downloading $fileName (HTTP $respCode - Not Found)")
                    throw GradleException("Failed to download required file $fileName from $urlStr (HTTP $respCode)")
                }
            } catch (e: Exception) {
                e.printStackTrace()
                throw GradleException("ERROR downloading $fileName from $urlStr: ${e.toString()}")
            }
        }
        dest.parentFile.mkdirs()
        dest.writeText(sb.toString())
        println("COMPLETED GBA CORE DOWNLOAD SUCCESS! Total size: ${dest.length()} bytes.")
    }
}

tasks.named("assemble") {
    dependsOn("copyApk")
}






