pluginManagement {
    repositories {
        gradlePluginPortal()
        maven("https://papermc.io/repo/repository/maven-public/")
    }
}

rootProject.name = "pluto"

include("pluto-api", "paper-api-generator", "pluto-server")