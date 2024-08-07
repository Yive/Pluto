import io.papermc.paperweight.util.constants.PAPERCLIP_CONFIG

plugins {
    java
    `maven-publish`
    id("io.papermc.paperweight.patcher") version "1.7.1"
}

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/") {
        content { onlyForConfigurations(PAPERCLIP_CONFIG) }
    }
}

dependencies {
    remapper("net.fabricmc:tiny-remapper:0.10.3:fat")
    decompiler("org.vineflower:vineflower:1.10.1")
    paperclip("io.papermc:paperclip:3.0.3")
}

subprojects {
    repositories {
        mavenCentral()
        maven("https://repo.papermc.io/repository/maven-public/")
        maven("https://jitpack.io")
    }
}

paperweight {
    serverProject.set(project(":pluto-server"))

    remapRepo.set("https://repo.papermc.io/repository/maven-public/")
    decompileRepo.set("https://repo.papermc.io/repository/maven-public/")

    useStandardUpstream("pufferfish") {
        url.set(github("pufferfish-gg", "Pufferfish"))
        ref.set(providers.gradleProperty("pufferfishRef"))

        withStandardPatcher {
            apiSourceDirPath.set("pufferfish-api")
            serverSourceDirPath.set("pufferfish-server")

            apiOutputDir.set(layout.projectDirectory.dir("pluto-api"))
            serverOutputDir.set(layout.projectDirectory.dir("pluto-server"))
        }

        patchTasks {
            register("PaperApiGenerator") {
                isBareDirectory.set(true)
                upstreamDirPath.set("paper-api-generator/generated")
                patchDir.set(layout.projectDirectory.dir("patches/paper-api-generator"))
                outputDir.set(layout.projectDirectory.dir("paper-api-generator/generated"))
            }
        }
    }
}

allprojects {
    apply(plugin = "java")
    apply(plugin = "maven-publish")

    java {
        toolchain {
            languageVersion.set(JavaLanguageVersion.of(21))
        }
    }

    tasks.withType<JavaCompile>().configureEach {
        options.encoding = Charsets.UTF_8.name()
        options.release.set(21)
    }
    tasks.withType<Javadoc> {
        options.encoding = Charsets.UTF_8.name()
    }
    tasks.withType<ProcessResources> {
        filteringCharset = Charsets.UTF_8.name()
    }

    publishing {
        repositories {
            maven {
                name = "yiveRepo"
                url = uri("https://repo.yive.dev/snapshots")
                credentials {
                    username = (System.getenv("YIVE_REPO_USERNAME") ?: project.property("yiveRepoUsername")).toString()
                    password = (System.getenv("YIVE_REPO_PASSWORD") ?: project.property("yiveRepoPassword")).toString()
                }
            }
        }
    }
}

tasks.generateDevelopmentBundle {
    apiCoordinates.set("dev.yive.pluto:pluto-api")
    libraryRepositories.set(
        listOf(
            "https://repo.maven.apache.org/maven2/",
            "https://repo.papermc.io/repository/maven-public/",
            "https://repo.yive.dev/snapshots",
        )
    )
}

publishing {
    publications.create<MavenPublication>("devBundle") {
        artifact(tasks.generateDevelopmentBundle) {
            artifactId = "dev-bundle"
        }
    }
}
