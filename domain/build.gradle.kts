plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
}

android {
    namespace = "uz.alphazet.domain"
    compileSdk = 34

    defaultConfig {
        minSdk = 21

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        consumerProguardFiles("consumer-rules.pro")
    }

    buildTypes {
        debug {
            buildConfigField("String", "BASE_URL", "\"https://api.hoopla.uz/api/\"")
            buildConfigField("String", "MAPKIT_API_KEY", "\"4115671a-34dd-47dc-a8e5-809ed17374c1\"")
            isMinifyEnabled = false
        }

        release {

            buildConfigField("String", "BASE_URL", "\"https://api.hoopla.uz/api/\"")
            buildConfigField("String", "MAPKIT_API_KEY", "\"4115671a-34dd-47dc-a8e5-809ed17374c1\"")

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
    kotlinOptions {
        jvmTarget = "11"
    }
}

dependencies {
    api(project(mapOf("path" to ":data")))
    api(project(mapOf("path" to ":imageviewer")))

    testImplementation(libs.junit)
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

    api("androidx.swiperefreshlayout:swiperefreshlayout:1.1.0")

    api(libs.dexter)

    api("com.google.android.gms:play-services-auth:21.3.0")
    api("com.google.android.gms:play-services-auth-api-phone:18.2.0")

}