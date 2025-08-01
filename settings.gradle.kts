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
        maven { url = uri("https://jitpack.io") } // For UCrop
    }
}
dependencyResolutionManagement {
    repositories {
        mavenCentral()
    }

}



rootProject.name = "Instant_Chat"
include(":app")
