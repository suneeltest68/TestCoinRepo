plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "com.example.runkoltin"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = "com.example.runkoltin"
        minSdk = 34
        targetSdk = 37
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            optimization {
                enable = false
            }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

dependencies {
    implementation(libs.androidx.activity.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.core.ktx)
    implementation(libs.material)

    // New libraries
    implementation(libs.gson)
    implementation(libs.jackson.databind)
    implementation(libs.ta4j.core)
    implementation(libs.commons.math3)
    implementation(libs.kotlin.dataframe)
    implementation(libs.xchart)

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
}