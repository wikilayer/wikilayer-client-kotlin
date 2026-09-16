plugins {
    id("com.android.library") version "9.4.0"
    `maven-publish`
    id("org.jlleitschuh.gradle.ktlint") version "14.2.0"
    id("io.gitlab.arturbosch.detekt") version "1.23.8"
    id("org.jetbrains.dokka") version "2.2.0"
}

group = "com.github.wikilayer"
version = "0.1.0"

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
    publishing { singleVariant("release") { withSourcesJar() } }
}

afterEvaluate {
    publishing {
        publications {
            create<MavenPublication>("release") {
                from(components["release"])
                artifactId = "wikilayer-client-kotlin"
            }
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
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin:2.21.1")
    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310:2.21.1")
    api("com.fasterxml.jackson.core:jackson-annotations:2.22")

    testImplementation("junit:junit:4.13.2")
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
