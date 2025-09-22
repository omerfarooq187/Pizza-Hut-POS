import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
    kotlin("jvm") version "1.9.20"
    id("org.jetbrains.compose") version "1.5.10"
}


group = "com.example"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
    maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
}

dependencies {
    implementation(compose.desktop.currentOs)
    implementation("org.jetbrains.exposed:exposed-core:0.44.1")
    implementation("org.jetbrains.exposed:exposed-dao:0.44.1")
    implementation("org.jetbrains.exposed:exposed-jdbc:0.44.1")
    implementation("org.jetbrains.exposed:exposed-jodatime:0.44.1")
    implementation("org.xerial:sqlite-jdbc:3.43.2.2")
    implementation("io.insert-koin:koin-core:3.5.3")
    implementation("io.insert-koin:koin-compose:1.1.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-swing:1.7.3")
    implementation("org.slf4j:slf4j-simple:2.0.9")
    implementation("joda-time:joda-time:2.14.0")
    // Compose dependencies (auto-versioned with the plugin)
    implementation(compose.material3)
    implementation(compose.materialIconsExtended)

    implementation("com.itextpdf:itext7-core:7.2.3")
    implementation("org.apache.poi:poi:5.2.2")


    implementation("com.github.anastaciocintra:escpos-coffee:4.1.0")
    implementation("org.jetbrains.compose.foundation:foundation:1.5.10")
    implementation("org.jetbrains.skiko:skiko-awt-runtime-windows-x64:0.7.80")

    implementation("com.google.api-client:google-api-client:2.0.0")
    implementation("com.google.oauth-client:google-oauth-client-jetty:1.34.1")
    implementation("com.google.apis:google-api-services-drive:v3-rev20230212-2.0.0")
    implementation("com.google.auth:google-auth-library-oauth2-http:1.19.0")
    implementation(kotlin("stdlib-jdk8"))

}

compose.desktop {
    application {
        mainClass = "MainKt"

        nativeDistributions {
            // Target formats
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)

            // Package metadata
            packageName = "Pizza Hut POS"
            packageVersion = "1.1.1"
            description = "Pizza Point of Sale System"
            vendor = "Mandra Pizza Hut"
            copyright = "© 2025 Mandra Pizza Hut"

            // Include all modules to bundle JVM
            includeAllModules = true
            modules(
                "java.instrument",
                "java.management",
                "jdk.unsupported",
                "java.sql",
                "jdk.crypto.ec"  // Add if using encryption
            )

            // Windows-specific configuration
            windows {
                menuGroup = "Pizza Hut POS"
                // Generate UUID: https://www.uuidgenerator.net/
                upgradeUuid = "5f5a1c30-1234-5678-9abc-def012345678"

                // Set icon (must be .ico format)
                iconFile.set(project.file("src/main/resources/logo/logo.ico"))

                // Create desktop shortcut
                shortcut = true

                // Installation options
                dirChooser = true
                perUserInstall = true

                // Add start menu entry
                menu = true
            }
        }

    }
}

tasks.processResources {
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
}

tasks.withType<JavaExec> {
    environment("XMODIFIERS", "")
}

sourceSets.main {
    resources.srcDirs("src/main/resources")
}
kotlin {
    jvmToolchain(21)
}