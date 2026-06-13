pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "Solomon"

include(":app")
include(":core")
include(":storage")
include(":analytics")
include(":email")
include(":web")
include(":llm")
include(":moments")
