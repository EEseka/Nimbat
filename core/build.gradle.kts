import com.codingfeline.buildkonfig.compiler.FieldSpec.Type.STRING
import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.androidMultiplatformLibrary)
    alias(libs.plugins.buildkonfig)
    alias(libs.plugins.ksp)  // Needed for Room
    alias(libs.plugins.room)
}

buildkonfig {
    packageName = "com.eeseka.nimbat"
    objectName = "AppConfig"
    exposeObjectWithName = "AppConfig"

    defaultConfigs {
        buildConfigField(STRING, "APP_VERSION", libs.versions.app.versionName.get())
    }
}

kotlin {
    android {
        compileSdk = libs.versions.android.compileSdk.get().toInt()
        minSdk = libs.versions.android.minSdk.get().toInt()
        namespace = "com.eeseka.nimbat.core"
        androidResources { enable = true }

        withHostTest {}
    }

    iosArm64()
    iosSimulatorArm64()

    jvm()

    @OptIn(ExperimentalWasmDsl::class)
    wasmJs {
        browser()
        binaries.executable()
    }

    sourceSets {
        androidMain.dependencies {
            implementation(libs.androidx.activity.compose)
            implementation(libs.compose.uiTooling)
            implementation(libs.sqlite.bundled)
        }
        iosMain.dependencies {
            implementation(libs.sqlite.bundled)
        }
        jvmMain.dependencies {
            implementation(libs.jna.platform)
            implementation(libs.sqlite.bundled)
        }
        wasmJsMain.dependencies {
            implementation(libs.sqlite.web)
            implementation(npm("nimbat-sqlite-worker", layout.projectDirectory.dir("sqlite-worker").asFile))
        }
        commonMain.dependencies {
            implementation(libs.compose.runtime)
            implementation(libs.compose.foundation)
            implementation(libs.compose.material3)
            implementation(libs.compose.ui)
            implementation(libs.compose.components.resources)
            implementation(libs.compose.uiToolingPreview)

            implementation(libs.androidx.lifecycle.viewmodelCompose)
            implementation(libs.androidx.lifecycle.runtimeCompose)

            implementation(libs.bundles.koin.common)

            implementation(libs.kotlinx.coroutines.core)
            implementation(libs.kotlinx.collections.immutable)

            implementation(libs.icons.lucide.cmp)

            implementation(libs.material3.adaptive)

            implementation(libs.adaptive.ui)

            implementation(libs.datastore.preferences.core)

            implementation(libs.room.runtime)

            implementation(libs.kotlinx.datetime)

            implementation(projects.sound)
        }
    }
}

// Room Schema Setup
room3 {
    schemaDirectory(layout.projectDirectory.dir("schemas"))
}

// KSP Configuration for Room
dependencies {
    add("kspAndroid", libs.room.compiler)
    add("kspIosArm64", libs.room.compiler)
    add("kspIosSimulatorArm64", libs.room.compiler)
    add("kspJvm", libs.room.compiler)
    add("kspWasmJs", libs.room.compiler)
}
