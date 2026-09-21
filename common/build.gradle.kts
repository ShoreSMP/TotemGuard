plugins {
    `java-library`
    id("totemguard.java.internal")
    id("totemguard.tg-version")
}

dependencies {
    testImplementation("org.junit.jupiter:junit-jupiter:5.11.4")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher:1.11.4")
    testImplementation(libs.snakeyaml)
    api(projects.api)
    api(projects.loader.host)
    implementation(projects.integrity)
    implementation(projects.bridge.protocol)
    implementation(libs.lettuce) {
        exclude(group = "org.slf4j")
    }
    implementation(libs.hikaricp) {
        exclude(group = "org.slf4j")
    }
    compileOnly(libs.mysql.jdbc)
    compileOnly(libs.packetevents.api)
    compileOnly(libs.bundles.adventure)
    compileOnly(libs.bundles.adventure.serializers)
    compileOnly(libs.bundles.adventure.minimessage)
    compileOnly(libs.guava)
    compileOnly(libs.snakeyaml)
    compileOnly(libs.cloud.core)
    compileOnly(libs.grim.api)
}

tasks.withType<Test>().configureEach { useJUnitPlatform() }
