plugins {
    alias(libs.plugins.kotlin.jvm)
    application
}

kotlin {
    jvmToolchain(17)
}

application {
    mainClass.set("com.example.testmodule.HelloWorldKt")
}

dependencies {
    implementation(kotlin("stdlib"))
    implementation(libs.kotlinx.coroutines)

    // New libraries
    implementation(libs.gson)
    implementation(libs.jackson.databind)
    implementation(libs.ta4j.core)
    implementation(libs.commons.math3)
    implementation(libs.kotlin.dataframe)
    implementation(libs.xchart)
}

tasks.jar {
    manifest {
        attributes["Main-Class"] = "com.example.testmodule.HelloWorldKt"
    }
    from(configurations.runtimeClasspath.get().map { if (it.isDirectory) it else zipTree(it) })
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    archiveBaseName.set("test-module-fat")
}
