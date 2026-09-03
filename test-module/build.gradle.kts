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
