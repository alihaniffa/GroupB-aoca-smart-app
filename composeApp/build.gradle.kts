import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import java.util.Properties

val ksFile = rootProject.file("keystore.properties")
val useReleaseSigning = providers.gradleProperty("USE_RELEASE_SIGNING")
    .map { it.equals("true", ignoreCase = true) }
    .getOrElse(false)

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    id("com.google.gms.google-services")
//  id("org.jetbrains.kotlin.native.cocoapods")
}

kotlin {
    // Add compiler options for all targets to suppress expect/actual classes warning
    @OptIn(ExperimentalKotlinGradlePluginApi::class)
    compilerOptions {
        freeCompilerArgs.add("-Xexpect-actual-classes")
    }
    
    androidTarget {
        @OptIn(ExperimentalKotlinGradlePluginApi::class)
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_11)
        }
    }
    
    /*
    iosX64()
    iosArm64()
    iosSimulatorArm64()
    */
    
    sourceSets {
        androidMain.dependencies {
            implementation(compose.preview)
            implementation(libs.androidx.activity.compose)
            implementation(project.dependencies.platform("com.google.firebase:firebase-bom:33.16.0"))
            implementation("com.google.firebase:firebase-auth:22.3.1")
            implementation("com.google.android.gms:play-services-auth:21.2.0")
            implementation("com.google.firebase:firebase-database-ktx")
            implementation("com.google.firebase:firebase-storage-ktx")
            implementation(compose.components.uiToolingPreview)
            implementation(libs.androidx.lifecycle.viewmodel)
            implementation(libs.androidx.lifecycle.runtimeCompose)
            implementation("androidx.activity:activity-compose:1.10.1")
            implementation("androidx.fragment:fragment-ktx:1.8.9")
            implementation("androidx.exifinterface:exifinterface:1.3.7")
        }
        commonMain.dependencies {
            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.material3)
            implementation(compose.ui)
            implementation(compose.components.resources)
            implementation("org.jetbrains.kotlinx:kotlinx-datetime:0.6.0")
            implementation("io.coil-kt.coil3:coil-compose:3.0.4")
            implementation("io.coil-kt.coil3:coil-network-okhttp:3.0.4")
            implementation("org.jetbrains.compose.material:material-icons-core:1.7.3")
            implementation("org.jetbrains.compose.material:material-icons-extended:1.7.3")
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
            implementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.9.0")

        }
        androidUnitTest.dependencies {
            implementation("io.mockk:mockk:1.13.11")
            implementation("io.mockk:mockk-android:1.13.11")
            implementation("io.mockk:mockk-agent:1.13.11")
            implementation("org.robolectric:robolectric:4.12.1")
            implementation("junit:junit:4.13.2")
        }
        /*
        val iosArm64Main by getting {
            languageSettings.optIn("kotlinx.cinterop.ExperimentalForeignApi")
        }
        val iosSimulatorArm64Main by getting {
            languageSettings.optIn("kotlinx.cinterop.ExperimentalForeignApi")
        }
        val iosX64Main by getting {
            languageSettings.optIn("kotlinx.cinterop.ExperimentalForeignApi")
        }
        */
    }

    /*
    cocoapods {
        name = "ComposeApp"
        summary = "Shared code"
        homepage = "https://example.com"
        version  = "1.0.0"
        ios.deploymentTarget = "18.0"

        podfile = project.file("../iosApp/Podfile")

        framework {
            baseName = "ComposeApp"
            isStatic = true
        }

        // Firebase pods need clang modules enabled for cinterop
        val moduleFlags = listOf(
            "-compiler-option", "-fmodules",
            "-compiler-option", "-fcxx-modules"
        )

        pod("FirebaseCore") {
            extraOpts += moduleFlags
        }
        pod("FirebaseAuth") {
            extraOpts += moduleFlags
        }
        pod("FirebaseDatabase") {
            extraOpts += moduleFlags
        }
    }
    */
}

android {
    namespace = "com.aoca.dementiatester"
    compileSdk = libs.versions.android.compileSdk.get().toInt()

    defaultConfig {
        applicationId = "com.aoca.dementiatester"
        minSdk = libs.versions.android.minSdk.get().toInt()
        targetSdk = libs.versions.android.targetSdk.get().toInt()
        versionCode = 3
        versionName = "3.0"
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
    signingConfigs {
        if (useReleaseSigning && ksFile.exists()) {
            val ks = Properties().apply { load(ksFile.inputStream()) }

            create("release") {
                // You can also read from env vars in CI:
                val storePath = System.getenv("KS_PATH") ?: ks["storeFile"]?.toString()
                val storePass = System.getenv("KS_STORE_PASS") ?: ks["storePassword"]?.toString()
                val keyAlias  = System.getenv("KS_KEY_ALIAS") ?: ks["keyAlias"]?.toString()
                val keyPass   = System.getenv("KS_KEY_PASS") ?: ks["keyPassword"]?.toString()

                require(!storePath.isNullOrBlank()) { "Missing storeFile" }
                require(!storePass.isNullOrBlank()) { "Missing storePassword" }
                require(!keyAlias.isNullOrBlank())  { "Missing keyAlias" }
                require(!keyPass.isNullOrBlank())   { "Missing keyPassword" }

                storeFile = file(storePath)
                storePassword = storePass
                this.keyAlias = keyAlias
                keyPassword = keyPass
            }
        } else {
            logger.lifecycle("Android release signing not configured; skipping (this is fine for iOS builds).")
        }
    }
    buildTypes {
        getByName("release") {
            isMinifyEnabled = true
            signingConfig = signingConfigs.findByName("release")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    testOptions {
        unitTests.isReturnDefaultValues = true
    }
}

dependencies {
    debugImplementation(compose.uiTooling)
}
