plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.jetbrains.kotlin.android)
    id("dagger.hilt.android.plugin")
    id("kotlin-kapt")
}

android {
    namespace = "com.anji.locationaccess"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.anji.locationaccess"
        minSdk = 24
        targetSdk = 34
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
        debug {
            isMinifyEnabled = false
            isShrinkResources = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )

        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlinOptions {
        jvmTarget = "1.8"
    }

    buildFeatures {
        dataBinding = true
        viewBinding = true
    }
}

dependencies {

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)
    implementation(project(":location_sdk"))
    implementation(libs.android.maps)
    implementation(libs.play.services.location)
    implementation(libs.bundles.android.hilt.dragger)
    implementation(libs.bundles.android.camera)
    implementation(libs.glide)
    implementation("net.zetetic:android-database-sqlcipher:4.5.4")
    implementation("androidx.sqlite:sqlite-ktx:2.3.1")
    implementation("androidx.fragment:fragment-ktx:1.5.2")
    implementation("com.google.guava:guava:31.0.1-android")
    implementation(libs.bundles.android.kotlin.room){
        exclude(group = "com.intellij", module = "annotations")
    }
    kapt(libs.androidx.room.compiler)
    implementation ("com.google.code.gson:gson:2.11.0")
    kapt(libs.google.dagger.hilt.android.compiler)
    kapt ("androidx.hilt:hilt-compiler:1.0.0")
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}