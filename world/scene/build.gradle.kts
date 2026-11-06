plugins {
    alias(libs.plugins.kotlin.jvm)
    application
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}
kotlin { compilerOptions { jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17) } }

application {
    // Dev tool: exports scene geometry as JSON for the WebGL2 shader/render harness (tools/render-harness).
    mainClass.set("com.nexus.world.scene.export.SceneExportKt")
}

dependencies {
    testImplementation(libs.junit)
}
