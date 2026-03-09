---
name: commit-message
description: Generate Conventional Commit messages for the Humanaity MCP server by analyzing git changes. Use when committing in humanaity-mcp, reviewing staged changes, or when the user asks for a commit message for MCP tools, backend client, config, or server wiring.
---

# MCP Commit Message Generator

## Goal

Generate concise Conventional Commit messages for changes in `humanaity-mcp`.
When the user asks to commit changes, group the diff into coherent work subjects and create the commits in the command line instead of only proposing messages.
Prefer staged changes because they reflect what will actually be committed.

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
- Prefer a single strong subject line; add a body only when the reason is not obvious
- Do not list changed files or implementation steps in the body
- Mention the MCP server or tool behavior, not "frontend" wording

## Recommended Types

Use the most accurate type:

- `feat` for new MCP tools or user-facing capabilities
- `fix` for bug fixes or broken request/response handling
- `refactor` for structural changes without behavior changes
- `perf` for meaningful runtime or request-efficiency improvements
- `test` for test-only changes
- `docs` for README or usage documentation updates
- `build` for TypeScript, scripts, packaging, or build pipeline changes
- `config` for environment/configuration behavior changes
- `chore` for maintenance work that does not fit better elsewhere

## Scope Guidance

Prefer a scope when one area clearly dominates the change:

- Tool domains: `auth`, `city`, `human`, `simulation`
- Core server areas: `server`, `client`, `config`, `types`, `error`
- Tooling areas: `build`, `docs`

Omit the scope when the commit spans several unrelated areas.

## Procedure

### 1. Detect changes

```bash
git status --porcelain
```

### 1.5. Decide whether work must be split

- Review the full set of changes and identify distinct work subjects
- If the changes cover more than one MCP capability or a separate docs/config topic, create multiple commits
- Keep each commit focused on one logical subject only
- Avoid mixing MCP feature work with unrelated README or config updates unless they are inseparable

### 2. Prefer staged diff

- If staged changes exist, inspect:

```bash
git diff --staged
```

- Otherwise inspect:

```bash
git diff
```

### 3. Analyze intent

- Identify the primary behavior change
- Choose the most accurate `type`
- Choose a scope only if one area clearly leads
- Keep the message focused on why the change matters
- If several subjects are present, define the commit boundaries before staging

### 4. Commit when requested

- If the user asks you to commit, stage only one work subject at a time
- Create each commit yourself from the command line
- Re-check the remaining worktree after each commit before creating the next one
- If the user only asked for a commit message, do not create a commit

### 5. Present the result

- If you did not commit, return the final commit message inside a fenced code block
- If you created commits, return the final list of commit subjects and hashes

## Examples

```text
feat(simulation): add snapshot tool with derived metrics
```

```text
fix(auth): refresh cached tokens after login fallback
```

```text
refactor(client): centralize backend error normalization
```

```text
config(server): support API timeout from environment
```

```text
docs: document local smoke-check flow for MCP tools
```
