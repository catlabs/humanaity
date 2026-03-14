# Sprint 6: Platform Hardening

## Execution status

- Current phase: Sprint 6 ready for implementation
- Active chunk: none
- Next chunk: `Task 3a` - UI API base-path cleanup and touched test stabilization
- Blocked items: none
- Last completed chunk: `Task 2b` - migration tooling and backend config cleanup (2026-03-14)

| Chunk ID | Status | Notes |
| --- | --- | --- |
| Task 1a | done | Sprint boundary locked: this sprint is limited to ownership hardening, migration/config groundwork, client base-path cleanup, and OIDC readiness notes. |
| Task 2a | done | Enforced owner-only city update/delete with explicit `401`/`403`/`404` behavior and added focused API authorization tests. |
| Task 2b | done | Added Flyway baseline migration, split backend config into common/local/dev profiles, and moved default JPA mode to `validate`. |
| Task 3a | planned | Remove hardcoded frontend API base URL usage and stabilize the touched UI tests. |
| Task 3b | planned | Align contract-generation workflow and MCP/client environment handling with the hardened backend/UI setup. |
| Task 4a | planned | Document OIDC/Keycloak readiness decisions, client boundaries, and deferred integration work. |
| Task 4b | planned | Run targeted validation, record residual risks, and close the sprint. |

## Sprint intent

Sprint 6 exists to make HUMANAIty safer to demo, easier to evolve, and less dependent on local-only assumptions.

This sprint is mixed backend, frontend, and docs work, but it is still a single milestone: tighten the platform seams around ownership, configuration, persistence, and contract stability.

It does not try to deliver a new flagship product feature. Its purpose is to reduce avoidable reliability and security debt before the next portfolio-facing expansion.

This sprint is intentionally sequenced as:

1. secure city mutation boundaries first
2. make backend schema/config evolution explicit
3. remove brittle client runtime assumptions
4. document future OIDC direction without implementing it yet

## Why this sprint comes next

Sprint 5 completed the MVP-visible AI enrichment layer, but the platform still shows early-alpha seams in places that matter during demos and future iteration:

- city update/delete paths do not currently appear to enforce ownership in the application flow
- backend persistence still depends on H2 file storage with `spring.jpa.hibernate.ddl-auto=update`
- UI API wiring still includes hardcoded `http://localhost:8080` defaults
- backend test coverage is concentrated in simulation/history slices, with little explicit authorization coverage
- Epic 6 roadmap items around OIDC/OAuth2 are still planning-level only

If Sprint 6 is skipped:

- demos can fail for environment or contract reasons unrelated to the product idea
- ownership bugs can leak across user boundaries
- schema growth from history/enrichment work stays tied to fragile local evolution
- future auth modernization starts without a clear boundary model for UI, backend, and MCP

## Sprint outcome

At the end of Sprint 6, HUMANAIty should have a minimum hardening baseline: user-owned city mutations are protected, persistence/configuration is migration-ready, frontend and MCP clients stop depending on brittle local API assumptions, and the repo has a documented path toward OIDC-compatible auth without pretending that full identity-provider integration is already done.

## Sprint scope

### In scope

- lock a concrete hardening baseline for this sprint using repo-visible gaps
- enforce ownership/authorization on city update and delete flows
- add focused backend tests for ownership and deterministic-critical touched paths
- introduce migration tooling and profile-based database/config groundwork
- remove hardcoded frontend API base URL usage from real application paths
- stabilize the touched UI tests around simulation/city flows if they regress under the config cleanup
- tighten contract-generation and environment handling where backend, UI, and MCP currently assume local defaults
- document OIDC/Keycloak readiness boundaries, roles, and deferred integration steps

### Out of scope

- full Keycloak or OIDC provider integration
- replacing JWT auth with a new auth stack in this sprint
- broad frontend redesign work
- CI/CD pipeline setup for the whole monorepo
- large-scale test-suite rescue outside the touched ownership/config/client slices
- production infrastructure rollout or hosting automation

### Locked non-goals for Task 1a

The following are explicitly not valid reasons to expand Sprint 6:

- "while we are here" cleanup unrelated to ownership, migration/config, or client runtime hardening
- adding new simulation/product features
- redesigning auth flows beyond what is required to protect current mutation boundaries
- changing generated API shapes unless the touched hardening work truly requires it
- introducing broad infra work such as Docker, CI pipelines, hosting, or deployment scripts

## Product and technical decisions for this sprint

### Global decision: hardening should remove real repo risks, not create a speculative platform program

Sprint 6 should target concrete weaknesses already visible in the repo and current roadmap. It is not a placeholder for generic enterprise cleanup.

### Decision 1: ownership protection must be enforced at backend mutation boundaries

City update/delete behavior must verify the authenticated owner before data changes happen.

Do not rely on the UI or MCP client to enforce ownership. Do not treat "hidden button" behavior as security.

### Decision 2: migration readiness beats database replacement

This sprint should introduce migration discipline and profile-aware configuration first.

It does not need to switch the default local developer experience away from H2 immediately, but it must stop relying on unmanaged schema drift as the only path forward.

### Decision 3: local host defaults are acceptable for development, not as hidden product assumptions

The UI and MCP can still have local development defaults, but the active application path should be configurable through explicit environment wiring rather than scattered hardcoded URLs.

### Decision 4: OIDC work is a readiness slice, not a full auth rewrite

Sprint 6 should define client boundaries, token-validation direction, and role/scope mapping for later work.

It should not try to ship a complete external identity-provider integration while ownership, migration, and config basics are still being tightened.

### Decision 5: test additions should follow the highest-risk seams first

The sprint should prioritize tests that catch:

- cross-user ownership regressions
- migration/config boot regressions
- contract drift in touched UI/MCP integration seams

### Decision 6: choose one API-semantics policy for unauthorized city mutation and keep tests aligned with it

Sprint 6 implementation must not leave authorization behavior ambiguous.

The preferred policy for this sprint is:

- `401 Unauthorized` when there is no authenticated user
- `403 Forbidden` when the caller is authenticated but does not own the target city
- `404 Not Found` only when the city truly does not exist

If implementation constraints force a different policy, the sprint doc should be updated in the same chunk that makes that choice.

## Deliverables

By the end of Sprint 6, the repo should contain:

- a sprint-locked hardening baseline and task plan
- backend ownership enforcement for city update/delete flows
- focused authorization tests around the hardened mutation paths
- migration tooling/config committed in the backend with a PostgreSQL-ready direction
- profile-aware or environment-aware config cleanup replacing the most brittle hardcoded assumptions
- UI API configuration cleanup on the real app path
- contract/client environment alignment notes or code changes for UI and MCP where touched
- a short OIDC readiness artifact inside the sprint doc or companion notes
- validation notes proving the hardened slices still work

Task 1a specifically locks these sprint outputs as mandatory:

- one backend chunk for ownership enforcement plus tests
- one backend chunk for migration/config groundwork
- one UI/client chunk for runtime base-path cleanup
- one docs chunk for OIDC readiness boundaries
- one closeout chunk for targeted validation and residual risks

## Definition of done

Sprint 6 is done only if all of the following are true:

- city update and delete operations reject unauthorized access for non-owners
- backend ownership behavior is covered by focused automated tests
- backend schema evolution no longer depends solely on `ddl-auto=update`
- the main UI API path no longer hardcodes `http://localhost:8080`
- touched UI/MCP contract consumers still work with explicit backend base-path configuration
- OIDC readiness decisions are documented clearly enough that Epic 6 auth follow-up can start without rediscovering boundaries
- sprint validation notes capture what was tested and what remains intentionally deferred

Task 1a additionally establishes these execution constraints:

- no Sprint 6 chunk should change more than one primary hardening seam unless it is a validation closeout task
- every implementation chunk must name its deferred follow-on work explicitly in the sprint status notes

## Suggested file targets

These are the most likely files or folders Sprint 6 will touch:

- `apps/backend/src/main/java/eu/catlabs/humanaity/city/`
- `apps/backend/src/main/java/eu/catlabs/humanaity/auth/`
- `apps/backend/src/main/resources/`
- `apps/backend/src/test/java/eu/catlabs/humanaity/`
- `apps/ui/src/app/app.config.ts`
- `apps/ui/src/app/features/city/city.service.ts`
- `apps/ui/src/app/features/city/pages/list/`
- `apps/ui/src/app/features/city/pages/simulation-detail/`
- `apps/ui/src/environments/` or equivalent environment wiring if introduced
- `apps/mcp/src/config.ts`
- `apps/mcp/src/backend-client.ts`
- `apps/ui/package.json`
- `apps/mcp/package.json`
- `docs/sprints/sprint06/`

Likely new code areas:

- `apps/backend/src/main/resources/db/migration/`
- `apps/backend/src/test/java/eu/catlabs/humanaity/city/`
- `docs/sprints/sprint06/sprint-06-platform-hardening.md`
- `docs/sprints/sprint06/sprint-06-prompt-pack.md`

## Features and task breakdown

## Feature 1: Hardening baseline lock

### Goal

Turn Epic 6 into a concrete, repo-specific sprint boundary so implementation does not sprawl into generic cleanup.

### Tasks

1. Confirm which Epic 6 items are immediate hardening work versus later platform evolution.
2. Lock acceptance criteria for ownership, config, migration, and readiness outputs.
3. Record explicit non-goals so the sprint does not grow into a full auth rewrite.

### Acceptance criteria

- a contributor can start Sprint 6 implementation without guessing which hardening items matter now
- the sprint doc clearly separates immediate code work from deferred platform direction

### Task 1a completion notes

Task 1a is complete because the sprint now has:

- a locked execution sequence
- explicit non-goals to prevent scope creep
- a preferred API policy for ownership failures
- mandatory output categories for the remaining chunks

### Best owner

- You

## Feature 2: Ownership and authorization tightening

### Goal

Close the current city mutation gap so only the owning user can update or delete a city.

### Tasks

1. Route city update/delete through authenticated-owner checks.
2. Return clear not-found/forbidden behavior consistent with the chosen API contract.
3. Add focused backend tests for owner and non-owner mutation attempts.

### Acceptance criteria

- non-owners cannot update or delete another user's city
- ownership behavior is covered by automated tests close to the controller/application boundary

### Best owner

- Codex

## Feature 3: Persistence and configuration hardening

### Goal

Replace fragile local-only persistence/config assumptions with a migration-ready, profile-aware baseline.

### Tasks

1. Introduce migration tooling and the first committed migration baseline.
2. Split or clean backend config so local/dev defaults are explicit rather than mixed into one file.
3. Preserve a workable local developer path while preparing for PostgreSQL-backed deployment.

### Acceptance criteria

- backend schema changes can evolve through committed migrations
- backend config clearly distinguishes local defaults from future deployment-ready settings

### Best owner

- Codex

## Feature 4: Client configuration and contract stability

### Goal

Make the UI and MCP consumers less brittle by removing hardcoded runtime assumptions and aligning touched client paths with explicit configuration.

### Tasks

1. Replace hardcoded UI API base-path usage on the active app path.
2. Stabilize touched UI tests affected by API/config cleanup.
3. Review MCP/UI generation and base-URL assumptions so touched client paths stay aligned with backend contracts.

### Acceptance criteria

- the main UI path can target a configured backend URL without source edits
- touched client tests or smoke paths still pass after the configuration cleanup

### Best owner

- Cursor chat

## Feature 5: OIDC readiness and sprint closeout

### Goal

Capture the future auth direction without expanding Sprint 6 into a full identity-provider integration.

### Tasks

1. Define the intended client and token model for `apps/ui`, `apps/backend`, and `apps/mcp`.
2. Note the likely resource-server/JWKS validation direction for the backend.
3. Record what remains deferred to a later auth-focused sprint or sub-epic.
4. Run targeted validation and close the sprint with explicit residual risks.

### Acceptance criteria

- the repo has a concrete OIDC readiness note tied to current app boundaries
- deferred auth modernization work is explicit instead of implied

### Best owner

- You and Codex

## Recommended implementation order

1. Lock Sprint 6 baseline and acceptance criteria.
2. Harden city ownership checks and add focused authorization tests.
3. Introduce migration/config groundwork in the backend.
4. Clean up UI runtime API configuration and touched tests.
5. Align MCP/client environment assumptions on the touched path.
6. Record OIDC readiness decisions and close the sprint with validation notes.

## Dependencies inside the sprint

- Feature 1 should be locked before implementation begins.
- Feature 2 can start immediately after the sprint boundary is confirmed.
- Feature 3 should land before any additional schema-heavy backend work starts.
- Feature 4 depends on the chosen configuration direction from Feature 3.
- Feature 5 depends on understanding the final Sprint 6 auth/config shape so the readiness notes reflect reality.

## Suggested delegation

### Best tasks for you

- lock the sprint baseline and non-goals
- review API semantics for unauthorized vs forbidden vs not-found behavior
- approve OIDC readiness boundaries and deferred work

### Best tasks for Cursor chat

- UI API configuration cleanup
- generated-client wiring updates
- small MCP/environment cleanup
- touched frontend test repair

### Best tasks for Codex

- backend ownership enforcement
- authorization test coverage
- migration/config refactor
- targeted validation and hardening closeout notes

## Ready-to-delegate task list

1. `Task 1a` - Lock Sprint 6 hardening baseline, acceptance criteria, and explicit non-goals in the sprint doc.
2. `Task 2a` - Enforce city ownership on update/delete and add focused backend authorization tests.
3. `Task 2b` - Introduce migration tooling plus profile-aware backend config for local/dev readiness.
4. `Task 3a` - Remove hardcoded UI API base URLs and stabilize touched city/simulation UI tests.
5. `Task 3b` - Align MCP/client base-URL and generation assumptions with the hardened setup.
6. `Task 4a` - Write OIDC readiness notes covering client roles, scopes, and backend token-validation direction.
7. `Task 4b` - Run targeted validation, sync execution status, and record deferred follow-on work.

## Risks

- migration-tool introduction can create churn if the current schema state is not captured carefully
- ownership hardening may reveal other endpoints that also assume trusted callers
- UI config cleanup can expose hidden dependencies on generated-client defaults
- OIDC readiness notes can become vague unless they stay tied to concrete current app boundaries
- Sprint 6 can bloat if "platform hardening" is treated as an excuse for unrelated cleanup

## Handoff to next sprint

If Sprint 6 completes successfully, the next sprint should choose one of two directions explicitly:

- resume product differentiation with Epic 7 MCP and agent workflow improvements on top of a more reliable platform
- start a narrower auth/infrastructure follow-up sprint for real OIDC integration, CI, or deployment hardening

Sprint 6 should not silently roll into a second undefined cleanup sprint.
