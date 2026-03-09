---
name: commit-message
description: Generate a Conventional Commit message from git changes (prefer staged). Use when the user is about to commit or asks for a commit message.
---

# Commit Message Generator (HumanAIty)

## Goal

Generate high-quality Conventional Commit messages by analyzing git changes.
When the user asks to commit changes, group them into coherent work subjects and create the commits in the command line instead of only proposing messages.
Prefer staged changes because they reflect what will actually be committed.

## Mandatory Guidelines (Single Source of Truth)

For the full reference (types, scopes, examples, best practices), see
[COMMIT_BEST_PRACTICES.md](../../../docs/best-practices/COMMIT_BEST_PRACTICES.md).

The rules below are the **strict subset** you must always enforce:

### Format

```
<type>(<scope>): <subject>

[optional body]

[optional footer]
```

### Rules (Strict)

- Never mention "frontend" or "frontend features"
- Keep the subject line under 72 characters
- Use imperative mood ("Add", "Fix", "Refactor", "Improve", "Update")
- First line should be a complete sentence
- Capitalize the first letter of the subject
- No period at the end of the subject line
- Prefer a single well-crafted subject line; add a body only if "why/context" is needed
- Do NOT list files changed or implementation steps in the body

### Types

Core: `feat`, `fix`, `refactor`, `perf`, `test`, `docs`, `chore`, `style`
Special: `build`, `ci`, `ui`, `ux`, `config`

### Scopes

- Prefer feature scope from folder structure: `app/<feature>/...` => scope = `<feature>`
  Examples: `auth`, `city`, `human`, `simulation`, `admin`, `<new-feature>`
- Use technical scopes for cross-cutting work:
  `api`, `service`, `component`, `guard`, `interceptor`, `route`, `style`, `config`
- Omit scope if changes are truly project-wide or span many unrelated areas

### Footer

- Breaking change: `BREAKING CHANGE: <description>`
- Issue refs (if user provides): `Closes #123` / `Fixes #456`

## Procedure (Do This Every Time)

### 1. Detect changes

```bash
git status --porcelain
```

### 1.5. Decide whether work must be split

- Review the full set of changes and identify distinct work subjects
- If the user worked on more than one subject, create multiple commits
- Keep each commit focused on one logical change only
- Do not force one commit per repository when one repository contains unrelated work
- If a repository has no changes, do not create a commit there

### 2. Prefer staged diff

- If there are staged changes, run:

```bash
git diff --staged
```

- If nothing is staged, fall back to the full working-tree diff:

```bash
git diff
```

- If there are also untracked files that look relevant, mention them but focus the message on the diff content.

### 3. Analyze the diff

- Identify which features / technical layers are affected
- Determine the primary intent: new feature, bug fix, refactor, etc.
- Decide on the most accurate `<type>` and `<scope>`
- Propose a commit grouping plan before staging when the changes span multiple subjects

### 4. Draft the message

- Write the subject line first (imperative, < 72 chars)
- Only add a body if the "why" isn't obvious from the subject
- Add a footer only if there is a breaking change or the user provides issue numbers

### 5. Commit when requested

- If the user asks you to commit, stage only the files for one work subject at a time
- Create the commit yourself from the command line
- Repeat for each additional work subject
- After each commit, verify the remaining worktree before creating the next one
- If the user only asked for a commit message, present the message without committing

### 6. Present the result

- If you did not commit, output the final commit message inside a fenced code block
- If you created commits, report the commit subjects and hashes back to the user

## Examples

**Simple feature:**

```
feat(city): add city list page with search
```

**Bug fix:**

```
fix(auth): resolve token refresh loop on expired session
```

**UI improvement:**

```
ui(simulation): improve timeline node spacing and alignment
```

**Refactoring with body (complex context):**

```
refactor(api): migrate HTTP calls to generated API client

Replace hand-written fetch wrappers with the auto-generated
OpenAPI client to reduce drift between backend and frontend types.
```

**Breaking change:**

```
feat(route): restructure city routes with lazy loading

BREAKING CHANGE: City routes now require authentication
```
