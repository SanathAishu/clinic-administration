plugins {
    java
    id("io.spring.dependency-management")
}

dependencies {
    // Jakarta EE
    implementation("jakarta.persistence:jakarta.persistence-api:3.1.0")
    implementation("jakarta.validation:jakarta.validation-api:3.0.2")

    // JSON
    implementation("com.fasterxml.jackson.core:jackson-annotations:2.17.0")
    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310:2.17.0")

    // Hypersistence Utils for JSONB support
    implementation("io.hypersistence:hypersistence-utils-hibernate-63:3.7.0")

    // Hibernate for @Type annotation
    implementation("org.hibernate.orm:hibernate-core:6.4.2.Final")

    // Logging
    implementation("org.slf4j:slf4j-api:2.0.9")

    // Lombok
    compileOnly("org.projectlombok:lombok:1.18.30")
    annotationProcessor("org.projectlombok:lombok:1.18.30")
}
