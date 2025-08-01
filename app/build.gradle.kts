plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.google.gms.google.services)
}

android {
    namespace = "com.example.instant_chat"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.instant_chat"
        minSdk = 24
        targetSdk = 35
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

    // Lottie dependency
    implementation ("com.airbnb.android:lottie:6.1.0")
    implementation ("com.google.firebase:firebase-auth:22.3.1") // latest version as of June 2025
    implementation ("com.google.android.material:material:1.12.0")
    implementation ("com.github.bumptech.glide:glide:4.16.0")
    implementation(libs.firebase.database)
    implementation(libs.firebase.firestore)
    annotationProcessor ("com.github.bumptech.glide:compiler:4.16.0")
    implementation ("com.github.yalantis:ucrop:2.2.8")
    implementation ("com.google.firebase:firebase-storage:20.3.0")

    implementation ("com.google.android.material:material:1.11.0")
    implementation ("androidx.recyclerview:recyclerview:1.3.2")
    implementation ("com.google.firebase:firebase-database:20.3.0")

    implementation("com.journeyapps:zxing-android-embedded:4.3.0")

    implementation ("com.google.zxing:core:3.5.0")

    implementation ("com.google.firebase:firebase-bom:32.7.0") // Use latest stable version


    implementation("io.agora.rtc:full-sdk:4.5.0") // or latest version

    implementation ("com.google.firebase:firebase-storage:20.3.0")
    implementation ("androidx.activity:activity-ktx:1.8.0") // Required for ActivityResultLauncher

    implementation ("com.journeyapps:zxing-android-embedded:4.3.0")

    // You might already have these, ensure you do:
    implementation ("androidx.appcompat:appcompat:1.6.1")
    implementation ("com.google.android.material:material:1.12.0")// Or your current material version

    implementation ("com.google.zxing:core:3.5.2")

    implementation ("androidx.transition:transition:1.4.1")


    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
}