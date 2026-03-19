# Deterministic Command Contract Spec

## Status

- Status: ACTIVE and implemented for milestone-oriented execution
- Applies to: backend-owned simulation command execution and the primary command-console UX in `apps/backend` and `apps/ui`
- Replaces forward-planning reliance on `/api/agent/cities/{cityId}/chat` for the main simulation control loop

## Purpose

This document fixes the contract for explicit, fail-closed simulation commands.

If implementation choices conflict with this spec during milestone execution, this spec is the source of truth for the primary command path.

## Primary Product Goal

The user types one short explicit command, the backend validates and executes it deterministically, and the UI refreshes from backend-owned state.

AI is not part of command parsing or command execution for this path.

## Endpoint

- Method and path: `POST /api/simulations/{cityId}/commands`
- Auth: same bearer-auth and city-ownership rules as the existing simulation endpoints
- Request body:

```json
{
  "commandText": "advance 5"
}
```

- Response body:

```json
{
  "ok": true,
  "commandType": "ADVANCE",
  "message": "Advanced city by 5 steps.",
  "mutated": true,
  "referencedEntities": {
    "humanId": null,
    "placeId": null
  },
  "uiEffects": [
    { "type": "REFRESH_SNAPSHOT" },
    { "type": "REFRESH_TIMELINE" }
  ]
}
```

## Supported commands

### `advance <count>`

- advances the city by an explicit deterministic step count
- `count` must be an integer from `1` to `20`
- successful execution returns `mutated = true`
- successful execution must return `REFRESH_SNAPSHOT` and `REFRESH_TIMELINE`

### `focus <human>`

- focuses one human without mutating simulation state
- `<human>` resolves by exact case-insensitive human name match or numeric human id
- successful execution returns `mutated = false`
- successful execution must return `FOCUS_HUMAN`

### `move <human> <place>`

- assigns one human toward one known symbolic place
- `<human>` resolves by exact case-insensitive human name match or numeric human id
- `<place>` must match one of the supported place ids:
  - `forest`
  - `river`
  - `church`
  - `campfire`
  - `house`
- successful execution returns `mutated = true`
- successful execution must return `REFRESH_SNAPSHOT`, `REFRESH_TIMELINE`, and may return `FOCUS_HUMAN` or `HIGHLIGHT_PLACE`

## Validation and parsing rules

- parsing is explicit and fail-closed
- no LLM, fuzzy NLP, or best-effort interpretation on the primary path
- unsupported verbs are rejected
- extra words outside the supported grammar are rejected
- ambiguous human matches are rejected
- unknown place ids are rejected
- rejected commands must not mutate simulation state

## Response semantics

### `ok`

- `true` only when the command was validated and executed
- `false` for unsupported, invalid, or ambiguous commands

### `commandType`

Allowed values:

- `ADVANCE`
- `FOCUS_HUMAN`
- `MOVE_HUMAN_TO_PLACE`
- `UNSUPPORTED`

### `message`

- always user-facing
- explains what happened or why the command was rejected

### `mutated`

- `true` only when backend state or deterministic goals changed
- `false` for read-only commands such as `focus`

### `referencedEntities`

- stable ids for UI binding
- initial shape:

```json
{
  "humanId": 12,
  "placeId": "forest"
}
```

### `uiEffects`

Allowed initial effects:

- `REFRESH_SNAPSHOT`
- `REFRESH_TIMELINE`
- `FOCUS_HUMAN`
- `HIGHLIGHT_PLACE`

The frontend should use these effects to drive refresh and focus behavior instead of inferring effects from free text.

## Boundary rules

- backend command execution remains canonical
- frontend submits command text and refreshes from backend responses
- AI narration may describe resulting events later, but it does not interpret or execute commands
- legacy `/api/agent/cities/{cityId}/chat` may remain temporarily for historical or transitional use, but it is not the forward product contract

## Deferred

- natural-language paraphrases
- multi-action commands
- director interventions
- AI-assisted command suggestions
