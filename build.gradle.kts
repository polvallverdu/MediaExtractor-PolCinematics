plugins {
    id("java")
    id("java-library")
    id("maven-publish")
    // id("com.github.johnrengelman.shadow").version("7.1.2")
    id("org.bytedeco.gradle-javacpp-build").version("1.5.8")
    id("org.bytedeco.gradle-javacpp-platform").version("1.5.8")
}

group = "dev.polv"
version = "0.3.1"

repositories {
    mavenCentral()
}

ext {
    set("javacppPlatform", "linux-x86_64,macosx-x86_64,windows-x86_64,etc")
    //set("javacppPlatform", "windows-x86_64")
}

dependencies {
   /* implementation("org.bytedeco:javacv-platform:1.5.8")
    implementation("org.bytedeco:ffmpeg-platform:5.1.2-1.5.8")*/


    //api("org.bytedeco:javacv:${project.extra["javacppVersion"]}")
    api("org.bytedeco:javacv-platform:${project.extra["javacppVersion"]}") {
        exclude(group = "org.bytedeco", module = "opencv-platform")
    }
    javacppPlatform("org.bytedeco:ffmpeg-platform:6.0-${project.extra["javacppVersion"]}")
}

tasks {
    withType<Jar> {
        duplicatesStrategy = DuplicatesStrategy.INCLUDE // allow duplicates
        // Otherwise you'll get a "No main manifest attribute" error
        manifest {
            attributes["Main-Class"] = "dev.polv.mediaextractor.Main"
        }

        // To add all of the dependencies otherwise a "NoClassDefFoundError" error
        from(sourceSets.main.get().output)

        dependsOn(configurations.runtimeClasspath)
        from({
            configurations.runtimeClasspath.get().filter { it.name.endsWith("jar") }.map { zipTree(it) }
        })
    }
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            groupId = project.group.toString()
            artifactId = "MediaExtractor"
            version = project.version.toString()

            from(components["java"])
        }
    }

    repositories {
        maven {
            url = uri("file://" + System.getenv("local_maven"))
        }
    }
}