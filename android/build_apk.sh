#!/bin/bash
# Build debug APK for Samsung S24 (arm64)
# Requires: Android SDK with build-tools 34, NDK
cd "$(dirname "$0")/.." 
./gradlew :android:assembleDebug
echo "APK: android/build/outputs/apk/debug/android-debug.apk"
