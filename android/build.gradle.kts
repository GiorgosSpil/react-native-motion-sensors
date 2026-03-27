plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
}

apply(plugin = "com.facebook.react")

android {
    namespace = "com.motionsensors"
    compileSdk = 36

    defaultConfig {
        minSdk = 24
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    sourceSets {
        named("main") {
            java.srcDirs("src/main/java")
        }
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
    }
}

dependencies {
    implementation("com.facebook.react:react-android")
}
