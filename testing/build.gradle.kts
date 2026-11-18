import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidMultiplatformLibrary)
}

kotlin {
    android {
        compileSdk = libs.versions.android.compileSdk.get().toInt()
        minSdk = libs.versions.android.minSdk.get().toInt()
        namespace = "com.eeseka.nimbat.testing"
    }

    iosArm64()
    iosSimulatorArm64()

    jvm()

    @OptIn(ExperimentalWasmDsl::class)
    wasmJs {
        browser()
    }

    sourceSets {
        commonMain.dependencies {
            // api, not implementation: every test file imports these, so they reach each module that uses :testing
            api(libs.kotlin.test)
            api(libs.assertk)
        }

        androidMain.dependencies {
            api(libs.kotlin.testJunit)
        }

        jvmMain.dependencies {
            api(libs.kotlin.testJunit)
        }
    }
}
