# Fresh Kotlin Implementation — Detailed Plan

**Version**: 2.0  
**Phase**: Execution Planning

---

## 1. Module Overview

### 1.1 `banking-commons`
This is a shared library containing common domain models, DTOs, security filters, and utilities.
It will be a dependency for both `auth-service` and `core-service`.

#### Key Files
- **Domain Models**: `Account.kt`, `Balance.kt`, `Card.kt` (all simple data classes)
- **Security**: `JwtUtil.kt` (token generation/validation), `ReactiveJwtFilter.kt` (Spring WebFlux filter)
- **DTOs**: Standardized error and response objects

### 1.2 `auth-service`
A lightweight, synchronous service responsible solely for authentication.

#### Key Files
- **Controller**: `AuthController.kt` (login endpoints)
- **Service**: `AuthApplicationService.kt` (business logic for auth)
- **Configuration**: `SecurityConfig.kt` (WebFlux security config allowing unauthenticated access to login)

### 1.3 `core-service`
The primary business logic service dealing with R2DBC data access and complex application flows.

#### Key Files
- **Controllers**: `AccountController.kt`, `HomeController.kt`
- **Services**: `AccountApplicationService.kt` (orchestrates use cases)
- **Repositories**: `AccountRepository.kt` (R2DBC interface), `AccountRepositoryAdapter.kt` (Port implementation)

---

## 2. Implementation Walkthrough

### 2.1 Setting up Gradle Dependencies

In each module's `build.gradle.kts`, ensure these foundational dependencies:

```kotlin
dependencies {
    implementation("org.jetbrains.kotlin:kotlin-stdlib")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-reactor")
    
    implementation("org.springframework.boot:spring-boot-starter-webflux")
    
    testImplementation("io.kotest:kotest-runner-junit5:5.7.0")
    testImplementation("io.mockk:mockk:1.13.5")
}
```
*Note: `core-service` will additionally require `spring-boot-starter-data-r2dbc` and `org.postgresql:r2dbc-postgresql`.*

### 2.2 Core Service Repository Example

Using R2DBC with Coroutines means leveraging `CoroutineCrudRepository`.

```kotlin
import org.springframework.data.repository.kotlin.CoroutineCrudRepository

interface AccountRepository : CoroutineCrudRepository<Account, Long> {
    suspend fun findByCustomerId(customerId: String): List<Account>
}
```

And mapping it via an Adapter to satisfy the Hexagonal Architecture Port:

```kotlin
@Component
class AccountRepositoryAdapter(
    private val repository: AccountRepository
) : AccountRepositoryPort {
    override suspend fun findByCustomerId(customerId: String): List<Account> {
        return repository.findByCustomerId(customerId)
    }
}
```

### 2.3 WebFlux Controller Example

Controllers use the `suspend` keyword to seamlessly handle async logic without wrapping responses in `Mono` or `Flux`.

```kotlin
@RestController
@RequestMapping("/api/accounts")
class AccountController(
    private val service: AccountApplicationService
) {
    @GetMapping("/{customerId}")
    suspend fun getAccounts(@PathVariable customerId: String): ResponseEntity<List<Account>> {
        val accounts = service.getByCustomerId(customerId)
        return ResponseEntity.ok(accounts)
    }
}
```

---

## 3. Verification Plan

Each module must be verified independently before progressing.

### Testing Commands

Run unit and integration tests using Gradle:
```bash
# Run tests for a specific module
./gradlew :banking-commons:test
./gradlew :auth-service:test
./gradlew :core-service:test

# Run all tests and generate coverage report
./gradlew clean test jacocoTestReport
```

### Coverage Expectations
- Target: **80% Minimum** line coverage across all modules.
- Ensure to check `build/reports/jacoco/test/html/index.html` after running coverage.
