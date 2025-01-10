import org.gradle.api.tasks.testing.logging.TestExceptionFormat
import org.gradle.api.tasks.testing.logging.TestLogEvent

plugins {
    java
    id("io.papermc.paperweight.patcher") version "2.0.0-SNAPSHOT" // Using local build of PR-279
}

val paperMavenPublicUrl = "https://repo.papermc.io/repository/maven-public/"

paperweight {
    upstreams.register("pufferfish") {
        repo = github("Yive", "Pufferfish")
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
        patchRepo("paperApiGenerator") {
            upstreamPath = "paper-api-generator"
            patchesDir = file("pluto-api-generator/paper-patches")
            outputDir = file("paper-api-generator")
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
        maven("https://oss.sonatype.org/content/groups/public/")
        maven("https://ci.emc.gs/nexus/content/groups/aikar/")
        maven("https://repo.aikar.co/content/groups/aikar")
        maven("https://repo.md-5.net/content/repositories/releases/")
        maven("https://hub.spigotmc.org/nexus/content/groups/public/")
        maven("https://jitpack.io")
    }

    dependencies {
        "testRuntimeOnly"("org.junit.platform:junit-platform-launcher")
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
            /*
            maven("https://repo.papermc.io/repository/maven-snapshots/") {
                name = "paperSnapshots"
                credentials(PasswordCredentials::class)
            }
             */
        }
    }
}
