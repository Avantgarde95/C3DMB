import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    application
    java
    kotlin("jvm") version "1.3.40"
}

group = "com.github.avantgarde95.c3dmb"

repositories {
    jcenter()
    mavenCentral()
    maven { setUrl("https://jitpack.io") }
    maven { setUrl("https://oss.sonatype.org/content/repositories/snapshots/") }
}

dependencies {
    implementation(kotlin("stdlib"))
    implementation("ch.qos.logback:logback-classic:1.2.3")
    implementation("io.ktor:ktor-client-cio:1.2.2")
    implementation("io.ktor:ktor-server-netty:1.2.2")
    implementation("com.beust:klaxon:5.0.1")
    implementation("com.github.kotlin-graphics.glm:glm:v0.9.9.0-build-13")

    arrayOf("", ":natives-windows-amd64", ":natives-linux-amd64").forEach { platform ->
        implementation("org.jogamp.gluegen:gluegen-rt:2.3.2$platform")
        implementation("org.jogamp.jogl:jogl-all:2.3.2$platform")
    }

    arrayOf("", "-glfw", "-jemalloc", "-openal", "-opengl", "-stb").forEach { type ->
        arrayOf("", ":natives-windows", ":natives-linux").forEach { platform ->
            implementation("org.lwjgl:lwjgl$type:3.2.3-SNAPSHOT$platform")
        }
    }
}

configure<JavaPluginConvention> {
    sourceCompatibility = JavaVersion.VERSION_1_8
}

tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "1.8"
}

application {
    mainClassName = "$group.MainKt"
}

val jar by tasks.getting(Jar::class) {
    manifest {
        attributes["Main-Class"] = application.mainClassName
    }

    from(configurations.runtimeClasspath.get().map {
        if (it.isDirectory) it else zipTree(it)
    }) {
        exclude("META-INF/*.SF")
        exclude("META-INF/*.DSA")
        exclude("META-INF/*.RSA")
    }
}
