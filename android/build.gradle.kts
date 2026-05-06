plugins {
    id("com.android.application")
    kotlin("android")
}

android {
    namespace = "ngo.xnet.zeeksworld"
    compileSdk = 34
    defaultConfig {
        applicationId = "ngo.xnet.zeeksworld"
        minSdk = 26
        targetSdk = 34
        versionCode = 1
        versionName = "0.1.0"
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
}

dependencies {
    implementation(project(":core"))
}
