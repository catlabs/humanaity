# Sprint 6 Prompt Pack (Copy-Paste Templates)

Use one prompt per sub-chunk. Do not ask for "implement Sprint 6" in a single pass.

Before using a prompt, check `docs/sprints/sprint06/sprint-06-platform-hardening.md` and keep its `## Execution status` section current so the active chunk, next chunk, and blocked items stay visible in one place.

Each template includes:

- exact task ID
- one-sentence goal
- acceptance criteria copied from `docs/sprints/sprint06/sprint-06-platform-hardening.md`
- explicit in-scope files
- explicit out-of-scope items
- whether the tool should modify code or only propose a patch

## Codex input contract and Cursor re-integration

Use this contract for every Codex-targeted prompt:

- include repo-visible references in the prompt body:
  - `docs/sprints/sprint06/sprint-06-platform-hardening.md` for scope and acceptance criteria
  - `docs/roadmap.md` for epic alignment
  - relevant `.cursor/rules/*.mdc` files when they contain mandatory policy for the chunk
- do not assume hidden Cursor skills are visible to Codex; restate mandatory constraints directly
- include a hard boundary line: "Implement only this task ID; do not expand to other sprint tasks"

Re-integrate Codex output before opening the next chunk:

- Cursor reviews ownership/auth semantics and migration/config decisions against the sprint definition of done
- Cursor runs chunk-level validation for touched backend/UI/MCP paths
- if runtime config or generated-client assumptions changed, Cursor confirms consumers still point at the intended backend
- Cursor updates sprint docs if implementation changes sprint-shaping decisions

## Per-chunk review/test/handoff checklist

Use `.cursor/rules/docs-chunk-review-loop.mdc` as the standard checklist and go/no-go gate after every chunk implementation.

For Sprint 6, also verify:

- ownership checks are enforced on backend mutation boundaries rather than UI-only
- config cleanup removes brittle runtime assumptions instead of moving them elsewhere
- migration setup is committed and repeatable
- deferred OIDC work is explicitly documented rather than implied

---

## Task 1a - Hardening Baseline and Acceptance Lock

```text
Task ID: Task 1a
Owner: You
Mode: modify code (docs only)

Goal (one sentence):
Lock Sprint 6 into a concrete, repo-specific hardening baseline so implementation stays focused on the highest-risk platform gaps.

Acceptance criteria (copied from sprint doc):
- a contributor can start Sprint 6 implementation without guessing which hardening items matter now
- the sprint doc clearly separates immediate code work from deferred platform direction

In-scope files:
- docs/sprints/sprint06/sprint-06-platform-hardening.md
- docs/roadmap.md (read-only reference)
- .cursor/rules/docs-sprint-planning.mdc (read-only reference)

Out of scope:
- backend implementation
- frontend changes
- migration tooling changes
- OIDC integration code

Instructions:
Implement only Task 1a. Tighten the sprint wording, acceptance criteria, and non-goals if needed so Sprint 6 is small, actionable, and roadmap-aligned.
```

## Task 2a - City Ownership Enforcement and Authorization Tests

```text
Task ID: Task 2a
Owner: Codex
Mode: modify code

Goal (one sentence):
Enforce owner-only city update/delete behavior in the backend and add focused tests that catch cross-user mutation regressions.

Acceptance criteria (copied from sprint doc):
- non-owners cannot update or delete another user's city
- ownership behavior is covered by automated tests close to the controller/application boundary

In-scope files:
- apps/backend/src/main/java/eu/catlabs/humanaity/city/
- apps/backend/src/main/java/eu/catlabs/humanaity/auth/
- apps/backend/src/test/java/eu/catlabs/humanaity/
- docs/sprints/sprint06/sprint-06-platform-hardening.md (read-only reference)
- .cursor/rules/backend-best-practices.mdc (read-only reference)

Out of scope:
- full auth-stack redesign
- OIDC provider integration
- frontend permission changes
- migration tooling

Instructions:
Implement only Task 2a. Enforce authenticated ownership for city update/delete at the backend mutation boundary and add focused automated tests for owner and non-owner cases.
```

## Task 2b - Migration Tooling and Backend Config Cleanup

```text
Task ID: Task 2b
Owner: Codex
Mode: modify code

Goal (one sentence):
Introduce committed migration tooling and profile-aware backend configuration so schema evolution and environment handling no longer depend on local-only defaults.

Acceptance criteria (copied from sprint doc):
- backend schema changes can evolve through committed migrations
- backend config clearly distinguishes local defaults from future deployment-ready settings

In-scope files:
- apps/backend/src/main/resources/
- apps/backend/pom.xml
- apps/backend/src/test/java/eu/catlabs/humanaity/ (only if touched for config validation)
- docs/sprints/sprint06/sprint-06-platform-hardening.md (read-only reference)

Out of scope:
- full PostgreSQL deployment
- unrelated backend refactors
- frontend or MCP runtime cleanup
- OIDC integration

Instructions:
Implement only Task 2b. Add migration tooling, capture a safe baseline migration strategy, and clean backend config so local defaults and future deployment-oriented settings are explicit.
```

## Task 3a - UI API Base-Path Cleanup and Touched Test Stabilization

```text
Task ID: Task 3a
Owner: Cursor
Mode: modify code

Goal (one sentence):
Remove hardcoded backend URL assumptions from the active UI path and keep touched city/simulation tests working after the config cleanup.

Acceptance criteria (copied from sprint doc):
- the main UI path can target a configured backend URL without source edits
- touched client tests or smoke paths still pass after the configuration cleanup

In-scope files:
- apps/ui/src/app/app.config.ts
- apps/ui/src/app/features/city/city.service.ts
- apps/ui/src/app/features/city/pages/list/
- apps/ui/src/app/features/city/pages/simulation-detail/
- apps/ui/src/app/api/ (usage/config only)
- apps/ui/src/environments/ if introduced

Out of scope:
- generated API redesign
- broad page redesign
- backend ownership logic
- full test-suite cleanup

Instructions:
Implement only Task 3a. Remove hardcoded runtime API base paths from the real UI path, keep generated-client usage aligned, and repair any touched city/simulation UI tests.
```

## Task 3b - MCP and Client Environment Alignment

```text
Task ID: Task 3b
Owner: Cursor
Mode: modify code

Goal (one sentence):
Align MCP and client generation/runtime assumptions with the hardened backend/UI configuration so local defaults stay explicit and non-brittle.

Acceptance criteria (copied from sprint doc):
- the main UI path can target a configured backend URL without source edits
- touched client tests or smoke paths still pass after the configuration cleanup

In-scope files:
- apps/mcp/src/config.ts
- apps/mcp/src/backend-client.ts
- apps/mcp/README.md
- apps/ui/package.json
- apps/mcp/package.json
- docs/sprints/sprint06/sprint-06-platform-hardening.md (read-only reference)

Out of scope:
- new MCP feature tools
- backend auth redesign
- broad docs rewrite outside touched setup paths

Instructions:
Implement only Task 3b. Keep client base-URL and contract-generation assumptions explicit and aligned with the Sprint 6 config hardening direction.
```

## Task 4a - OIDC Readiness Note

```text
Task ID: Task 4a
Owner: You or Codex
Mode: modify code (docs only unless narrowly needed)

Goal (one sentence):
Document the intended OIDC/Keycloak-ready client, token, and role model for UI, backend, and MCP without turning Sprint 6 into a full auth rewrite.

Acceptance criteria (copied from sprint doc):
- the repo has a concrete OIDC readiness note tied to current app boundaries
- deferred auth modernization work is explicit instead of implied

In-scope files:
- docs/sprints/sprint06/sprint-06-platform-hardening.md
- docs/roadmap.md (read-only reference)
- relevant auth/backend docs if a small companion note is needed

Out of scope:
- actual Keycloak setup
- backend resource-server migration
- UI login flow replacement
- MCP delegated-auth implementation

Instructions:
Implement only Task 4a. Document client boundaries, token-validation direction, likely roles/scopes, and explicit deferred work for later OIDC integration.
```

## Task 4b - Validation and Sprint Closeout

```text
Task ID: Task 4b
Owner: Codex
Mode: modify code (tests/docs/status sync allowed)

Goal (one sentence):
Run targeted validation for the hardened slices, record residual risks, and close Sprint 6 with an accurate execution-status update.

Acceptance criteria (copied from sprint doc):
- the repo has a concrete OIDC readiness note tied to current app boundaries
- deferred auth modernization work is explicit instead of implied

In-scope files:
- docs/sprints/sprint06/sprint-06-platform-hardening.md
- touched test files from Tasks 2a, 2b, 3a, and 3b
- nearby docs only if validation changes sprint-shaping decisions

Out of scope:
- new feature implementation beyond fixing validation blockers
- broad cleanup unrelated to Sprint 6 deliverables

Instructions:
Implement only Task 4b. Run the nearest targeted validation for touched hardening paths, sync the sprint execution status block, and record deferred follow-on work and residual risks.
```
