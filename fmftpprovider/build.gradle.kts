version = 1

plugins {
    id("com.android.library")
    id("kotlin-android")
}

version = 1

cloudstream {
    //name = "Fmftp BDIX"
    description = "Movies from fmftp.net – Hollywood, Bollywood, Hindi Dubbed, Indian Bangla"
    authors = listOf("mpshimul")
    status = 1
    tvTypes = listOf("Movie")
    language = "bn"
    requiresResources = false
}

android {
    namespace = "com.fmftp"
    // No compileOptions or kotlinOptions needed here – they are inherited from the root
}

dependencies {
    implementation("com.github.Blatzar:NiceHttp:0.4.11")
    implementation("org.jsoup:jsoup:1.18.3")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin:2.13.1")
}