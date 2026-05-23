# Phase 4 Implementation Plan - Developer Guide

**Purpose**: Step-by-step implementation instructions with code examples and explanations.

**Audience**: Developers executing Phase 4 migration.

**Reference Docs**: See SPEC.md (what), APPROACH.md (how), CONSTITUTION.md (rules)

---

## Part 1: Gradle Configuration

### Step 1.1: Update Root `build.gradle.kts`

**File**: `corebank-microservices-java/build.gradle.kts`

**Action**: Add Kotlin plugins and dependency BOMs

**Before**:
```kotlin
plugins {
    id("org.springframework.boot") version "4.0.6" apply false
    id("io.spring.dependency-management") version "1.1.7" apply false
}
```

**After**:
```kotlin
plugins {
    id("org.springframework.boot") version "4.0.6" apply false
    id("io.spring.dependency-management") version "1.1.7" apply false
    id("org.jetbrains.kotlin.jvm") version "1.9.22" apply false
    id("org.jetbrains.kotlin.plugin.spring") version "1.9.22" apply false
}

extra["kotlinVersion"] = "1.9.22"
```

**Explanation**:
- `kotlin.jvm`: Enables Kotlin compilation to JVM bytecode
- `kotlin.plugin.spring`: Generates no-arg constructors for Spring annotations (required for Spring)
- `extra["kotlinVersion"]`: Version constant used by submodules

**Verify**:
```bash
./gradlew clean build
# Expected: Build fails (no Kotlin sources yet) — this is OK
```

---

### Step 1.2: Add Dependency BOMs to Root

**In `allprojects { }` block**, add after `repositories`:

```kotlin
allprojects {
    // ... existing repositories ...

    configure<io.spring.gradle.dependencymanagement.dsl.DependencyManagementExtension> {
        imports {
            mavenBom("org.jetbrains.kotlinx:kotlinx-coroutines-bom:1.7.3")
            mavenBom("io.kotest:kotest-bom:5.7.0")
            mavenBom("io.mockk:mockk-bom:1.13.5")
        }
    }
}
```

**Explanation**:
- **kotlinx-coroutines-bom**: Ensures all coroutine libraries version-match (avoid conflicts)
- **kotest-bom**: Kotest testing framework versions
- **mockk-bom**: MockK mocking library versions

---

### Step 1.3: Configure JaCoCo for Kotlin

**In subprojects block**, update JaCoCo configuration:

```kotlin
subprojects {
    apply(plugin = "java")
    apply(plugin = "jacoco")
    apply(plugin = "org.jetbrains.kotlin.jvm")

    configure<JavaPluginExtension> {
        toolchain {
            languageVersion.set(JavaLanguageVersion.of(21))
        }
    }

    // JaCoCo configuration (unchanged from Phase 2)
    tasks.withType<JacocoReport> {
        reports {
            xml.required.set(true)
            html.required.set(true)
        }
    }
    
    // Rest of JaCoCo config...
}
```

**Explanation**:
- JaCoCo works automatically with Kotlin — no special configuration needed
- Line coverage calculation is consistent with Java bytecode

---

### Step 1.4: Update `banking-commons/build.gradle.kts`

**File**: `corebank-microservices-java/banking-commons/build.gradle.kts`

**Before**:
```kotlin
plugins {
    `java-library`
    id("io.spring.dependency-management")
}
```

**After**:
```kotlin
plugins {
    `kotlin`
    `kotlin-spring`
    `java-library`
    id("io.spring.dependency-management")
}

repositories {
    mavenCentral()
}

dependencies {
    // Keep existing Spring + JWT dependencies
    api("org.springframework.boot:spring-boot-starter-web")
    api("org.springframework.boot:spring-boot-starter-security")
    // ... rest of existing ...

    // ADD: Kotlin stdlib
    implementation("org.jetbrains.kotlin:kotlin-stdlib")

    // ADD: Kotest
    testImplementation("io.kotest:kotest-runner-junit5")
    testImplementation("io.kotest:kotest-assertions-core")

    // ADD: MockK
    testImplementation("io.mockk:mockk")

    // REMOVE ALL LOMBOK:
    // (delete these lines if present)
    // compileOnly("org.projectlombok:lombok")
    // annotationProcessor("org.projectlombok:lombok")
}
```

**Verify**:
```bash
./gradlew :banking-commons:dependencies | grep -i mockk
# Should show: io.mockk:mockk
./gradlew :banking-commons:dependencies | grep -i lombok
# Should show: NOTHING
```

---

### Step 1.5: Update `auth-service/build.gradle.kts`

**Similar to banking-commons**:

```kotlin
plugins {
    id("org.springframework.boot")
    id("io.spring.dependency-management")
    id("org.jetbrains.kotlin.jvm")
    id("org.jetbrains.kotlin.plugin.spring")
}

dependencies {
    implementation(project(":banking-commons"))
    
    // Existing Spring starters (unchanged)
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-data-redis")
    // ...
    
    // ADD: Kotlin stdlib
    implementation("org.jetbrains.kotlin:kotlin-stdlib")
    
    // ADD: Test frameworks
    testImplementation("io.kotest:kotest-runner-junit5")
    testImplementation("io.kotest:kotest-assertions-core")
    testImplementation("io.mockk:mockk")
    
    // REMOVE: All Lombok
    // (delete compileOnly + annotationProcessor)
}
```

---

### Step 1.6: Update `core-service/build.gradle.kts` (Important!)

**CRITICAL**: Remove Reactor, add R2DBC

```kotlin
plugins {
    id("org.springframework.boot")
    id("io.spring.dependency-management")
    id("org.jetbrains.kotlin.jvm")
    id("org.jetbrains.kotlin.plugin.spring")
}

dependencies {
    implementation(project(":banking-commons"))

    // REMOVE WebFlux (Reactor):
    // ❌ implementation("org.springframework.boot:spring-boot-starter-webflux")

    // ADD R2DBC (non-blocking relational database)
    implementation("org.springframework.boot:spring-boot-starter-data-r2dbc")
    runtimeOnly("org.postgresql:r2dbc-postgresql")
    
    // ADD Kotlin stdlib
    implementation("org.jetbrains.kotlin:kotlin-stdlib")
    
    // ADD Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-reactive")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-reactor")
    
    // Keep existing security, actuator, etc.
    implementation("org.springframework.boot:spring-boot-starter-security")
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("org.springframework.boot:spring-boot-starter-validation")

    // Resilience4j (unchanged from Phase 2)
    implementation("org.springframework.cloud:spring-cloud-starter-circuitbreaker-reactor-resilience4j")

    // ADD: Test frameworks
    testImplementation("io.kotest:kotest-runner-junit5")
    testImplementation("io.kotest:kotest-assertions-core")
    testImplementation("io.mockk:mockk")
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.testcontainers:testcontainers:1.19.0")
    testImplementation("org.testcontainers:postgresql:1.19.0")
    
    // REMOVE: All Lombok
}

dependencyManagement {
    imports {
        mavenBom("org.springframework.cloud:spring-cloud-dependencies:2025.1.1")
    }
}
```

**Key Changes**:
- ❌ **Removed** `spring-boot-starter-webflux` (Reactor/Mono/Flux)
- ✅ **Added** `spring-boot-starter-data-r2dbc` (async relational DB)
- ✅ **Added** `kotlinx-coroutines-*` (Kotlin async)
- ✅ **Added** `testcontainers-postgresql` (for integration tests)

**Verify**:
```bash
./gradlew :core-service:dependencies | grep -i webflux
# Should show: NOTHING

./gradlew :core-service:dependencies | grep -i r2dbc
# Should show: r2dbc-postgresql

./gradlew :core-service:dependencies | grep -i coroutines
# Should show: kotlinx-coroutines-*
```

---

## Part 2: banking-commons Migration

### Step 2.1: Create Directory Structure

```bash
cd corebank-microservices-java/banking-commons

# Main source
mkdir -p src/main/kotlin/com/corebank/commons/{model,security,exception,dto}

# Test source
mkdir -p src/test/kotlin/com/corebank/commons/{model,security,exception}
```

---

### Step 2.2: Create DTOs (model/)

**File**: `banking-commons/src/main/kotlin/com/corebank/commons/model/ResponseDTO.kt`

```kotlin
package com.corebank.commons.model

/**
 * Standardized response wrapper used across all CoreBank microservices.
 * Preserves the identical contract from Phase 1 monolith.
 */
data class ResponseDTO<T>(
    val statusCode: Int,
    val body: T,
    val extraArgs: Map<String, Any>? = null
) {
    companion object {
        fun <T> success(body: T) = ResponseDTO(
            statusCode = 200,
            body = body,
            extraArgs = null
        )

        fun <T> error(statusCode: Int, body: T) = ResponseDTO(
            statusCode = statusCode,
            body = body,
            extraArgs = null
        )

        fun <T> error(statusCode: Int, body: T, extraArgs: Map<String, Any>) = ResponseDTO(
            statusCode = statusCode,
            body = body,
            extraArgs = extraArgs
        )
    }
}
```

**Key Points**:
- `data class`: Kotlin generates `equals()`, `hashCode()`, `toString()`, `copy()` automatically
- `companion object`: Contains factory methods (replaces `@Builder` from Lombok)
- Default parameter `extraArgs = null`: Optional, idiomatic Kotlin

**Repeat this pattern for all DTOs** (LoginRequestDTO, AccountDTO, CardDTO, BalanceDTO, HomeAggregateDTO).

---

### Step 2.3: Create JwtUtil (security/)

**File**: `banking-commons/src/main/kotlin/com/corebank/commons/security/JwtUtil.kt`

```kotlin
package com.corebank.commons.security

import io.jsonwebtoken.Claims
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.security.Keys
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import java.nio.charset.StandardCharsets
import java.util.Date
import javax.crypto.SecretKey

/**
 * JWT utility shared across all CoreBank microservices.
 * Extracted from Phase 1 monolith — same signing/validation logic.
 */
@Component
class JwtUtil(
    @Value("\${jwt.secret:super-secret-for-demo-only-change-in-prod}")
    private val secret: String,

    @Value("\${jwt.expiration:3600000}")
    private val expiration: Long
) {

    private fun getSigningKey(): SecretKey {
        return Keys.hmacShaKeyFor(secret.toByteArray(StandardCharsets.UTF_8))
    }

    /**
     * Generate a signed JWT with custom claims.
     */
    fun generateToken(username: String, claims: Map<String, Any>): String {
        return Jwts.builder()
            .claims(claims)
            .subject(username)
            .issuedAt(Date())
            .expiration(Date(System.currentTimeMillis() + expiration))
            .signWith(getSigningKey())
            .compact()
    }

    /**
     * Validate a JWT token (signature + expiration).
     */
    fun validateToken(token: String): Boolean {
        return try {
            Jwts.parser().verifyWith(getSigningKey()).build().parseSignedClaims(token)
            true
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Extract the subject (username) from a valid JWT.
     */
    fun extractUsername(token: String): String {
        return extractAllClaims(token).subject
    }

    /**
     * Extract all claims from a valid JWT.
     */
    fun extractAllClaims(token: String): Claims {
        return Jwts.parser()
            .verifyWith(getSigningKey())
            .build()
            .parseSignedClaims(token)
            .payload
    }
}
```

**Key Differences from Java**:
- No `private SecretKey getSigningKey()` — Kotlin uses `private fun` keyword
- Constructor injection via primary constructor: `constructor(val secret: String, val expiration: Long)` (but using `@Value` here)
- Exception handling: `try-catch` syntax unchanged, but more concise with Kotlin's scope functions (not used here for clarity)

---

### Step 2.4: Create HeaderConstants (security/)

**File**: `banking-commons/src/main/kotlin/com/corebank/commons/security/HeaderConstants.kt`

```kotlin
package com.corebank.commons.security

/**
 * Constants for custom banking headers used across all CoreBank services.
 * These headers simulate the production banking header propagation pattern
 * used in real financial institutions.
 */
object HeaderConstants {
    const val X_RQ_UID = "X-RqUid"
    const val X_SES_ID = "X-SesID"
    const val X_CUST_IDENT_NUM = "X-CustIdentNum"
    const val X_CUST_IDENT_TYPE = "X-CustIdentType"
    const val AUTHORIZATION = "Authorization"
    const val BEARER_PREFIX = "Bearer "

    val PROPAGATED_HEADERS = arrayOf(
        X_RQ_UID, X_SES_ID, X_CUST_IDENT_NUM, X_CUST_IDENT_TYPE
    )
}
```

**Key Differences from Java**:
- `object` singleton (not `public final class` with private constructor)
- `const val` for compile-time constants (not `public static final`)

---

### Step 2.5: Create Tests (Kotest + MockK)

**File**: `banking-commons/src/test/kotlin/com/corebank/commons/model/ResponseDTOTest.kt`

```kotlin
package com.corebank.commons.model

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

class ResponseDTOTest : StringSpec({
    "success() should create ResponseDTO with status 200" {
        val response = ResponseDTO.success("test-body")

        response.statusCode shouldBe 200
        response.body shouldBe "test-body"
        response.extraArgs shouldBe null
    }

    "error() should create ResponseDTO with given status code" {
        val response = ResponseDTO.error(401, "Unauthorized")

        response.statusCode shouldBe 401
        response.body shouldBe "Unauthorized"
        response.extraArgs shouldBe null
    }

    "error() with extraArgs should include them in response" {
        val extras = mapOf("field" to "username")
        val response = ResponseDTO.error(400, "Validation failed", extras)

        response.statusCode shouldBe 400
        response.body shouldBe "Validation failed"
        response.extraArgs shouldNotBe null
        response.extraArgs?.get("field") shouldBe "username"
    }
})
```

**Key Differences from Mockito**:
- `class ResponseDTOTest : StringSpec({ ... })` — inherits StringSpec DSL
- `"test description"` — block syntax (no `@Test` annotations)
- `shouldBe`, `shouldNotBe` — Kotest assertions (not JUnit's `assertEquals`)
- No `@ExtendWith`, no mocking setup (this test doesn't need mocks)

---

## Part 3: auth-service Migration (Simple, Synchronous)

### Step 3.1: Create Domain Models

**File**: `auth-service/src/main/kotlin/com/corebank/auth/domain/model/Credentials.kt`

```kotlin
package com.corebank.auth.domain.model

/**
 * Domain value object representing login credentials.
 */
data class Credentials(
    val username: String,
    val password: String
) {
    /**
     * Validate credentials against mock data.
     * In a real system this would delegate to a user store.
     */
    fun isValid() = username == "user" && password == "password"
}
```

**File**: `auth-service/src/main/kotlin/com/corebank/auth/domain/model/AuthToken.kt`

```kotlin
package com.corebank.auth.domain.model

import java.time.Instant
import kotlin.collections.Map

/**
 * Domain value object representing an authentication token.
 */
data class AuthToken(
    val token: String,
    val username: String,
    val expiresAt: Instant,
    val claims: Map<String, Any>
)
```

---

### Step 3.2: Create Application Service

**File**: `auth-service/src/main/kotlin/com/corebank/auth/application/port/input/AuthenticateUseCase.kt`

```kotlin
package com.corebank.auth.application.port.input

/**
 * Input port (use case) for client authentication.
 * Implemented by the application service, called by the web adapter.
 */
interface AuthenticateUseCase {
    /**
     * Authenticate a client and return a signed JWT.
     */
    fun authenticate(
        username: String,
        password: String,
        custIdentNum: String?,
        custIdentType: String?
    ): String
}
```

**File**: `auth-service/src/main/kotlin/com/corebank/auth/application/port/output/TokenCachePort.kt`

```kotlin
package com.corebank.auth.application.port.output

/**
 * Output port for token caching (Redis adapter).
 * Defines what the application needs without knowing the implementation.
 */
interface TokenCachePort {
    fun cacheToken(key: String, token: String, ttlSeconds: Long)
    fun getCachedToken(key: String): String?
    fun invalidateToken(key: String)
}
```

**File**: `auth-service/src/main/kotlin/com/corebank/auth/application/service/AuthApplicationService.kt`

```kotlin
package com.corebank.auth.application.service

import com.corebank.auth.application.port.input.AuthenticateUseCase
import com.corebank.auth.application.port.output.TokenCachePort
import com.corebank.auth.domain.model.Credentials
import com.corebank.commons.security.JwtUtil
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

/**
 * Application service implementing the authentication use case.
 * Orchestrates: credential validation → JWT generation → Redis caching.
 */
@Service
class AuthApplicationService(
    private val jwtUtil: JwtUtil,
    private val tokenCachePort: TokenCachePort
) : AuthenticateUseCase {

    private val log = LoggerFactory.getLogger(javaClass)

    override fun authenticate(
        username: String,
        password: String,
        custIdentNum: String?,
        custIdentType: String?
    ): String {
        val credentials = Credentials(username, password)

        if (!credentials.isValid()) {
            log.warn("Authentication failed for user: {}", username)
            throw SecurityException("Invalid credentials")
        }

        val claims = mapOf(
            "custIdentNum" to (custIdentNum ?: "123456789"),
            "custIdentType" to (custIdentType ?: "CC"),
            "X-SesID" to "session-${System.currentTimeMillis()}"
        )

        val token = jwtUtil.generateToken(credentials.username, claims)

        // Cache the token in Redis (1 hour TTL)
        val cacheKey = "auth:token:${credentials.username}"
        tokenCachePort.cacheToken(cacheKey, token, 3600L)

        log.info("Authentication successful for user: {}, custIdentNum: {}", username, custIdentNum)
        return token
    }
}
```

**Key Points**:
- Constructor injection via primary constructor (Kotlin style)
- `mapOf(...)` instead of `HashMap`
- `custIdentNum ?: "123456789"` instead of ternary operator
- No `suspend` keyword (this service remains synchronous)

---

### Step 3.3: Continue with Controller & Adapters

(Follow same pattern as JwtUtil — straightforward Kotlin translation, no async changes)

---

## Part 4: core-service Migration (ASYNC REWRITE)

### Step 4.1: Remove WebFlux, Add R2DBC

**Already done in gradle config** (Step 1.6). Verify:

```bash
./gradlew :core-service:dependencies | grep webflux
# Should show: NOTHING

./gradlew :core-service:dependencies | grep r2dbc
# Should show: r2dbc-postgresql
```

---

### Step 4.2: Create R2DBC Domain Models

**File**: `core-service/src/main/kotlin/com/corebank/core/domain/model/Account.kt`

```kotlin
package com.corebank.core.domain.model

import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Column
import org.springframework.data.relational.core.mapping.Table
import java.math.BigDecimal

/**
 * Domain entity representing a customer account.
 * Mapped to R2DBC (non-blocking relational database).
 */
@Table("account")
data class Account(
    @Id
    val id: Long? = null,

    @Column("account_number")
    val accountNumber: String,

    @Column("account_type")
    val accountType: String,

    val balance: BigDecimal,

    @Column("customer_id")
    val customerId: String
)
```

**Key Differences from JPA**:
- `@Table` (R2DBC) instead of `@Entity` (JPA) — but same concept
- `@Column` for custom column names
- `@Id` same as JPA
- No `@GeneratedValue` needed (R2DBC handles auto-increment)
- `data class` replaces Lombok `@Data`

**Repeat for Card.kt and Balance.kt** (same pattern).

---

### Step 4.3: Create Async Application Service (CRITICAL)

**File**: `core-service/src/main/kotlin/com/corebank/core/application/port/input/GetHomeBalanceUseCase.kt`

```kotlin
package com.corebank.core.application.port.input

import com.corebank.core.domain.model.HomeAggregate

/**
 * Input port (use case) for aggregated home balance.
 */
interface GetHomeBalanceUseCase {
    /**
     * Fetch aggregated balance data for a customer (async via coroutines).
     */
    suspend fun getAggregatedBalance(customerId: String): HomeAggregate
}
```

**File**: `core-service/src/main/kotlin/com/corebank/core/application/port/output/AccountRepositoryPort.kt`

```kotlin
package com.corebank.core.application.port.output

import com.corebank.core.domain.model.Account
import org.springframework.data.r2dbc.repository.R2dbcRepository

/**
 * Output port for account data access.
 * Extends CoroutineCrudRepository for suspend function support.
 */
interface AccountRepositoryPort : R2dbcRepository<Account, Long> {
    suspend fun findByCustomerId(customerId: String): List<Account>
}
```

**File**: `core-service/src/main/kotlin/com/corebank/core/application/service/HomeApplicationService.kt` (ASYNC REWRITE)

```kotlin
package com.corebank.core.application.service

import com.corebank.core.application.port.input.GetHomeBalanceUseCase
import com.corebank.core.application.port.output.AccountRepositoryPort
import com.corebank.core.application.port.output.BalanceRepositoryPort
import com.corebank.core.application.port.output.CardRepositoryPort
import com.corebank.core.domain.model.HomeAggregate
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker
import io.github.resilience4j.retry.annotation.Retry
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

/**
 * Application service implementing the home balance aggregation use case.
 * Uses coroutineScope + async for structured concurrent data fetching.
 * Protected by Resilience4j Circuit Breaker + Retry.
 */
@Service
class HomeApplicationService(
    private val accountRepositoryPort: AccountRepositoryPort,
    private val cardRepositoryPort: CardRepositoryPort,
    private val balanceRepositoryPort: BalanceRepositoryPort
) : GetHomeBalanceUseCase {

    private val log = LoggerFactory.getLogger(javaClass)

    @Override
    @CircuitBreaker(name = "homeService", fallbackMethod = "getAggregatedBalanceFallback")
    @Retry(name = "homeService")
    override suspend fun getAggregatedBalance(customerId: String): HomeAggregate {
        log.info("Fetching aggregated balance for customer: {}", customerId)

        return coroutineScope {
            // Launch all three queries concurrently
            val accountsDeferred = async {
                accountRepositoryPort.findByCustomerId(customerId)
            }
            val cardsDeferred = async {
                cardRepositoryPort.findByCustomerId(customerId)
            }
            val balanceDeferred = async {
                balanceRepositoryPort.findByCustomerId(customerId)
            }

            // Wait for all to complete (structured concurrency)
            HomeAggregate(
                accounts = accountsDeferred.await(),
                cards = cardsDeferred.await(),
                balance = balanceDeferred.await()
            )
        }
    }

    /**
     * Fallback method when circuit breaker is open.
     */
    @Suppress("unused")
    private suspend fun getAggregatedBalanceFallback(
        customerId: String,
        t: Throwable
    ): HomeAggregate {
        log.warn("Circuit breaker fallback triggered for customer: {}, reason: {}", customerId, t.message)
        return HomeAggregate(
            accounts = emptyList(),
            cards = emptyList(),
            balance = null
        )
    }
}
```

**CRITICAL DIFFERENCES FROM PHASE 2**:

| Phase 2 (Java + Reactor) | Phase 4 (Kotlin + Coroutines) |
|---|---|
| `Mono<HomeAggregate>` return | `suspend fun ... : HomeAggregate` |
| `Mono.zip(...)` | `coroutineScope { async { } }` |
| `.map(tuple -> ...)` | Direct `HomeAggregate(...)` construction |
| `.block()` or `StepVerifier` in tests | `runTest { }` |

**Key Points**:
1. `suspend fun` keyword — declares async function (must be called from async context)
2. `coroutineScope { }` — structured concurrency block (all child tasks must complete)
3. `async { }` — launches child task concurrently
4. `.await()` — suspends until task completes (safe, non-blocking)
5. `@CircuitBreaker`, `@Retry` — work with suspend functions seamlessly

---

### Step 4.4: Create Repository Adapters

**File**: `core-service/src/main/kotlin/com/corebank/core/infrastructure/adapter/persistence/AccountRepositoryAdapter.kt`

```kotlin
package com.corebank.core.infrastructure.adapter.persistence

import com.corebank.core.application.port.output.AccountRepositoryPort
import com.corebank.core.domain.model.Account
import org.springframework.data.r2dbc.repository.Query
import org.springframework.stereotype.Repository

/**
 * R2DBC adapter implementing AccountRepositoryPort.
 * All methods are suspend functions (non-blocking).
 */
@Repository
interface AccountRepositoryAdapter : AccountRepositoryPort {
    @Query("SELECT * FROM account WHERE customer_id = :customerId")
    override suspend fun findByCustomerId(customerId: String): List<Account>
}
```

**Explanation**:
- `@Repository` tells Spring this is a data access component
- Extends `AccountRepositoryPort` (output port)
- `@Query` for custom SQL
- `suspend fun` — Spring Data R2DBC automatically provides implementation

---

### Step 4.5: Create Controller (Suspend Endpoint)

**File**: `core-service/src/main/kotlin/com/corebank/core/infrastructure/adapter/web/HomeController.kt`

```kotlin
package com.corebank.core.infrastructure.adapter.web

import com.corebank.commons.model.ResponseDTO
import com.corebank.commons.security.HeaderConstants
import com.corebank.core.application.port.input.GetHomeBalanceUseCase
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

/**
 * Web adapter for home balance endpoint.
 * Suspend endpoint — Spring WebFlux automatically dispatches to coroutine dispatcher.
 */
@RestController
@RequestMapping("/api/home")
class HomeController(
    private val getHomeBalanceUseCase: GetHomeBalanceUseCase
) {

    @GetMapping("/balance")
    suspend fun getBalance(
        @RequestHeader(HeaderConstants.AUTHORIZATION) authHeader: String,
        @RequestHeader(HeaderConstants.X_CUST_IDENT_NUM) custIdentNum: String
    ): ResponseEntity<ResponseDTO<String>> {
        // Extract customerId from JWT (simplified)
        val customerId = custIdentNum // In real code: parse JWT to get ID

        val aggregate = getHomeBalanceUseCase.getAggregatedBalance(customerId)

        // Convert to DTO (toDTO() extension function)
        val dto = aggregate.toHomeAggregateDTO()

        return ResponseEntity.ok(ResponseDTO.success(dto))
    }
}
```

**Key Points**:
- `suspend fun` — Spring WebFlux recognizes this and handles async dispatch
- No need for `.block()` or `Mono<>`
- Clean, imperative code

---

## Part 5: Testing with Kotest + MockK

### Example: Service Test (Unit, No Spring Context)

**File**: `core-service/src/test/kotlin/com/corebank/core/application/service/HomeApplicationServiceTest.kt`

```kotlin
package com.corebank.core.application.service

import com.corebank.core.application.port.output.AccountRepositoryPort
import com.corebank.core.application.port.output.BalanceRepositoryPort
import com.corebank.core.application.port.output.CardRepositoryPort
import com.corebank.core.domain.model.Account
import com.corebank.core.domain.model.Balance
import com.corebank.core.domain.model.Card
import com.corebank.core.domain.model.HomeAggregate
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.mockk.coEvery
import io.mockk.mockk
import java.math.BigDecimal

class HomeApplicationServiceTest : StringSpec({
    val accountRepository = mockk<AccountRepositoryPort>()
    val cardRepository = mockk<CardRepositoryPort>()
    val balanceRepository = mockk<BalanceRepositoryPort>()
    val service = HomeApplicationService(accountRepository, cardRepository, balanceRepository)

    "getAggregatedBalance should return complete HomeAggregate" {
        val customerId = "123456789"

        // Setup mocks with coroutine support (coEvery)
        coEvery { accountRepository.findByCustomerId(customerId) } returns listOf(
            Account(id = 1, accountNumber = "ACC-001", accountType = "SAVINGS", 
                    balance = BigDecimal("1000.00"), customerId = customerId)
        )
        coEvery { cardRepository.findByCustomerId(customerId) } returns listOf(
            Card(id = 1, cardNumber = "CARD-001", cardType = "CREDIT",
                 creditLimit = BigDecimal("5000.00"), 
                 availableBalance = BigDecimal("3200.00"), customerId = customerId)
        )
        coEvery { balanceRepository.findByCustomerId(customerId) } returns Balance(
            id = 1, customerId = customerId, 
            totalBalance = BigDecimal("3500.50"), 
            availableBalance = BigDecimal("3200.00")
        )

        // Test async function (automatically runs in test coroutine scope)
        val result = service.getAggregatedBalance(customerId)

        // Assertions
        result.accounts.size shouldBe 1
        result.cards.size shouldBe 1
        result.balance shouldNotBe null
        result.balance?.customerId shouldBe customerId
    }

    "getAggregatedBalance should handle empty results" {
        val customerId = "999999999"

        coEvery { accountRepository.findByCustomerId(customerId) } returns emptyList()
        coEvery { cardRepository.findByCustomerId(customerId) } returns emptyList()
        coEvery { balanceRepository.findByCustomerId(customerId) } returns Balance(
            customerId = customerId,
            totalBalance = BigDecimal.ZERO,
            availableBalance = BigDecimal.ZERO
        )

        val result = service.getAggregatedBalance(customerId)

        result.accounts.isEmpty() shouldBe true
        result.cards.isEmpty() shouldBe true
    }
})
```

**Key Kotest + MockK Patterns**:

| Pattern | Meaning |
|---------|---------|
| `coEvery { } returns` | Mock a suspend function (coroutine-aware) |
| `val result = service.getAggregatedBalance(...)` | Call suspend function directly in test (Kotest handles context) |
| `result.size shouldBe 1` | Assertion (Kotest infix) |
| `StringSpec({ })` | Test specification DSL |

---

## Verification Commands

### After Each Phase

```bash
# Phase 1 (Gradle)
./gradlew clean build

# Phase 2 (banking-commons)
./gradlew :banking-commons:build jacocoTestCoverageVerification

# Phase 3 (auth-service)
./gradlew :auth-service:build jacocoTestCoverageVerification

# Phase 4 (core-service)
./gradlew :core-service:build jacocoTestCoverageVerification

# Final verification
./gradlew clean build jacocoTestReport
./gradlew test

# Check for violations
grep -r "Mono\|Flux" core-service/src/main/kotlin  # Should return nothing
grep -r "@Data\|@Getter" */src/main/kotlin        # Should return nothing
grep -r "when()\.thenReturn\|Mockito" */src/test/kotlin  # Should return nothing
```

---

## Common Issues & Solutions

| Issue | Solution |
|-------|----------|
| `suspend fun` not recognized | Ensure Kotlin plugin is applied in `build.gradle.kts` |
| `coEvery` not found | Add import: `import io.mockk.coEvery` |
| `Mono`/`Flux` still showing up | `grep` for them and remove; replace with coroutines |
| Gradle can't find Kotlin stdlib | Run: `./gradlew clean --refresh-dependencies` |
| Tests fail with "no handler" error | Use `@SpringBootTest` for integration tests; plain units don't need Spring |
| R2DBC queries not working | Check Docker: `docker-compose ps` and `docker-compose logs postgres` |

---

This plan should be executed phase by phase, with verification at each step.

