plugins {
    kotlin("jvm") version "2.1.0" apply false
    id("com.android.application") version "8.2.0" apply false
    kotlin("android") version "2.1.0" apply false
}

allprojects {
    repositories {
        google()
        mavenCentral()
    }
}
