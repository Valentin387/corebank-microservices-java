# Fresh Kotlin Implementation — Execution Checklist

## Pre-Implementation

- [ ] Read all .specs2 documents
- [ ] Spring Initializr project created
- [ ] Git repo initialized
- [ ] IDE configured (Kotlin 2.3.21)
- [ ] PostgreSQL running locally
- [ ] Gradle clean build succeeds

---

## Week 1

### Monday: Project Bootstrap

- [ ] Gradle build works
- [ ] IDE has zero Kotlin errors
- [ ] Test runner configured
- [ ] First test passes (hello world test)
- [ ] Git commit: "Initial scaffold"

**Checkpoint**: Gradle buildable, IDE ready

### Tuesday: banking-commons Port

**Domain Models**:
- [ ] Account.kt (data class)
- [ ] Balance.kt (data class)
- [ ] Card.kt (data class)
- [ ] All tests passing

**Security**:
- [ ] JwtUtil.kt ported
- [ ] ReactiveJwtFilter.kt ported
- [ ] SecurityConfig.kt ported
- [ ] JwtUtil tests pass (Kotest)
- [ ] Filter tests pass (mocked)

**Banking Commons Tests**:
- [ ] 80%+ coverage
- [ ] All tests pass
- [ ] Git commit: "banking-commons complete"

**Checkpoint**: Shared library ready, no Java code

### Wednesday-Thursday: auth-service Port

**Controllers & Services**:
- [ ] AuthApplicationService.kt
- [ ] AuthController.kt with suspend endpoints
- [ ] SecurityConfig.kt (WebFlux)
- [ ] Integration tests (WebTestClient)

**Auth Tests**:
- [ ] Login endpoint test passes
- [ ] Logout endpoint test passes
- [ ] Invalid token test passes
- [ ] 80%+ coverage
- [ ] All tests pass
- [ ] Git commit: "auth-service complete"

**Checkpoint**: Auth service complete, working endpoints

### Friday: core-service Structure

**R2DBC Repositories**:
- [ ] AccountRepository interface (R2DBC)
- [ ] BalanceRepository interface (R2DBC)
- [ ] CardRepository interface (R2DBC)
- [ ] Tables created in PostgreSQL
- [ ] Basic test queries work

**Domain Models**:
- [ ] Account.kt (with @Table)
- [ ] Balance.kt
- [ ] Card.kt
- [ ] Aggregates defined

**Structure**: 
- [ ] All modules compile
- [ ] No compilation errors
- [ ] Git commit: "core-service structure ready"

**Checkpoint**: Database schema ready, repos compile

---

## Week 2

### Monday-Tuesday: R2DBC Implementation

**Repository Adapters**:
- [ ] AccountRepositoryAdapter.kt (with suspend)
- [ ] BalanceRepositoryAdapter.kt
- [ ] CardRepositoryAdapter.kt
- [ ] All adapters follow port interface

**Application Services**:
- [ ] AccountApplicationService.kt (suspend functions)
- [ ] BalanceApplicationService.kt
- [ ] CardApplicationService.kt
- [ ] Coroutine scope usage correct

**R2DBC Learning**:
- [ ] Queries written without JPA
- [ ] Suspend functions work end-to-end
- [ ] Database inserts/selects verified
- [ ] No blocking I/O detected

**Tests**:
- [ ] Repository adapter tests (mocked R2DBC)
- [ ] Service tests (mocked repos)
- [ ] 70%+ coverage
- [ ] Git commit: "R2DBC adapters complete"

**Checkpoint**: Database layer async, all queries tested

### Wednesday-Thursday: Integration & Full Tests

**Controllers**:
- [ ] AccountController.kt (suspend endpoints)
- [ ] HomeController.kt
- [ ] All endpoints return suspend

**Integration Tests**:
- [ ] End-to-end controller tests (WebTestClient)
- [ ] Database tests (real R2DBC)
- [ ] Service mocking tests
- [ ] Security tests
- [ ] Error handling tests

**Coverage**:
- [ ] 80%+ coverage (all modules)
- [ ] All tests passing
- [ ] No flaky tests
- [ ] Git commit: "All integration tests passing"

**Checkpoint**: Full system tested, production-ready code

### Friday: Documentation & Validation

**Documentation**:
- [ ] README.md updated (stack, setup, running)
- [ ] Architecture documented
- [ ] Migration guide completed
- [ ] Known issues captured
- [ ] Code examples in comments

**Final Validation**:
- [ ] `./gradlew clean build` passes
- [ ] All tests pass (100%)
- [ ] 80%+ coverage (all modules)
- [ ] No IDE warnings
- [ ] API contract matches Phase 2
- [ ] Performance acceptable

**Deployment Readiness**:
- [ ] Code review prep
- [ ] Migration guide from old repo complete
- [ ] Deployment instructions ready
- [ ] Git commit: "Ready for deployment"
- [ ] Git tag: "v2.0-kotlin-release"

**Checkpoint**: Production ready, all validation passed

---

## Daily Verification

**Every day, ensure**:

- [ ] `./gradlew clean build` succeeds
- [ ] IDE shows zero Kotlin errors
- [ ] Tests run locally (70%+ passing)
- [ ] No blocking I/O in async code
- [ ] Coroutine context makes sense
- [ ] Git commit made (progress tracked)

---

## Red Flags (Escalate Immediately)

🚨 **Gradle won't build** → Check Kotlin version, plugin conflicts
🚨 **R2DBC connection fails** → PostgreSQL not running, check config
🚨 **Suspend functions cause errors** → Missing `suspend` keyword, context issue
🚨 **Tests timeout** → Coroutine context deadlock, blocking I/O
🚨 **IDE can't resolve Kotlin** → IDE not recognizing Kotlin plugin
🚨 **Performance degradation** → Blocking I/O or thread pool exhaustion

---

## Module Sign-Off

**banking-commons**:
- [ ] Compiles
- [ ] Tests passing
- [ ] 80%+ coverage
- [ ] Code reviewed

**auth-service**:
- [ ] Compiles
- [ ] Tests passing
- [ ] 80%+ coverage
- [ ] Endpoints verified
- [ ] Code reviewed

**core-service**:
- [ ] Compiles
- [ ] Tests passing
- [ ] 80%+ coverage
- [ ] R2DBC working
- [ ] Coroutines integrated
- [ ] Code reviewed

---

## End-to-End Test Scenarios

Before final sign-off:

- [ ] Create account via API
- [ ] Query account by customer ID
- [ ] JWT authentication works
- [ ] Unauthorized access blocked
- [ ] Database data persists
- [ ] Performance acceptable (< 100ms per request)

---

**Status**: ✅ Ready to begin