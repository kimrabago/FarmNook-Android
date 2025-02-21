plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.jetbrains.kotlin.android)
}

android {
    namespace = "com.ucb.eldroid.farmnook"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.ucb.eldroid.farmnook"
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

    //Mapbox SDK
    implementation("com.mapbox.maps:android:11.9.2")


    implementation("androidx.annotation:annotation:1.9.1")
    implementation("de.hdodenhof:circleimageview:3.1.0") // For Circular Profile Image
}

configurations.all {
    resolutionStrategy.eachDependency {
        if (requested.group == "com.mapbox.maps") {
            useVersion("11.9.1")
        }
    }
}