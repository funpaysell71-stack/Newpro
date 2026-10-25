plugins { id("com.android.application") }
android {
    namespace = "dev.nevertime.client"
    compileSdk = 35
    buildToolsVersion = "35.0.0"
    defaultConfig {
        applicationId = "dev.nevertime.client"
        minSdk = 26
        targetSdk = 35
        versionCode = 2
        versionName = "0.2.0"
        testInstrumentationRunner = "dev.nevertime.client.SmokeInstrumentation"
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    lint { abortOnError = true }
}
val packageAddon by tasks.registering(Zip::class) {
    from("../../bedrock-android/behavior_pack")
    archiveFileName.set("Nevertime-Bedrock.mcpack")
    destinationDirectory.set(layout.buildDirectory.dir("generated/addon"))
    isPreserveFileTimestamps = false
    isReproducibleFileOrder = true
}
android.sourceSets["main"].assets.srcDir(layout.buildDirectory.dir("generated/addon"))
tasks.named("preBuild") { dependsOn(packageAddon) }
