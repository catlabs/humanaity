---
name: commit-message
description: Generate Conventional Commit messages for the Humanaity backend by analyzing git changes. Use when committing in humanaity-be, reviewing staged changes, or when the user asks for a commit message for modules, DTOs, services, repositories, security, or configuration.
---

# Backend Commit Message Generator

## Goal

Generate concise Conventional Commit messages for changes in `humanaity-be`.
When the user asks to commit changes, group the diff into coherent work subjects and create the commits in the command line instead of only proposing messages.
Prefer staged changes because they reflect what will actually be committed.

## Mandatory Guidelines (Source of Truth)

For the full reference on format, rules, scopes, examples, and best practices, see
[`COMMIT_BEST_PRACTICES.md`](../../../docs/best-practices/COMMIT_BEST_PRACTICES.md).

The rules below are the strict subset to enforce every time:

## Format

```text
<type>(<scope>): <subject>

[optional body]

[optional footer]
```

## Rules

- Never mention `backend` or `backend features`
- Keep the subject line under 72 characters
- Use imperative mood: `Add`, `Fix`, `Refactor`, `Improve`, `Update`
- Capitalize the first letter of the subject
- Do not end the subject with a period
- Prefer a single strong subject line; add a body only when the reason is not obvious
- Do not list changed files or implementation steps in the body
- Focus the message on the behavior or domain change, not the mechanical edit

## Recommended Types

Use the most accurate type:

- `feat` for new endpoints, module capabilities, or domain behavior
- `fix` for bug fixes or incorrect API, service, or persistence behavior
- `refactor` for structural changes without behavior changes
- `perf` for meaningful performance improvements
- `test` for test-only changes
- `docs` for documentation changes
- `chore` for maintenance that does not fit better elsewhere
- `style` for formatting-only changes
- `build` for Maven, dependency, or build pipeline changes
- `ci` for CI/CD configuration changes
- `security` for auth, token, permission, or security hardening work
- `config` for runtime or environment configuration changes

## Scope Guidance

Prefer a scope when one area clearly dominates the change.

### Domain or module scopes

Use the module name from the package structure:

- `eu.catlabs.humanaity.auth` -> `auth`
- `eu.catlabs.humanaity.city` -> `city`
- `eu.catlabs.humanaity.human` -> `human`
- `eu.catlabs.humanaity.simulation` -> `simulation`
- `eu.catlabs.humanaity.ai` -> `ai`

For new modules, use the new module name as the scope.

### Technical scopes

Use a technical scope when the change spans several modules in one layer:

- `api`
- `service`
- `entity`
- `dto`
- `repository`
- `config`
- `security`

### No scope

Omit the scope when the commit is truly project-wide or spans unrelated areas.

## Procedure

### 1. Detect changes

Always inspect the repo state first:

```bash
git status --porcelain
```

### 1.5. Decide whether work must be split

- Review the full set of changes and identify distinct work subjects
- If the changes cover more than one subject, create multiple commits
- Keep each commit focused on one logical behavior or documentation/config change
- Do not mix unrelated README, config, and feature work in the same commit unless they are inseparable

### 2. Prefer staged diff

- If staged changes exist, inspect:

```bash
git diff --staged
```

- Otherwise inspect:

```bash
git diff
```

- If relevant untracked files exist, mention them in your reasoning before drafting the message.

### 3. Analyze intent

- Identify the main behavior or domain change
- Choose the most accurate `type`
- Choose a scope only if one area clearly leads
- Keep the message focused on why the change matters
- If multiple subjects are present, define the commit boundaries before staging

### 4. Draft the message

- Write the subject line first
- Add a body only if the context is not obvious from the subject
- Add a footer only for breaking changes or issue references provided by the user

### 5. Commit when requested

- If the user asks you to commit, stage only one work subject at a time
- Create each commit yourself from the command line
- Re-check `git status` after every commit before preparing the next one
- Continue until all requested subjects are committed
- If the user only asked for a commit message, do not create a commit

### 6. Present the result

- If you did not commit, return the final commit message inside a fenced code block
- If you created commits, return the final list of commit subjects and hashes

## Examples

```text
feat(city): add owner filter to city search endpoint
```

```text
fix(auth): resolve refresh token expiration handling
```

```text
refactor(ai): extract provider orchestration into application service
```

```text
security(auth): tighten JWT validation for expired sessions
```

```text
config: update production mail settings
```
