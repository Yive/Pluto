pluginManagement {
    repositories {
        gradlePluginPortal()
        maven("https://repo.papermc.io/repository/maven-public/")
    }
}

rootProject.name = "pluto"

include("pluto-api", "pluto-api-generator", "pluto-server")