plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    // This applies the Google Services plugin to process your google-services.json
    id("com.google.gms.google-services")
}

android {
    namespace = "com.example.accident_detection"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.example.accident_detection"
        minSdk = 26
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildFeatures {
        // This enables ViewBinding for all your Activity...Binding classes
        viewBinding = true
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlinOptions {
        jvmTarget = "1.8"
    }
}

// This 'dependencies' block contains EVERYTHING your app needs.
dependencies {
    // Core & UI
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("com.google.android.material:material:1.11.0")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")
    implementation("androidx.cardview:cardview:1.0.0")

    // ****** THIS IS THE CRITICAL MISSING DEPENDENCY ******
    // Provides LifecycleOwner, ViewModel, and other essential components for activities.
    // This will fix the "Cannot access 'LifecycleOwner'" error.
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.7.0")
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.7.0")

    // Firebase (using the BoM - Bill of Materials to manage versions)
    // This provides FirebaseAuth and FirebaseFirestore.
    implementation(platform("com.google.firebase:firebase-bom:32.7.2"))
    implementation("com.google.firebase:firebase-auth-ktx")
    implementation("com.google.firebase:firebase-firestore-ktx")
    implementation("com.google.firebase:firebase-messaging-ktx")
    // Google Sign-In & Credentials
    // This provides GoogleSignInClient and OneTap.
    implementation("com.google.android.gms:play-services-auth:21.0.0")
    implementation("androidx.credentials:credentials:1.2.0")
    implementation("androidx.credentials:credentials-play-services-auth:1.2.0")
    implementation("com.google.android.libraries.identity.googleid:googleid:1.1.0")

    // Networking with Retrofit
    // This provides Retrofit, Call, and Callback for your APIClient.
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")
    // Optional but recommended: for debugging network calls
    implementation("com.squareup.okhttp3:logging-interceptor:4.11.0")
    implementation("androidx.activity:activity-ktx:1.8.2")

    // Location Services
    // This provides FusedLocationProviderClient for getting GPS coordinates.
    implementation("com.google.android.gms:play-services-location:21.2.0")

    // Testing
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
}

    