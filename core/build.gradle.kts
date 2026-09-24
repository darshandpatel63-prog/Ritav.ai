plugins {
    id("org.jetbrains.kotlin.multiplatform")
}

kotlin {
    jvm()
    iosArm64 {
        compilations.getByName("main") {
            cinterops {
                val ritavKeychain by creating {
                    definitionFile.set(project.file("src/nativeInterop/cinterop/ritav_keychain.def"))
                }
            }
        }
    }
    iosSimulatorArm64 {
        compilations.getByName("main") {
            cinterops {
                val ritavKeychain by creating {
                    definitionFile.set(project.file("src/nativeInterop/cinterop/ritav_keychain.def"))
                }
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
