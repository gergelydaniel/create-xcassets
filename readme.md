## CreateXcAssets

Gradle plugin for generating `Assets.car` file based on a directory containing the asset files\
Please note that it's in experimental status.

### Motivation

Kotlin Multiplatform enables sharing code between various platforms, including iOS.
With that said, when creating a multiplatform application, it may also be necessary to
share assets between the platforms, such as images and binary resources. The goal of
this plugin is to come in handy in case the iOS binary is shared via an XCode framework, as it
allows a folder, that is already the basis for resources for other platforms
(eg assets on Android or for JAR files) to also be the source of iOS resources.

### How it works

The `CreateXcAssets` task organizes the files into the `Resources.xcassets` folder, in the same
structure XCode would expect it, then run `xcrun actool`.

Currently two kind of assets are supported:
 - Imageset
 - Dataset

The filter for files determining either the asset type for it (or to skip it) must be provided by
the project (see the example code)

### Installation

Add my repository to the plugin resolution config at `settings.gradle.kts`:
```kotlin
pluginManagement {
    repositories {
        ...
        maven("https://maven.danielgergely.com/releases")
    }
}
```

Then you can use it in your project:
```kotlin
plugins {
    id("com.danielgergely.createxcassets") version "0.0.1"
}


tasks.register<CreateXcAssets>("createXcAssets") {
    dependsOn("assembleXCFramework")

    resourcesDirectory = File(project.rootDir, "resources")
    fileFilter { file ->
        when {
            file.extension.equals("png", true) -> IMAGE
            else -> DATA
        }
    }

    doLast {
        File(project.buildDir, "XCFrameworks")
            .walkTopDown()
            .asIterable()
            .filter { it.isDirectory && it.name.endsWith(".framework") }
            .forEach { frameworkDir ->
                carFile.copyTo(File(frameworkDir, carFile.name), overwrite = true)
            }
    }
}.apply {
    tasks.getByName("build").dependsOn(get())
}
```

In this example, right after the `Assets.car` file is created, it is copied to all of the
produced frameworks
