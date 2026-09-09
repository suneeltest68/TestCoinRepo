plugins {
    alias(libs.plugins.kotlin.jvm)
    application
}

kotlin {
    jvmToolchain(17)
}

application {
    mainClass.set("com.example.testmodule.RunnerKt")
}

dependencies {
    implementation(kotlin("stdlib"))
}

tasks.jar {
    manifest {
        attributes["Main-Class"] = "com.example.testmodule.RunnerKt"
    }
    from(configurations.runtimeClasspath.get().map { if (it.isDirectory) it else zipTree(it) })
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    archiveBaseName.set("test-module-fat")
}
