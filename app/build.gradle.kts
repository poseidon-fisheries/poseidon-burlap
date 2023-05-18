/*
 * This file was generated by the Gradle 'init' task.
 *
 * This generated file contains a sample Java application project to get you started.
 * For more details take a look at the 'Building Java & JVM projects' chapter in the Gradle
 * User Manual available at https://docs.gradle.org/7.6/userguide/building_java_projects.html
 */

plugins {
    // Apply the application plugin to add support for building a CLI application in Java.
    application
}

repositories {
    // Use Maven Central for resolving dependencies.
    mavenCentral()
}

dependencies {

    implementation("edu.brown.cs.burlap:burlap:3.0.1")
    implementation(fileTree(mapOf("dir" to "libs/xstream/lib", "include" to listOf("*.jar"))))
    implementation("com.beust:jcommander:1.81")
    implementation("com.opencsv:opencsv:3.8")
    implementation("de.openea:eva2:2.2.0")

    // Use JUnit test framework.
    testImplementation("junit:junit:4.13.2")

    // This dependency is used by the application.
    implementation("com.google.guava:guava:31.1-jre")
    implementation("uk.ac.ox.oxfish:POSEIDON")

    // Reference implementation of JSR-385 for units of measure
    implementation("si.uom:si-quantity:2.1")
    implementation("si.uom:si-units:2.1")

    testImplementation("org.mockito:mockito-core:4.3.1")
}

tasks.withType<Test> {
    maxHeapSize = "4G"
}

tasks.withType<Tar> {
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
}

tasks.withType<Zip> {
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
}