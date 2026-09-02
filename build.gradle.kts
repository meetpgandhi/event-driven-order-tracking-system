plugins {
    java
    id("org.springframework.boot") version "3.4.3" apply false
    id("io.spring.dependency-management") version "1.1.7" apply false
}

allprojects {
    group = "com.meetpgandhi.edots"
    version = "1.0.0"

    repositories {
        mavenCentral()
    }
}

subprojects {
    apply(plugin = "java")
    apply(plugin = "io.spring.dependency-management")

    java {
        toolchain {
            languageVersion.set(JavaLanguageVersion.of(21))
        }
    }

    the<io.spring.gradle.dependencymanagement.dsl.DependencyManagementExtension>().apply {
        imports {
            mavenBom("org.springframework.boot:spring-boot-dependencies:3.4.3")
            mavenBom("org.testcontainers:testcontainers-bom:1.20.4")
        }
    }

    dependencies {
        add("implementation", "org.slf4j:slf4j-api")
        add("implementation", "com.fasterxml.jackson.core:jackson-databind")
        add("implementation", "com.fasterxml.jackson.datatype:jackson-datatype-jsr310")

        add("testImplementation", "org.junit.jupiter:junit-jupiter")
        add("testImplementation", "org.assertj:assertj-core")
        add("testImplementation", "org.mockito:mockito-core")
        add("testImplementation", "org.mockito:mockito-junit-jupiter")
        add("testRuntimeOnly", "org.junit.platform:junit-platform-launcher")
    }

    tasks.withType<Test> {
        useJUnitPlatform()
    }
}
