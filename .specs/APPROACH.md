# Phase 4 Implementation Approach

**Date:** May 23, 2026  
**Baseline:** SPEC.md  
**Target Audience:** Development team + code reviewers

---

## 1. STRATEGY: Incremental Layer-by-Layer Migration

### Why This Approach?

1. **Low Risk**: Each layer builds independently; failures isolated
2. **Testability**: Layer tests don't require full Spring context
3. **Reviewability**: Small PRs, clear diffs, easy to revert if needed
4. **Parallelizability**: Future: Multiple developers can work on services in parallel

### Order of Migration

```
1. Gradle Configuration (root + 3 submodules)
        ↓
2. banking-commons (Shared library — no business logic)
        ↓
3. auth-service (Simple, no async patterns)
        ↓
4. core-service (Complex, async rewrite — R2DBC + coroutines)
        ↓
5. Validation & Testing (End-to-end verification)
        ↓
6. Documentation & Cleanup
```

**Rationale**: Dependencies flow top-down; later services depend on earlier ones.

---

## 2. BUILD STRATEGY

### Gradle Layering

**Root `build.gradle.kts`**:
- Define Kotlin version (1.9.22)
- Define dependency BOMs (coroutines, Kotest, Spring Data)
- Configure JaCoCo coverage rules (≥80% per module)
- Disable Lombok globally

**Submodule `build.gradle.kts`**:
- Apply Kotlin plugin
- Add Kotlin-specific dependencies
- Configure test framework (Kotest)
- Configure mocking (MockK)

### Why Gradle Kotlin DSL?

✅ Already used in Phase 2 (no learning curve)  
✅ Type-safe, IDE autocomplete  
✅ Kotlin-first syntax aligns with Phase 4 goals

---

## 3. MIGRATION PATTERN: Template-Driven

### banking-commons: DTOs & Utils

**Pattern for DTOs**:
```kotlin
// Java Phase 2
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ResponseDTO<T> { ... }

// Kotlin Phase 4
data class ResponseDTO<T>(
    val statusCode: Int,
    val body: T,
    val extraArgs: Map<String, Any>? = null
) {
    companion object {
        fun <T> success(body: T) = ResponseDTO(200, body)
        fun <T> error(code: Int, body: T) = ResponseDTO(code, body)
    }
}
```

**Pattern for Utilities**:
```kotlin
// Java Phase 2 (static utility class)
public final class HeaderConstants {
    private HeaderConstants() { }
    public static final String X_RQ_UID = "X-RqUid";
}

// Kotlin Phase 4 (object singleton)
object HeaderConstants {
    const val X_RQ_UID = "X-RqUid"
    const val X_SES_ID = "X-SesID"
    // ... more constants
    val PROPAGATED_HEADERS = arrayOf(X_RQ_UID, X_SES_ID, ...)
}
```

### auth-service: Synchronous Service (No Async)

**Pattern for Value Objects**:
```kotlin
// Kotlin data class (1:1 from Java)
data class Credentials(
    val username: String,
    val password: String
) {
    fun isValid() = username == "user" && password == "password"
}
```

**Pattern for Application Service**:
```kotlin
@Service
class AuthApplicationService(
    private val jwtUtil: JwtUtil,
    private val tokenCachePort: TokenCachePort
) : AuthenticateUseCase {

    override fun authenticate(
        username: String,
        password: String,
        custIdentNum: String?,
        custIdentType: String?
    ): String {
        val credentials = Credentials(username, password)
        if (!credentials.isValid()) {
            throw SecurityException("Invalid credentials")
        }
        // ... rest of logic (unchanged from Java)
    }
}
```

**Key**: No `suspend` keyword (synchronous remains synchronous)

### core-service: Async Rewrite (R2DBC + Coroutines)

**Pattern for R2DBC Repository Port**:
```kotlin
interface AccountRepositoryPort : CoroutineCrudRepository<Account, Long> {
    suspend fun findByCustomerId(customerId: String): List<Account>
    
    @Query("SELECT * FROM account WHERE customer_id = :customerId")
    suspend fun findByCustomerIdNative(customerId: String): List<Account>
}
```

**Pattern for Application Service (Async Orchestration)**:
```kotlin
@Service
class HomeApplicationService(
    private val accountRepository: AccountRepositoryPort,
    private val cardRepository: CardRepositoryPort,
    private val balanceRepository: BalanceRepositoryPort
) : GetHomeBalanceUseCase {

    override suspend fun getAggregatedBalance(customerId: String): HomeAggregate {
        return coroutineScope {
            val accountsDeferred = async { 
                accountRepository.findByCustomerId(customerId) 
            }
            val cardsDeferred = async { 
                cardRepository.findByCustomerId(customerId) 
            }
            val balanceDeferred = async { 
                balanceRepository.findByCustomerId(customerId) 
            }

            HomeAggregate(
                accounts = accountsDeferred.await(),
                cards = cardsDeferred.await(),
                balance = balanceDeferred.await()
            )
        }
    }
}
```

**Key**: `coroutineScope { async { } }` for structured concurrency (all must complete)

---

## 4. TESTING PHILOSOPHY

### Layer-Based Testing (No Integration Bloat)

```
Domain Layer (Unit Tests)
├── No Spring context
├── Test value objects & aggregates
├── Mockito-free (no external dependencies)

Application Layer (Unit Tests)
├── Light Spring context (just AutoConfiguration if needed)
├── Mock all ports (repositories, caches, external services)
├── Use Kotest + MockK for assertions & mocking

Infrastructure Layer (Integration Tests)
├── Full Spring context (@SpringBootTest)
├── Real databases (Testcontainers for R2DBC)
├── Verify adapter contracts
```

### Kotest Syntax

```kotlin
// Nested describe blocks (organize by feature)
class HomeApplicationServiceTest : StringSpec({
    val accountRepository = mockk<AccountRepositoryPort>()
    val cardRepository = mockk<CardRepositoryPort>()
    val balanceRepository = mockk<BalanceRepositoryPort>()
    val service = HomeApplicationService(accountRepository, cardRepository, balanceRepository)

    "getAggregatedBalance" should {
        "return complete HomeAggregate for valid customer" {
            coEvery { accountRepository.findByCustomerId("123") } returns listOf(Account(...))
            coEvery { cardRepository.findByCustomerId("123") } returns listOf(Card(...))
            coEvery { balanceRepository.findByCustomerId("123") } returns Balance(...)

            val result = service.getAggregatedBalance("123")
            
            result.accounts.size shouldBe 1
            result.cards.size shouldBe 1
            result.balance shouldNotBe null
        }

        "handle circuit breaker fallback" {
            coEvery { accountRepository.findByCustomerId("999") } throws Exception("DB error")
            
            val result = service.getAggregatedBalance("999")
            
            result.accounts.shouldBeEmpty()
            result.cards.shouldBeEmpty()
        }
    }
})
```

### Coverage Verification Command

```bash
./gradlew clean build jacocoTestReport

# Output shows:
# banking-commons: 85% coverage
# auth-service: 82% coverage
# core-service: 84% coverage
# ✅ All modules > 80% threshold
```

---

## 5. MIGRATION EXECUTION PHASES

### Phase 1A: Git & Gradle Setup (1 hour)

**Checklist**:
- [ ] Create branch: `git checkout -b feature/phase-4-kotlin-migration`
- [ ] Root `build.gradle.kts`: Add Kotlin plugin + BOMs
- [ ] Each submodule: Add Kotlin plugin + test frameworks
- [ ] Run: `./gradlew clean build` (should fail — no .kt files yet)

**Acceptance**: Gradle recognizes Kotlin, provides helpful error about missing sources.

### Phase 1B: banking-commons Migration (3 hours)

**Order**:
1. Create Kotlin source directories: `src/main/kotlin`, `src/test/kotlin`
2. Migrate DTOs (ResponseDTO, LoginRequestDTO, etc.)
3. Migrate JwtUtil (straightforward translation)
4. Migrate security filters (BankingSecurityFilter, ReactiveJwtFilter)
5. Migrate exception handler
6. Migrate all tests (JUnit 5 + Mockito → Kotest + MockK)

**Acceptance**: `./gradlew :banking-commons:build` passes, ≥80% coverage

### Phase 2A: auth-service Migration (2.5 hours)

**Order**:
1. Migrate domain models (Credentials, AuthToken)
2. Migrate application service & ports
3. Migrate infrastructure (controller, adapters, config)
4. Migrate all tests

**Acceptance**: `./gradlew :auth-service:build` passes, POST `/api/auth/login` works

### Phase 2B: core-service Migration (5 hours)

**Order**:
1. Update `build.gradle.kts`: Remove WebFlux (Project Reactor), add R2DBC
2. Migrate domain models (Account, Card, Balance, HomeAggregate) as R2DBC entities
3. Migrate application service (ASYNC REWRITE: Mono.zip → coroutineScope + async)
4. Migrate repository adapters (R2DBC CoroutineCrudRepository)
5. Migrate controller (suspend endpoints)
6. Migrate config (R2DBC, reactive security)
7. Migrate all tests (StepVerifier → Kotest runTest)

**Acceptance**: `./gradlew :core-service:build` passes, GET `/api/home/balance` works, ≥80% coverage

### Phase 3: Validation (1.5 hours)

**Checklist**:
- [ ] All modules: `./gradlew clean build jacocoTestReport` ✅
- [ ] Docker: `docker-compose up -d` (Postgres + Redis)
- [ ] Start services: `./gradlew bootRun` for each
- [ ] Test endpoints: Insomnia scripts match Phase 2 responses exactly
- [ ] Coverage: All modules ≥80%

**Acceptance**: No differences in request/response behavior vs Phase 2

### Phase 4: Documentation & Cleanup (1 hour)

**Checklist**:
- [ ] Update `README.md` with Phase 4 section + comparison table
- [ ] Remove old Java files (make sure no .java in /src)
- [ ] Check git diff: Only .kt files, no .java remains
- [ ] Commit: `git add . && git commit -m "Phase 4: Kotlin migration complete"`

---

## 6. RISK MITIGATION

| Risk | Probability | Impact | Mitigation |
|------|---|---|---|
| **R2DBC lacks CoroutineCrudRepository** | 🟢 Low | 🔴 High | ✅ Already confirmed in Spring Data 4.0.5 docs |
| **Coroutine deadlocks in tests** | 🟡 Medium | 🟡 Medium | Use `runTest {}` dispatcher; isolate in unit tests |
| **Performance regression** | 🟡 Medium | 🟡 Medium | Benchmark startup time; profile under load (Phase 4.1) |
| **Gradle build times increase** | 🟡 Medium | 🟢 Low | Use `--parallel` flag; monitor in CI/CD |
| **Team unfamiliar with Kotest** | 🟡 Medium | 🟡 Medium | Provide Kotest examples in IMPLEMENTATION_PLAN.md |

---

## 7. CODE REVIEW GATES

Each phase requires:

1. **Compilation**: `./gradlew clean build` passes without warnings
2. **Tests**: ≥80% JaCoCo coverage per module
3. **Static Analysis**: No Spotbugs/Detekt errors (if enabled)
4. **API Contract**: Insomnia tests match Phase 2 responses byte-for-byte
5. **Code Style**: Ktlint compliance (optional, for consistency)

---

## 8. ROLLBACK STRATEGY

If critical blocker found:
1. Git: `git reset --hard HEAD~N` (depends on commit granularity)
2. Gradle: Revert `build.gradle.kts` to Phase 2 version
3. Source: Keep Phase 2 `.java` files (branch isolation)

**Fallback**: Switch back to Phase 2 branch; Phase 4 attempt does not affect Phase 2.

---

## 9. SUCCESS METRICS

| Metric | Target | Verification |
|--------|--------|---|
| **Compilation** | 0 errors | `./gradlew clean build` exit code = 0 |
| **Test Coverage** | ≥80% per module | JaCoCo HTML report |
| **API Compatibility** | 100% match | Insomnia collection (phase-2 scripts work unchanged) |
| **Code Clarity** | Subjective (team review) | Code review feedback |
| **Performance** | No regression | Docker container startup time (Phase 2 vs Phase 4) |
| **Git History** | Clean | Feature branch with logical commits |

