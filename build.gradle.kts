plugins {
    java
}

group = "com.reandroid"
version = "1.3.9"

java {
    sourceCompatibility = JavaVersion.VERSION_1_8
    targetCompatibility = JavaVersion.VERSION_1_8
}

if (JavaVersion.current().isJava8Compatible) {
    allprojects {
        tasks.compileJava {
            //options.addStringOption("-Xlint:unchecked", "-quiet")
        }
    }
}

repositories {
    mavenCentral()
    mavenLocal()
}

dependencies {
    testImplementation("junit:junit:4.12")
}

tasks.processResources {
    filesMatching("arsclib.properties") {
        expand("version" to version)
    }
}

tasks.javadoc {
    exclude("com/reandroid/test/**")
    exclude("zorg/**")
    exclude("com/reandroid/unity/**")
    //exclude("com/reandroid/dex/**")
}

tasks.jar {
    exclude("com/reandroid/test/**")
    exclude("zorg/**")
    exclude("com/reandroid/unity/**")
    //exclude("com/reandroid/dex/**")
}

