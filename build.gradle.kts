plugins {
    kotlin("jvm") version "1.5.20"
    application
}

group = "dev.onyx.deobfuscator"
version = "1.0.0"

repositories {
    mavenLocal()
    mavenCentral()
}

dependencies {
    implementation(kotlin("stdlib"))
    implementation(kotlin("reflect"))
    implementation("org.tinylog:tinylog-api-kotlin:_")
    implementation("org.tinylog:tinylog-impl:_")
    implementation("org.ow2.asm:asm:_")
    implementation("org.ow2.asm:asm-commons:_")
    implementation("org.ow2.asm:asm-util:_")
    implementation("org.ow2.asm:asm-tree:_")
    implementation("com.github.ajalt.clikt:clikt:_")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:_")
    implementation("com.google.guava:guava:_")
}

tasks.withType<Wrapper> {
    gradleVersion = "7.1.1"
}

tasks.named<JavaExec>("run") {
    mainClass.set("dev.onyx.deobfuscator.Launcher")
    workingDir = rootProject.projectDir
}
