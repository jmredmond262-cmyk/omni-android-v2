plugins {
  alias(libs.plugins.android.application)
  alias(libs.plugins.kotlin.compose)
  alias(libs.plugins.google.services)
}

android {
  namespace = "com.omni.admin"
  compileSdk { version = release(36) { minorApiLevel = 1 } }

  defaultConfig {
    applicationId = "com.omni.adminpanel"
    minSdk = 24
    targetSdk = 36
    // GitHub Actions run numbers increase monotonically, allowing in-place Android updates.
    versionCode = (System.getenv("OMNI_ADMIN_VERSION_CODE") ?: System.getenv("GITHUB_RUN_NUMBER") ?: "4").toInt()
    versionName = "1.4.0"
  }

  signingConfigs {
    create("debugConfig") {
      // Share the persistent debug signing identity used by the main OMNI app.
      storeFile = file("${rootDir}/debug.keystore")
      storePassword = "android"
      keyAlias = "androiddebugkey"
      keyPassword = "android"
    }
  }

  buildTypes {
    debug {
      signingConfig = signingConfigs.getByName("debugConfig")
    }
  }

  compileOptions {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
  }
  buildFeatures { compose = true }
}


dependencies {
  implementation(platform(libs.androidx.compose.bom))
  implementation(platform(libs.firebase.bom))
  implementation(libs.androidx.activity.compose)
  implementation(libs.androidx.core.ktx)
  implementation(libs.androidx.compose.material3)
  implementation(libs.androidx.compose.material.icons.core)
  implementation(libs.androidx.compose.material.icons.extended)
  implementation(libs.androidx.compose.ui)
  implementation(libs.androidx.compose.ui.graphics)
  implementation(libs.androidx.compose.ui.tooling.preview)
  implementation(libs.firebase.auth)
  implementation(libs.kotlinx.coroutines.play.services)
}
