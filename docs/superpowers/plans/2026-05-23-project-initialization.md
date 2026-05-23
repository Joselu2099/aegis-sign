# Project Scaffolding Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Initialize the Spring Boot project with Maven, reactive dependencies, and the Hexagonal Architecture directory structure.

**Architecture:** Hexagonal Architecture (Ports and Adapters). The structure separates `domain`, `application`, and `infrastructure` layers.

**Tech Stack:** Java 21, Spring Boot 3.x, Spring WebFlux, R2DBC, PostgreSQL, Redis, Flyway, Maven.

---

### Task 1: Create Base Directory Structure

**Files:**
- Create: `src/main/java/com/aegis/sign/domain`
- Create: `src/main/java/com/aegis/sign/application/ports/in`
- Create: `src/main/java/com/aegis/sign/application/ports/out`
- Create: `src/main/java/com/aegis/sign/application/services`
- Create: `src/main/java/com/aegis/sign/infrastructure/adapters/in/web`
- Create: `src/main/java/com/aegis/sign/infrastructure/adapters/out/persistence`
- Create: `src/main/java/com/aegis/sign/infrastructure/config`
- Create: `src/main/resources/db/migration`
- Create: `src/test/java/com/aegis/sign`

- [ ] **Step 1: Create directories**
Run: `mkdir -p src/main/java/com/aegis/sign/domain src/main/java/com/aegis/sign/application/ports/in src/main/java/com/aegis/sign/application/ports/out src/main/java/com/aegis/sign/application/services src/main/java/com/aegis/sign/infrastructure/adapters/in/web src/main/java/com/aegis/sign/infrastructure/adapters/out/persistence src/main/java/com/aegis/sign/infrastructure/config src/main/resources/db/migration src/test/java/com/aegis/sign`

- [ ] **Step 2: Verify directory structure**
Run: `find src -type d`
Expected: Directories match the Hexagonal Architecture layers.

- [ ] **Step 3: Commit**
```bash
git add src
git commit -m "chore: initialize directory structure for hexagonal architecture"
```

---

### Task 2: Create pom.xml with Dependencies

**Files:**
- Create: `pom.xml`

- [ ] **Step 1: Write pom.xml**

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
	xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
	<modelVersion>4.0.0</modelVersion>
	<parent>
		<groupId>org.springframework.boot</groupId>
		<artifactId>spring-boot-starter-parent</artifactId>
		<version>3.2.5</version>
		<relativePath/> <!-- lookup parent from repository -->
	</parent>
	<groupId>com.aegis</groupId>
	<artifactId>sign</artifactId>
	<version>0.0.1-SNAPSHOT</version>
	<name>aegis-sign</name>
	<description>Self-hosted Identity Verification and Electronic Signature Service</description>
	<properties>
		<java.version>21</java.version>
		<springdoc.version>2.5.0</springdoc.version>
		<testcontainers.version>1.19.7</testcontainers.version>
		<archunit.version>1.2.1</archunit.version>
	</properties>
	<dependencies>
		<!-- WebFlux -->
		<dependency>
			<groupId>org.springframework.boot</groupId>
			<artifactId>spring-boot-starter-webflux</artifactId>
		</dependency>

		<!-- Persistence: R2DBC & PostgreSQL -->
		<dependency>
			<groupId>org.springframework.boot</groupId>
			<artifactId>spring-boot-starter-data-r2dbc</artifactId>
		</dependency>
		<dependency>
			<groupId>org.postgresql</groupId>
			<artifactId>postgresql</artifactId>
			<scope>runtime</scope>
		</dependency>
		<dependency>
			<groupId>org.postgresql</groupId>
			<artifactId>r2dbc-postgresql</artifactId>
			<scope>runtime</scope>
		</dependency>

		<!-- Redis Reactive -->
		<dependency>
			<groupId>org.springframework.boot</groupId>
			<artifactId>spring-boot-starter-data-redis-reactive</artifactId>
		</dependency>

		<!-- Flyway Migration -->
		<dependency>
			<groupId>org.flywaydb</groupId>
			<artifactId>flyway-core</artifactId>
		</dependency>
		<dependency>
			<groupId>org.flywaydb</groupId>
			<artifactId>flyway-database-postgresql</artifactId>
		</dependency>

		<!-- Documentation: OpenAPI -->
		<dependency>
			<groupId>org.springdoc</groupId>
			<artifactId>springdoc-openapi-starter-webflux-ui</artifactId>
			<version>${springdoc.version}</version>
		</dependency>

		<!-- Actuator -->
		<dependency>
			<groupId>org.springframework.boot</groupId>
			<artifactId>spring-boot-starter-actuator</artifactId>
		</dependency>

		<!-- Utilities -->
		<dependency>
			<groupId>org.projectlombok</groupId>
			<artifactId>lombok</artifactId>
			<optional>true</optional>
		</dependency>

		<!-- Testing -->
		<dependency>
			<groupId>org.springframework.boot</groupId>
			<artifactId>spring-boot-starter-test</artifactId>
			<scope>test</scope>
		</dependency>
		<dependency>
			<groupId>io.projectreactor</groupId>
			<artifactId>reactor-test</artifactId>
			<scope>test</scope>
		</dependency>
		<dependency>
			<groupId>org.springframework.boot</groupId>
			<artifactId>spring-boot-testcontainers</artifactId>
			<scope>test</scope>
		</dependency>
		<dependency>
			<groupId>org.testcontainers</groupId>
			<artifactId>junit-jupiter</artifactId>
			<scope>test</scope>
		</dependency>
		<dependency>
			<groupId>org.testcontainers</groupId>
			<artifactId>postgresql</artifactId>
			<scope>test</scope>
		</dependency>
		<dependency>
			<groupId>com.tngtech.archunit</groupId>
			<artifactId>archunit-junit5</artifactId>
			<version>${archunit.version}</version>
			<scope>test</scope>
		</dependency>
	</dependencies>

	<build>
		<plugins>
			<plugin>
				<groupId>org.springframework.boot</groupId>
				<artifactId>spring-boot-maven-plugin</artifactId>
				<configuration>
					<excludes>
						<exclude>
							<groupId>org.projectlombok</groupId>
							<artifactId>lombok</artifactId>
						</exclude>
					</excludes>
				</configuration>
			</plugin>
		</plugins>
	</build>
</project>
```

- [ ] **Step 2: Verify pom.xml syntax**
Run: `mvn help:effective-pom -DskipTests` (Note: requires mvn installed, or just check file exists)
Expected: No XML errors.

- [ ] **Step 3: Commit**
```bash
git add pom.xml
git commit -m "chore: add pom.xml with reactive and testing dependencies"
```

---

### Task 3: Create Main Application Class

**Files:**
- Create: `src/main/java/com/aegis/sign/AegisSignApplication.java`

- [ ] **Step 1: Write AegisSignApplication.java**

```java
package com.aegis.sign;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class AegisSignApplication {

    public static void main(String[] args) {
        SpringApplication.run(AegisSignApplication.class, args);
    }
}
```

- [ ] **Step 2: Verify compilation**
Run: `mvn compile`
Expected: BUILD SUCCESS

- [ ] **Step 3: Commit**
```bash
git add src/main/java/com/aegis/sign/AegisSignApplication.java
git commit -m "feat: add main application entry point"
```
