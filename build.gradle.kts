plugins {
    kotlin("jvm") version "2.1.0"
    kotlin("plugin.serialization") version "2.1.0"
    application
    id("org.jetbrains.kotlinx.kover") version "0.7.5"
}

val ktorVersion = "3.2.0"
val kmongoVersion = "4.11.0"
val logback = "1.5.18"
val ktorPluginsVersion = "2.2.1"
val koinVersion = "3.5.3"

repositories {
    mavenCentral()
}

dependencies {
    implementation("io.ktor:ktor-server-core-jvm:$ktorVersion")
    implementation("io.ktor:ktor-server-netty-jvm:$ktorVersion")
    implementation("io.ktor:ktor-server-content-negotiation-jvm:$ktorVersion")
    implementation("io.ktor:ktor-server-auth-jvm:$ktorVersion")
    implementation("io.ktor:ktor-server-auth-jwt-jvm:$ktorVersion")
    implementation("io.ktor:ktor-server-resources-jvm:$ktorVersion")
    implementation("io.ktor:ktor-server-status-pages-jvm:$ktorVersion")
    implementation("io.ktor:ktor-server-call-logging-jvm:$ktorVersion")
    implementation("io.ktor:ktor-server-cors-jvm:$ktorVersion")
    implementation("io.ktor:ktor-server-swagger-jvm:$ktorVersion")
    implementation("io.ktor:ktor-serialization-kotlinx-json-jvm:$ktorVersion")
    implementation("io.ktor:ktor-client-core-jvm:$ktorVersion")
    implementation("io.ktor:ktor-client-cio-jvm:$ktorVersion")
    implementation("io.ktor:ktor-client-content-negotiation-jvm:$ktorVersion")
    implementation("io.ktor:ktor-server-rate-limit-jvm:$ktorVersion")
    implementation("io.github.flaxoos:ktor-server-task-scheduling-core:$ktorPluginsVersion")
    implementation("io.github.flaxoos:ktor-server-task-scheduling-mongodb:$ktorPluginsVersion")
    implementation("org.jetbrains.kotlinx:kotlinx-datetime:0.5.0")
    implementation("org.litote.kmongo:kmongo-coroutine-serialization:$kmongoVersion")
    implementation(kotlin("reflect"))
    implementation("ch.qos.logback:logback-classic:$logback")
    implementation("io.insert-koin:koin-core:$koinVersion")
    implementation("io.insert-koin:koin-ktor:$koinVersion")
    implementation("io.insert-koin:koin-logger-slf4j:$koinVersion")
    implementation("org.mindrot:jbcrypt:0.4")
    testImplementation("io.ktor:ktor-client-mock:$ktorVersion")
    testImplementation("io.mockk:mockk:1.13.9")
    testRuntimeOnly("io.mockk:mockk-agent-jvm:1.13.9")
    testImplementation(kotlin("test"))
}

application {
    mainClass.set("pl.cuyer.thedome.ApplicationKt")
}

kotlin {
    jvmToolchain(21)
}

tasks.test {
    useJUnitPlatform()
    testLogging {
        events("passed", "skipped", "failed")
    }
}

koverReport {
    filters {
        excludes {
            classes("pl.cuyer.thedome.domain.rust.*")
            classes("pl.cuyer.thedome.domain.server.*")
            classes("pl.cuyer.thedome.resources.*")
            classes("pl.cuyer.thedome.ApplicationKt")
        }
        includes {
            classes("pl.cuyer.thedome.domain.server.WipeSchedule")
            classes("pl.cuyer.thedome.domain.battlemetrics.ServerExtensionsKt")
        }
    }
}
