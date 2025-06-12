plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
}

android {
    namespace = "uz.alphazet.hoopla"
    compileSdk = 35

    defaultConfig {
        applicationId = "uz.alphazet.hoopla"
        minSdk = 21
        targetSdk = 34
        versionCode = 7
        versionName = "1.0.7"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    signingConfigs {
        create("release") {
            keyAlias = "hoopla_alias"
            keyPassword = "hoopla"
            storePassword = "hoopla"
            storeFile = file("..\\app\\hoopla_key_store")
        }
    }

    bundle {
        language {
            // This property is set to true by default.
            // You can specify `false` to turn off
            // generating configuration APKs for language resources.
            // These resources are instead packaged with each base and
            // feature APK.
            // Continue reading below to learn about situations when an app
            // might change setting to `false`, otherwise consider leaving
            // the default on for more optimized downloads.
            enableSplit = false
        }
        density {
            // This property is set to true by default.
            enableSplit = true
        }
        abi {
            // This property is set to true by default.
            enableSplit = true
        }
    }

    buildTypes {
        getByName("release") {
            isMinifyEnabled = true
            isDebuggable = false
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            signingConfig = signingConfigs.getByName("release")
        }
        getByName("debug") {
            isDebuggable = true
            isShrinkResources = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            signingConfig = signingConfigs.getByName("debug")
            isMinifyEnabled = false
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }
    buildFeatures {
        viewBinding = true
    }
}

dependencies {
    api(project(mapOf("path" to ":domain")))
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.play.services.location)

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}