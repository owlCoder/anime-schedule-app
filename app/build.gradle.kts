import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.hilt)
    alias(libs.plugins.ksp)
    alias(libs.plugins.apollo)
}

val localProperties = Properties().apply {
    val file = rootProject.file("local.properties")
    if (file.exists()) load(file.inputStream())
}

android {
    namespace = "com.owlcoder.animeschedule"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.owlcoder.animeschedule"
        minSdk = 31
        targetSdk = 36
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        versionCode = 41
        versionName = "5.12.4"

        buildConfigField(
            "String",
            "MAL_CLIENT_ID",
            "\"${localProperties.getProperty("MAL_CLIENT_ID", "")}\""
        )
        buildConfigField(
            "String",
            "MAL_REDIRECT_URI",
            "\"${localProperties.getProperty("MAL_REDIRECT_URI", "com.owlcoder.animeschedule://oauth")}\""
        )
    }

    signingConfigs {
        create("release") {
            val keystorePath = localProperties.getProperty("KEYSTORE_PATH") ?: ""
            val keystorePass = localProperties.getProperty("KEYSTORE_PASSWORD") ?: ""
            val keyAlias = localProperties.getProperty("KEY_ALIAS") ?: "animeschedule"
            val keyPass = localProperties.getProperty("KEY_PASSWORD") ?: keystorePass
            if (keystorePath.isNotEmpty()) {
                storeFile = file(keystorePath)
                storePassword = keystorePass
                this.keyAlias = keyAlias
                keyPassword = keyPass
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            signingConfig = signingConfigs.getByName("release")
        }
    }

    compileOptions {
        isCoreLibraryDesugaringEnabled = true
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    testOptions {
        // Production classes log through android.util.Log; unit tests should not need Robolectric.
        unitTests.isReturnDefaultValues = true
    }

    sourceSets.getByName("androidTest").assets.srcDir("$projectDir/schemas")

    // The in-app language switcher can pick a language the device is not set to. Play's
    // per-language splits would leave that language's resources out of the install.
    bundle {
        language {
            enableSplit = false
        }
    }
}

kotlin {
    compilerOptions {
        jvmTarget = JvmTarget.JVM_11
        // Constructor-injected qualifiers such as @ApplicationContext apply to the parameter and
        // its property, which is what Hilt expects.
        freeCompilerArgs.add("-Xannotation-default-target=param-property")
    }
}

ksp {
    // Exported schemas are what future Room migrations are written and tested against.
    arg("room.schemaLocation", "$projectDir/schemas")
}

apollo {
    service("anilist") {
        packageName.set("com.owlcoder.animeschedule.data.api.anilist.generated")
        schemaFile.set(file("src/main/graphql/anilist/schema.graphqls"))
        srcDir("src/main/graphql/anilist")
    }
}

dependencies {
    coreLibraryDesugaring(libs.desugar.jdk.libs)

    // Compose BOM
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.haze)
    implementation(libs.haze.materials)

    // AndroidX
    implementation(libs.androidx.core.ktx)
    // Keep the app and instrumentation classpaths aligned for Android 16 test support.
    debugImplementation("androidx.concurrent:concurrent-futures-ktx:1.2.0")
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.splashscreen)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.browser)
    implementation(libs.androidx.security.crypto)

    // Hilt
    implementation(libs.hilt.android)
    ksp(libs.hilt.android.compiler)
    implementation(libs.androidx.hilt.navigation.compose)
    implementation(libs.androidx.hilt.work)
    ksp(libs.androidx.hilt.compiler)

    // Room
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)

    // DataStore
    implementation(libs.androidx.datastore.preferences)

    // WorkManager
    implementation(libs.androidx.work.runtime.ktx)

    // OkHttp
    implementation(platform(libs.okhttp.bom))
    implementation(libs.okhttp)
    implementation(libs.okhttp.logging.interceptor)

    // Retrofit
    implementation(libs.retrofit)
    implementation(libs.retrofit.kotlinx.serialization)

    // Apollo
    implementation(libs.apollo.runtime)

    // Coil
    implementation(libs.coil.compose)
    implementation(libs.coil.network.okhttp)

    // Kotlinx
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.kotlinx.coroutines.android)

    // Test
    testImplementation(libs.junit)
    testImplementation(libs.mockk)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.okhttp.mockwebserver)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
    androidTestImplementation("androidx.test.ext:junit:1.3.0")
    androidTestImplementation("androidx.test:runner:1.7.0")
    androidTestImplementation("androidx.room:room-testing:${libs.versions.room.get()}")
    androidTestImplementation("androidx.work:work-testing:${libs.versions.workmanager.get()}")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.7.0")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
    debugImplementation(libs.androidx.compose.ui.tooling)
}
