# Sledger Backend

[![codecov](https://codecov.io/gh/yong-space/sledger-backend/branch/main/graph/badge.svg?token=494TUr5TBt)](https://codecov.io/gh/yong-space/sledger-backend)

Sledger is a personal finance ledger for tracking bank accounts, credit cards, CPF (retirement) balances, and investment portfolios. This repository is the REST API backend; the React frontend lives in [sledger-web](https://github.com/yong-space/sledger-web).

## What it does

- **Accounts** — cash, credit card (with billing cycles), and CPF retirement accounts, grouped under bank issuers
- **Transactions** — full CRUD with automatic running-balance recalculation, multi-currency support, and same-day ordering
- **Statement imports** — parses OCBC / Citi / Grab CSV and UOB XLS exports, with template-based auto-categorisation of imported transactions
- **Insights** — dashboard aggregations: category spending insights, monthly balance history, credit card bill summaries
- **Suggestions** — autocomplete for remarks, categories, and merchant codes based on transaction history
- **Portfolio** — snapshots of trading positions pulled from a companion portfolio service, with HTML email summaries
- **Users** — registration with email activation (via Resend), JWT authentication, profile management

## Tech stack

| | |
|---|---|
| Language / Runtime | Java 25, compiled to a GraalVM native image for deployment |
| Framework | Spring Boot 4 (Web MVC, Security, Data MongoDB, Cache, Validation, Actuator) |
| Database | MongoDB |
| Auth | Stateless JWT (HMAC512, 7-day expiry), Argon2id password hashing |
| Email | Resend API with Handlebars templates |
| Parsing | OpenCSV (CSV statements), Apache POI (XLS statements) |
| Build | Gradle, Lombok |

## Getting started

Requires JDK 25 and a MongoDB instance.

```bash
# Run tests (spins up MongoDB via TestContainers — Docker required)
./gradlew test

# Run locally with the dev profile
./gradlew bootRun --args='--spring.profiles.active=dev'

# Build a native Docker image
./gradlew bootBuildImage
```

Required environment variables:

| Variable | Purpose |
|---|---|
| `MONGO_URI` | MongoDB connection string |
| `SLEDGER_SECRET_KEY` | JWT signing key |
| `SLEDGER_RESEND_KEY` | Resend email API key |

Optional: `portfolio-endpoint` property (default `http://portfolio`) points to the external portfolio service backing `/api/portfolio`.

## API overview

All endpoints are under `/api` and require a JWT Bearer token unless noted.

| Area | Endpoints | Notes |
|---|---|---|
| Auth | `POST /register`, `GET /activate/{code}`, `POST /authenticate`, `GET /refresh-token` | Public (except refresh); first registered user becomes admin |
| Profile | `PUT /profile`, `GET /profile/challenge` | |
| Accounts | `GET/POST /account`, `PUT/DELETE /account/{id}`, `PUT /account/{id}/sort/{dir}` | |
| Issuers | `GET /account-issuer` | Managed via `POST/PUT/DELETE /admin/account-issuer` (`ROLE_ADMIN`) |
| Transactions | `POST/PUT /transaction`, `PUT /transaction/bulk`, `DELETE /transaction/{ids}`, `GET /transaction/{accountId}` | |
| Import | `POST /import` | Multipart statement upload |
| Templates | `GET/POST/PUT /template`, `DELETE /template/{id}` | Auto-categorisation rules |
| Suggestions | `GET /suggest/{remarks,code,company,categories}` | |
| Dashboard | `GET /dash/insights`, `GET /dash/balance-history`, `GET /dash/credit-card-bills/{accountId}` | |
| Portfolio | `GET /portfolio`, `GET /portfolio/refresh`, `GET /portfolio/email-snapshot` | Requires `ROLE_TRADING` |

## Architecture

```
endpoints/    REST controllers — thin, delegate to services
service/      Business logic, bank statement importers, email
repo/         Spring Data MongoDB repositories + custom aggregation impls
model/        Entities, DTOs, custom validation annotations
config/       Security, JWT filter, CORS, Jackson, exception handling, AOT hints
```

Notable design points:

- **Polymorphic transactions** — all transaction types (`CashTransaction`, `CreditTransaction`, `CpfTransaction`, and foreign-currency variants) share one MongoDB collection using Jackson type discriminators. IDs are sequential longs assigned by the application, not ObjectIds.
- **Balance recalculation** — `TransactionService.updateBalances()` recomputes running balances forward from the earliest affected date whenever transactions change.
- **Custom aggregations** — `AccountOpsRepoImpl` builds raw `$lookup` aggregation pipelines via `MongoOperations` for account metrics, insights, and suggestions.
- **Caching** — Spring Cache with `tx` (transactions per account) and `authorise` (account-ownership checks) caches, evicted through `CacheService`.
- **Importer dispatch** — `ImportService` selects an `Importer` implementation based on the account's issuer name.
- **Scheduled cleanup** — `CleanupService` runs daily at 04:00 to purge expired, unactivated registrations.

## Testing

Tests are integration tests: `@SpringBootTest` + `MockMvc` against a real MongoDB 8 TestContainer shared across the suite — there are no mocked-repository unit tests. `BaseTest` provisions three users (basic, trading, admin); `UserBaseTest` additionally mocks the email service. Coverage is reported to Codecov via JaCoCo.

```bash
./gradlew test --tests "tech.sledger.AccountTests"   # single class
```

## CI / Deployment

- **Pull requests** (`.github/workflows/pr.yml`) — run tests and upload coverage.
- **Main** (`.github/workflows/release.yml`) — tests, auto-semver git tag, GraalVM native Docker image via `bootBuildImage`, push to a private registry, then a rolling `kubectl set image` update on an on-prem Kubernetes cluster.

GraalVM reflection hints live in `config/AotHints.java` — update it when adding types that need reflection at native runtime.
