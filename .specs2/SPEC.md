# Fresh Kotlin Microservices — Specification

**Version**: 2.0  
**Date**: May 25, 2026  
**Scope**: New CoreBank microservices in pure Kotlin

---

## Executive Summary

Build production-ready CoreBank microservices using Kotlin, Spring Boot 4.0.6, R2DBC, and Coroutines. Fresh start (no migration). Optimal async-first architecture from day 1.

**Target**: Deploy working Kotlin microservices in 2 weeks.

---

## 1. Objectives

### Primary

1. **Production-ready Kotlin** — All code in idiomatic Kotlin (2.3.21)
2. **True async database** — R2DBC instead of blocking JPA
3. **Native coroutines** — Kotlin suspend functions, not Reactor
4. **Clean architecture** — Hexagonal (same as Phase 2)
5. **Modern testing** — Kotest + MockK (no Mockito)

### Secondary

1. Preserve API contract from Phase 2 (endpoints identical)
2. 80%+ test coverage (enforced)
3. Documentation comparable to Phase 2
4. Knowledge transfer (team learns Kotlin patterns)

---

## 2. Scope

### In Scope

✅ 3 microservices:
- `banking-commons` (shared library, JWT, security)
- `auth-service` (synchronous authentication)
- `core-service` (main service, async database, WebFlux)

✅ Features:
- JWT security (same as Phase 2)
- Account management via R2DBC
- Reactive endpoints (WebFlux)
- Coroutine suspend functions
- Comprehensive tests (Kotest)

### Out of Scope

❌ New features beyond Phase 2
❌ Migration tools or converters
❌ Backward compatibility with Java clients
❌ Lombok → data class conversion tools (manual)

---

## 3. Success Criteria

### Build Level

- [ ] `./gradlew clean build` passes
- [ ] All 3 modules compile with zero warnings
- [ ] No deprecation warnings in tests
- [ ] Gradle cache stable (no flakes)

### Code Quality

- [ ] 80%+ JaCoCo coverage (all modules)
- [ ] Zero test failures
- [ ] Zero compiler errors in IDE
- [ ] Code follows idiomatic Kotlin style

### Functional

- [ ] All endpoints match Phase 2 API contract
- [ ] JWT authentication works end-to-end
- [ ] Database queries work with R2DBC
- [ ] Tests pass with Kotest + MockK
- [ ] No blocking I/O in async paths

### Documentation

- [ ] README explains stack choices
- [ ] Architecture documented
- [ ] Migration guide (Java → Kotlin)
- [ ] Known issues noted

---

## 4. Architecture Overview

```
┌─────────────────────────────┐
│   WebFlux Controllers       │  (Kotlin, suspend)
│   (reactive endpoints)      │
└────────────────┬────────────┘
                 │
┌────────────────▼────────────┐
│  Application Services       │  (ports, coroutines)
│  (use cases)                │
└────────────────┬────────────┘
                 │
┌────────────────▼────────────┐
│  Domain Models              │  (data classes, value objects)
│  (Kotlin data classes)      │
└────────────────┬────────────┘
                 │
┌────────────────▼────────────┐
│  Infrastructure Adapters    │  (R2DBC, config)
│  (R2DBC repositories)       │
└─────────────────────────────┘
```

**Difference from Phase 2**:
- No JPA ORM (blocking)
- No Reactor (async wrappers on top of blocking)
- Direct R2DBC (native async) ✅
- Direct Coroutines ✅
- Kotlin data classes (not Lombok) ✅

---

## 5. Tech Stack

| Layer | Tech | Version | Why |
|-------|------|---------|-----|
| **Language** | Kotlin | 2.3.21 | Latest stable, production-ready, full coroutine support |
| **Framework** | Spring Boot | 4.0.6 | Modern, coroutine support, WebFlux built-in |
| **Async** | Coroutines | 1.7.3 | Native Kotlin, better than Reactor for this use case |
| **Database** | R2DBC | (Spring Boot) | True reactive, no blocking, works great with coroutines |
| **Database Driver** | PostgreSQL JDBC4 | - | Proven, widely used |
| **Testing** | Kotest | 5.7.0 | Idiomatic Kotlin, test suspension support |
| **Mocking** | MockK | 1.13.5 | Kotlin-native, better than Mockito for Kotlin |
| **Build** | Gradle | 9.4+ | Kotlin DSL, modern |

---

## 6. Module Breakdown

### `banking-commons` (library)

**Purpose**: Shared JWT security, filters, domain models

**Key Code**:
- `JwtUtil` — JWT generation/validation
- `ReactiveJwtFilter` — WebFlux security filter
- Domain models (Account, Balance, Card)
- DTOs and responses

**Timeline**: 2 hours

**Dependencies**: Spring Security, Spring WebFlux, JWT library

---

### `auth-service` (synchronous)

**Purpose**: Authentication service (simple, no DB)

**Key Code**:
- `AuthController` — Login/logout endpoints
- `AuthApplicationService` — auth logic
- Security config (WebFlux)

**Timeline**: 3 hours

**Dependencies**: banking-commons, Spring WebFlux, Spring Security

---

### `core-service` (main, async)

**Purpose**: Account management with R2DBC database

**Key Code**:
- `HomeController` — home endpoint
- `AccountApplicationService` — use cases
- `AccountRepository` (R2DBC interface)
- Domain aggregates (Account, Balance, Card)
- Adapters (repositories)

**Timeline**: 5 hours (including R2DBC learning)

**Dependencies**: banking-commons, R2DBC, Coroutines, PostgreSQL

---

## 7. Key Differences from Phase 4 Migration

| Aspect | Phase 4 (Migration) | Phase 2.0 (Fresh) |
|--------|-------------------|------------------|
| **Starting point** | Java + Reactor (broken) | Fresh scaffold |
| **Gradle issues** | Multiple BOM conflicts | Clean from start |
| **Architecture** | Retrofit async on blocking | Async-first design |
| **Database** | JPA then switch to R2DBC | R2DBC from day 1 |
| **Testing** | Retrofit Kotest | Kotest from day 1 |
| **Timeline** | 4-6 weeks (circular issues) | 2 weeks (linear) |
| **Risk** | High (incremental failures) | Low (proven patterns) |

---

## 8. Phase Timeline

### Week 1

**Monday**:
- [ ] Create new project via Spring Initializr
- [ ] Configure gradle (Kotlin 2.3.21, R2DBC, Coroutines)
- [ ] First build test
- **Output**: buildable Kotlin project

**Tuesday**:
- [ ] Port `banking-commons` domain models (data classes)
- [ ] Port JWT utilities
- [ ] Port ReactiveJwtFilter
- [ ] First tests passing
- **Output**: banking-commons compiles, tests pass

**Wednesday-Thursday**:
- [ ] Port `auth-service` logic
- [ ] Security configuration (WebFlux)
- [ ] Auth tests
- [ ] All auth tests passing
- **Output**: auth-service working, 80%+ coverage

**Friday**:
- [ ] Port `core-service` structure
- [ ] Create R2DBC repository interfaces
- [ ] Domain models in place
- [ ] Compile check
- **Output**: core-service structure ready, not yet tested

### Week 2

**Monday-Tuesday**:
- [ ] Write R2DBC queries (first time learning)
- [ ] Implement repository adapters
- [ ] Coroutine integration
- [ ] Database tests
- **Output**: R2DBC working end-to-end

**Wednesday-Thursday**:
- [ ] Full integration tests
- [ ] All tests to 80%+ coverage
- [ ] Security tests
- [ ] Performance validation
- **Output**: all tests passing

**Friday**:
- [ ] Documentation
- [ ] README updates
- [ ] Known issues capture
- [ ] Code review prep
- **Output**: ready for deployment

---

## 9. Known Constraints

1. **PostgreSQL required** — R2DBC driver hardcoded to PostgreSQL
2. **Suspend functions** — All database operations must be suspend
3. **No blocking I/O** — Any blocking call will show in profiler
4. **Coroutine context** — Must understand context propagation
5. **Test context** — Spring Boot coroutine test context is different

---

## 10. Go/No-Go Checklist

**Before coding starts**:
- [ ] Spring Initializr project created
- [ ] Gradle builds successfully
- [ ] Team has Kotlin 2.3.21 IDE support
- [ ] PostgreSQL available for local testing
- [ ] Git repo initialized

**Weekly checkpoints**:
- [ ] Week 1 Friday: 2/3 modules compiling, 50% tests passing
- [ ] Week 2 Wednesday: All 3 modules compiled, 70%+ coverage
- [ ] Week 2 Friday: All tests passing, 80%+ coverage, ready for prod

---

## 11. Success Indicators (Daily)

✅ **Gradle clean build succeeds**  
✅ **IntelliJ/IDE shows zero Kotlin errors**  
✅ **Tests run and mostly pass (70%+)**  
✅ **No blocking I/O detected**  
✅ **Coroutines compile and run**  

❌ **Red flags**:
- Gradle build fails
- R2DBC can't connect
- Test context issues
- Coroutine scope errors
- Threading issues

---

**Next**: Read [APPROACH.md](./APPROACH.md) for methodology