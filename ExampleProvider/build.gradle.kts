buildscript {
    repositories {
        google()
        mavenCentral()
        maven("https://jitpack.io")
    }
    dependencies {
        classpath("com.android.tools.build:gradle:8.2.0")
        // এখানে লেটেস্ট কটলিন ভার্সন দেওয়া হলো
        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:1.9.24") 
    }
}

allprojects {
    repositories {
        google()
        mavenCentral()
        maven("https://jitpack.io")
    }
}
