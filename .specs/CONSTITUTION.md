# Phase 4 Constitution: Overriding Rules & Constraints

**Purpose**: Define non-negotiable rules that override model instinct, team preference, or external suggestions.

**Authority**: These rules are FINAL. Any deviation requires explicit written approval.

---

## I. ARCHITECTURAL CONSTRAINTS

### Rule A1: Hexagonal Architecture is Immutable

**Statement**: The 3-layer Hexagonal structure (domain → application → infrastructure) MUST be preserved exactly as in Phase 2.

**Rationale**: DDD bounded contexts + clean architecture enable testability and maintainability.

**Non-Negotiable**: Even if "shortcutting" layers seems faster, it violates the baseline.

**Enforcement**:
- Code review: Verify no infrastructure code in domain or application layers
- Package structure: Enforce `domain/`, `application/`, `infrastructure/` directories
- Tests: Domain tests must not touch Spring context

---

### Rule A2: External API Contract is Frozen

**Statement**: Endpoints, request/response structure, and HTTP status codes CANNOT change.

```
POST /api/auth/login :8081
  Request: { username, password } + headers X-CustIdentNum, X-CustIdentType
  Response: ResponseDTO { statusCode: 200, body: "jwt-token-string", extraArgs: null }
  Status: 200 OK or 401 Unauthorized

GET /api/home/balance :8082
  Request: Headers Authorization: Bearer <jwt>, X-CustIdentNum, X-CustIdentType, X-RqUid, X-SesID
  Response: ResponseDTO { statusCode: 200, body: HomeAggregateDTO, extraArgs: null }
  Status: 200 OK or 401 Unauthorized
```

**Why**: Phase 4 is a language migration, not a business logic change. External clients depend on this contract.

**Enforcement**:
- Insomnia tests MUST return identical JSON as Phase 2
- No response field reordering
- No new optional fields without backward compatibility discussion

**Exception Process**: Any change requires:
1. Written requirement (not "it would be cleaner")
2. Stakeholder approval (documented)
3. Phase 5 (separate, planned effort)

---

### Rule A3: No Skip-Layer Dependencies

**Statement**: Infrastructure code CANNOT directly call domain code that skips application layer.

**Bad Example**:
```kotlin
// ❌ VIOLATION: Controller directly instantiates domain
@RestController
class HomeController(private val balanceRepository: BalanceRepository) {
    fun getBalance() = balanceRepository.findByCustomerId(...)
}
```

**Good Example**:
```kotlin
// ✅ CORRECT: Controller uses application service (port)
@RestController
class HomeController(private val getHomeBalanceUseCase: GetHomeBalanceUseCase) {
    suspend fun getBalance(customerId: String) = getHomeBalanceUseCase.getAggregatedBalance(customerId)
}
```

**Enforcement**: Code review checklist; architecture tests (ArchUnit in Phase 4.1)

---

## II. TECHNOLOGY CONSTRAINTS

### Rule T1: No Blocking I/O in core-service

**Statement**: core-service MUST NOT use blocking database operations. All database access via R2DBC (suspend functions).

**Rationale**: Spring WebFlux requires non-blocking handlers. Blocking calls defeat coroutines + async benefits.

**What's Forbidden**:
- ❌ `JpaRepository` (synchronous, blocking)
- ❌ `javax.sql.DataSource` (JDBC, blocking)
- ❌ Thread.sleep() or blocking operations in suspend functions
- ❌ `.block()` or `.blockFirst()` on Reactor types

**What's Required**:
- ✅ `CoroutineCrudRepository<T, ID>` (Spring Data R2DBC)
- ✅ `suspend fun` repository methods
- ✅ `coroutineScope { async { } }` for orchestration
- ✅ Non-blocking timers if needed: `delay(ms)` (from kotlinx.coroutines)

**Enforcement**:
- Gradle: Add spotbugs/detekt rule to flag Thread.sleep() usage
- Code review: Search for `.block()` and `.blockFirst()` — auto-reject if found
- Tests: Verify no blocking operations with TimeoutException checks

---

### Rule T2: Kotlin Data Classes Only (No Lombok)

**Statement**: All domain models, DTOs, value objects MUST be Kotlin `data class`. Lombok annotations are FORBIDDEN.

**Rationale**: 
- Kotlin data classes are language-native (1 line vs 4 Lombok annotations)
- Eliminates annotation processing overhead
- Consistent with Kotlin idioms

**What's Forbidden**:
- ❌ `@Data`, `@Getter`, `@Setter`, `@Builder` annotations
- ❌ Any `compileOnly("org.projectlombok:lombok")` dependency
- ❌ `annotationProcessor("org.projectlombok:lombok")`

**What's Required**:
```kotlin
// ✅ Phase 4 Style
data class Account(
    @Id val id: Long? = null,
    val accountNumber: String,
    val accountType: String,
    val balance: BigDecimal,
    val customerId: String
)

// Builder if needed (Kotlin .copy() preferred)
Account(accountNumber = "ACC-001", ...).copy(balance = BigDecimal("5000"))
```

**Enforcement**:
- Gradle: Remove all Lombok dependencies from submodules
- Build: `./gradlew build` must not reference Lombok
- Code review: Reject any `@Data` or `@Getter`

---

### Rule T3: Kotest + MockK (No Mockito)

**Statement**: All tests MUST use Kotest for assertions and MockK for mocking. Mockito is forbidden.

**Rationale**:
- Kotest: Spec-driven DSL (describe/it/should blocks) idiomatic for Kotlin
- MockK: Kotlin-native mocking (better with suspend functions, extension functions)
- Cleaner test code; better error messages

**What's Forbidden**:
- ❌ `org.mockito:mockito-core` dependency
- ❌ `@ExtendWith(MockitoExtension.class)`
- ❌ `when().thenReturn()` syntax
- ❌ `Mockito.verify()` calls

**What's Required**:
```kotlin
// ✅ Phase 4 Style
class AuthApplicationServiceTest : StringSpec({
    val jwtUtil = mockk<JwtUtil>()
    val service = AuthApplicationService(jwtUtil, tokenCachePort)

    "authenticate" should {
        "return token for valid credentials" {
            every { jwtUtil.generateToken(any(), any()) } returns "mock-token"
            
            val result = service.authenticate("user", "password", "123", "CC")
            
            result shouldBe "mock-token"
        }
    }
})
```

**Enforcement**:
- Gradle: `build.gradle.kts` MUST NOT reference Mockito
- Build: Any Mockito dependency causes build failure
- Code review: Auto-reject any `when().thenReturn()` pattern

---

### Rule T4: JUnit 5 is Optional; Kotest is Required

**Statement**: Kotest is the REQUIRED framework for all tests. JUnit 5 can coexist for integration tests if needed, but Kotest must be primary.

**Rationale**: Kotest's spec-driven approach aligns with readable, maintainable tests.

**What's OK**:
- ✅ `@SpringBootTest` + Kotest (integration tests)
- ✅ `@WebMvcTest` + Kotest

**What's Forbidden**:
- ❌ JUnit 5 `@Test` + Mockito (old pattern)
- ❌ Mixing Kotest + `@Test` in same file

**Enforcement**: Code review; prefer Kotest StringSpec/DescribeSpec for organization

---

## III. CODE QUALITY CONSTRAINTS

### Rule Q1: No Reactive Types in Main Code (core-service)

**Statement**: `Mono<T>`, `Flux<T>` from Project Reactor are FORBIDDEN in core-service business code.

**Except**: Only in Spring configuration or adapters bridging to external reactive libraries.

**Rationale**: Phase 4 replaces Reactor with coroutines. Mixing both defeats purpose.

**What's Forbidden**:
- ❌ `fun getAggregatedBalance(): Mono<HomeAggregate>`
- ❌ `Mono.zip(...)` calls
- ❌ `.map()`, `.flatMap()` on Mono/Flux

**What's Required**:
- ✅ `suspend fun getAggregatedBalance(): HomeAggregate`
- ✅ `coroutineScope { async { } }` for orchestration

**Enforcement**:
- Grep: Search for `Mono` or `Flux` in src/main/kotlin — auto-fail if found
- Code review: Immediately reject any Reactor usage

---

### Rule Q2: 80% JaCoCo Coverage is Hard Floor

**Statement**: Each module (banking-commons, auth-service, core-service) MUST achieve ≥80% line coverage.

**Calculation**: JaCoCo XML report, `<counter type="LINE">` coverage.

**Acceptable**:
- 80.0% to 100% coverage ✅
- Excluding generated code (if configured in Gradle)

**Unacceptable**:
- 79.9% or lower ❌
- Uncovered main methods (must test critical paths)
- Generated code that inflates/deflates coverage artificially

**Enforcement**:
```bash
./gradlew :module:jacocoTestCoverageVerification

# Build FAILS if coverage < 80%
# Output: "JaCoCo Coverage Verification failed: coverage < 0.80"
```

**Exception Process**:
1. If justified (e.g., untestable legacy code), document in code comment
2. Increase Gradle rule threshold only after written approval
3. Rare exceptions; not the default

---

### Rule Q3: No null Values in Domain Objects

**Statement**: Domain models MUST NOT have `var` fields with `= null` defaults. Use Kotlin's nullable types (`String?`) with explicit handling.

**Rationale**: Prevents accidental null dereferences; forces explicit null checks.

**Bad Example**:
```kotlin
// ❌ VIOLATION
data class Account(
    val id: Long? = null,  // OK (nullable)
    var balance: BigDecimal? = null  // ❌ Bad: mutable null field
)
```

**Good Example**:
```kotlin
// ✅ CORRECT
data class Account(
    val id: Long? = null,  // OK: immutable, explicit nullable
    val balance: BigDecimal  // Or: BigDecimal? if optional
)
```

**Enforcement**: Code review; architectural test (Phase 4.1)

---

### Rule Q4: All suspend Functions Must Be Testable in Isolation

**Statement**: Every `suspend fun` must have at least one unit test using Kotest `runTest {}`.

**Rationale**: Ensures coroutine behavior is verifiable; catches deadlocks/cancellation issues early.

**Example**:
```kotlin
test("getAggregatedBalance should orchestrate parallel queries") {
    val customerId = "123"
    coEvery { accountRepo.findByCustomerId(customerId) } returns listOf()
    coEvery { cardRepo.findByCustomerId(customerId) } returns listOf()
    coEvery { balanceRepo.findByCustomerId(customerId) } returns Balance()
    
    val result = service.getAggregatedBalance(customerId)
    
    result.accounts.shouldBeEmpty()
}
```

**Enforcement**: Code review + coverage threshold

---

## IV. DOCUMENTATION CONSTRAINTS

### Rule D1: No External Breaking Changes Without READMEs

**Statement**: Any deviation from Phase 2 contract must be documented in `README.md` under "Phase 4 Migration Notes".

**Required Documentation**:
- Version changes (Spring Boot, Kotlin, dependencies)
- Async pattern explanation (Mono → coroutines)
- Database change (JPA → R2DBC)
- Testing framework change (Mockito → MockK)

**Enforcement**: Code review gates PR merge on this

---

### Rule D2: Architecture Diagrams Match Code

**Statement**: If architecture diagrams exist (in documentation), they MUST match implemented code structure.

**Rationale**: Out-of-date docs mislead developers.

**Enforcement**: Code review; update diagrams in same PR as structural changes

---

## V. RELEASE & DEPLOYMENT CONSTRAINTS

### Rule R1: Phase 4 is a Feature Branch Only

**Statement**: Phase 4 lives on `feature/phase-4-kotlin-migration` branch. No merging to `main` without explicit approval.

**Rationale**: Parallel branches allow Phase 2 stability while Phase 4 develops.

**Enforcement**:
- Git: Branch protection rules prevent merge to `main` without review
- CI/CD: Feature branch builds must pass all tests

---

### Rule R2: No Half-Implemented Commits

**Statement**: Every commit to Phase 4 branch MUST:
1. Compile successfully (`./gradlew clean build` passes)
2. Have ≥80% coverage for affected modules
3. Include tests for new code

**Rationale**: Enables git bisect for debugging; keeps history clean.

**Enforcement**: Pre-commit hooks (optional); code review

---

## VI. ESCALATION & EXCEPTION PROCESS

### How to Override a Rule

**Only** if:
1. **Written justification**: Why this rule must be broken for this specific case
2. **Impact analysis**: What breaks if we follow the rule?
3. **Approval**: Explicit written approval from tech lead / architect
4. **Documentation**: Record exception in `EXCEPTIONS.md` with date + rationale

**Example**:
```
Date: 2026-05-25
Rule: T1 (No Blocking I/O in core-service)
Justification: Legacy auth provider only offers sync client; no async version available
Impact: auth-service call in core-service will use `.block()` (1 call, 50ms latency)
Approval: [Signed approval]
Mitigation: Circuit breaker + fallback implemented
```

---

## VII. SUMMARY TABLE

| Rule | Category | Severity | Violation Consequence |
|------|----------|----------|---|
| **A1** | Architecture | 🔴 Critical | Code review rejection |
| **A2** | API Contract | 🔴 Critical | Build failure (tests fail) |
| **A3** | Layering | 🔴 Critical | Code review rejection |
| **T1** | Tech (core-service) | 🟠 High | Code review rejection |
| **T2** | Tech (Kotlin) | 🟠 High | Build failure (grep check) |
| **T3** | Tech (Testing) | 🟠 High | Code review rejection |
| **T4** | Tech (Testing) | 🟡 Medium | Code review guidance |
| **Q1** | Quality | 🟠 High | Grep check failure |
| **Q2** | Quality | 🔴 Critical | Build failure (JaCoCo) |
| **Q3** | Quality | 🟡 Medium | Code review guidance |
| **Q4** | Quality | 🟠 High | Code review rejection |
| **D1** | Documentation | 🟡 Medium | Code review guidance |
| **D2** | Documentation | 🟡 Medium | Code review guidance |
| **R1** | Release | 🟠 High | Git branch protection |
| **R2** | Release | 🟠 High | Code review rejection |

