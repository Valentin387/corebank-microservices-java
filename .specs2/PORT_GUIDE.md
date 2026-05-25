# Porting Logic from Java Repository

## Module: banking-commons

### Step 1: Domain Models

**From**: `corebank-microservices-java/core-service/src/main/java/.../Account.java`

**Convert from**:
```java
@Entity
@Data
@Builder
public class Account {
    @Id private Long id;
    private String accountNumber;
    private String accountType;
    // ...
}
```

**Convert to**:
```kotlin
@Table("accounts")
data class Account(
    @Id val id: Long? = null,
    val accountNumber: String,
    val accountType: String,
    // ...
)
```

**Key changes**:
- `@Entity` → `@Table`
- `@Data @Builder` → `data class` (Kotlin generates automatically)
- Add `? = null` for optional fields

### Step 2: JWT Utilities

**From**: `JwtUtil.java` (blocking methods)

**Convert from**:
```java
public class JwtUtil {
    public String generateToken(String username) {
        // blocking Jwts.builder()
    }
    public boolean validateToken(String token) {
        // blocking Jwts.parserBuilder()
    }
}
```

**Convert to**:
```kotlin
@Component
class JwtUtil {
    fun generateToken(username: String): String {
        return Jwts.builder()
            .subject(username)
            .issuedAt(Date())
            .signWith(key)
            .compact()
    }

    fun validateToken(token: String): Boolean {
        return try {
            Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token)
            true
        } catch (e: Exception) {
            false
        }
    }
}
```

**No suspend needed** — JWT operations are fast, CPU-bound

### Step 3: Reactive JWT Filter

**From**: `ReactiveJwtFilter.java`

**Convert from**:
```java
public class ReactiveJwtFilter implements WebFilter {
    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        // blocking .block() calls
    }
}
```

**Convert to**:
```kotlin
class ReactiveJwtFilter(private val jwtUtil: JwtUtil) : WebFilter {
    override fun filter(exchange: ServerWebExchange, chain: WebFilterChain): Mono<Void> {
        val token = extractToken(exchange.request)
        
        return if (token != null && jwtUtil.validateToken(token)) {
            val username = jwtUtil.extractUsername(token)
            val auth = UsernamePasswordAuthenticationToken(username, null, emptyList())
            chain.filter(exchange.mutate().principal(Mono.just(auth)).build())
        } else {
            chain.filter(exchange)
        }
    }
    
    private fun extractToken(request: ServerHttpRequest): String? {
        return request.headers.getFirst("Authorization")
            ?.removePrefix("Bearer ")
    }
}
```

**Key change**: No blocking calls needed (Spring handles async)

---

## Module: auth-service

### SecurityConfig

**From**: `SecurityConfig.java`

**Convert from**:
```java
@Configuration
@EnableWebFluxSecurity
public class SecurityConfig {
    @Bean
    public SecurityWebFilterChain securityWebFilterChain(ServerHttpSecurity http) {
        return http.csrf().disable()
            .authorizeExchange()
            .pathMatchers("/actuator/**").permitAll()
            .anyExchange().authenticated()
            .and()
            .addFilterBefore(...)
            .build();
    }
}
```

**Convert to**:
```kotlin
@Configuration
@EnableWebFluxSecurity
class SecurityConfig {
    @Bean
    fun securityWebFilterChain(http: ServerHttpSecurity): SecurityWebFilterChain {
        return http
            .csrf { it.disable() }
            .authorizeExchange { exchanges ->
                exchanges
                    .pathMatchers("/actuator/**").permitAll()
                    .anyExchange().authenticated()
            }
            .addFilterBefore(ReactiveJwtFilter(jwtUtil), SecurityWebFiltersOrder.AUTHENTICATION)
            .build()
    }
}
```

**Key changes**:
- Builder lambda syntax
- `it.disable()` instead of `.disable()`

### AuthController

**From**: `AuthController.java`

**Convert from**:
```java
@RestController
@RequestMapping("/api/auth")
public class AuthController {
    @PostMapping("/login")
    public Mono<ResponseEntity<LoginResponse>> login(@RequestBody LoginRequest req) {
        return Mono.just(ResponseEntity.ok(/* ... */));
    }
}
```

**Convert to** (WebFlux supports suspend!):
```kotlin
@RestController
@RequestMapping("/api/auth")
class AuthController(private val authService: AuthApplicationService) {
    @PostMapping("/login")
    suspend fun login(@RequestBody request: LoginRequest): ResponseEntity<LoginResponse> {
        val response = authService.login(request.username)
        return ResponseEntity.ok(response)
    }
}
```

**Key change**: Use `suspend` instead of `Mono<T>`

---

## Module: core-service

### Domain Models

Same as banking-commons. Example:

```kotlin
@Table("accounts")
data class Account(
    @Id val id: Long? = null,
    val accountNumber: String,
    val balance: BigDecimal,
    val customerId: String
)
```

### Repository Interface (R2DBC)

**From**: `AccountJpaRepository extends JpaRepository`

**Convert from**:
```java
@Repository
public interface AccountJpaRepository extends JpaRepository<Account, Long> {
    List<Account> findByCustomerId(String customerId);
}
```

**Convert to** (R2DBC):
```kotlin
interface AccountRepository : CoroutineCrudRepository<Account, Long> {
    suspend fun findByCustomerId(customerId: String): List<Account>
}
```

**Key differences**:
- `CoroutineCrudRepository` (Spring Data R2DBC)
- `suspend` function
- Direct List return (not Mono/Flux)

### Repository Adapter

**From**: `AccountRepositoryAdapter` (wraps JPA with Reactor)

**Convert from**:
```java
@Component
public class AccountRepositoryAdapter implements AccountRepositoryPort {
    private final AccountJpaRepository jpaRepository;
    
    @Override
    public Mono<List<Account>> findByCustomerId(String customerId) {
        return Mono.fromCallable(() -> jpaRepository.findByCustomerId(customerId))
            .subscribeOn(Schedulers.boundedElastic());
    }
}
```

**Convert to** (direct coroutine):
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

**Key change**:
- Direct suspend function (no Mono wrapper)
- No `subscribeOn` (coroutine context handles threading)

### Application Service

**From**: `AccountApplicationService`

**Convert from**:
```java
@Service
public class AccountApplicationService {
    public Mono<List<Account>> getAllAccounts() {
        return accountRepository.findAll()
            .map(accounts -> processAccounts(accounts))
            .onErrorResume(e -> Mono.empty());
    }
}
```

**Convert to** (native coroutines):
```kotlin
@Service
class AccountApplicationService(
    private val repository: AccountRepositoryPort
) {
    suspend fun getAllAccounts(): List<Account> {
        return try {
            repository.findAll()
        } catch (e: Exception) {
            logger.error("Error fetching accounts", e)
            emptyList()
        }
    }
}
```

**Key changes**:
- `suspend` function
- No Mono/Flux
- Exception handling with try/catch

### Controller

**From**: `AccountController` (returns Mono<T>)

**Convert from**:
```java
@RestController
@RequestMapping("/api/accounts")
public class AccountController {
    @GetMapping("/{customerId}")
    public Mono<ResponseEntity<List<Account>>> getAccounts(@PathVariable String customerId) {
        return service.getByCustomerId(customerId)
            .map(ResponseEntity::ok);
    }
}
```

**Convert to** (suspend endpoints):
```kotlin
@RestController
@RequestMapping("/api/accounts")
class AccountController(private val service: AccountApplicationService) {
    @GetMapping("/{customerId}")
    suspend fun getAccounts(@PathVariable customerId: String): ResponseEntity<List<Account>> {
        val accounts = service.getByCustomerId(customerId)
        return ResponseEntity.ok(accounts)
    }
}
```

**Key change**: Simple suspend function (Spring handles async)

---

## Common Port Patterns

| Java Pattern | Kotlin Pattern |
|--------------|----------------|
| `Mono<T>` | `suspend fun(): T` |
| `Flux<T>` | `suspend fun(): List<T>` |
| `Mono.just()` | Return value directly |
| `Mono.empty()` | Return `emptyList()` or throw |
| `.map()` | Call function & transform |
| `.flatMap()` | Use `coroutineScope { async { } }` |
| `.subscribeOn()` | Handled by coroutine dispatcher |
| `@Builder` on data class | Kotlin `copy()` function |

---

## Testing Conversions

### Repository Test

From JUnit:
```java
@Test
public void testFindByCustomerId() {
    List<Account> result = repository.findByCustomerId("cust123");
    assertEquals(1, result.size());
}
```

To Kotest:
```kotlin
test("findByCustomerId returns accounts") {
    val result = repository.findByCustomerId("cust123")
    result.size shouldBe 1
}
```

### Service Test (Mocking)

From Mockito:
```java
when(repository.findByCustomerId("cust123"))
    .thenReturn(Mono.just(listOf(account)));

StepVerifier.create(service.getByCustomerId("cust123"))
    .expectNextCount(1)
    .verifyComplete();
```

To MockK:
```kotlin
coEvery { repository.findByCustomerId("cust123") } returns listOf(account)

val result = service.getByCustomerId("cust123")
result.size shouldBe 1
```

---

**Total porting time**: 8-10 hours of copy/paste + minor edits