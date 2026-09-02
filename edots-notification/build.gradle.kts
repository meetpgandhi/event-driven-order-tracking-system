plugins {
    `java-library`
}

dependencies {
    api(project(":edots-domain"))
    api(project(":edots-event-engine"))
    implementation("org.springframework.boot:spring-boot-starter-web")
    testImplementation("org.springframework.boot:spring-boot-starter-test")
}
