plugins {
    id("java")
}

group = "org.example"
version = "0.1.2"

repositories {
    mavenCentral()
}

dependencies {
    annotationProcessor("org.immutables:value:2.10.0")

    testImplementation(platform("org.junit:junit-bom:6.0.0"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")

    implementation("com.fasterxml.jackson.dataformat:jackson-dataformat-yaml:2.18.4")
    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jdk8")
    implementation("com.fasterxml.jackson.core:jackson-databind:2.18.4")
    implementation("com.fasterxml.jackson.core:jackson-annotations:2.18.4")
    implementation("io.prometheus:prometheus-metrics-core:1.9.0")
    implementation("io.prometheus:prometheus-metrics-instrumentation-jvm:1.9.0")
    implementation("io.prometheus:prometheus-metrics-exporter-httpserver:1.9.0")
    implementation(platform("org.apache.logging.log4j:log4j-bom:2.26.1"))
    implementation("org.apache.logging.log4j:log4j-api")
    implementation("org.apache.logging.log4j:log4j-core")
    implementation("org.immutables:value:2.10.0")
    implementation("org.immutables:value-annotations:2.10.0")

}

tasks.test {
    useJUnitPlatform()
}

tasks.jar {
    manifest {
        attributes(
            "Main-Class" to "org.proxy.Main"
        )
    }
    val runtimeClasspath = configurations.runtimeClasspath.get()
    from(runtimeClasspath.map { if (it.isDirectory) it else zipTree(it) })

    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
}