# Phase 4 Kotlin Microservices Migration Specification

**Document Version:** 1.0  
**Date:** May 23, 2026  
**Status:** Specification (Not Started)  
**Baseline:** Phase 2 (Java Microservices)  
**Target:** Kotlin + Coroutines + R2DBC Implementation

---

## 1. WHAT – Objectives & Success Criteria

### Primary Goal

Migrate **Phase 2 Java microservices** (auth-service, core-service, banking-commons) to **idiomatic Kotlin** while preserving 100% external API contract and achieving ≥80% test coverage per module.

### What is Being Built

**corebank-microservices-kotlin** (on separate git branch: `feature/phase-4-kotlin-migration`)

Consists of:
- **banking-commons**: Shared Kotlin library with DTOs, security, exception handling, utilities
- **auth-service**: MVC-based authentication service (Spring Boot MVC remains, logic → Kotlin)
- **core-service**: WebFlux-based product aggregation (Reactor → **Coroutines**, JPA → **R2DBC**)

### What is NOT Changing

- External API endpoints (`/api/auth/login`, `/api/home/balance`)
- Response structure (ResponseDTO contract)
- Banking header propagation logic
- Hexagonal + DDD architecture
- Infrastructure (Docker Compose, Postgres, Redis)
- Spring Boot version (4.0.6)

### Success Criteria

✅ **Must-Have**:
1. All three modules compile to Kotlin with zero Java files
2. POST `/api/auth/login` returns identical ResponseDTO as Phase 2
3. GET `/api/home/balance` returns identical aggregated data as Phase 2
4. ≥80% JaCoCo coverage per module (enforced in Gradle)
5. All 5 original domain/app/infra layers preserved in each service
6. Kotest + MockK used for all tests (no Mockito)
7. Zero Lombok annotations (replaced by Kotlin data classes)
8. No Reactor/Mono/Flux in codebase (only coroutines + Flow)

✅ **Should-Have**:
1. Git history clean (single feature branch, squash commits before merge)
2. README updated with Phase 4 comparison table
3. Coroutine patterns documented (fallback, resilience4j integration)

✅ **Nice-to-Have**:
1. Performance benchmarking (coroutines vs Reactor)
2. Structured concurrency examples
3. Virtual threads (Java 21) exploration

---

## 2. SCOPE

### In Scope (Phase 4 MVP)

| Component | Status | Notes |
|-----------|--------|-------|
| **banking-commons** | ✅ Migrate | DTOs, JwtUtil, filters, exception handlers, security |
| **auth-service** | ✅ Migrate | Kotlin conversion; no async pattern changes |
| **core-service** | ✅ Migrate + Async Rewrite | Reactor Mono.zip() → coroutineScope { async } |
| **Gradle config** | ✅ Update | Kotlin plugin, coroutines BOM, Kotest, MockK |
| **Tests** | ✅ Rewrite | JUnit 5 + Mockito → Kotest + MockK (all layers) |
| **Docker infra** | ✅ Reuse | No changes to docker-compose.yml |

### Out of Scope (Future Phases)

| Component | Why | Future Phase |
|-----------|-----|---|
| **Kafka integration** | Phase 3 features deferred | Phase 4.1 (async event streams) |
| **Virtual threads** | Spring Boot 4.0.6 works with coroutines | Phase 4.2 (experimental) |
| **Distributed tracing** | Works with Micrometer (no changes needed) | Phase 4.1 (optional) |

---

## 3. KEY TRANSFORMATIONS

### auth-service: Straightforward Conversion

```
Java (Phase 2)                          Kotlin (Phase 4)
────────────────────────────────────────────────────────────
@Data @Builder @NoArgsConstructor      data class Credentials(val username: String, val password: String)
@AllArgsConstructor                    
class Credentials { }

if (credentials.isValid())              if (credentials.isValid())
  // Lombok getter methods                // Kotlin primary constructor

@Service                                @Service
public class AuthApplicationService     class AuthApplicationService(
  public String authenticate(...)         val jwtUtil: JwtUtil,
                                          val tokenCachePort: TokenCachePort
                                        ) : AuthenticateUseCase {
                                          override suspend fun authenticate(...): String { }
                                        }
```

**No async changes**. All methods remain synchronous.

### core-service: Async Pattern Rewrite

```
Java (Phase 2, Reactor)                 Kotlin (Phase 4, Coroutines)
──────────────────────────────────────────────────────────────────
return Mono.zip(                        return coroutineScope {
  accountRepositoryPort                   val accounts = async { 
    .findByCustomerId(customerId),          accountRepositoryPort.findByCustomerId(customerId)
  cardRepositoryPort                      }
    .findByCustomerId(customerId),        val cards = async {
  balanceRepositoryPort                     cardRepositoryPort.findByCustomerId(customerId)
    .findByCustomerId(customerId)         }
).map(tuple -> HomeAggregate              val balance = async {
  .builder()                                balanceRepositoryPort.findByCustomerId(customerId)
  .accounts(tuple.getT1())               }
  ...                                     HomeAggregate(
  .build()                                  accounts = accounts.await(),
);                                         cards = cards.await(),
                                          balance = balance.await()
                                        )
                                        }

// JPA repositories with Mono<>         // R2DBC repositories with suspend
interface AccountRepositoryPort {       interface AccountRepositoryPort :
  Mono<List<Account>>                     CoroutineCrudRepository<Account, Long> {
    findByCustomerId(String);               suspend fun findByCustomerId(customerId: String)
}                                         : List<Account>
                                        }
```

**Key change**: JPA → R2DBC (non-blocking by design)

---

## 4. VERSION MATRIX

| Dependency | Phase 2 (Java) | Phase 4 (Kotlin) |
|-----------|---|---|
| Spring Boot | 4.0.6 | 4.0.6 (unchanged) |
| Spring Data Relational | 4.0.5 | 4.0.5 (unchanged) |
| Kotlin | N/A | 1.9.22 |
| kotlinx-coroutines | N/A | 1.7.3 |
| JUnit 5 | ✅ Used | Kept (Kotest alongside) |
| Mockito | ✅ Used | → Removed (MockK only) |
| StepVerifier | ✅ Used (Reactor tests) | → Removed (Kotest runTest) |
| Kotest | N/A | 5.7.0 (new) |
| MockK | N/A | 1.13.5 (new) |
| Lombok | ✅ Used | → Removed (data classes) |

---

## 5. TESTING STRATEGY

### Coverage Target

- **Minimum**: 80% per module (JaCoCo enforced)
- **Target**: 85%+ (comprehensive, layered testing)
- **Approach**: Test domain/application/infrastructure separately (no integrated context loading for unit tests)

### Kotest Pattern Example

```kotlin
// Old (JUnit 5 + Mockito)
@ExtendWith(MockitoExtension.class)
class AuthApplicationServiceTest {
  @Mock private JwtUtil jwtUtil;
  @InjectMocks private AuthApplicationService service;
  
  @Test
  void authenticateShouldReturnToken() {
    when(jwtUtil.generateToken(...)).thenReturn("token");
    String result = service.authenticate(...);
    assertEquals("token", result);
  }
}

// New (Kotest + MockK)
class AuthApplicationServiceTest : StringSpec({
  val jwtUtil = mockk<JwtUtil>()
  val service = AuthApplicationService(jwtUtil, tokenCachePort)
  
  "authenticate should return token for valid credentials" {
    every { jwtUtil.generateToken(any(), any()) } returns "token"
    val result = service.authenticate("user", "password", "123", "CC")
    result shouldBe "token"
  }
})
```

### Coroutine Testing (core-service)

```kotlin
test("getAggregatedBalance should return complete HomeAggregate") {
  val customerId = "123456789"
  coEvery { accountRepositoryPort.findByCustomerId(customerId) } returns listOf(Account(...))
  coEvery { cardRepositoryPort.findByCustomerId(customerId) } returns listOf(Card(...))
  coEvery { balanceRepositoryPort.findByCustomerId(customerId) } returns Balance(...)
  
  val result = homeService.getAggregatedBalance(customerId)
  result.accounts.size shouldBe 1
}
```

**Key**: `coEvery` (coroutine mock), `runTest` (coroutine test scope), no `StepVerifier`

---

## 6. ARCHITECTURE PRESERVATION

### Hexagonal Pattern (Unchanged)

```
domain/             → Value objects, aggregates (Kotlin data classes)
application/        → Use cases, ports (suspend functions)
infrastructure/     → Adapters (controllers, repositories, config)
```

### Port Example (auth-service)

```kotlin
// Port (application/port/output)
interface TokenCachePort {
    suspend fun cacheToken(key: String, token: String, ttlSeconds: Long)
    suspend fun getCachedToken(key: String): String?
    suspend fun invalidateToken(key: String)
}

// Adapter (infrastructure/adapter/persistence)
class RedisTokenCacheAdapter(private val redisTemplate: StringRedisTemplate) : TokenCachePort {
    override suspend fun cacheToken(key: String, token: String, ttlSeconds: Long) {
        redisTemplate.opsForValue().set(key, token, ttlSeconds, TimeUnit.SECONDS)
    }
}
```

---

## 7. DELIVERABLES

| Phase | Deliverable | Location |
|-------|---|---|
| 1 | Gradle setup, branch created | `build.gradle.kts` (root + 3 submodules) |
| 2 | banking-commons (Kotlin) | `banking-commons/src/main/kotlin/**/*.kt` |
| 3 | auth-service (Kotlin) | `auth-service/src/main/kotlin/**/*.kt` |
| 4 | core-service (Kotlin + Async) | `core-service/src/main/kotlin/**/*.kt` |
| 5 | All tests (Kotest + MockK) | `**/*Test.kt` (all modules) |
| 6 | Documentation | `README.md` (Phase 4 comparison table) |

---

## 8. TIMELINE & EFFORT

| Task | Est. Duration | Blocker? |
|------|---|---|
| Gradle setup | 1 hr | None |
| banking-commons | 3 hrs | None |
| auth-service | 2.5 hrs | None |
| core-service (async rewrite) | 5 hrs | **Spring Data R2DBC CoroutineCrudRepository** ✅ (confirmed available) |
| Testing (all modules) | 3 hrs | None |
| Validation & docs | 1.5 hrs | None |
| **Total** | **~15.5 hrs** | **None** |

**Realistic timeline**: 1-2 weeks (6-8 hrs/week effort)

---

## 9. GO/NO-GO CRITERIA

| Criteria | Status | Decision |
|----------|--------|----------|
| Spring Boot 4.0.6 available? | ✅ Yes (April 23, 2026) | **GO** |
| CoroutineCrudRepository stable? | ✅ Yes (Spring Data 4.0.5) | **GO** |
| Phase 2 baseline complete? | ✅ Yes (all tests pass) | **GO** |
| Kotlin 1.9.22+ available? | ✅ Yes | **GO** |
| Gradle Kotlin DSL ready? | ✅ Yes (already used) | **GO** |

**Overall Decision**: ✅ **PROCEED WITH IMPLEMENTATION**

---

## 10. ASSUMPTIONS & DEPENDENCIES

### Assumptions
1. Phase 2 codebase is stable (no breaking changes during Phase 4 development)
2. Docker Compose (Postgres + Redis) works without changes
3. Spring Boot 4.0.6 Kotlin support is production-ready
4. Team is comfortable with Kotest + MockK syntax

### Dependencies
1. **External**: Spring Boot 4.0.6, Spring Data Relational 4.0.5, Kotlin 1.9.22
2. **Internal**: Phase 2 codebase (baseline for comparison)
3. **Tools**: Gradle 8.13+, Java 21

---

## 11. PHASE 4 COMPARISON TABLE (Deliverable)

To be included in final README.md:

| Aspect | Phase 2 (Java) | Phase 4 (Kotlin) |
|--------|---|---|
| **Async Handling** | Project Reactor (Mono/Flux) | Coroutines + Flow |
| **Data Classes** | Lombok @Data @Builder (boilerplate) | Kotlin data class (1 line) |
| **Null Safety** | Optional + manual checks | Compiler-enforced (? vs !) |
| **Sync Code** | Imperative, functional chains | Imperative, readable |
| **Extension Functions** | Limited to utility classes | Native, extensively used |
| **Type System** | Standard Java generics | Kotlin's reified generics |
| **Database** | JPA (blocking) | R2DBC (non-blocking) |
| **Testing** | JUnit 5 + Mockito | Kotest + MockK |
| **Lines of Code** | Higher boilerplate | ~15-20% fewer lines |
| **Code Clarity** | Good (DDD, Hexagonal) | Excellent (Kotlin idioms) |

