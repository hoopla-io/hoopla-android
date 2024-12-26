plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
}

android {
    namespace = "uz.i_tv.domain"
    compileSdk = 34

    defaultConfig {
        minSdk = 21

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        consumerProguardFiles("consumer-rules.pro")
    }

    buildTypes {
        debug {
            buildConfigField("String", "BASE_URL", "\"http://192.168.31.72:8000/api/\"")
            isMinifyEnabled = false
        }

        release {

            buildConfigField("String", "BASE_URL", "\"http://192.168.31.72:8000/api/\"")

            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    buildFeatures {
        buildConfig = true
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

    api(libs.loading.button.android)

    api(libs.pinview)
}