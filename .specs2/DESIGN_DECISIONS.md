# Architecture & Design Decisions

This document captures the rationale behind key technical choices made during the fresh Kotlin implementation of CoreBank Microservices.

## 1. R2DBC vs JPA
**Decision:** We are using **R2DBC** exclusively, with no JPA dependencies.
**Rationale:**
- JPA is inherently blocking. While tools exist to wrap JPA calls in async blocks, they still consume threads from a finite pool when executing queries.
- R2DBC provides true non-blocking database interaction from top to bottom.
- Aligning R2DBC with Kotlin Coroutines provides optimal resource utilization for I/O bound microservices.

## 2. Coroutines vs Reactor (Mono/Flux)
**Decision:** We are using **Kotlin Coroutines** as our primary async mechanism instead of Project Reactor's Mono/Flux.
**Rationale:**
- Coroutines allow us to write sequential, imperative-style code while maintaining asynchronous execution.
- Reactor APIs often lead to complex, nested lambda structures and cognitive overhead when handling complex business logic.
- Spring Boot provides native integration for Coroutines in WebFlux and R2DBC, translating `suspend` functions and `Flow` to `Mono` and `Flux` under the hood where needed.

## 3. Hexagonal Architecture Implementation
**Decision:** We are strictly adhering to Hexagonal Architecture (Ports and Adapters) across all microservices, especially `core-service`.
**Rationale:**
- Decouples core business logic (Domain) from infrastructure concerns (DB, HTTP).
- Repositories are defined as Ports in the Application layer, while the actual Spring Data R2DBC interfaces and adapters reside in the Infrastructure layer.
- Enables much easier testing via mocking of interfaces (`mockk`) without needing to spin up database contexts.

## 4. Kotest and MockK vs JUnit and Mockito
**Decision:** We are using **Kotest** and **MockK** for all testing needs.
**Rationale:**
- Kotest provides expressive DSLs (like `FunSpec` and `StringSpec`) that fit Kotlin perfectly.
- MockK natively understands Kotlin's `suspend` functions, object declarations, and final classes (which are default in Kotlin), making mocking significantly easier compared to Mockito.

## 5. Mono-repo Multi-module Build
**Decision:** The project is structured as a multi-module Gradle project rather than separate repositories.
**Rationale:**
- Eases dependency management and shared code (`banking-commons`).
- Allows synchronized builds and easier refactoring across boundaries during the initial development phase.
