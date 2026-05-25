# CoreBank Microservices — Fresh Kotlin Start (v2.0)

**Version**: 2.0  
**Date**: May 25, 2026  
**Project**: CoreBank Microservices (Green-Field Kotlin Implementation)  
**Status**: 🟢 Ready for Scaffold

---

## Overview

This `.specs2` folder contains complete specifications for building CoreBank microservices in **pure Kotlin** from scratch, using modern frameworks and async-first architecture.

**Key Difference from Phase 4**: No migration. Fresh project. Optimal patterns from day 1.

**Reading Order**:
1. **SPEC.md** — What we're building
2. **APPROACH.md** — How we'll build it
3. **SETUP_GUIDE.md** — Bootstrap the project
4. **IMPLEMENTATION_PLAN.md** — Code examples and patterns
5. **PORT_GUIDE.md** — How to port logic from old repo
6. **ARCHITECTURE.md** — Hexagonal architecture in Kotlin
7. **MIGRATION_CHECKLIST.md** — Verification for each module

---

## Quick Reference

| Document | Purpose | Read Time |
|----------|---------|-----------|
| [SPEC.md](./SPEC.md) | Objectives, success criteria, scope | 10 min |
| [APPROACH.md](./APPROACH.md) | Methodology, phases, tech stack | 15 min |
| [SETUP_GUIDE.md](./SETUP_GUIDE.md) | Step-by-step bootstrap | 5 min |
| [IMPLEMENTATION_PLAN.md](./IMPLEMENTATION_PLAN.md) | Detailed code examples | Reference |
| [PORT_GUIDE.md](./PORT_GUIDE.md) | Migrate logic from Java repo | Reference |
| [ARCHITECTURE.md](./ARCHITECTURE.md) | Hexagonal patterns in Kotlin | 10 min |
| [MIGRATION_CHECKLIST.md](./MIGRATION_CHECKLIST.md) | Execution tracking | Checklist |

---

## Stack Overview

| Layer | Technology | Version | Notes |
|-------|-----------|---------|-------|
| **Language** | Kotlin | 2.3.21 (latest) | Stable, production-ready |
| **Framework** | Spring Boot | 4.0.6 | WebFlux, modern APIs |
| **Database** | PostgreSQL (R2DBC) | - | Async I/O, no blocking |
| **Async** | Coroutines | 1.7.3 | Native Kotlin async |
| **Testing** | Kotest + MockK | 5.7.0 + 1.13.5 | Idiomatic Kotlin |
| **Build** | Gradle | 9.4+ | Kotlin DSL |

---

## Timeline

```
Week 1:
  Mon:    Project scaffold + gradle setup (1 hr)
  Tue:    banking-commons port + setup (2 hrs)
  Wed-Thu: auth-service (sync, simpler) (3 hrs)
  Fri:    core-service structure + repos (3 hrs)

Week 2:
  Mon-Tue: R2DBC queries + adapters (5 hrs)
  Wed-Thu: Coroutine integration + tests (4 hrs)
  Fri:    Validation + documentation (2 hrs)

Total: ~22 hours (production-ready)
```

---

## Success Criteria (Go/No-Go)

Before deployment:

- [ ] `./gradlew clean build` succeeds
- [ ] All 3 modules compile with 0 warnings
- [ ] Coverage ≥80% (JaCoCo)
- [ ] All tests pass with Kotest
- [ ] No Java files in codebase
- [ ] No Reactor types (Coroutines only)
- [ ] R2DBC queries tested and working
- [ ] API contract matches Phase 2
- [ ] Documentation complete

---

## Key Decisions (vs Phase 4 Migration)

✅ **Fresh start** (no legacy baggage)  
✅ **R2DBC from day 1** (async database)  
✅ **Coroutines only** (no Reactor mix)  
✅ **Pure Kotlin** (no Java compat)  
✅ **Modern tooling** (Kotest, MockK)  
✅ **Fast timeline** (2 weeks vs 4-6 weeks)  

---

## Next Steps

1. Read [SPEC.md](./SPEC.md) for full objectives
2. Follow [SETUP_GUIDE.md](./SETUP_GUIDE.md) to bootstrap
3. Use [IMPLEMENTATION_PLAN.md](./IMPLEMENTATION_PLAN.md) while coding
4. Reference [PORT_GUIDE.md](./PORT_GUIDE.md) for logic porting
5. Track progress with [MIGRATION_CHECKLIST.md](./MIGRATION_CHECKLIST.md)

---

**Status**: 🟢 Ready  
**Last Updated**: May 25, 2026