import org.gradle.api.tasks.testing.logging.TestExceptionFormat
import org.gradle.api.tasks.testing.logging.TestLogEvent

plugins {
    java
    id("io.papermc.paperweight.patcher") version "2.0.0-beta.17"
}

val paperMavenPublicUrl = "https://repo.papermc.io/repository/maven-public/"

paperweight {
    upstreams.register("pufferfish") {
        repo = github("Yive", "Tetraodontidae")
        ref = providers.gradleProperty("pufferfishRef")

        patchFile {
            path = "pufferfish-server/build.gradle.kts"
            outputFile = file("pluto-server/build.gradle.kts")
            patchFile = file("pluto-server/build.gradle.kts.patch")
        }
        patchFile {
            path = "pufferfish-api/build.gradle.kts"
            outputFile = file("pluto-api/build.gradle.kts")
            patchFile = file("pluto-api/build.gradle.kts.patch")
        }
        patchRepo("paperApi") {
            upstreamPath = "paper-api"
            patchesDir = file("pluto-api/paper-patches")
            outputDir = file("paper-api")
        }
        patchDir("pufferfishApi") {
            upstreamPath = "pufferfish-api"
            excludes = listOf("build.gradle.kts", "build.gradle.kts.patch", "paper-patches")
            patchesDir = file("pluto-api/pufferfish-patches")
            outputDir = file("pufferfish-api")
        }
    }
}

subprojects {
    apply(plugin = "java-library")
    apply(plugin = "maven-publish")

    extensions.configure<JavaPluginExtension> {
        toolchain {
            languageVersion = JavaLanguageVersion.of(21)
        }
    }

    repositories {
        mavenCentral()
        maven(paperMavenPublicUrl)
    }

    tasks.withType<AbstractArchiveTask>().configureEach {
        isPreserveFileTimestamps = false
        isReproducibleFileOrder = true
    }
    tasks.withType<JavaCompile> {
        options.encoding = Charsets.UTF_8.name()
        options.release = 21
        options.isFork = true
    }
    tasks.withType<Javadoc> {
        options.encoding = Charsets.UTF_8.name()
    }
    tasks.withType<ProcessResources> {
        filteringCharset = Charsets.UTF_8.name()
    }
    tasks.withType<Test> {
        testLogging {
            showStackTraces = true
            exceptionFormat = TestExceptionFormat.FULL
            events(TestLogEvent.STANDARD_OUT)
        }
    }

    extensions.configure<PublishingExtension> {
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
