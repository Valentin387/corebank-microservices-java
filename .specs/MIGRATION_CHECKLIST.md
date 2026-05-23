# Phase 4 Migration Checklist

**Purpose**: Step-by-step execution checklist. Mark items as completed during implementation.

**Format**: `[ ]` = not started, `[x]` = completed, `[?]` = blocked/on hold

---

## PHASE 0: Pre-Implementation Setup

### Git & Environment

- [ ] Git branch created: `git checkout -b feature/phase-4-kotlin-migration`
- [ ] Branch protection configured (if using shared repo)
- [ ] Local environment: Kotlin 1.9.22+ installed (`kotlinc -version`)
- [ ] Gradle 8.13+ available (`./gradlew --version`)
- [ ] Java 21 in JAVA_HOME (`java -version`)
- [ ] Docker Desktop running (for Postgres + Redis)

### Repository Cleanliness

- [ ] Phase 2 `main` branch is clean (no uncommitted changes)
- [ ] Create tag: `git tag phase-2-baseline`
- [ ] `.gitignore` updated to exclude: `*.class`, `build/`, `.gradle/`

---

## PHASE 1: Gradle Configuration (1 hour)

### Root `build.gradle.kts`

- [ ] Read current root `build.gradle.kts`
- [ ] Add Kotlin JVM plugin: `id("org.jetbrains.kotlin.jvm") version "1.9.22"`
- [ ] Add Kotlin Spring plugin: `id("org.jetbrains.kotlin.plugin.spring") version "1.9.22"`
- [ ] Define Kotlin version in `extra`: `extra["kotlinVersion"] = "1.9.22"`
- [ ] Add kotlinx-coroutines BOM: version `1.7.3`
- [ ] Add Kotest BOM: version `5.7.0`
- [ ] Add MockK BOM: version `1.13.5`
- [ ] Remove Lombok BOM entirely (delete dependency-management section if Lombok-only)
- [ ] Verify: `./gradlew clean build` (should fail: no Kotlin sources yet)

### Submodule: banking-commons

- [ ] Read current `banking-commons/build.gradle.kts`
- [ ] Replace `id("java")` with `id("kotlin")`
- [ ] Add test dependency: `testImplementation("io.kotest:kotest-runner-junit5:5.7.0")`
- [ ] Add test dependency: `testImplementation("io.mockk:mockk:1.13.5")`
- [ ] Remove: `compileOnly("org.projectlombok:lombok")`
- [ ] Remove: `annotationProcessor("org.projectlombok:lombok")`
- [ ] Verify: `./gradlew :banking-commons:dependencies` (no Lombok)

### Submodule: auth-service

- [ ] Read current `auth-service/build.gradle.kts`
- [ ] Replace `id("java")` with `id("kotlin")`
- [ ] Add test dependency: `testImplementation("io.kotest:kotest-runner-junit5:5.7.0")`
- [ ] Add test dependency: `testImplementation("io.mockk:mockk:1.13.5")`
- [ ] Remove Lombok (as above)
- [ ] Verify: `./gradlew :auth-service:dependencies` (Kotest present)

### Submodule: core-service

- [ ] Read current `core-service/build.gradle.kts`
- [ ] Replace `id("java")` with `id("kotlin")`
- [ ] **IMPORTANT**: Remove `org.springframework.boot:spring-boot-starter-webflux` (Reactor)
- [ ] Add: `implementation("org.springframework.boot:spring-boot-starter-data-r2dbc")`
- [ ] Add: `runtimeOnly("org.postgresql:r2dbc-postgresql")`
- [ ] Add: `implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core")`
- [ ] Add: `implementation("org.jetbrains.kotlinx:kotlinx-coroutines-reactive")`
- [ ] Add: `implementation("org.jetbrains.kotlinx:kotlinx-coroutines-reactor")`
- [ ] Add test dependency: `testImplementation("io.kotest:kotest-runner-junit5:5.7.0")`
- [ ] Add test dependency: `testImplementation("io.mockk:mockk:1.13.5")`
- [ ] Remove: `testImplementation("org.springframework.boot:spring-boot-starter-webflux-test")` → replace with R2DBC test
- [ ] Remove Lombok (as above)
- [ ] Verify: `./gradlew :core-service:dependencies` (no Reactor, has R2DBC + coroutines)

### Gradle Verification

- [ ] Run: `./gradlew clean build --refresh-dependencies` (may fail: no .kt files yet, expected)
- [ ] Check: No Java files should be compiled from `/src/main/java` paths (all should be in `/src/main/kotlin` after migration)

---

## PHASE 2: banking-commons Migration (3 hours)

### Create Kotlin Directory Structure

- [ ] Create: `banking-commons/src/main/kotlin/com/corebank/commons/`
- [ ] Create: `banking-commons/src/main/kotlin/com/corebank/commons/model/`
- [ ] Create: `banking-commons/src/main/kotlin/com/corebank/commons/security/`
- [ ] Create: `banking-commons/src/main/kotlin/com/corebank/commons/exception/`
- [ ] Create: `banking-commons/src/test/kotlin/com/corebank/commons/`

### Migrate DTOs (model/)

- [ ] ResponseDTO.kt → `data class` + companion object factories
- [ ] LoginRequestDTO.kt → `data class`
- [ ] AccountDTO.kt → `data class`
- [ ] CardDTO.kt → `data class`
- [ ] BalanceDTO.kt → `data class`
- [ ] HomeAggregateDTO.kt → `data class`
- [ ] Verify: `./gradlew :banking-commons:classes` (compiles)

### Migrate Security Layer (security/)

- [ ] JwtUtil.kt: Straightforward translation from Java
  - [ ] Use `fun` instead of method declarations
  - [ ] Replace `SecretKey getSigningKey()` → `private fun getSigningKey(): SecretKey`
  - [ ] Keep `@Component` annotation
  - [ ] Test: `./gradlew :banking-commons:classes`

- [ ] HeaderConstants.kt: Convert to `object` singleton
  - [ ] `const val X_RQ_UID = "X-RqUid"` (not `public static final`)
  - [ ] `val PROPAGATED_HEADERS = arrayOf(...)`
  - [ ] Test: `./gradlew :banking-commons:classes`

- [ ] BankingSecurityFilter.kt (MVC filter)
  - [ ] Extend `OncePerRequestFilter`
  - [ ] Implement `doFilterInternal` with suspend function support (if needed)
  - [ ] Replace manual null checks with Kotlin `?.let`, `?:` operators
  - [ ] Test: `./gradlew :banking-commons:classes`

- [ ] ReactiveJwtFilter.kt (WebFlux filter)
  - [ ] Implement `WebFilter`
  - [ ] Replace `authHeader.substring(...)` with Kotlin string extension
  - [ ] Keep Mono chains (not converting to coroutines in commons — only core-service)
  - [ ] Test: `./gradlew :banking-commons:classes`

### Migrate Exception Handling (exception/)

- [ ] GlobalExceptionHandler.kt
  - [ ] `@RestControllerAdvice`
  - [ ] Replace `@ExceptionHandler` methods with Kotlin fun
  - [ ] Use `when` expression instead of if-else if-else chains (optional, readability)
  - [ ] Test: `./gradlew :banking-commons:classes`

### Migrate Tests

- [ ] **ResponseDTOTest.kt**: JUnit 5 + Mockito → Kotest + MockK
  - [ ] Change: `class ResponseDTOTest : StringSpec({ ... })`
  - [ ] Change: `when(...).thenReturn(...)` → `every { } returns`
  - [ ] Change: `assertEquals(...)` → `shouldBe`
  - [ ] Test: `./gradlew :banking-commons:test`

- [ ] **JwtUtilTest.kt**: Straightforward translation
  - [ ] Same pattern as ResponseDTOTest
  - [ ] Test: `./gradlew :banking-commons:test`

- [ ] **BankingSecurityFilterTest.kt**: Kotest conversion
  - [ ] Test: `./gradlew :banking-commons:test`

- [ ] **ReactiveJwtFilterTest.kt**: Kotest conversion
  - [ ] Use `runTest { }` for reactive assertions
  - [ ] Test: `./gradlew :banking-commons:test`

- [ ] **GlobalExceptionHandlerTest.kt**: Kotest conversion
  - [ ] Test: `./gradlew :banking-commons:test`

### Coverage & Build

- [ ] Run: `./gradlew :banking-commons:jacocoTestCoverageVerification`
- [ ] Verify: Coverage ≥80%
- [ ] Run: `./gradlew :banking-commons:build`
- [ ] Verify: Zero compilation errors

---

## PHASE 3: auth-service Migration (2.5 hours)

### Create Kotlin Directory Structure

- [ ] Create: `auth-service/src/main/kotlin/com/corebank/auth/domain/model/`
- [ ] Create: `auth-service/src/main/kotlin/com/corebank/auth/application/port/`
- [ ] Create: `auth-service/src/main/kotlin/com/corebank/auth/application/service/`
- [ ] Create: `auth-service/src/main/kotlin/com/corebank/auth/infrastructure/adapter/`
- [ ] Create: `auth-service/src/main/kotlin/com/corebank/auth/infrastructure/config/`
- [ ] Create: `auth-service/src/test/kotlin/com/corebank/auth/`

### Migrate Domain Models

- [ ] Credentials.kt
  - [ ] `data class Credentials(val username: String, val password: String)`
  - [ ] `fun isValid() = username == "user" && password == "password"`
  - [ ] Test: `./gradlew :auth-service:classes`

- [ ] AuthToken.kt
  - [ ] `data class AuthToken(...)`
  - [ ] Remove `@Builder` (use Kotlin `.copy()` instead)
  - [ ] Test: `./gradlew :auth-service:classes`

### Migrate Application Layer

- [ ] AuthenticateUseCase.kt (port interface)
  - [ ] `interface AuthenticateUseCase { fun authenticate(...): String }`
  - [ ] Test: `./gradlew :auth-service:classes`

- [ ] TokenCachePort.kt (output port)
  - [ ] `interface TokenCachePort { suspend fun cacheToken(...) }`
  - [ ] Test: `./gradlew :auth-service:classes`

- [ ] AuthApplicationService.kt
  - [ ] `@Service class AuthApplicationService(...) : AuthenticateUseCase`
  - [ ] Migrate `authenticate()` method (synchronous, no suspend)
  - [ ] Replace `HashMap` with `mapOf()`
  - [ ] Replace `null checks` with `?:` operator
  - [ ] Test: `./gradlew :auth-service:classes`

### Migrate Infrastructure

- [ ] AuthController.kt
  - [ ] `@RestController @RequestMapping("/api/auth")`
  - [ ] Migrate `login()` endpoint (no suspend)
  - [ ] Use Kotlin named parameters for readability
  - [ ] Test: `./gradlew :auth-service:classes`

- [ ] SecurityConfig.kt
  - [ ] `@Configuration @EnableWebSecurity`
  - [ ] Keep `@Bean` methods, lambda DSL
  - [ ] Test: `./gradlew :auth-service:classes`

- [ ] RedisConfig.kt
  - [ ] Straightforward Kotlin translation
  - [ ] Test: `./gradlew :auth-service:classes`

- [ ] RedisTokenCacheAdapter.kt (implements TokenCachePort)
  - [ ] Implement `cacheToken()`, `getCachedToken()`, `invalidateToken()`
  - [ ] Test: `./gradlew :auth-service:classes`

### Migrate Tests

- [ ] **CredentialsTest.kt**: Kotest StringSpec
- [ ] **AuthApplicationServiceTest.kt**: Kotest + MockK
- [ ] **AuthControllerTest.kt**: Kotest + @WebMvcTest
- [ ] **SecurityConfigTest.kt**: Kotest conversion
- [ ] **RedisConfigTest.kt**: Kotest conversion
- [ ] **RedisTokenCacheAdapterTest.kt**: Kotest + MockK

For each test:
- [ ] Change class declaration: `class XTest : StringSpec({ ... })`
- [ ] Change mocking: `mockk<Type>()` instead of `@Mock`
- [ ] Change stubbing: `every { } returns` instead of `when().thenReturn()`
- [ ] Change assertions: `shouldBe`, `shouldNotBe`, `shouldThrow`, etc.
- [ ] Run: `./gradlew :auth-service:test`

### Coverage & Build

- [ ] Run: `./gradlew :auth-service:jacocoTestCoverageVerification`
- [ ] Verify: Coverage ≥80%
- [ ] Run: `./gradlew :auth-service:build`
- [ ] Verify: Zero compilation errors

### Functional Verification (Manual)

- [ ] Build and start service: `./gradlew :auth-service:bootRun`
- [ ] POST `/api/auth/login` with Insomnia (Phase 2 script)
- [ ] Verify: Response matches Phase 2 exactly (statusCode, body, headers)

---

## PHASE 4: core-service Migration (5 hours)

### Create Kotlin Directory Structure

- [ ] Create: `core-service/src/main/kotlin/com/corebank/core/domain/model/`
- [ ] Create: `core-service/src/main/kotlin/com/corebank/core/application/port/`
- [ ] Create: `core-service/src/main/kotlin/com/corebank/core/application/service/`
- [ ] Create: `core-service/src/main/kotlin/com/corebank/core/infrastructure/adapter/`
- [ ] Create: `core-service/src/main/kotlin/com/corebank/core/infrastructure/config/`
- [ ] Create: `core-service/src/test/kotlin/com/corebank/core/`

### Migrate Domain Models (R2DBC Entities)

**CRITICAL**: These are now R2DBC entities (not JPA), use `@Table`, `@Column`, `@Id`

- [ ] Account.kt
  - [ ] `@Table("account") data class Account(...)`
  - [ ] `@Id val id: Long?` (nullable for auto-generated)
  - [ ] `@Column("customer_id") val customerId: String`
  - [ ] Test: `./gradlew :core-service:classes`

- [ ] Card.kt
  - [ ] Same pattern as Account
  - [ ] Test: `./gradlew :core-service:classes`

- [ ] Balance.kt
  - [ ] Same pattern
  - [ ] Test: `./gradlew :core-service:classes`

- [ ] HomeAggregate.kt (not a table entity; domain value object)
  - [ ] `data class HomeAggregate(val accounts: List<Account>, ...)`
  - [ ] Test: `./gradlew :core-service:classes`

### Migrate Application Layer (ASYNC REWRITE)

**CRITICAL**: This is the main async conversion.

- [ ] GetHomeBalanceUseCase.kt (input port)
  - [ ] `interface GetHomeBalanceUseCase { suspend fun getAggregatedBalance(...): HomeAggregate }`
  - [ ] Test: `./gradlew :core-service:classes`

- [ ] AccountRepositoryPort.kt (output port)
  - [ ] `interface AccountRepositoryPort : CoroutineCrudRepository<Account, Long>`
  - [ ] `suspend fun findByCustomerId(customerId: String): List<Account>`
  - [ ] Test: `./gradlew :core-service:classes`

- [ ] CardRepositoryPort.kt (output port)
  - [ ] Same pattern as AccountRepositoryPort
  - [ ] Test: `./gradlew :core-service:classes`

- [ ] BalanceRepositoryPort.kt (output port)
  - [ ] Same pattern
  - [ ] Test: `./gradlew :core-service:classes`

- [ ] **HomeApplicationService.kt** (CRITICAL — async rewrite)
  - [ ] Change signature: `override suspend fun getAggregatedBalance(customerId: String): HomeAggregate`
  - [ ] **REMOVE**: All `Mono.zip()` calls
  - [ ] **ADD**: `coroutineScope { async { ... } }` block
  - [ ] Structure:
    ```kotlin
    return coroutineScope {
        val accountsDeferred = async { accountRepositoryPort.findByCustomerId(customerId) }
        val cardsDeferred = async { cardRepositoryPort.findByCustomerId(customerId) }
        val balanceDeferred = async { balanceRepositoryPort.findByCustomerId(customerId) }
        
        HomeAggregate(
            accounts = accountsDeferred.await(),
            cards = cardsDeferred.await(),
            balance = balanceDeferred.await()
        )
    }
    ```
  - [ ] Keep `@CircuitBreaker`, `@Retry` annotations (Resilience4j compatible)
  - [ ] Fallback: `private suspend fun getAggregatedBalanceFallback(...): HomeAggregate`
  - [ ] Test: `./gradlew :core-service:classes`
  - [ ] **VERIFY**: No `Mono`, `Flux`, `.block()` in file

### Migrate Infrastructure

- [ ] AccountRepositoryAdapter.kt
  - [ ] Implement AccountRepositoryPort (extends CoroutineCrudRepository)
  - [ ] Add `@Repository` annotation
  - [ ] Implement `suspend fun findByCustomerId(...): List<Account>`
  - [ ] Optional: `@Query("SELECT * FROM account WHERE customer_id = :customerId")`
  - [ ] Test: `./gradlew :core-service:classes`

- [ ] CardRepositoryAdapter.kt
  - [ ] Same pattern as AccountRepositoryAdapter
  - [ ] Test: `./gradlew :core-service:classes`

- [ ] BalanceRepositoryAdapter.kt
  - [ ] Same pattern
  - [ ] Test: `./gradlew :core-service:classes`

- [ ] HomeController.kt (WebFlux controller)
  - [ ] Keep `@RestController`, `@GetMapping`
  - [ ] Change method: `suspend fun getBalance(...): ResponseEntity<ResponseDTO<HomeAggregateDTO>>`
  - [ ] Extract customerId from JWT/headers
  - [ ] Call `homeUseCase.getAggregatedBalance(customerId)`
  - [ ] Return `ResponseEntity.ok(ResponseDTO.success(aggregate.toDTO()))`
  - [ ] Test: `./gradlew :core-service:classes`

- [ ] SecurityConfig.kt (WebFlux + coroutines)
  - [ ] `@Configuration @EnableWebFluxSecurity`
  - [ ] Register `ReactiveJwtFilter` from commons
  - [ ] Lambda DSL for authorization rules
  - [ ] Test: `./gradlew :core-service:classes`

- [ ] R2dbcConfig.kt (if needed)
  - [ ] `@Configuration` for R2DBC setup
  - [ ] ConnectionFactory bean (provided by Spring auto-config usually)
  - [ ] Optional: Custom R2DBC settings
  - [ ] Test: `./gradlew :core-service:classes`

### Migrate Tests (CRITICAL — Coroutine Tests)

**Pattern**:
```kotlin
class HomeApplicationServiceTest : StringSpec({
    val accountRepository = mockk<AccountRepositoryPort>()
    val service = HomeApplicationService(accountRepository, cardRepository, balanceRepository)
    
    "getAggregatedBalance" should {
        "return complete aggregate" {
            coEvery { accountRepository.findByCustomerId("123") } returns listOf(...)
            coEvery { cardRepository.findByCustomerId("123") } returns listOf(...)
            coEvery { balanceRepository.findByCustomerId("123") } returns Balance(...)
            
            val result = service.getAggregatedBalance("123")
            
            result.accounts.size shouldBe 1
        }
    }
})
```

- [ ] **HomeApplicationServiceTest.kt**
  - [ ] Use `coEvery { } returns` for suspend functions
  - [ ] Use `runTest { }` context if needed (likely automatic in Kotest)
  - [ ] Test both success and fallback paths
  - [ ] Test: `./gradlew :core-service:test`

- [ ] **AccountRepositoryAdapterTest.kt** (integration test)
  - [ ] Use `@SpringBootTest` + `Testcontainers` for real PostgreSQL
  - [ ] Test: `suspend fun findByCustomerId(...)` returns correct data
  - [ ] Test: `./gradlew :core-service:test`

- [ ] **CardRepositoryAdapterTest.kt**
  - [ ] Same pattern as AccountRepositoryAdapterTest
  - [ ] Test: `./gradlew :core-service:test`

- [ ] **BalanceRepositoryAdapterTest.kt**
  - [ ] Same pattern
  - [ ] Test: `./gradlew :core-service:test`

- [ ] **HomeControllerTest.kt** (@WebFluxTest)
  - [ ] Test suspend endpoint
  - [ ] Verify response matches Phase 2 contract
  - [ ] Test: `./gradlew :core-service:test`

- [ ] **SecurityConfigTest.kt**
  - [ ] Kotest conversion
  - [ ] Verify JWT validation works
  - [ ] Test: `./gradlew :core-service:test`

### Coverage & Build

- [ ] Run: `./gradlew :core-service:jacocoTestCoverageVerification`
- [ ] Verify: Coverage ≥80%
- [ ] Run: `./gradlew :core-service:build`
- [ ] Verify: Zero compilation errors
- [ ] **CRITICAL GREP CHECK**:
  ```bash
  grep -r "Mono\|Flux" core-service/src/main/kotlin
  # Should return: NO RESULTS (empty output)
  ```
  - [ ] If any matches, investigate and remove

### Functional Verification (Manual)

- [ ] Start Docker: `docker-compose up -d`
- [ ] Build and start service: `./gradlew :core-service:bootRun`
- [ ] GET `/api/home/balance` with Insomnia (Phase 2 script)
- [ ] Verify: Response matches Phase 2 exactly (JSON structure, statusCode)

---

## PHASE 5: Full Build & Validation (1.5 hours)

### Build Verification

- [ ] Run: `./gradlew clean build jacocoTestReport`
- [ ] Verify:
  - [ ] banking-commons: ✅ Compiles, ≥80% coverage
  - [ ] auth-service: ✅ Compiles, ≥80% coverage
  - [ ] core-service: ✅ Compiles, ≥80% coverage
  - [ ] No Java files in src/main (only .kt)
  - [ ] No Lombok imports
  - [ ] No Reactor types in core-service

### Docker Verification

- [ ] `docker-compose up -d` (PostgreSQL + Redis)
- [ ] Verify containers running: `docker ps`
- [ ] Check logs: `docker-compose logs`

### Endpoint Testing

- [ ] Start all services in separate terminals or background
- [ ] Use Insomnia (Phase 2 collection):
  - [ ] POST `/api/auth/login` → verify JWT response
  - [ ] Copy JWT token
  - [ ] GET `/api/home/balance` with JWT → verify aggregated response
  - [ ] Verify response structure matches Phase 2 exactly (byte-for-byte JSON)

### Code Quality

- [ ] Run spotbugs (if enabled): `./gradlew spotbugsMain`
- [ ] Run detekt (optional): `./gradlew detekt`
- [ ] No critical issues

### Git Cleanup

- [ ] Status: `git status` (no untracked files except build/)
- [ ] Add all: `git add .`
- [ ] Commit: `git commit -m "Phase 4: Kotlin migration complete (all 3 modules)"`
- [ ] Verify: `git log --oneline -5` (shows commits)

---

## PHASE 6: Documentation & Final Review (1 hour)

### README Update

- [ ] Open: `corebank-microservices-java/README.md`
- [ ] Add section: "## Phase 4: Kotlin Edition"
- [ ] Include:
  - [ ] Kotlin version (1.9.22)
  - [ ] Architecture changes (Reactor → coroutines)
  - [ ] Database changes (JPA → R2DBC)
  - [ ] Testing framework (Mockito → Kotest + MockK)
  - [ ] Comparison table (Phase 2 vs Phase 4)
- [ ] Update commands: `./gradlew` examples (ensure Kotlin files mentioned)

### Spec Files

- [ ] Verify: All spec files exist in `.specs/`
  - [ ] `SPEC.md` ✅
  - [ ] `APPROACH.md` ✅
  - [ ] `CONSTITUTION.md` ✅
  - [ ] `MIGRATION_CHECKLIST.md` ✅ (this file)
  - [ ] `IMPLEMENTATION_PLAN.md` (if created)

- [ ] Mark this checklist: All items `[x]` complete

### Code Review Readiness

- [ ] Branch: `feature/phase-4-kotlin-migration` has all changes
- [ ] Commits: Logical, each compiles + passes tests
- [ ] Tests: All pass (`./gradlew test`)
- [ ] Coverage: All modules ≥80%
- [ ] Documentation: README updated

### Final Checks

- [ ] Clean build: `./gradlew clean build` ✅
- [ ] Full test suite: `./gradlew test` ✅
- [ ] Grep for violations:
  - [ ] No `@Data`, `@Getter`, `@Setter` annotations
  - [ ] No `when().thenReturn()` calls
  - [ ] No `StepVerifier` usage
  - [ ] No `Mono`, `Flux` in core-service main code

---

## POST-COMPLETION CHECKLIST

### Once All Phases Complete

- [ ] Branch status: Ready for pull request
- [ ] Assign reviewers: Tech lead, architecture team
- [ ] Create PR: With reference to SPEC.md, APPROACH.md, CONSTITUTION.md
- [ ] Review feedback: Address in follow-up commits
- [ ] Merge (optional): Merge to `main` after approval (or keep as feature branch)
- [ ] Tag: `git tag phase-4-kotlin-migration` for history
- [ ] Document: Update project knowledge base with Phase 4 outcomes

---

## TROUBLESHOOTING QUICK REFERENCE

| Issue | Solution |
|-------|----------|
| Gradle Kotlin plugin not found | Run `./gradlew clean --refresh-dependencies` |
| Tests fail with "no Spring context" | Use `@SpringBootTest` for integration tests; plain unit tests don't need it |
| `coEvery` not recognized | Import MockK: `import io.mockk.*` and `import io.mockk.coEvery` |
| Build complains about Lombok | Remove all Lombok dependencies from `build.gradle.kts` |
| `Mono`/`Flux` still in code | Grep and remove; replace with coroutines |
| R2DBC tests fail with DB connection | Check Docker: `docker-compose ps` and `docker-compose logs` |

---

## SIGN-OFF

**Migration Start Date**: _____________  
**Completed By**: _____________  
**Reviewed By**: _____________  
**Date Reviewed**: _____________  
**Status**: ☐ Complete ☐ In Progress ☐ Blocked

