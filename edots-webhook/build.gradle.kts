plugins {
    `java-library`
}

dependencies {
    api(project(":edots-domain"))
    api(project(":edots-event-engine"))
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    compileOnly("io.swagger.core.v3:swagger-annotations-jakarta:2.2.28")
    testImplementation("org.springframework.boot:spring-boot-starter-test")
}
