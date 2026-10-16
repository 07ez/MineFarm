plugins {
    id("java-library")
    // id("xyz.jpenilla.run-paper") version "3.1.0"
}

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")
    maven("https://repo.extendedclip.com/content/repositories/placeholderapi/")
}

dependencies {
    compileOnly("io.papermc.paper:paper-api:26.3.build.+")
    compileOnly("me.clip:placeholderapi:2.11.5")
}

java {
    toolchain.languageVersion = JavaLanguageVersion.of(25)
}

val copyJar = tasks.register<Copy>("copyJar") {
    from(tasks.jar.flatMap { it.archiveFile })
    into(file("D:/03 MineCraftPlugin/MineFarm/plugin"))
}

tasks.jar {
    destinationDirectory.set(file("D:/기타/01 MineFarm/plugins"))
    finalizedBy(copyJar)
}