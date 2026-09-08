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
    id("org.jetbrains.kotlin.native.cocoapods")
}

kotlin {

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

    iosX64()
    iosArm64()
    iosSimulatorArm64()

    sourceSets {

        androidMain.dependencies {
            implementation(compose.preview)
            implementation(libs.androidx.activity.compose)

            implementation(
                project.dependencies.platform(
                    "com.google.firebase:firebase-bom:33.16.0"
                )
            )

            implementation(
                "com.google.firebase:firebase-auth:22.3.1"
            )

            implementation(
                "com.google.android.gms:play-services-auth:21.2.0"
            )

            implementation(
                "com.google.firebase:firebase-firestore:24.10.0"
            )

            implementation(
                "com.google.firebase:firebase-database-ktx"
            )

            implementation(
                "com.google.firebase:firebase-storage-ktx"
            )

            implementation(
                "io.coil-kt.coil3:coil-network-okhttp:3.0.4"
            )

            implementation(
                compose.components.uiToolingPreview
            )

            implementation(
                libs.androidx.lifecycle.viewmodel
            )

            implementation(
                libs.androidx.lifecycle.runtimeCompose
            )

            implementation(
                "androidx.activity:activity-compose:1.10.1"
            )

            implementation(
                "androidx.fragment:fragment-ktx:1.8.9"
            )

            implementation(
                "androidx.exifinterface:exifinterface:1.3.7"
            )
        }

        commonMain.dependencies {
            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.material3)
            implementation(compose.ui)
            implementation(compose.components.resources)

            implementation(
                "org.jetbrains.kotlinx:kotlinx-datetime:0.6.0"
            )

            implementation(
                "io.coil-kt.coil3:coil-compose:3.0.4"
            )

            implementation(
                "org.jetbrains.compose.material:material-icons-core:1.7.3"
            )

            implementation(
                "org.jetbrains.compose.material:material-icons-extended:1.7.3"
            )
        }

        commonTest.dependencies {
            implementation(libs.kotlin.test)

            implementation(
                "org.jetbrains.kotlinx:kotlinx-coroutines-test:1.9.0"
            )
        }

        androidUnitTest.dependencies {
            implementation(
                "io.mockk:mockk:1.13.11"
            )

            implementation(
                "io.mockk:mockk-android:1.13.11"
            )

            implementation(
                "io.mockk:mockk-agent:1.13.11"
            )

            implementation(
                "org.robolectric:robolectric:4.12.1"
            )

            implementation(
                "junit:junit:4.13.2"
            )
        }

        val iosArm64Main by getting {
            languageSettings.optIn(
                "kotlinx.cinterop.ExperimentalForeignApi"
            )
        }

        val iosSimulatorArm64Main by getting {
            languageSettings.optIn(
                "kotlinx.cinterop.ExperimentalForeignApi"
            )
        }

        val iosX64Main by getting {
            languageSettings.optIn(
                "kotlinx.cinterop.ExperimentalForeignApi"
            )
        }
    }

    cocoapods {
        name = "ComposeApp"
        summary = "Shared code"
        homepage = "https://example.com"
        version = "1.0.0"

        ios.deploymentTarget = "18.0"

        podfile = project.file("../iosApp/Podfile")

        framework {
            baseName = "ComposeApp"
            isStatic = true
        }

        val moduleFlags = listOf(
            "-compiler-option",
            "-fmodules",
            "-compiler-option",
            "-fcxx-modules"
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

        pod("FirebaseStorage") {
            extraOpts += moduleFlags
        }

        pod("FirebaseFirestore") {
            extraOpts += moduleFlags
        }

        pod("FirebaseDatabaseBridge") {
            source = path(
                project.file("../iosApp/iosApp")
            )

            interopBindingDependencies.add(
                "FirebaseDatabase"
            )
        }

        pod("FirebaseFirestoreBridge") {
            source = path(
                project.file("../iosApp/iosApp")
            )

            interopBindingDependencies.add(
                "FirebaseFirestore"
            )
        }
    }
}

android {
    namespace =
        "org.example.dementia_tester_app"

    compileSdk =
        libs.versions.android.compileSdk
            .get()
            .toInt()

    defaultConfig {
        applicationId =
            "org.example.dementia_tester_app"

        minSdk =
            libs.versions.android.minSdk
                .get()
                .toInt()

        targetSdk =
            libs.versions.android.targetSdk
                .get()
                .toInt()

        versionCode = 1
        versionName = "1.0"
    }

    signingConfigs {
        if (useReleaseSigning) {

            if (!ksFile.exists()) {
                throw GradleException(
                    "USE_RELEASE_SIGNING=true but keystore.properties is missing."
                )
            }

            val props = Properties().apply {
                ksFile.inputStream().use {
                    load(it)
                }
            }

            create("release") {
                storeFile =
                    rootProject.file(
                        props.getProperty("storeFile")
                    )

                storePassword =
                    props.getProperty("storePassword")

                keyAlias =
                    props.getProperty("keyAlias")

                keyPassword =
                    props.getProperty("keyPassword")
            }
        }
    }

    buildTypes {
        getByName("release") {
            isMinifyEnabled = false

            if (useReleaseSigning) {
                signingConfig =
                    signingConfigs.getByName(
                        "release"
                    )
            }
        }
    }

    compileOptions {
        sourceCompatibility =
            JavaVersion.VERSION_11

        targetCompatibility =
            JavaVersion.VERSION_11
    }
}

dependencies {
    debugImplementation(
        compose.uiTooling
    )
}