buildscript {
    repositories {
        google()
        mavenCentral()
        maven("https://jitpack.io")
    }
    dependencies {
        classpath("com.android.tools.build:gradle:8.2.0") // আপডেট করা হলো
        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:1.9.22") // আপডেট করা হলো
    }
}

allprojects {
    repositories {
        google()
        mavenCentral()
        maven("https://jitpack.io")
    }
}
