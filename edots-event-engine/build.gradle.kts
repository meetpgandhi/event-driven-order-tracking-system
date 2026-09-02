plugins {
    `java-library`
}

dependencies {
    api(project(":edots-domain"))
    api("org.springframework.kafka:spring-kafka")
    implementation("org.springframework:spring-messaging")
    testImplementation("org.springframework.kafka:spring-kafka-test")
}
