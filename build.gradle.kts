import com.vanniktech.maven.publish.AndroidSingleVariantLibrary
import com.vanniktech.maven.publish.DeploymentValidation
import com.vanniktech.maven.publish.JavadocJar
import com.vanniktech.maven.publish.SourcesJar

plugins {
    id("com.android.library") version "9.4.1"
    id("com.vanniktech.maven.publish") version "0.37.0"
    id("org.jlleitschuh.gradle.ktlint") version "14.2.0"
    id("io.gitlab.arturbosch.detekt") version "1.23.8"
    id("org.jetbrains.dokka") version "2.2.0"
}

group = "org.wikilayer"
version =
    requireNotNull(
        Regex("""^## (\d+\.\d+\.\d+)$""", RegexOption.MULTILINE)
            .find(file("CHANGELOG.md").readText()),
    ) { "CHANGELOG.md has no released version heading" }.groupValues[1]

android {
    namespace = "org.wikilayer.client"
    compileSdk = 37

    defaultConfig { minSdk = 28 }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlin {
        compilerOptions {
            jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
            allWarningsAsErrors.set(true)
        }
    }
}

mavenPublishing {
    configure(
        AndroidSingleVariantLibrary(
            javadocJar = JavadocJar.Dokka("dokkaGeneratePublicationHtml"),
            sourcesJar = SourcesJar.Sources(),
            variant = "release",
        ),
    )
    publishToMavenCentral(automaticRelease = true, validateDeployment = DeploymentValidation.PUBLISHED)
    if (!providers.gradleProperty("unsignedLocalPublish").isPresent) {
        signAllPublications()
    }
    coordinates("org.wikilayer", "wikilayer-client-kotlin", version.toString())
    pom {
        name.set("Wikilayer Client for Kotlin")
        description.set("The Kotlin/Android client for Wikilayer's API.")
        url.set("https://github.com/wikilayer/wikilayer-client-kotlin")
        licenses {
            license {
                name.set("MIT License")
                url.set("https://opensource.org/licenses/MIT")
            }
        }
        developers {
            developer {
                id.set("wikilayer")
                name.set("Wikilayer")
                url.set("https://github.com/wikilayer")
            }
        }
        scm {
            url.set("https://github.com/wikilayer/wikilayer-client-kotlin")
            connection.set("scm:git:https://github.com/wikilayer/wikilayer-client-kotlin.git")
            developerConnection.set("scm:git:ssh://git@github.com/wikilayer/wikilayer-client-kotlin.git")
        }
    }
}

dokka {
    dokkaPublications.html {
        moduleName.set("Wikilayer Client for Kotlin")
        moduleVersion.set(project.version.toString())
        outputDirectory.set(layout.buildDirectory.dir("dokka/html"))
        includes.from("docs/module.md")
    }
    dokkaSourceSets.configureEach {
        sourceRoots.from(file("src/main/java"))
        sourceLink {
            localDirectory.set(file("src/main/java"))
            remoteUrl.set(uri("https://github.com/wikilayer/wikilayer-client-kotlin/tree/main/src/main/java"))
            remoteLineSuffix.set("#L")
        }
    }
}

dependencies {
    api("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.11.0")
    implementation("com.squareup.okhttp3:okhttp:5.5.0")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin:2.22.3")
    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310:2.21.1")
    api("com.fasterxml.jackson.core:jackson-annotations:2.22")

    testImplementation("junit:junit:4.13.2")
    testImplementation("com.fasterxml.jackson.dataformat:jackson-dataformat-yaml:2.22.3")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.11.0")
    testImplementation("com.squareup.okhttp3:mockwebserver:5.5.0")
    testImplementation("org.robolectric:robolectric:4.16.1")
    testImplementation("androidx.test:core:1.7.0")
}

ktlint {
    version.set("1.7.1")
    android.set(true)
    outputToConsole.set(true)
    ignoreFailures.set(false)
}

detekt {
    buildUponDefaultConfig = true
    allRules = false
}
