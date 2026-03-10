---
name: commit-message
description: Generate Conventional Commit messages for the Humanaity monorepo by analyzing git changes, preferring staged diffs and splitting unrelated work into separate commits. Use when the user asks for a commit message, wants help reviewing staged changes, or asks you to create commits.
---

# Commit Message Generator

## Goal

Generate high-quality Conventional Commit messages for the Humanaity monorepo.
Prefer staged changes because they reflect what will actually be committed.
When the user asks to commit changes, group the diff into coherent work subjects and create the commits in the command line instead of only proposing messages.
When the user says `use commit skill` (or equivalent), treat it as explicit authorization to:

1. stage focused commits,
2. create the commit(s), and
3. push them to the tracked remote branch.

Only skip push when the user explicitly asks not to push.

## Source Of Truth

Before drafting commit messages, read:

- `apps/backend/docs/best-practices/COMMIT_BEST_PRACTICES.md`

If that file is unavailable, follow this skill's rules.

## Format

```text
<type>(<scope>): <subject>

[optional body]

[optional footer]
```

## Rules

- Keep the subject line under 72 characters
- Use imperative mood: `Add`, `Fix`, `Refactor`, `Improve`, `Update`
- Capitalize the first letter of the subject
- Do not end the subject with a period
- Prefer a single strong subject line
- Add a body only when the reason is not obvious
- Do not list changed files or implementation steps in the body
- Focus on the behavioral or product outcome, not the mechanical edit
- Prefer domain or technical scopes over repo labels like `frontend`, `backend`, or `mcp`

## Types

Core:

- `feat`
- `fix`
- `refactor`
- `perf`
- `test`
- `docs`
- `chore`
- `style`

Additional:

- `build`
- `ci`
- `config`
- `security`
- `ui`
- `ux`

## Scope Guidance

Prefer a scope when one area clearly dominates the change.

Common domain scopes:

- `auth`
- `city`
- `human`
- `simulation`
- `ai`

UI-oriented scopes:

- `component`
- `guard`
- `interceptor`
- `route`
- `style`

Backend-oriented scopes:

- `api`
- `service`
- `entity`
- `dto`
- `repository`
- `security`
- `config`

MCP-oriented scopes:

- `server`
- `client`
- `types`
- `error`
- `config`

Omit the scope when the commit is truly project-wide or spans unrelated areas.

## Footer

- Breaking changes: `BREAKING CHANGE: <description>`
- Issue references, if supplied by the user: `Closes #123`, `Fixes #456`

## Procedure

### 1. Detect changes

Always inspect repo state first:

```bash
git status --porcelain
```

### 2. Decide whether work must be split

- Review the full set of changes
- Identify distinct work subjects
- If more than one subject exists, create multiple commits
- Keep each commit focused on one logical change

### 3. Prefer staged diff

If staged changes exist:

```bash
git diff --staged
```

Otherwise:

```bash
git diff
```

Mention relevant untracked files in your reasoning when needed.

### 4. Analyze intent

- Identify the main behavior or domain change
- Choose the most accurate `type`
- Choose a scope only if one area clearly leads
- Keep the message focused on why the change matters
- Define commit boundaries before staging when several subjects exist

### 5. Draft the message

- Write the subject line first
- Add a body only if needed for context
- Add a footer only for breaking changes or issue references

### 6. Commit when requested

- Stage only one work subject at a time
- Create each commit from the command line
- Re-check `git status` after each commit
- If the user only asked for a message, do not create a commit

### 6b. Push by default for commit-skill requests

- After creating commit(s), verify branch tracking status
- If upstream is missing, push with `-u origin HEAD`
- If upstream exists, push with `git push`
- Report pushed commit hash(es) and destination branch

### 7. Present the result

- If no commit was created, return the final message in a fenced code block
- If commits were created, return the commit subjects and hashes

## Examples

```text
feat(city): add owner filter to city search
```

```text
fix(auth): resolve refresh token expiration handling
```

```text
refactor(client): centralize backend error normalization
```

```text
security(auth): tighten JWT validation for expired sessions
```

```text
ui(simulation): improve timeline node spacing and alignment
```

```text
docs: document monorepo local run workflow
```
