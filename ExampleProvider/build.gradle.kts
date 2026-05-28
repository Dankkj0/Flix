buildscript {
    repositories {
        google()
        mavenCentral()
        maven("https://jitpack.io")
    }
    dependencies {
        // Android Gradle Plugin ভার্সন
        classpath("com.android.tools.build:gradle:8.2.0")
        // Kotlin ভার্সন আপডেট করে 2.1.0 করা হলো
        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:2.1.0") 
    }
}

allprojects {
    repositories {
        google()
        mavenCentral()
        maven("https://jitpack.io")
    }
}
