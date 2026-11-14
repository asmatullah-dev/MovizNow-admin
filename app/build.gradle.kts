plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

if (file("google-services.json").exists()) {
    apply(plugin = "com.google.gms.google-services")
}

android {
    namespace = "com.moviznow.admin"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.moviznow.admin"
        minSdk = 24
        targetSdk = 35
        versionCode = 1
        versionName = "5.4.3-admin"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        buildConfigField("String", "FIREBASE_PROJECT_ID", "\"app-moviznow\"")
        buildConfigField("String", "FIRESTORE_DATABASE_ID", "\"moviznow-app\"")
        buildConfigField("String", "FIREBASE_API_KEY", "\"AIzaSyBogF7pfzJOkkIKu0190KurpQKIgDJ0CAg\"")
        buildConfigField("String", "FIREBASE_APP_ID", "\"1:460140141169:web:c906282a0ae274657799d0\"")
        buildConfigField("String", "FIREBASE_STORAGE_BUCKET", "\"app-moviznow.firebasestorage.app\"")
        buildConfigField("String", "FIREBASE_SENDER_ID", "\"460140141169\"")
        buildConfigField("String", "API_BASE_URL", "\"https://moviznow.com\"")
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            signingConfig = signingConfigs.getByName("debug")
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
        debug {
            applicationIdSuffix = ".debug"
            versionNameSuffix = "-debug"
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.material.icons.extended)
    implementation(libs.androidx.navigation.compose)

    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.auth.ktx)
    implementation(libs.firebase.firestore.ktx)
    implementation(libs.firebase.messaging.ktx)
    implementation(libs.kotlinx.coroutines.play.services)

    implementation(libs.coil.compose)
    implementation(libs.okhttp)

    debugImplementation(libs.androidx.ui.tooling)
}
