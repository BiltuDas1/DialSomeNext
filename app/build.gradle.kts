plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "com.github.biltudas1.dialsomev2"
    compileSdk {
        version = release(36) {
            minorApiLevel = 1
        }
    }

    defaultConfig {
        applicationId = "com.github.biltudas1.dialsomev2"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)

    // WebRTC
    implementation(libs.android)

    // Firebase (For Push Notifications to ring the phone)
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.messaging)

    // Google Login
    implementation(libs.androidx.credentials)
    implementation(libs.androidx.credentials.play.services.auth)
    implementation(libs.googleid)

    // Secure Storage
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.tink.android)

    // Kotlin Coroutines (Background tasks & API Calls)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.kotlinx.coroutines.play.services)

    // Networking
    implementation(libs.retrofit)
    implementation(libs.converter.gson)
}