plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.jetbrains.kotlin.android)
    alias(libs.plugins.google.gms.google.services)

}

android {
    namespace = "com.ucb.capstone.farmnook"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.ucb.capstone.farmnook"
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
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
    buildFeatures {
        viewBinding = true
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.credentials)
    implementation(libs.androidx.credentials.play.services.auth)
    implementation(libs.googleid)
    implementation(libs.firebase.firestore.ktx)
    implementation(libs.play.services.location)
    implementation(libs.volley)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)

    //Firebase
    implementation(platform("com.google.firebase:firebase-bom:32.7.0"))
    implementation("com.google.firebase:firebase-auth-ktx")
    implementation("com.google.firebase:firebase-database-ktx")
    implementation("com.google.android.gms:play-services-auth:20.5.0")
    // Update Firebase Storage version here
    implementation("com.google.firebase:firebase-storage:20.3.0")

    //Mapbox SDK
    implementation("com.mapbox.maps:android:11.9.2")
    implementation("androidx.annotation:annotation:1.9.1")

    // Search
    implementation("com.mapbox.search:autofill:2.7.0")
    implementation("com.mapbox.search:discover:2.7.0")
    implementation("com.mapbox.search:place-autocomplete:2.7.0")
    implementation("com.mapbox.search:mapbox-search-android:2.7.0")
    implementation("com.mapbox.search:offline:2.7.0")

    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")

    implementation("de.hdodenhof:circleimageview:3.1.0") // For Circular Profile Image

    implementation ("com.github.bumptech.glide:glide:4.15.1")
    annotationProcessor ("com.github.bumptech.glide:compiler:4.15.1")
    implementation("com.squareup.okhttp3:okhttp:4.9.3")

    //OneSignal
    implementation("com.onesignal:OneSignal:5.0.4")
    implementation("com.google.firebase:firebase-messaging:23.4.1")
}

configurations.all {
    resolutionStrategy.eachDependency {
        if (requested.group == "com.mapbox.maps") {
            useVersion("11.9.2")
        }
    }
}