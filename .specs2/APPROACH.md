# Fresh Kotlin Implementation — Approach

**Version**: 2.0  
**Strategy**: Sequential module build, R2DBC-first, coroutines native

---

## 1. Core Principles

1. **Start fresh** — No Java compat, no legacy constraints
2. **Async-first** — Every layer assumes async I/O
3. **R2DBC native** — Coroutines talk directly to R2DBC (not wrapped)
4. **Idiomatic Kotlin** — Leverage language features (data classes, extensions, scope functions)
5. **Test-driven** — Each module tested before next starts
6. **No Reactor** — Coroutines only (simpler mental model)

---

## 2. Module Build Order

**Why this order**:
1. banking-commons first (dependency)
2. auth-service second (simpler, no DB)
3. core-service last (depends on both)

### Phase 1: banking-commons (2 hours)

```
Java Code                    Kotlin Code
─────────────────────────────────────────
JwtUtil                  →   JwtUtil (object with suspend)
JwtFilter (blocking)     →   ReactiveJwtFilter (suspend)
Account (Lombok @Data)   →   Account (data class)
Balance (Lombok @Data)   →   Balance (data class)
Card (Lombok @Data)      →   Card (data class)
Response DTOs (Lombok)   →   Response DTOs (data class)
```

**Gradle changes**:
- Add Kotlin plugin
- Add Spring Security starter
- Add JWT library
- Remove Lombok

**Testing**:
- JwtUtil tests with Kotest
- JwtFilter tests (mocked WebFlux)

---

### Phase 2: auth-service (3 hours)

```
Java Code                    Kotlin Code
─────────────────────────────────────────
AuthController          →   AuthController (suspend endpoints)
AuthApplicationService  →   AuthApplicationService (suspend)
SecurityConfig          →   SecurityConfig (WebFlux)
AuthControllerTest      →   AuthControllerTest (Kotest)
```

**No database layer** — This keeps it simple, dependency on banking-commons only.

**Testing**:
- Controller tests with WebTestClient
- Mocking JwtUtil with MockK

---

### Phase 3: core-service (5 hours)

**Most complex — R2DBC learning curve.**

```
Java Code                           Kotlin Code
──────────────────────────────────────────────────
AccountJpaRepository         →   AccountRepository (R2DBC interface)
BalanceJpaRepository         →   BalanceRepository (R2DBC interface)
CardJpaRepository            →   CardRepository (R2DBC interface)
AccountRepositoryAdapter     →   AccountRepositoryAdapter (suspend)
BalanceRepositoryAdapter     →   BalanceRepositoryAdapter (suspend)
CardRepositoryAdapter        →   CardRepositoryAdapter (suspend)
HomeApplicationService       →   HomeApplicationService (coroutines)
CoreApplicationService       →   CoreApplicationService (coroutines)
CoreApplication              →   CoreApplication (Spring Boot app)
All tests                    →   All tests (Kotest + MockK)
```

**Key difference**:
- No JPA `@Entity` annotations
- R2DBC `@Table` annotations instead
- Suspend functions in repository interfaces
- Coroutine scope in application services

---

## 3. R2DBC Patterns (New Learning)

### Repository Interface (R2DBC)

```kotlin
interface AccountRepository : CoroutineCrudRepository<Account, Long> {
    suspend fun findByCustomerId(customerId: String): List<Account>
}
```

**Key points**:
- Extends `CoroutineCrudRepository` (not `JpaRepository`)
- All custom methods are `suspend`
- Return types are non-reactive (not `Mono<T>`, not `List<T>`)

### Entity Mapping (R2DBC)

```kotlin
@Table("accounts")
data class Account(
    @Id val id: Long? = null,
    val accountNumber: String,
    val accountType: String,
    val balance: BigDecimal,
    val customerId: String
)
```

**Key points**:
- Use `@Table` (not `@Entity`)
- Data class constructor (not Lombok)
- Nullable `id` for inserts

### Service Layer (Coroutines)

```kotlin
@Service
class AccountApplicationService(
    private val accountRepository: AccountRepository
) {
    suspend fun getByCustomerId(customerId: String): List<Account> {
        return accountRepository.findByCustomerId(customerId)
    }
}
```

**Key points**:
- `suspend` on all DB-touching methods
- Direct repository calls (no wrappers)
- Coroutine context flows naturally

### Controller (WebFlux + Coroutines)

```kotlin
@RestController
@RequestMapping("/api/accounts")
class AccountController(
    private val service: AccountApplicationService
) {
    @GetMapping("/{customerId}")
    suspend fun getAccounts(@PathVariable customerId: String): List<Account> {
        return service.getByCustomerId(customerId)
    }
}
```

**Key points**:
- Suspend endpoints in WebFlux
- Spring automatically handles coroutine context
- No Mono/Flux wrapper needed

---

## 4. Testing Strategy (Kotest)

### Repository Test (Database)

```kotlin
class AccountRepositoryTest(
    private val repository: AccountRepository
) : FunSpec({
    
    test("findByCustomerId returns accounts") {
        val account = Account(accountNumber = "123", ...)
        repository.save(account)
        
        val result = repository.findByCustomerId(account.customerId)
        
        result.size shouldBe 1
    }
})
```

**Key points**:
- FunSpec (more readable than JUnit)
- No explicit mocking (real R2DBC)
- Suspend naturally in test

### Service Test (Mocked)

```kotlin
class AccountApplicationServiceTest {
    private val repository = mockk<AccountRepository>()
    private val service = AccountApplicationService(repository)
    
    init {
        coEvery { repository.findByCustomerId(any()) } returns listOf(/*...test account*/)
    }
    
    @Test
    suspend fun `getByCustomerId returns accounts`() {
        val result = service.getByCustomerId("cust123")
        result.size shouldBe 1
    }
}
```

**Key points**:
- `mockk` instead of Mockito
- `coEvery` for suspend mocks
- Test function marked `suspend`

### Controller Test (WebTestClient)

```kotlin
class AccountControllerTest(
    @Autowired val client: WebTestClient
) : FunSpec({
    
    test("GET /api/accounts/{customerId} returns 200") {
        client.get()
            .uri("/api/accounts/cust123")
            .exchange()
            .expectStatus().isOk
    }
})
```

**Key points**:
- WebTestClient handles WebFlux
- Still Kotest (not JUnit)
- No explicit suspend needed (client handles it)

---

## 5. Coroutine Context Management

### In Application Services

```kotlin
@Service
class MyService {
    suspend fun complexOperation() {
        coroutineScope {
            val result1 = async { dbCall1() }
            val result2 = async { dbCall2() }
            
            // Both run in parallel
            val final = combine(result1.await(), result2.await())
            return@coroutineScope final
        }
    }
}
```

**Key points**:
- Use `coroutineScope` for structured concurrency
- `async` for parallel DB calls
- Exceptions propagate naturally

### In Controllers

```kotlin
@GetMapping("/complex")
suspend fun complexEndpoint(): Result {
    return withContext(Dispatchers.Default) {
        expensiveComputation()
    }
}
```

**Key points**:
- Spring manages coroutine context
- Use `withContext` for specific dispatchers
- Let Spring handle scope lifecycle

---

## 6. Gradle Configuration (New)

```kotlin
plugins {
    kotlin("jvm") version "2.3.21"
    kotlin("plugin.spring") version "2.3.21"
    id("org.springframework.boot") version "4.0.6"
    id("io.spring.dependency-management") version "1.1.7"
}

dependencies {
    implementation("org.jetbrains.kotlin:kotlin-stdlib")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-reactor:1.7.3")
    
    implementation("org.springframework.boot:spring-boot-starter-webflux")
    implementation("org.springframework.boot:spring-boot-starter-data-r2dbc")
    implementation("org.springframework.boot:spring-boot-starter-security")
    
    runtimeOnly("org.postgresql:r2dbc-postgresql")
    runtimeOnly("org.postgresql:postgresql")
    
    testImplementation("io.kotest:kotest-runner-junit5:5.7.0")
    testImplementation("io.kotest:kotest-assertions-core:5.7.0")
    testImplementation("io.mockk:mockk:1.13.5")
    testImplementation("org.springframework.boot:spring-boot-starter-test")
}

tasks.test {
    useJUnitPlatform()
}
```

---

## 7. Migration Flow (from Java codebase)

1. **Domain Models** → Copy Java, convert to data classes
2. **Utils** → Copy logic, convert to object/functions
3. **Adapters** → Copy logic, make suspend
4. **Services** → Copy logic, add suspend, replace Mono/Flux
5. **Controllers** → Copy endpoints, add suspend
6. **Tests** → Rewrite in Kotest, use MockK

**Total effort**: ~8-10 hours (mostly copy/paste + minor rewrites)

---

## 8. Common Pitfalls (Avoid)

❌ **Mixing Reactor and Coroutines**  
✅ Use coroutines only

❌ **Blocking calls in suspend functions**  
✅ Use `withContext(Dispatchers.IO)` if needed

❌ **Forgetting suspend on repository methods**  
✅ All DB methods must be suspend

❌ **Using Mono/Flux in services**  
✅ Let Spring handle subscription

❌ **Global coroutine scope**  
✅ Use structured concurrency (coroutineScope)

---

## 9. Validation Checkpoints

**Daily**:
- [ ] Code compiles
- [ ] Tests pass (70%+)
- [ ] No IDE warnings

**End of module**:
- [ ] 80%+ coverage
- [ ] All tests pass
- [ ] No blocking I/O

**End of week**:
- [ ] All 3 modules completed
- [ ] Integration tests pass
- [ ] Ready for deployment

---

**Next**: Follow SETUP_GUIDE.md to bootstrap