import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
}

android {
    namespace = "uz.alphazet.domain"
    compileSdk = 36

    defaultConfig {
        minSdk = 26

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        consumerProguardFiles("consumer-rules.pro")
    }

    buildTypes {
        debug {
            buildConfigField("String", "BASE_URL", "\"https://api.hoopla.uz/api/\"")
            isMinifyEnabled = false
        }

        release {

            buildConfigField("String", "BASE_URL", "\"https://api.hoopla.uz/api/\"")

            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    buildFeatures {
        buildConfig = true
        viewBinding = true
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
    api(project(mapOf("path" to ":data")))

    testImplementation(libs.junit)
    testImplementation(libs.mockk)
    testImplementation(libs.turbine)
    testImplementation(libs.coroutines.test)
    testImplementation(libs.robolectric)
    testImplementation(libs.androidx.test.core)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)

    annotationProcessor(libs.lifecycle.compiler)
    api(libs.androidx.lifecycle.extensions)
    api(libs.lifecycle.viewmodel.ktx)
    api(libs.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.common.java8)

    api(libs.androidx.paging.runtime.ktx)

    //localization
    api(libs.localization)
    api(libs.locale.helper.android)

    //Navigation
    api(libs.androidx.navigation.fragment.ktx)

    api(libs.cicerone)

    api(libs.loading.button.android)

    api(libs.pinview)

    api(libs.coil)
    api(libs.coil.compose)

    api("com.facebook.fbui.textlayoutbuilder:staticlayout-proxy:1.6.0")

    api("com.github.alexzhirkevich:custom-qr-generator:1.6.2")

    api("io.github.g00fy2.quickie:quickie-unbundled:1.11.0")

    api("androidx.swiperefreshlayout:swiperefreshlayout:1.1.0")

    api(libs.dexter)

    api("com.google.android.gms:play-services-auth:21.3.0")
    api("com.google.android.gms:play-services-auth-api-phone:18.2.0")

    api(platform("com.google.firebase:firebase-bom:32.7.0"))
    api("com.google.firebase:firebase-analytics")
    api("com.google.firebase:firebase-crashlytics")

    api(libs.play.services.maps)
    api(libs.android.maps.utils)

    api("com.github.skydoves:powerspinner:1.2.7")

    api("com.qmdeve.blurview:core:1.1.3")

}
