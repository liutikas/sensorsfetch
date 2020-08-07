plugins {
    id("org.jetbrains.kotlin.jvm") version "1.3.72"
    application
    kotlin("kapt") version "1.3.72"
}

repositories {
    jcenter()
    maven("https://dl.bintray.com/mipt-npm/dataforge")
    maven("https://dl.bintray.com/mipt-npm/scientifik")
}

dependencies {
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8:1.3.72")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.3.8")
    implementation("ru.gildor.coroutines:kotlin-coroutines-okhttp:1.0")
    implementation("com.squareup.okhttp3:okhttp:4.7.2")
    implementation("com.squareup.moshi:moshi:1.8.0")
    implementation("org.apache.commons:commons-csv:1.8")
    implementation("scientifik:plotlykt-core:0.1.2")
    kapt("com.squareup.moshi:moshi-kotlin-codegen:1.8.0")
}

application {
    mainClassName = "net.liutikas.sensorsfetch.AppKt"
}

version = "1.0.0"