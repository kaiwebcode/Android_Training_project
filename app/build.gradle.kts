import java.net.URI

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.ksp)
}

val mockSyncBaseUrl = providers.gradleProperty("mockSyncBaseUrl")
    .orElse("http://10.0.2.2:3000/")
    .map { value ->
        val endpoint = URI(value.trim())
        require(
            endpoint.scheme in listOf("http", "https") && endpoint.host != null &&
                endpoint.rawUserInfo == null && endpoint.rawQuery == null && endpoint.rawFragment == null
        ) { "mockSyncBaseUrl must be an HTTP(S) base URL without credentials, a query, or a fragment." }
        endpoint.toASCIIString().trimEnd('/') + "/"
    }

android {
    buildFeatures {
        buildConfig = true
    }
    namespace = "com.example.a1weektaskproject"
    compileSdk = 37

    defaultConfig {
        applicationId = "com.example.a1weektaskproject"
        minSdk = 24
        targetSdk = 37
        versionCode = 1
        versionName = "1.0"
        buildConfigField("String", "MOCK_SYNC_BASE_URL", "\"\"")

        testInstrumentationRunner = "com.example.a1weektaskproject.IsolatedTestRunner"

        ksp {
            arg("room.schemaLocation", "$projectDir/schemas")
        }
    }

    sourceSets.getByName("androidTest").assets.directories.add("$projectDir/schemas")

    // LOCAL TESTING ONLY: the default Android debug key is not suitable for production.
    signingConfigs {
        getByName("debug") {
            storeFile = file("${System.getProperty("user.home")}/.android/debug.keystore")
            storePassword = "android"
            keyAlias = "androiddebugkey"
            keyPassword = "android"
            enableV1Signing = true
            enableV2Signing = true
        }
    }

    buildTypes {
        getByName("debug") {
            buildConfigField("String", "MOCK_SYNC_BASE_URL", "\"${mockSyncBaseUrl.get()}\"")
            signingConfig = signingConfigs.getByName("debug")
            if (providers.gradleProperty("day13IsolatedTest").orNull == "true") {
                applicationIdSuffix = ".day13test"
            }
        }
        release {
            signingConfig = signingConfigs.getByName("debug")
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
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.core.ktx)
    implementation(libs.material)
    implementation(libs.androidx.lifecycle.viewmodel.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.ktx)
    implementation(libs.retrofit)
    implementation(libs.retrofit.converter.gson)
    implementation(libs.okhttp)
    implementation(libs.okhttp.logging)
    implementation(libs.gson)
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)
    implementation(libs.androidx.work.runtime.ktx)
    testImplementation(libs.junit)
    testImplementation(libs.androidx.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.androidx.work.testing)
    testImplementation(libs.androidx.test.core.ktx)
    testImplementation("com.squareup.okhttp3:mockwebserver:4.12.0")
    testImplementation("org.mockito:mockito-core:5.20.0")
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.test.core.ktx)
    androidTestImplementation(libs.androidx.test.runner)
    androidTestImplementation(libs.kotlinx.coroutines.test)
}
