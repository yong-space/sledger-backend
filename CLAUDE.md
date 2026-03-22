# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

Sledger is a personal finance/savings ledger backend. It manages bank accounts, transactions, and portfolio snapshots for users. Built with Spring Boot 4.0.4 on Java 25, backed by MongoDB.

## Commands

```bash
# Build
./gradlew build

# Run tests (all)
./gradlew test

# Run a single test class
./gradlew test --tests "tech.sledger.AccountTests"

# Run with dev profile (requires application-dev.yml env vars)
./gradlew bootRun --args='--spring.profiles.active=dev'

# Check for dependency updates
./gradlew dependencyUpdates

# Build native Docker image
./gradlew bootBuildImage
```

## Environment Variables

Required at runtime:
- `MONGO_URI` — MongoDB connection string
- `SLEDGER_SECRET_KEY` — JWT signing key
- `SLEDGER_RESEND_KEY` — Resend email API key

The `src/main/resources/application-dev.yml` and `src/test/resources/application-dev.yml` provide dev/test overrides.

## Architecture

### Layer Structure

```
endpoints/    → REST controllers (@RestController), all under /api/
service/      → Business logic; endpoints delegate here
repo/         → Spring Data MongoDB repositories + custom aggregation impls
model/        → Entities, DTOs, validation annotations
config/       → Security, JWT filter, CORS, Jackson, exception handling
```

### Security

- Stateless JWT auth via `JwtRequestFilter` (Bearer token in Authorization header)
- Public endpoints: `/api/register`, `/api/activate/**`, `/api/authenticate`, `/api/profile/challenge`, `/actuator/**`
- Role-gated: `/api/admin/**` requires `ROLE_ADMIN`; `/api/portfolio/**` requires `ROLE_TRADING`
- First registered user automatically gets `ROLE_ADMIN`
- Account ownership is enforced in `UserService.authorise()` and cached under the `authorise` cache

### Domain Model

**Account hierarchy** (`model/account/`):
- `Account` (base) — has `owner` (User DBRef), `issuer` (AccountIssuer DBRef), `type`, `sortOrder`
- `CashAccount` — adds `multiCurrency`
- `CreditAccount` — adds `billingCycle`, `billingMonthOffset`, `multiCurrency`
- `CPFAccount` — Retirement account with `ordinaryRatio`, `specialRatio`, `medisaveRatio`

**Transaction hierarchy** (`model/tx/`), stored in a single `transaction` collection with `@JsonTypeInfo` polymorphism:
- `Transaction` (base) — `id`, `accountId`, `date`, `amount`, `balance`
- `CashTransaction` → `ForeignCashTransaction` (adds fx fields)
- `CreditTransaction` → `ForeignCreditTransaction` (adds fx fields)
- `CpfTransaction` (type name: `retirement`)

IDs are manually assigned sequential longs (not MongoDB ObjectIds) for both accounts and transactions.

### Key Patterns

**Balance recalculation**: When transactions are added/edited/deleted, `TransactionService.updateBalances()` recomputes running balances for all affected transactions from the earliest changed date forward.

**Date sequencing**: `TransactionService.processDates()` ensures transactions on the same calendar day get distinct timestamps (incremented by 1 second) to maintain ordering.

**Custom MongoDB aggregations**: `AccountOpsRepoImpl` uses `MongoOperations` with raw `$lookup` stage strings for complex aggregations (account metrics with latest balance, category insights, suggestions). The pattern is interface (`AccountOpsRepo`) + impl (`AccountOpsRepoImpl`) — Spring Data MongoDB auto-wires the impl.

**Caching**: Spring Cache with two caches: `tx` (keyed by accountId) and `authorise` (keyed by accountId). Evicted via `CacheService`.

**Bank importers**: `ImportService` dispatches to `OcbcImporter`, `UobImporter`, or `GrabImporter` based on `account.getIssuer().getName()`. Templates (via `TemplateService`) are applied to auto-categorise imported transactions.

### Email

Handlebars templates in `src/main/resources/email/`. Sent via Resend API (`ResendService`). `EmailService` wraps async sends.

## Deployment

The app is deployed as a GraalVM native image. The CI pipeline (`.github/workflows/release.yml`) builds a native Docker image via `./gradlew bootBuildImage` using GraalVM 25, then performs a rolling update to a Kubernetes deployment (`kubectl set image`). AOT hints are registered in `config/AotHints.java` — update this when adding types that need reflection at native runtime.

## Testing

Tests use `@SpringBootTest` + `MockMvc` + TestContainers (MongoDB 8 container spun up statically). `BaseTest` sets up three test users (basic, trading-role, admin-role) and wires all repos/services. `UserBaseTest` extends `BaseTest` and mocks `EmailService`. All test classes extend `BaseTest` or `UserBaseTest`.

Tests are integration tests — no unit tests with mocked repos. The MongoDB container is shared across all test classes in a JVM session.
