plugins {
    java
    id("io.spring.dependency-management")
    id("org.flywaydb.flyway") version "10.8.1"
}

dependencies {
    implementation("org.flywaydb:flyway-core:10.8.1")
    implementation("org.flywaydb:flyway-database-postgresql:10.8.1")
    implementation("org.postgresql:postgresql:42.7.2")
}

configurations {
    create("flywayMigration") {
        extendsFrom(configurations.implementation.get())
    }
}

dependencies {
    "flywayMigration"("org.postgresql:postgresql:42.7.2")
    "flywayMigration"("org.flywaydb:flyway-database-postgresql:10.8.1")
}

flyway {
    url = System.getenv("DB_URL") ?: "jdbc:postgresql://localhost:5432/clinic"
    user = System.getenv("DB_USER") ?: "clinic_user"
    password = System.getenv("DB_PASSWORD") ?: "clinic_password"
    locations = arrayOf("filesystem:src/main/resources/db/migration")
    cleanDisabled = false
    configurations = arrayOf("flywayMigration")
}
