---
name: commit-message
description: Generate Conventional Commit messages for the Humanaity monorepo by analyzing git changes, preferring staged diffs and splitting unrelated work into separate commits. Use when the user asks for a commit message, wants help reviewing staged changes, or asks you to create commits.
---

# Commit Message Generator

## Goal

Generate high-quality Conventional Commit messages for the Humanaity monorepo.
Commit grouping should follow roadmap structure: `Epic` first, then `Task`.
Prefer staged changes because they reflect what will actually be committed.
When the user asks to commit changes, group the diff into coherent work subjects and create the commits in the command line instead of only proposing messages.
When the user says `use commit skill` (or equivalent), treat it as explicit authorization to:

1. stage focused commits,
2. create the commit(s).

Do not push, sync, or otherwise update the remote unless the user explicitly asks for it.

## Source Of Truth

Before drafting commit messages, read:

- repository commit history (`git log`) and current diff (`git diff` / `git diff --staged`)
- `docs/roadmap.md` to map the change to an epic number
- the active sprint doc in `docs/sprints/` to map the change to a task number when available

Then apply this skill's rules.

## Format

```text
<type>(<scope>): E<epic> T<task> <subject>

[optional body]

[optional footer]
```

Example:

```text
feat(simulation): E1 T2b Add run lifecycle persistence
```

## Rules

- Keep the subject line under 72 characters
- Use imperative mood: `Add`, `Fix`, `Refactor`, `Improve`, `Update`
- Capitalize the first letter of the subject
- Do not end the subject with a period
- Prefer a single strong subject line
- Include epic number before task number in the subject when the mapping is known
- Prefer `E<epic> T<task>` over prose such as `Epic 1` or `Task 2b`
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

## Epic And Task Mapping

Before deciding commit boundaries, classify the work in this order:

1. map each changed file or behavior to one roadmap epic in `docs/roadmap.md`
2. within that epic, map the work to the most specific sprint task available in `docs/sprints/`
3. if several changed files belong to different tasks, split them into separate commits whenever practical
4. never mix changes from different epics in the same commit unless the user explicitly asks for a combined commit

Grouping priority:

- first by epic number
- then by task number
- then by coherent behavior within that task if more splitting is still needed

If the epic is clear but the task is not, use `E<epic>` in the subject and explain the missing task mapping briefly in the body if needed.
If neither epic nor task can be mapped with confidence, fall back to the standard Conventional Commit format and say that the work could not be mapped to a roadmap item.

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
- Map each subject to an epic number first, then a task number
- If more than one subject exists, create multiple commits
- Keep each commit focused on one logical change within one epic/task bucket

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
- Identify the roadmap epic number that best matches the change
- Identify the most specific task number available for that epic
- Choose the most accurate `type`
- Choose a scope only if one area clearly leads
- Keep the message focused on why the change matters
- Define commit boundaries before staging when several subjects exist

### 5. Draft the message

- Write the subject line first
- Prefer subject pattern: `<type>(<scope>): E<epic> T<task> <subject>`
- Add a body only if needed for context
- Use the body to clarify epic/task mapping when the relationship is non-obvious or partial
- Add a footer only for breaking changes or issue references

### 6. Commit when requested

- Stage only one work subject at a time
- Create each commit from the command line
- Re-check `git status` after each commit
- If the user only asked for a message, do not create a commit

### 6b. Do not sync automatically

- After creating commit(s), stop at the local commit step
- Do not run `git push`, `git pull`, `git sync`, or any remote update command unless the user explicitly asks
- Report local commit hash(es) and subject(s)

### 7. Present the result

- If no commit was created, return the final message in a fenced code block
- If commits were created, return the commit subjects and hashes

## Examples

```text
feat(simulation): E1 T2b Add run lifecycle persistence
```

```text
refactor(simulation): E1 T3a Extract deterministic step flow
```

```text
test(simulation): E1 T5a Add same-seed reproducibility coverage
```

```text
feat(api): E3 T2c Expose simulation run lifecycle endpoints
```

```text
ui(simulation): E4 T1 Improve city simulation timeline layout
```

```text
docs: E7 T1 Document MCP smoke validation flow
```
