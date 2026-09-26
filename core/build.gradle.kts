plugins {
    id("org.jetbrains.kotlin.multiplatform")
}

kotlin {
    jvm()
    iosArm64()
    iosSimulatorArm64()
    mingwX64()
    macosArm64()
    linuxX64 {
        compilations.getByName("main") {
            val libsecret by cinterops.creating {
                definitionFile.set(
                    project.file("src/nativeInterop/cinterop/ritav_libsecret.def")
                )
            }
        }
    }

    sourceSets {
        commonMain.dependencies {
        }

        commonTest.dependencies {
            implementation(kotlin("test"))
        }
    }
}
