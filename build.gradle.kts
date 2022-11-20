plugins {
    `java-gradle-plugin`
    `maven-publish`
    id("org.jetbrains.kotlin.jvm") version "1.7.21"
}

group = "com.danielgergely.createxcassets"
version = "0.0.1"

repositories {
    mavenLocal()
    mavenCentral()
    google()
}

gradlePlugin {
    plugins {
        register("createxcassets") {
            id = "com.danielgergely.createxcassets"
            implementationClass = "com.danielgergely.createxcassets.CreateXcAssetsPlugin"
        }
    }
}

dependencies {
    implementation("org.json:json:20220924")
}

val sourcesJar by tasks.registering(Jar::class) {
    archiveClassifier.set("sources")
    from(sourceSets.main.get().allSource)
}

publishing {
    repositories {
        val publishingSetup = project.getPublishingSetup()

        if (publishingSetup != null) {
            maven {
                setUrl(publishingSetup.url)
                credentials {
                    username = publishingSetup.username
                    password = publishingSetup.password
                }
            }
        }
    }

    publications {
        register<MavenPublication>("mavenSources") {
            from(components["java"])
            artifact(sourcesJar.get())
        }
    }
}

data class PublishingSetup(
    val url: String,
    val username: String,
    val password: String,
)

fun Project.getPublishingSetup(): PublishingSetup? {
    val url = getLocalProperty("MAVEN_PUBLISH_URL")
    val user = getLocalProperty("MAVEN_PUBLISH_USER")
    val password = getLocalProperty("MAVEN_PUBLISH_PASS")

    return if (url != null && user != null && password != null) {
        PublishingSetup(url, user, password)
    } else {
        null
    }
}

fun Project.getLocalProperty(key: String): String? =
    rootDir.resolve("local.properties")
        .let { file ->
            if (file.exists()) {
                file.readLines()
            } else {
                null
            }
        }?.let { lines ->
            lines.find { it.startsWith(key) }?.substringAfter("=")
        } ?: System.getenv(key)