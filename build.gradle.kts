plugins {
    id("java")
    // id("com.github.johnrengelman.shadow").version("7.1.2")
}

group = "dev.polv"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.8.1")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.8.1")

    implementation("org.bytedeco:javacv-platform:1.5.8")
    implementation("org.bytedeco:ffmpeg-platform:5.1.2-1.5.8")
}

tasks.getByName<Test>("test") {
    useJUnitPlatform()
}
