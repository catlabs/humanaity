# Monorepo History Import Workflow

This runbook imports the three existing repositories into a new monorepo while preserving full commit history per app path.

## Scope

- Import `humanaity-ui` into `apps/ui`
- Import `humanaity-be` into `apps/backend`
- Import `humanaity-mcp` into `apps/mcp`
- Keep old repositories untouched and archive after cutover

## Preconditions

1. All source repos are clean (`git status` shows no pending changes).
2. The new monorepo exists and has an initial commit on its default branch (for example `main`).
3. Local clones are available at:
   - `/Users/julien/dev/humanaity/humanaity-ui`
   - `/Users/julien/dev/humanaity/humanaity-be`
   - `/Users/julien/dev/humanaity/humanaity-mcp`

## One-Time Setup (inside the new monorepo)

```bash
set -euo pipefail

# Adjust to the local path where you create the new monorepo.
MONOREPO_DIR="$HOME/dev/humanaity/humanaity"
mkdir -p "$MONOREPO_DIR"
cd "$MONOREPO_DIR"

# Initialize if not already initialized.
if [ ! -d .git ]; then
  git init -b main
  printf "# Humanaity Monorepo\n" > README.md
  git add README.md
  git commit -m "chore: initialize monorepo"
fi
```

## Add Source Repos as Remotes

```bash
cd "$MONOREPO_DIR"

git remote add ui "/Users/julien/dev/humanaity/humanaity-ui" || true
git remote add backend "/Users/julien/dev/humanaity/humanaity-be" || true
git remote add mcp "/Users/julien/dev/humanaity/humanaity-mcp" || true

git fetch ui --tags
git fetch backend --tags
git fetch mcp --tags
```

## Resolve Default Branches for Each Remote

```bash
cd "$MONOREPO_DIR"

ui_branch="$(git symbolic-ref --short refs/remotes/ui/HEAD | sed 's#^ui/##' || true)"
backend_branch="$(git symbolic-ref --short refs/remotes/backend/HEAD | sed 's#^backend/##' || true)"
mcp_branch="$(git symbolic-ref --short refs/remotes/mcp/HEAD | sed 's#^mcp/##' || true)"

# Fallbacks if remote HEAD is not configured.
[ -n "$ui_branch" ] || ui_branch="main"
[ -n "$backend_branch" ] || backend_branch="main"
[ -n "$mcp_branch" ] || mcp_branch="main"

echo "ui branch: $ui_branch"
echo "backend branch: $backend_branch"
echo "mcp branch: $mcp_branch"
```

## Import with Preserved History (`git subtree add`)

Run these commands in order from the monorepo root:

```bash
cd "$MONOREPO_DIR"

mkdir -p apps

git subtree add --prefix=apps/ui ui "$ui_branch"
git subtree add --prefix=apps/backend backend "$backend_branch"
git subtree add --prefix=apps/mcp mcp "$mcp_branch"
```

Notes:
- Do not use `--squash`; squashing drops detailed commit history.
- If a subtree command fails due to path conflicts, stop and resolve before continuing.

## Post-Import Verification

```bash
cd "$MONOREPO_DIR"

# Verify imported trees exist.
test -f apps/ui/package.json
test -f apps/backend/pom.xml
test -f apps/mcp/package.json

# Verify history is present per path.
git log --oneline -- apps/ui | head -n 20
git log --oneline -- apps/backend | head -n 20
git log --oneline -- apps/mcp | head -n 20

# Verify commit attribution survives.
git log --format='%h %an <%ae> %s' -- apps/ui | head -n 10
git log --format='%h %an <%ae> %s' -- apps/backend | head -n 10
git log --format='%h %an <%ae> %s' -- apps/mcp | head -n 10
```

## Cutover Commit and Push

```bash
cd "$MONOREPO_DIR"

git status
git push -u origin main
```

## Rollback Strategy (before push)

If you imported the wrong branch/path and have not pushed yet:

```bash
cd "$MONOREPO_DIR"

# Move back to state before subtree imports.
git reset --hard HEAD~3
```

If already pushed, revert with explicit revert commits instead of history rewrites:

```bash
cd "$MONOREPO_DIR"

git revert --no-edit HEAD
git revert --no-edit HEAD
git revert --no-edit HEAD
git push
```

## Immediate Follow-Up After Import

1. Add monorepo root README run instructions for:
   - `apps/backend`
   - `apps/ui`
   - `apps/mcp`
2. Update backend `.cursor/mcp.json` paths to monorepo-relative paths.
3. Validate:
   - backend OpenAPI available on `http://localhost:8080/v3/api-docs`
   - UI and MCP codegen commands still work from their new app directories
