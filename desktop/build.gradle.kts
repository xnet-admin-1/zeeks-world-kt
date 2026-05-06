plugins {
    kotlin("jvm")
    application
}

dependencies {
    implementation(project(":core"))
}

application {
    mainClass.set("ngo.xnet.zeeksworld.DesktopLauncherKt")
}
