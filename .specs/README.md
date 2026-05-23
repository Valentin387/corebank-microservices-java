# Phase 4 Specification Suite

**Version**: 1.0  
**Date**: May 23, 2026  
**Project**: CoreBank Microservices Java → Kotlin Migration (Phase 4)

---

## Overview

This `.specs` folder contains the complete specification-driven development documentation for Phase 4 Kotlin Migration. All files are interconnected and must be read in order for full context.

**Reading Order**:
1. **SPEC.md** — What we're building and why
2. **APPROACH.md** — How we'll do it (strategy and patterns)
3. **CONSTITUTION.md** — Immutable rules that override everything else
4. **MIGRATION_CHECKLIST.md** — Step-by-step execution tracking
5. **IMPLEMENTATION_PLAN.md** — Detailed code examples and developer guide

---

## Quick Reference

| Document | Purpose | Audience | Read Time |
|----------|---------|----------|-----------|
| [SPEC.md](./SPEC.md) | **What & Why**: Objectives, success criteria, scope, architecture | PMs, Tech Leads, Architects | 15 min |
| [APPROACH.md](./APPROACH.md) | **How & Strategy**: Methodology, patterns, execution phases | Tech Leads, Developers | 20 min |
| [CONSTITUTION.md](./CONSTITUTION.md) | **Rules & Constraints**: Non-negotiable principles, violations, escalation | **EVERYONE** (before coding) | 10 min |
| [MIGRATION_CHECKLIST.md](./MIGRATION_CHECKLIST.md) | **Execution Tracking**: Step-by-step checklist to mark as complete | Developers (during work) | Reference |
| [IMPLEMENTATION_PLAN.md](./IMPLEMENTATION_PLAN.md) | **Code Examples**: Detailed Kotlin examples, gradle configs, testing patterns | Developers (while coding) | Reference |

---

## Key Decision Matrix

### GO/NO-GO Status

✅ **ALL GREEN — PROCEED WITH IMPLEMENTATION**

| Criterion | Status | Evidence |
|-----------|--------|----------|
| Spring Boot 4.0.6 available? | ✅ Yes | Released April 23, 2026 |
| R2DBC CoroutineCrudRepository exists? | ✅ Yes | Spring Data 4.0.5 official docs |
| Phase 2 baseline stable? | ✅ Yes | All tests pass, 80%+ coverage |
| Kotlin 1.9.22+ available? | ✅ Yes | Latest stable release |
| Team ready? | ✅ Yes | Specifications complete |

---

## Phase 4 at a Glance

### What's Changing

| Component | Phase 2 (Java) | Phase 4 (Kotlin) | Effort |
|-----------|---|---|---|
| **Lang** | Java 21 | Kotlin 1.9.22 | ~ 1 hr (Gradle) |
| **Async** | Project Reactor (Mono/Flux) | Coroutines + Flow | ~5 hrs (core-service) |
| **Database** | JPA (blocking) | R2DBC (suspend) | ~2 hrs (adapters) |
| **Data Models** | Lombok @Data @Builder | Kotlin data class | ~1 hr (domain) |
| **Testing** | JUnit 5 + Mockito | Kotest + MockK | ~3 hrs (all tests) |
| **Code Clarity** | Good (Hexagonal) | Excellent (idiomatic Kotlin) | ~1 hr (review) |

**Total Effort**: ~13-15 hours (1-2 weeks part-time)

---

## Architecture Immutable

Phase 4 preserves the **Hexagonal + DDD architecture** 100%:

```
┌─────────────────────────────────────────┐
│           Web Adapters                  │
│      (Controllers, Filters)             │
└────────────────────┬────────────────────┘
                     │
┌────────────────────▼────────────────────┐
│        Application Services             │
│   (Ports, Use Cases, Orchestration)     │
└────────────────────┬────────────────────┘
                     │
┌────────────────────▼────────────────────┐
│           Domain Models                 │
│    (Entities, Value Objects, Agg.)      │
└────────────────────┬────────────────────┘
                     │
┌────────────────────▼────────────────────┐
│    Infrastructure Adapters              │
│  (Repos, Caches, Config, Filters)       │
└─────────────────────────────────────────┘
```

**No layer changes**. Only language + async pattern upgrade.

---

## Critical Rules (From CONSTITUTION.md)

**Read CONSTITUTION.md BEFORE coding**. These are non-negotiable:

### 🔴 Critical (Build Fails if Violated)

1. **A2**: External API contract frozen (responses identical to Phase 2)
2. **T2**: No Lombok annotations (only Kotlin data classes)
3. **Q2**: 80% JaCoCo coverage minimum (enforced in Gradle)

### 🟠 High (Code Review Rejection)

1. **A1**: Hexagonal architecture immutable
2. **T1**: No blocking I/O in core-service (R2DBC only)
3. **T3**: Kotest + MockK required (no Mockito)
4. **Q1**: No Reactor types in main code

### 🟡 Medium (Code Review Guidance)

1. **D1**: Document any deviations in README

---

## Timeline

```
Week 1:
  Mon: Gradle setup + banking-commons (1-2 hrs)
  Tue-Wed: auth-service (2-3 hrs)
  Thu-Fri: core-service start (2 hrs)

Week 2:
  Mon-Tue: core-service async rewrite (3-4 hrs)
  Wed-Thu: All tests + validation (2-3 hrs)
  Fri: Documentation + cleanup (1 hr)
```

---

## Success Criteria (Go/No-Go for Merge)

Before submitting PR to merge `feature/phase-4-kotlin-migration`:

- [ ] All 3 modules compile: `./gradlew clean build` ✅
- [ ] Coverage ≥80% per module: `./gradlew jacocoTestReport` ✅
- [ ] API contract identical: Insomnia tests match Phase 2 responses ✅
- [ ] No Java files in `/src/main` (all .kt) ✅
- [ ] No Lombok, Mockito, Reactor in codebase ✅
- [ ] README updated with Phase 4 comparison ✅

---

## Execution Path

### 1. Pre-Implementation
- [ ] Read: SPEC.md, APPROACH.md, CONSTITUTION.md
- [ ] Create branch: `git checkout -b feature/phase-4-kotlin-migration`
- [ ] Review gradle configs (IMPLEMENTATION_PLAN.md Part 1)

### 2. Implementation (use MIGRATION_CHECKLIST.md + IMPLEMENTATION_PLAN.md)
- [ ] Phase 1: Gradle configuration
- [ ] Phase 2: banking-commons migration
- [ ] Phase 3: auth-service migration
- [ ] Phase 4: core-service migration (async rewrite)
- [ ] Phase 5: Testing + validation
- [ ] Phase 6: Documentation + cleanup

### 3. Code Review & Merge
- [ ] All checklist items marked `[x]`
- [ ] Open PR with reference to .specs documents
- [ ] Address feedback
- [ ] Merge to feature branch (not main, unless approved)

---

## FAQ

**Q: What if I find a blocker in Phase 4?**  
A: Document in `EXCEPTIONS.md` (create if needed). Requires tech lead approval. See CONSTITUTION.md VI for process.

**Q: Can I deviate from CONSTITUTION.md rules?**  
A: No, unless written approval from tech lead. Rules exist for reason (JaCoCo coverage is safety net, etc.).

**Q: What if tests fail after migration?**  
A: Start with MIGRATION_CHECKLIST.md troubleshooting section. Common issues: missing imports, Spring context setup, coroutine dispatcher. IMPLEMENTATION_PLAN.md Part 5 has detailed test patterns.

**Q: How much time should Phase 4 async rewrite take?**  
A: ~5 hours. Start with IMPLEMENTATION_PLAN.md Part 4 code examples. Core pattern: `coroutineScope { async { } }` replaces `Mono.zip()`.

**Q: Can I use both Reactor and coroutines?**  
A: No. CONSTITUTION.md Rule Q1 forbids mixing. Phase 4 is **coroutines only**. (R2DBC has built-in suspend support.)

---

## Escalation Contact

**Tech Lead**: [Name]  
**Approval for Exceptions**: Required before deviating from CONSTITUTION.md  
**Slack Channel**: #phase-4-migration

---

## Version History

| Version | Date | Changes |
|---------|------|---------|
| 1.0 | May 23, 2026 | Initial specification suite |

---

**Last Updated**: May 23, 2026  
**Status**: 🟢 Ready for Implementation  
**Next Steps**: Read SPEC.md → APPROACH.md → CONSTITUTION.md → Begin Phase 1

