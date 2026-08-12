import com.android.build.api.dsl.LibraryExtension
import org.gradle.api.publish.maven.MavenPublication

plugins {
    id("com.android.library") version "9.3.1"
    id("org.jetbrains.kotlin.plugin.compose") version "2.4.10"
    `maven-publish`
}

group = "com.github.woojaeHEO"
version = System.getenv("VERSION") ?: "1.2.1"

android {
    namespace = "io.github.woojaeheo.prismglass"
    compileSdk {
        version = release(37) { minorApiLevel = 1 }
    }

    defaultConfig {
        minSdk = 26
    }

    buildFeatures {
        compose = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    publishing {
        singleVariant("release") {
            withSourcesJar()
        }
    }

    testOptions {
        unitTests.isIncludeAndroidResources = true
    }
}

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2026.06.01")
    api(composeBom)
    api("androidx.compose.ui:ui")
    api("androidx.compose.foundation:foundation")
    api("androidx.compose.material3:material3")
    testImplementation("junit:junit:4.13.2")
}

afterEvaluate {
    publishing {
        publications {
            create<MavenPublication>("release") {
                from(components["release"])
                artifactId = "PrismGlass"
                pom {
                    name.set("PrismGlass")
                    description.set("Adaptive liquid glass surfaces and navigation for Jetpack Compose")
                    url.set("https://github.com/woojaeHEO/PrismGlass")
                    licenses {
                        license {
                            name.set("The Apache License, Version 2.0")
                            url.set("https://www.apache.org/licenses/LICENSE-2.0.txt")
                        }
                    }
                    developers {
                        developer {
                            id.set("woojaeHEO")
                            name.set("woojae.heo")
                        }
                    }
                    scm {
                        url.set("https://github.com/woojaeHEO/PrismGlass")
                        connection.set("scm:git:git://github.com/woojaeHEO/PrismGlass.git")
                        developerConnection.set("scm:git:ssh://git@github.com/woojaeHEO/PrismGlass.git")
                    }
                }
            }
        }
    }
}
