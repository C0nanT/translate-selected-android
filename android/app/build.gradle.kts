plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

dependencies {
    testImplementation("junit:junit:4.13.2")
    testImplementation("com.google.code.gson:gson:2.10.1")
}

android {
    namespace = "com.conan.tradutor"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.conan.tradutor"
        minSdk = 23
        targetSdk = 34
        versionCode = 1
        versionName = "0.1"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
}

// os testes leem os casos compartilhados da raiz: mudar um caso invalida o cache do teste
tasks.withType<Test>().configureEach {
    inputs.dir(rootProject.file("../casos")).withPropertyName("casos")
}
