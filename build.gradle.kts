plugins {
    id("java")
}

group = "net.savagedev"
version = "1.0.1"

repositories {
    maven("https://hub.spigotmc.org/nexus/content/repositories/snapshots")

    mavenCentral()
}

dependencies {
    compileOnly("org.spigotmc:spigot-api:1.20-R0.1-SNAPSHOT")
}

tasks {
    processResources {
        duplicatesStrategy = DuplicatesStrategy.INCLUDE

        from(sourceSets.main.get().resources.srcDirs) {
            expand(Pair("version", project.version))
                .include("plugin.yml")
        }
    }
}
