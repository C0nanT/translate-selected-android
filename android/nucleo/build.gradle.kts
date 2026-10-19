// nucleo: regras puras em Kotlin/JVM, sem o plugin Android e sem o SDK no classpath.
// importar android.* aqui faz a compilacao falhar (de proposito).
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

// os testes leem os cases compartilhados da raiz: mudar um caso invalida o cache do teste
tasks.withType<Test>().configureEach {
    inputs.dir(rootProject.file("../cases")).withPropertyName("cases")
}
