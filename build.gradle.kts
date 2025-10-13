plugins {
    id("fabric-loom") version "1.11-SNAPSHOT"
}

version = "0.1.0"
group = "net.blosson.lflagger"

repositories {
    mavenCentral()
    maven { url = uri("https://maven.fabricmc.net/") }
    maven { url = uri("https://libraries.minecraft.net/") }
}

dependencies {
    minecraft("com.mojang:minecraft:1.21.10")
    mappings("net.fabricmc:yarn:1.21.10+build.2")
    modImplementation("net.fabricmc:fabric-loader:0.17.3")
    modImplementation("net.fabricmc.fabric-api:fabric-api:0.135.0+1.21.10")
}

loom {
    accessWidenerPath.set(file("src/main/resources/lflagger.accesswidener"))
}

tasks.withType<JavaCompile> {
    options.encoding = "UTF-8"
    options.release.set(21)
}

java {
    sourceCompatibility = JavaVersion.VERSION_21
    targetCompatibility = JavaVersion.VERSION_21
}