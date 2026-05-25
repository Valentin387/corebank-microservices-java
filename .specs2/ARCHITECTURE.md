# Hexagonal Architecture in Kotlin

## Layers

```
┌─────────────────────────────────┐
│     Presentation Layer          │
│  (Controllers, DTOs)            │
└────────────────┬────────────────┘
                 │ (HTTP/gRPC)
┌────────────────▼────────────────┐
│   Application Layer             │
│  (Services, Ports, UseCases)    │
└────────────────┬────────────────┘
                 │ (domain logic)
┌────────────────▼────────────────┐
│      Domain Layer               │
│ (Entities, ValueObjects, Agg)   │
└────────────────┬────────────────┘
                 │ (domain contracts)
┌────────────────▼────────────────┐
│   Infrastructure Layer          │
│  (Adapters, Repositories)       │
└─────────────────────────────────┘
       │ (R2DBC, HTTP, etc)
```

## Layer Responsibilities

### 1. Presentation (Controllers)

**Package**: `com.corebank.core.infrastructure.adapter.input.web`

```kotlin
@RestController
@RequestMapping("/api/accounts")
class AccountController(
    private val service: AccountApplicationService
) {
    @GetMapping("/{customerId}")
    suspend fun getAccounts(
        @PathVariable customerId: String
    ): ResponseEntity<List<Account>> {
        val accounts = service.getByCustomerId(customerId)
        return ResponseEntity.ok(accounts)
    }
}
```

**Responsibilities**:
- HTTP request/response handling
- Parameter validation
- Error response mapping
- No business logic

**Technology**: Spring WebFlux, suspend functions

---

### 2. Application (Services & Ports)

**Package**: `com.corebank.core.application`

**Ports (interfaces)**:

```kotlin
interface AccountRepositoryPort {
    suspend fun findByCustomerId(customerId: String): List<Account>
    suspend fun save(account: Account): Account
}

interface AccountNotificationPort {
    suspend fun notifyNewAccount(account: Account)
}
```

**Services (implementation)**:

```kotlin
@Service
class AccountApplicationService(
    private val repository: AccountRepositoryPort,
    private val notificationPort: AccountNotificationPort
) {
    suspend fun createAccount(request: CreateAccountRequest): Account {
        val account = Account(
            accountNumber = generateAccountNumber(),
            customerId = request.customerId,
            accountType = request.type
        )
        val saved = repository.save(account)
        notificationPort.notifyNewAccount(saved)
        return saved
    }

    suspend fun getByCustomerId(customerId: String): List<Account> {
        return repository.findByCustomerId(customerId)
    }
}
```

**Responsibilities**:
- Use case orchestration
- Transaction boundaries
- Cross-cutting concerns (notifications, logging)
- No infrastructure details

**Technology**: Coroutines, port abstractions

---

### 3. Domain (Models)

**Package**: `com.corebank.core.domain.model`

```kotlin
@Table("accounts")
data class Account(
    @Id val id: Long? = null,
    val accountNumber: String,
    val accountType: String,
    val balance: BigDecimal,
    val customerId: String,
    val createdAt: Instant = Instant.now()
)

data class Money(val amount: BigDecimal, val currency: String = "USD")

sealed class AccountType {
    object Savings : AccountType()
    object Checking : AccountType()
    object Credit : AccountType()
}
```

**Responsibilities**:
- Business rules
- Domain invariants
- No technical concerns
- No framework dependencies

**Technology**: Pure Kotlin data classes

---

### 4. Infrastructure (Adapters)

**Package**: `com.corebank.core.infrastructure.adapter`

**Input Adapters** (REST Controllers):

```kotlin
// Already shown in Presentation layer
```

**Output Adapters** (Repositories):

```kotlin
interface AccountRepositoryKotlin : CoroutineCrudRepository<Account, Long> {
    suspend fun findByCustomerId(customerId: String): List<Account>
}

@Component
class AccountRepositoryAdapter(
    private val repository: AccountRepositoryKotlin
) : AccountRepositoryPort {
    override suspend fun findByCustomerId(customerId: String): List<Account> {
        return repository.findByCustomerId(customerId)
    }

    override suspend fun save(account: Account): Account {
        return repository.save(account)
    }
}
```

**Other Adapters** (Notifications, Logging, etc):

```kotlin
@Component
class EmailNotificationAdapter : AccountNotificationPort {
    override suspend fun notifyNewAccount(account: Account) {
        // Send email via coroutine (no blocking)
        withContext(Dispatchers.IO) {
            emailService.sendAccountCreationEmail(account)
        }
    }
}
```

**Responsibilities**:
- Data persistence (R2DBC)
- External service calls
- Framework integration
- Environment-specific details

**Technology**: R2DBC, Coroutines, Spring

---

## Module Structure

### banking-commons (Library)

```
banking-commons/
├── src/main/kotlin/com/corebank/commons/
│   ├── security/
│   │   ├── JwtUtil.kt
│   │   ├── ReactiveJwtFilter.kt
│   │   └── SecurityConfig.kt
│   ├── domain/
│   │   └── model/
│   │       ├── Account.kt
│   │       ├── Balance.kt
│   │       └── Card.kt
│   └── dto/
│       ├── JwtTokenResponse.kt
│       └── ErrorResponse.kt
└── src/test/kotlin/...
```

---

### auth-service (Sync Service)

```
auth-service/
├── src/main/kotlin/com/corebank/auth/
│   ├── application/
│   │   └── service/
│   │       └── AuthApplicationService.kt
│   ├── infrastructure/
│   │   └── adapter/
│   │       └── input/
│   │           └── web/
│   │               └── AuthController.kt
│   └── AuthApplication.kt
└── src/test/kotlin/...
```

---

### core-service (Async Service with R2DBC)

```
core-service/
├── src/main/kotlin/com/corebank/core/
│   ├── application/
│   │   ├── port/
│   │   │   ├── output/
│   │   │   │   ├── AccountRepositoryPort.kt
│   │   │   │   ├── BalanceRepositoryPort.kt
│   │   │   │   └── CardRepositoryPort.kt
│   │   │   └── input/
│   │   │       └── (controllers act as input adapters)
│   │   └── service/
│   │       ├── AccountApplicationService.kt
│   │       ├── BalanceApplicationService.kt
│   │       └── CardApplicationService.kt
│   ├── domain/
│   │   ├── model/
│   │   │   ├── Account.kt
│   │   │   ├── Balance.kt
│   │   │   └── Card.kt
│   │   └── aggregate/
│   │       └── AccountAggregate.kt
│   ├── infrastructure/
│   │   ├── adapter/
│   │   │   ├── input/
│   │   │   │   └── web/
│   │   │   │       ├── HomeController.kt
│   │   │   │       ├── AccountController.kt
│   │   │   │       └── BalanceController.kt
│   │   │   └── output/
│   │   │       ├── persistence/
│   │   │       │   ├── AccountRepository.kt (R2DBC)
│   │   │       │   ├── AccountRepositoryAdapter.kt
│   │   │       │   ├── BalanceRepository.kt
│   │   │       │   ├── BalanceRepositoryAdapter.kt
│   │   │       │   ├── CardRepository.kt
│   │   │       │   └── CardRepositoryAdapter.kt
│   │   │       └── notification/
│   │   │           └── EmailNotificationAdapter.kt
│   │   └── config/
│   │       ├── SecurityConfig.kt
│   │       └── WebFluxConfig.kt
│   └── CoreApplication.kt
└── src/test/kotlin/...
```

---

## Data Flow Example

**User creates account**:

```
1. HTTP POST /api/accounts
           ↓
2. AccountController.createAccount()
           ↓
3. AccountApplicationService.createAccount()
           ↓
4. Domain.Account aggregate validates
           ↓
5. AccountRepositoryPort.save()
           ↓
6. AccountRepositoryAdapter.save()
           ↓
7. R2DBC AccountRepository.save()
           ↓
8. PostgreSQL INSERT
           ↓
9. Return new Account entity
           ↓
10. Notify (AccountNotificationPort)
           ↓
11. HTTP 201 Created with Account
```

**Each layer**:
- ✅ Has single responsibility
- ✅ Testable in isolation
- ✅ Can be replaced (e.g., swap R2DBC for MongoDB)
- ✅ No framework leakage

---

## Dependency Injection (Spring)

All components autowired via constructor:

```kotlin
@Service
class AccountApplicationService(
    private val repository: AccountRepositoryPort,  // Injected interface
    private val logger: Logger
)

@Component
class AccountRepositoryAdapter(
    private val repository: AccountRepositoryKotlin  // R2DBC injected
) : AccountRepositoryPort

@RestController
class AccountController(
    private val service: AccountApplicationService  // Service injected
)
```

**Benefits**:
- Testable (mock dependencies)
- Decoupled (interfaces, not implementations)
- Spring manages lifecycle

---

## Testing Each Layer

### Unit Test (Domain)

```kotlin
test("Account enforces minimum balance") {
    val account = Account(balance = BigDecimal.ZERO)
    val withdrew = account.withdraw(BigDecimal("100"))
    
    withdrew shouldBe null  // Can't withdraw below 0
}
```

### Unit Test (Service, mocked)

```kotlin
val mockRepository = mockk<AccountRepositoryPort>()
val service = AccountApplicationService(mockRepository)

coEvery { mockRepository.findByCustomerId(any()) } returns listOf(account)

val result = service.getByCustomerId("cust123")
result.size shouldBe 1
```

### Integration Test (Controller + Service + DB)

```kotlin
@SpringBootTest
class AccountControllerTest(
    @Autowired val client: WebTestClient
) {
    test("GET /api/accounts returns 200") {
        client.get().uri("/api/accounts/cust123")
            .exchange()
            .expectStatus().isOk
    }
}
```

---

## Key Rules

1. **Presentation** never calls Presentation
2. **Application** never calls Application directly (use ports)
3. **Domain** never imports Application or Infrastructure
4. **Infrastructure** can call everything (it's the outermost layer)
5. **No cyclic dependencies** (always point inward)

---

**Architecture Principles**:
- ✅ Testable
- ✅ Maintainable
- ✅ Technology-agnostic business logic
- ✅ Easy to replace adapters (R2DBC → MongoDB)