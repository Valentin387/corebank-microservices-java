# Fresh Project Bootstrap Guide

## Step 1: Create Project via Spring Initializr

Visit: https://start.spring.io/

**Configure**:
- Project: Gradle - Kotlin
- Language: Kotlin
- Spring Boot: 4.0.6
- Packaging: Jar
- Java: 21

**Dependencies** (search & add):
- Spring WebFlux
- Spring Data R2DBC
- Spring Security
- PostgreSQL Driver
- Kotlin Coroutines Reactor

**Project name**: `corebank-microservices-kotlin`

Click "Generate" → Download ZIP

## Step 2: Extract & Open in IntelliJ

```bash
unzip corebank-microservices-kotlin.zip
cd corebank-microservices-kotlin
```

Open in IntelliJ:
- File → Open → select project folder
- Let Gradle sync
- Trust project

## Step 3: Create Module Structure

```
corebank-microservices-kotlin/
├── banking-commons/
│   ├── src/main/kotlin/
│   ├── src/test/kotlin/
│   └── build.gradle.kts
├── auth-service/
│   ├── src/main/kotlin/
│   ├── src/test/kotlin/
│   └── build.gradle.kts
├── core-service/
│   ├── src/main/kotlin/
│   ├── src/test/kotlin/
│   └── build.gradle.kts
├── settings.gradle.kts
└── build.gradle.kts
```

See IMPLEMENTATION_PLAN.md for detailed module setup.

## Step 4: Verify Build

```bash
./gradlew clean build
```

**Expected**: All tests pass, build succeeds

## Step 5: Configure IDE

IntelliJ → Preferences:
- Build, Execution, Deployment → Compiler → Kotlin Compiler
  - Target JVM version: 21
  - Strict mode: Enable

## Ready!

Proceed to IMPLEMENTATION_PLAN.md