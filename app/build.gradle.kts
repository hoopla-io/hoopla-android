import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    id("com.google.gms.google-services")
    id("com.google.firebase.crashlytics")
}

val mapsApiKey: String = run {
    val props = Properties()
    val localProps = rootProject.file("local.properties")
    if (localProps.exists()) {
        localProps.inputStream().use(props::load)
    }
    props.getProperty("MAPS_API_KEY") ?: "YOUR_GOOGLE_MAPS_API_KEY"
}

android {
    namespace = "uz.alphazet.hoopla"
    compileSdk = 36

    defaultConfig {
        applicationId = "uz.alphazet.hoopla"
        minSdk = 26
        targetSdk = 36
        versionCode = 28
        versionName = "1.3.9"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        manifestPlaceholders["MAPS_API_KEY"] = mapsApiKey
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

    kotlin {
        compilerOptions {
            jvmTarget = JvmTarget.JVM_11
        }
    }

    buildFeatures {
        viewBinding = true
    }

    lint {
        disable += "NullSafeMutableLiveData"
        checkReleaseBuilds = false
    }

    testOptions {
        unitTests.isIncludeAndroidResources = true
        unitTests.isReturnDefaultValues = true
    }
}

dependencies {
    api(project(mapOf("path" to ":domain")))
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.viewpager2)
    implementation(libs.play.services.location)
    implementation(libs.play.app.update)
    implementation(libs.play.app.update.ktx)

    testImplementation(libs.junit)
    testImplementation(libs.mockk)
    testImplementation(libs.turbine)
    testImplementation(libs.coroutines.test)
    testImplementation(libs.robolectric)
    testImplementation(libs.androidx.test.core)
    testImplementation(libs.fragment.testing)
    testImplementation(libs.koin.test.junit4)
    debugImplementation(libs.fragment.testing.manifest)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.uiautomator)
}