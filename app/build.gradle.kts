plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.hilt.android)
    alias(libs.plugins.ksp)
}

android {
    namespace = "com.shop.billing"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.shop.billing"
        minSdk = 26
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"
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
        compose = true
    }

    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.8"
    }

    packaging {
        resources {
            excludes += "/META-INF/**"
        }
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.documentfile)
    implementation(libs.zxing.core)
    implementation(libs.androidx.activity.compose)

    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.graphics)
    implementation(libs.compose.ui.tooling.preview)
    implementation(libs.compose.material3)
    implementation(libs.compose.material.icons.extended)
    debugImplementation(libs.compose.ui.tooling)

    implementation(libs.navigation.compose)

    implementation(libs.lifecycle.runtime.compose)
    implementation(libs.lifecycle.viewmodel.compose)

    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    ksp(libs.room.compiler)

    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(libs.hilt.navigation.compose)

    implementation(libs.datastore.preferences)

    implementation(libs.coroutines.core)
    implementation(libs.coroutines.android)

    // iText 7 Android (HTML → vector PDF)
    implementation("com.itextpdf.android:itext-core-android:8.0.5")
    implementation("com.itextpdf.android:svg-android:8.0.5")
    implementation("com.itextpdf:html2pdf:5.0.5") {
        exclude(group = "com.itextpdf", module = "forms")
        exclude(group = "com.itextpdf", module = "layout")
        exclude(group = "com.itextpdf", module = "svg")
        exclude(group = "com.itextpdf", module = "pdfa")
        exclude(group = "com.itextpdf", module = "kernel")
        exclude(group = "com.itextpdf", module = "io")
        exclude(group = "com.itextpdf", module = "commons")
        exclude(group = "com.itextpdf", module = "bouncy-castle-connector")
        exclude(group = "com.itextpdf", module = "bouncy-castle-adapter")
        exclude(group = "com.itextpdf", module = "barcodes")
        exclude(group = "com.itextpdf", module = "sign")
        exclude(group = "com.itextpdf", module = "pdfua")
        exclude(group = "com.itextpdf", module = "font-asian")
        exclude(group = "com.itextpdf", module = "hyph")
        exclude(group = "com.itextpdf", module = "styled-xml-parser")
    }

    // SVG preview in Settings screen
    implementation("com.caverock:androidsvg:1.4")

    // ZXing QR scanner
    implementation("com.journeyapps:zxing-android-embedded:4.3.0")

    // OkHttp for WebSocket (Supabase Realtime)
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
}
