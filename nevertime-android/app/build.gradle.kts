plugins { id("com.android.application") }
android {
    namespace = "dev.nevertime.client"
    compileSdk = 35
    buildToolsVersion = "35.0.0"
    defaultConfig {
        applicationId = "dev.nevertime.client"
        minSdk = 26
        targetSdk = 35
        versionCode = 500
        versionName = "5.0.0-preview"
        testInstrumentationRunner = "dev.nevertime.client.SmokeInstrumentation"
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    lint { abortOnError = true }
}
