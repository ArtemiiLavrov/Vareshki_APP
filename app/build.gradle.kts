plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.jetbrains.kotlin.android)
    id("kotlin-parcelize")
}
android {
    namespace = "com.example.vareshki"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.vareshki"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }
    buildFeatures {
        compose = true
    }
    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.0"
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
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlinOptions {
        jvmTarget = "1.8"
    }

}

dependencies {
    implementation ("javax.xml.stream:stax-api:1.0-2")
    implementation ("com.fasterxml.woodstox:woodstox-core:6.5.1")
    implementation("io.minio:minio:8.5.3")
    implementation("com.itextpdf:itext7-core:7.2.5")
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)
    implementation("androidx.activity:activity-compose:1.10.1")
    implementation("androidx.compose.ui:ui:1.6.8")
    implementation("androidx.compose.material3:material3:1.3.1")
    implementation ("androidx.compose.material3:material3-window-size-class:1.3.1")
    //implementation("androidx.compose.material3:material3-icons-extended:1.3.1")
    implementation("androidx.compose.runtime:runtime:1.6.8")
    implementation ("com.google.code.gson:gson:2.10.1")
    implementation("mysql:mysql-connector-java:5.1.49") // Драйвер MySQL
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.1") // Для асинхронности
    implementation("androidx.compose.material:material-icons-extended:1.7.2")
    implementation("androidx.navigation:navigation-compose:2.7.7")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.6")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.6")
    implementation(libs.androidx.ui.tooling.preview.android)
    implementation(libs.play.services.drive)
    //implementation(libs.androidx.navigation.runtime.android)
    implementation(libs.androidx.navigation.compose)
    implementation("com.itextpdf:itext7-core:7.2.5")
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    configurations.all {
        resolutionStrategy {
            force("androidx.navigation:navigation-compose:2.7.7")
        }
    }
}