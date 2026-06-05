import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
}

// Date-based version YYYY.M.PATCH, passed at build time:
//   ./gradlew assembleRelease -PholocronVersion=2026.6.3
// Falls back to "dev" locally (which maps to versionCode 1 — see versionCodeFrom).
val appVersionName: String = (project.findProperty("holocronVersion") as String?) ?: "dev"

fun versionCodeFrom(v: String): Int {
    val m = Regex("""(\d+)\.(\d+)\.(\d+)""").matchEntire(v) ?: return 1
    val (y, mo, p) = m.destructured
    return (y.toInt() - 2000) * 10000 + mo.toInt() * 100 + p.toInt()
}

// Signing: env vars first (CI), git-ignored keystore.properties second (local),
// default debug signing when neither is present (contributors without the key).
val keystorePropsFile = rootProject.file("keystore.properties")
val keystoreProps = Properties().apply {
    if (keystorePropsFile.exists()) keystorePropsFile.inputStream().use { load(it) }
}
fun cred(env: String, key: String): String? = System.getenv(env) ?: keystoreProps.getProperty(key)
val releaseStoreFile: String? = cred("HOLOCRON_KEYSTORE_FILE", "storeFile")

android {
    namespace = "dev.tomerklein.holocron"
    compileSdk = 35

    defaultConfig {
        applicationId = "dev.tomerklein.holocron"
        minSdk = 26
        targetSdk = 35
        versionCode = versionCodeFrom(appVersionName)
        versionName = appVersionName

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        buildConfigField("String", "APP_VERSION", "\"$appVersionName\"")
        buildConfigField(
            "String",
            "BUILD_DATE",
            "\"${SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())}\"",
        )
    }

    signingConfigs {
        if (releaseStoreFile != null) {
            create("release") {
                storeFile = file(releaseStoreFile)
                storePassword = cred("HOLOCRON_KEYSTORE_PASSWORD", "storePassword")
                keyAlias = cred("HOLOCRON_KEY_ALIAS", "keyAlias")
                keyPassword = cred("HOLOCRON_KEY_PASSWORD", "keyPassword")
            }
        }
    }

    buildTypes {
        // Sign debug with the release key too, so sideloaded debug/release APKs
        // are signature-compatible and update in place (no data-wiping reinstall).
        debug {
            if (releaseStoreFile != null) signingConfig = signingConfigs.getByName("release")
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
            if (releaseStoreFile != null) signingConfig = signingConfigs.getByName("release")
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
        buildConfig = true
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
            excludes += "/META-INF/INDEX.LIST"
            excludes += "/META-INF/io.netty.versions.properties"
        }
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.process)
    implementation(libs.androidx.activity.compose)

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.androidx.navigation.compose)
    debugImplementation(libs.androidx.compose.ui.tooling)

    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(libs.hilt.navigation.compose)
    implementation(libs.androidx.hilt.work)
    ksp(libs.androidx.hilt.compiler)

    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)

    implementation(libs.androidx.work.runtime.ktx)

    implementation(libs.okhttp)
    implementation(libs.hivemq.mqtt.client)
    implementation(libs.libphonenumber)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.androidx.security.crypto)
    implementation(libs.androidx.datastore.preferences)

    testImplementation(libs.junit)
    testImplementation(libs.okhttp.mockwebserver)
    testImplementation(libs.kotlinx.coroutines.test)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
}
