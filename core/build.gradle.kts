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
                compilerOpts(
                    "-I${project.file("src/nativeInterop/cinterop").absolutePath}",
                    "-I/usr/include/libsecret-1",
                    "-I/usr/include/glib-2.0",
                    "-I/usr/lib/x86_64-linux-gnu/glib-2.0/include"
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
