plugins { id("com.android.application") }
android {
 namespace = "dev.nevertime.client"
 compileSdk = 35
 ndkVersion = "27.2.12479018"
 defaultConfig {
  applicationId = "dev.nevertime.client"
  minSdk = 32; targetSdk = 34; versionCode = 3; versionName = "1.0-visual-probe"
  ndk { abiFilters += "arm64-v8a" }
  externalNativeBuild { cmake { cppFlags += "-std=c++17" } }
 }
 externalNativeBuild { cmake { path = file("src/main/cpp/CMakeLists.txt"); version = "3.22.1" } }
 packaging { jniLibs { useLegacyPackaging = false } }
 compileOptions { sourceCompatibility = JavaVersion.VERSION_17; targetCompatibility = JavaVersion.VERSION_17 }
}
dependencies {
 compileOnly("de.robv.android.xposed:api:82")
 testImplementation("junit:junit:4.13.2")
}
