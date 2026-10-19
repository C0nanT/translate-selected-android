// core: pure Kotlin/JVM rules, without the Android plugin and without the SDK on the classpath.
// importing android.* here makes compilation fail (on purpose).
plugins {
    id("org.jetbrains.kotlin.jvm")
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
    }
}

dependencies {
    testImplementation("junit:junit:4.13.2")
    testImplementation("com.google.code.gson:gson:2.10.1")
}

// the tests read the shared cases from the repo root: changing a case invalidates the test cache
tasks.withType<Test>().configureEach {
    inputs.dir(rootProject.file("../cases")).withPropertyName("cases")
}
