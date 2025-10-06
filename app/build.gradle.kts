plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "com.taile.runner"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.taile.runner"
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

    buildFeatures {
        viewBinding = true
    }
}

dependencies {

    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)

    // Location Services
    implementation("com.google.android.gms:play-services-location:21.1.0")

    // Google Maps
    implementation("com.google.android.gms:play-services-maps:18.2.0")

    // Gson for JSON serialization
    implementation("com.google.code.gson:gson:2.10.1")

    // Lifecycle components
    implementation("androidx.lifecycle:lifecycle-runtime:2.7.0")
    implementation("androidx.lifecycle:lifecycle-service:2.7.0")

    // RecyclerView for displaying run records
    implementation("androidx.recyclerview:recyclerview:1.3.2")

    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
}