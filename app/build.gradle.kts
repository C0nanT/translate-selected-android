plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

dependencies {
    implementation(project(":core"))
    testImplementation("junit:junit:4.13.2")
}

// the contract test reads the real manifest: changing it invalidates the test cache
tasks.withType<Test>().configureEach {
    inputs.file(file("src/main/AndroidManifest.xml")).withPropertyName("manifest")
}

android {
    namespace = "com.conan.translator"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.conan.translator"
        minSdk = 23
        targetSdk = 34
        // CI passes these from the tag (v1.2.3) and the run number; local builds keep the defaults
        versionCode = System.getenv("VERSION_CODE")?.toInt() ?: 1
        versionName = System.getenv("VERSION_NAME") ?: "0.1"
    }

    // release key comes from the environment (GitHub secrets in CI); without it, release is left unsigned
    val keystore = System.getenv("KEYSTORE_PATH")
    signingConfigs {
        if (keystore != null) {
            create("release") {
                storeFile = file(keystore)
                storePassword = System.getenv("KEYSTORE_PASSWORD")
                keyAlias = System.getenv("KEY_ALIAS") ?: "release"
                keyPassword = System.getenv("KEYSTORE_PASSWORD")
            }
        }
    }
    buildTypes {
        release {
            if (keystore != null) signingConfig = signingConfigs.getByName("release")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
}
