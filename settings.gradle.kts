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

rootProject.name = "Air Controle"

include(
    ":app",
    ":core",
    ":gestures",
    ":camera",
    ":overlay",
    ":accessibility",
    ":control",
)
