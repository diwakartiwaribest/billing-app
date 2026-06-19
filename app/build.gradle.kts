plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.hilt.android)
    alias(libs.plugins.ksp)
    id("com.google.gms.google-services")
}

android {
    namespace = "com.shop.billing"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.shop.billing"
        minSdk = 26
        targetSdk = 34
        versionCode = 20260619
        versionName = "15.6.28"
    }

    buildTypes {
        debug {
            isDebuggable = true
            isMinifyEnabled = false
        }
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    testOptions {
        unitTests.isReturnDefaultValues = true
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
    implementation(libs.lifecycle.process)

    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(libs.hilt.navigation.compose)

    // Room
    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    implementation(libs.room.paging)
    ksp(libs.room.compiler)

    // WorkManager
    implementation(libs.work.runtime.ktx)
    implementation(libs.hilt.work)

    implementation(libs.datastore.preferences)

    implementation(libs.coroutines.core)
    implementation(libs.coroutines.android)

    // Paging 3
    implementation(libs.paging.runtime.ktx)
    implementation(libs.paging.compose)

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

    // Firebase
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.firestore)
    implementation(libs.firebase.auth)
    implementation(libs.play.services.auth)

    // Tests
    testImplementation(libs.junit)
    testImplementation(libs.mockk)
    testImplementation(libs.coroutines.test)
    androidTestImplementation(libs.androidx.test.core)
    androidTestImplementation(libs.androidx.test.runner)
    androidTestImplementation(libs.androidx.test.rules)
    androidTestImplementation(libs.room.testing)
    androidTestImplementation(libs.junit)
}
