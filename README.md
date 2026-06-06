# Fee Engine AI Assistant Service

AI-powered fee rule assistant for the `pisp` fee-engine. Uses Spring AI (Anthropic-compatible API) to generate, review, and manage fee rule drafts through a human-in-the-loop workflow. Administrators describe fee rules in natural language; the AI produces structured JSON, which is validated, dry-run against the fee-engine, and only persisted to production after explicit approval.

Part of the `pisp` platform alongside the [fee-engine](../fee-engine) (downstream rule engine) and [saga-orchestrator](../saga-orchestrator) services.

For detailed sequence and flow diagrams covering rule generation, AI chat, dry-run, approval, draft lifecycle, retention, security, and error handling, see [docs/flow-diagrams.md](docs/flow-diagrams.md).

## Endpoints

### REST API — `/ai/drafts` (JWT bearer required)

| Method | Path | Required scope | Description |
|--------|------|---------------|-------------|
| `POST` | `/ai/drafts/generate` | `ai-rules:write` | Generate or update a fee rule from natural language |
| `POST` | `/ai/drafts/review` | `ai-rules:read` | AI review of a fee rule JSON |
| `GET` | `/ai/drafts` | `ai-rules:read` | List drafts (paginated, filterable by status/creator) |
| `GET` | `/ai/drafts/{id}` | `ai-rules:read` | Get a single draft |
| `PUT` | `/ai/drafts/{id}` | `ai-rules:write` | Update a draft's rule JSON |
| `DELETE` | `/ai/drafts/{id}` | `ai-rules:write` | Delete a draft |
| `POST` | `/ai/drafts/{id}/dry-run` | `ai-rules:write` | Execute dry-run against fee-engine |
| `POST` | `/ai/drafts/{id}/approve` | `ai-rules:write` | Approve and push rule to fee-engine |
| `POST` | `/ai/drafts/{id}/reject` | `ai-rules:write` | Reject a draft |

### Actuator (unauthenticated)

| Path | Description |
|------|-------------|
| `/actuator/health` | Health check with datasource status |
| `/actuator/info` | Application info |
| `/actuator/prometheus` | Micrometer Prometheus metrics |

## Architecture

Hexagonal (ports + adapters) with `AiDraft` as the aggregate root.

```
domain/
  model/               AiDraft, DraftStatus, DraftType (pure Java records/enums, no Spring)
application/
  exception/           AiDisabledException, AiQuotaExceededException, AiOutputParseException,
                       AiInputTooLargeException, DraftNotFoundException, TargetRuleNotFoundException,
                       InvalidDraftRequestException, InvalidDraftStatusException, FeeEnginePermissionDeniedException
  model/               FeeRuleSchema — structural validation model for fee-engine rule payloads
  port/in/             GenerateRuleUseCase, ReviewRuleUseCase, RunDryRunUseCase,
                       ApproveDraftUseCase, ManageDraftsUseCase
  port/out/            AiChatPort, DraftRepository, FeeEnginePort
                       GenerationResult, DryRunResult, FeeRuleResult (output records)
  service/             GenerateRuleService, ReviewRuleService, RunDryRunService,
                       ApproveDraftService, ManageDraftsService
adapter/
  in/rest/             DraftController — all /ai/drafts endpoints
                       dto/ — GenerateRequest, ReviewRequest, UpdateDraftRequest, DraftResponse, ReviewResponse
                       DraftDtoMapper, GlobalExceptionHandler (RFC 7807 ProblemDetail)
  out/ai/              SpringAiChatAdapter — Spring AI ChatModel → Anthropic-compatible API
  out/persistence/     DraftRepositoryAdapter, AiDraftMapper
                       jpa/ — AiDraftEntity, AiDraftJpaRepository
  out/rest/            FeeEngineRestAdapter — RestClient → fee-engine /admin/fee-rules
infrastructure/
  ai/                  SystemPromptLoader — loads system-prompt.txt with schema version placeholder
  budget/              AiChatBudget — daily token usage tracking
  config/              SpringAiConfig, AiChatProperties, AiSchemaProperties,
                       FeeEngineClientConfig, JpaAuditingConfig, RetentionConfig, RetentionProperties
  json/                JsonCanonicaliser — alphabetical key sort for deterministic JSON storage
  security/            SecurityConfig — OAuth2 resource server with scope-based authorisation
                       AuditorAwareImpl — JWT sub → createdBy/updatedBy
```

## Draft Lifecycle

### State Machine

```
PENDING ──► DRY_RUN_PASSED ──► APPROVED (terminal)
  │              │
  │              └────────────► REJECTED (terminal)
  └──► DRY_RUN_FAILED ──► PENDING (re-edit cycle)
         │
         └──────────────────► REJECTED (terminal)
```

- `GENERATE` drafts create a new fee rule; `UPDATE` drafts modify an existing rule (requires `targetRuleId`, fetches current rule from fee-engine as AI context)
- Approved drafts are pushed to fee-engine via `POST /admin/fee-rules` or `PUT /admin/fee-rules/{id}` and store the resulting `feeRuleId`
- Terminal states (`APPROVED`, `REJECTED`) are immutable — no further transitions allowed

### Fee-Type Validation

AI-generated rules are validated against `FeeRuleSchema` constraints before persisting:

| feeType | Required | Forbidden |
|---------|----------|-----------|
| `FLAT` | `flatAmount` | `percentage`, `tiers`, `minFee`, `maxFee` |
| `PERCENTAGE` | `percentage` | `flatAmount`, `tiers` |
| `TIERED` | `tiers` (≥1 entry) | `flatAmount`, `percentage`, `minFee`, `maxFee` |
| `FREE` | — | `flatAmount`, `percentage`, `tiers`, `minFee`, `maxFee` |

## Security

Spring Security with a single OAuth2 resource server filter chain:

| Matcher | Auth | Required scope |
|---------|------|---------------|
| `/actuator/**` | None | — |
| `POST /ai/drafts/review` | JWT bearer | `SCOPE_ai-rules:read` |
| `GET /ai/**` | JWT bearer | `SCOPE_ai-rules:read` |
| All other `/ai/**` | JWT bearer | `SCOPE_ai-rules:write` |
| Any other path | — | Denied |

Unauthenticated or forbidden requests receive `application/problem+json` responses with status 401 or 403.

## Tech Stack

- **Java 21** with virtual threads
- **Spring Boot 3.5.14** (Web, Data JPA, Actuator, Validation, OAuth2 Resource Server)
- **Spring AI 1.0.0** — Anthropic chat model integration
- **PostgreSQL 16** — default schema, Flyway migrations
- **Testcontainers 2.0.5** — PostgreSQL integration tests
- **WireMock 3.9.1** — HTTP client testing
- **Micrometer + Prometheus** — AI chat metrics and application monitoring

## Prerequisites

- Java 21+
- Maven 3.9+
- PostgreSQL 16 (or run via Docker/Testcontainers for tests)
- Docker (for Testcontainers during `mvn test`)
- Running [fee-engine](../fee-engine) service (for dry-runs and rule approval)
- Anthropic-compatible AI API endpoint (e.g. Anthropic API or compatible proxy)

## Build & Run

```bash
# Compile
mvn compile

# Run tests (Testcontainers spins up PostgreSQL automatically; smoke tests excluded)
mvn test

# Run a single test class
mvn test -Dtest=GenerateRuleServiceTest

# Run all tests including smoke tests
mvn verify

# Run the service (requires PostgreSQL, OIDC provider, AI API, fee-engine)
mvn spring-boot:run
```

> **Note:** Maven Surefire is configured to exclude tests tagged `@Tag("smoke")` by default. Smoke tests (e.g. `SpringAiProxySmokeTests`) are included only during `mvn verify`.

## Configuration

### Environment Variables

| Variable | Default | Description |
|----------|---------|-------------|
| `DB_URL` | `jdbc:postgresql://localhost:5432/fee_ai_assistant` | PostgreSQL JDBC URL |
| `DB_USERNAME` | `fee_ai` | Database user |
| `DB_PASSWORD` | `fee_ai` | Database password |
| `OIDC_ISSUER_URI` | `https://auth.example.com` | OAuth2 issuer URI for JWT validation |
| `ANTHROPIC_BASE_URL` | `https://api.z.ai/api/anthropic` | Anthropic-compatible API base URL |
| `ANTHROPIC_AUTH_TOKEN` | `dummy` | API key for the AI model |
| `AI_MODEL` | `glm-5.1` | Model identifier |
| `AI_CHAT_ENABLED` | `true` | Toggle AI features (returns 503 when disabled) |
| `AI_TIMEOUT_SECONDS` | `30` | AI chat timeout (1–300s) |
| `AI_MAX_INPUT_CHARS` | `20000` | Max combined system + user prompt size (1–50,000) |
| `AI_DAILY_TOKEN_LIMIT` | `0` | Daily token budget (0 = unlimited) |
| `FEE_ENGINE_BASE_URL` | `http://localhost:8080` | Downstream fee-engine base URL |
| `FEE_ENGINE_TIMEOUT_SECONDS` | `10` | Fee-engine HTTP client timeout |
| `FEE_ENGINE_SCHEMA_VERSION` | `V7` | Fee-engine schema version injected into system prompt |
| `PROMPT_REDACT_DAYS` | `30` | Days before redacting prompts on terminal drafts |
| `DRAFT_PURGE_DAYS` | `90` | Days before deleting terminal drafts |

See [`.env.example`](.env.example) for a copy-paste-ready template.

### Key Spring Properties

```yaml
spring:
  datasource:
    url: ${DB_URL:jdbc:postgresql://localhost:5432/fee_ai_assistant}
    username: ${DB_USERNAME:fee_ai}
    password: ${DB_PASSWORD:fee_ai}
  jpa:
    hibernate:
      ddl-auto: validate            # Schema managed by Flyway only
    open-in-view: false
  flyway:
    locations: classpath:db/migration
  security:
    oauth2:
      resourceserver:
        jwt:
          issuer-uri: ${OIDC_ISSUER_URI:https://auth.example.com}
  ai:
    anthropic:
      base-url: ${ANTHROPIC_BASE_URL:https://api.z.ai/api/anthropic}
      api-key: ${ANTHROPIC_AUTH_TOKEN:dummy}
      chat:
        options:
          model: ${AI_MODEL:glm-5.1}
          max-tokens: 4096
          temperature: 0.2

ai-assistant:
  chat:
    enabled: ${AI_CHAT_ENABLED:true}
    timeout-seconds: ${AI_TIMEOUT_SECONDS:30}
    max-input-chars: ${AI_MAX_INPUT_CHARS:20000}
    daily-token-limit: ${AI_DAILY_TOKEN_LIMIT:0}
  retention:
    prompt-redact-days: ${PROMPT_REDACT_DAYS:30}
    purge-days: ${DRAFT_PURGE_DAYS:90}
  schema:
    fee-engine-schema-version: ${FEE_ENGINE_SCHEMA_VERSION:V7}

fee-engine:
  base-url: ${FEE_ENGINE_BASE_URL:http://localhost:8080}
  timeout-seconds: ${FEE_ENGINE_TIMEOUT_SECONDS:10}

server:
  port: 8081

management:
  endpoints:
    web:
      exposure:
        include: health,info,prometheus
```

## Database Schema

Flyway manages the schema. Single table:

- **`ai_drafts`** — AI-generated fee rule drafts: typed columns (`type`, `status`, `created_by`, `updated_by`) + JSONB (`rule_json`, `dry_run_result`). Nullable `target_rule_id` for UPDATE drafts and `fee_rule_id` linking to the persisted rule after approval.

### Check Constraints

| Constraint | Rule |
|---|---|
| `chk_prompt_length` | `char_length(prompt) <= 2000` |
| `chk_rule_json_size` | `length(rule_json::text) <= 50000` |

### Indexes

- **`idx_ai_drafts_status`** — Index on `status` for filtered list queries
- **`idx_ai_drafts_created_at`** — Index on `created_at DESC` for chronological listing
- **`idx_ai_drafts_created_by`** — Index on `created_by` for user-specific queries

### Migrations

| Version | Description |
|---------|-------------|
| V1 | Create `ai_drafts` table with check constraints and indexes |

### Retention

`RetentionConfig` runs a scheduled job at 02:00 daily:

1. **Redact prompts** older than `prompt-redact-days` (default 30) on terminal drafts — uses bulk JPQL directly against the JPA repository to avoid `@PreUpdate` bumping `updated_at`
2. **Purge terminal drafts** older than `purge-days` (default 90) — deletes APPROVED/REJECTED drafts past retention

## Testing

| Layer | Scope | Tooling |
|-------|-------|---------|
| Domain unit | AiDraft, DraftStatus state machine | JUnit 5 + AssertJ (zero Spring) |
| Application unit | Generate/Review/DryRun/Approve/Manage services with mocked ports | Mockito |
| Adapter unit — REST controller | DraftController — validation, auth, error handling | `@WebMvcTest` + MockMvc |
| Adapter unit — AI | SpringAiChatAdapter — prompt building, output parsing, metrics | Mock ChatModel |
| Adapter unit — persistence | DraftRepositoryAdapter, AiDraftJpaRepository — JPA mapping, queries | `@DataJpaTest` + Testcontainers |
| Adapter unit — HTTP client | FeeEngineRestAdapter — dry-run, create, update, fetch | WireMock |
| Infrastructure unit | SystemPromptLoader, AiChatBudget, JsonCanonicaliser, properties validation | JUnit 5 |
| Context load | Full Spring context startup | `@SpringBootTest` + Testcontainers |
| Smoke tests | End-to-end AI proxy validation (requires real API) | `@Tag("smoke")`, excluded from default `mvn test` |

Test infrastructure:
- `PostgresTestSupport` — abstract base with shared PostgreSQL 16 Testcontainer
- `TestSecurityConfig` — no-op `JwtDecoder` for test contexts

## Error Responses

All errors return RFC 7807 `ProblemDetail` (`application/problem+json`).

| Scenario | HTTP | Detail |
|----------|------|--------|
| Missing/invalid request field | 400 | `Request validation failed` |
| Draft not found | 404 | `Draft {id} not found` |
| Target rule not found (fee-engine) | 404 | `Target rule {id} no longer exists` |
| Invalid draft status transition | 409 | Status-specific message |
| Concurrent update (stale version) | 409 | `Draft was modified concurrently; reload and retry` |
| Invalid draft request parameter | 422 | Parameter-specific message |
| AI output parse failure | 422 | Parse error with preview |
| AI input too large | 422 | Combined prompt size exceeded |
| JSON canonicalisation failure | 422 | `Invalid JSON: ...` |
| Fee-engine rejected the request | 422 | `Fee-engine rejected the request` + `feeEngineProblem` detail |
| Fee-engine permission denied | 403 | Permission denied message |
| AI assistant disabled | 503 | `AI assistant is currently disabled` |
| Daily AI token quota exceeded | 429 | `Daily AI token quota exceeded` |
| Missing or invalid JWT | 401 | `Unauthorized` |
| Insufficient scope | 403 | `Access Denied` |

## Observability

### Metrics (Prometheus)

| Metric | Type | Tags | Description |
|--------|------|------|-------------|
| `ai.assistant.chat.duration` | Timer | `operation`, `outcome` | AI chat call latency |
| `ai.assistant.chat.tokens.input` | DistributionSummary | `operation` | Input token count (from API) |
| `ai.assistant.chat.tokens.output` | DistributionSummary | `operation` | Output token count (from API) |
| `ai.assistant.chat.tokens.input.estimated` | DistributionSummary | `operation` | Estimated input tokens (chars/4) |
| `ai.assistant.chat.tokens.output.estimated` | DistributionSummary | `operation` | Estimated output tokens (chars/4) |
| `ai.assistant.chat.errors` | Counter | `operation`, `error_type` | Error count by type (disabled, quota_exceeded, parse_error, etc.) |

## Related

- [fee-engine](../fee-engine) — Fee calculation engine (downstream service for dry-runs and rule persistence)
- [saga-orchestrator](../saga-orchestrator) — Choreographed saga orchestration for payment workflows
- [domestic-payments](../domestic-payments) — Domestic payment service that consumes fee calculations
