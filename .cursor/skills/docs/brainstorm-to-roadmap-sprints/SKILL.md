---
name: brainstorm-to-roadmap-sprints
description: Convert a raw brainstorm, ChatGPT strategy dump, or unstructured feature notes into implementation-ready Humanaity planning artifacts. Use when the user pastes ideas that are not yet organized and wants them reconciled with `docs/roadmap.md`, then split into one or more sprint docs, prompt packs, and usually spec files.
---

# Brainstorm To Roadmap And Sprints

## Goal

Turn messy planning input into repo-ready execution artifacts without losing roadmap alignment.

This skill is for the step between:

- "here are a lot of ideas"
- and "here is a roadmap update plus concrete sprint docs I can start implementing"

## Related durable context

- Pair this skill with `.cursor/rules/docs-sprint-planning.mdc` for stable sprint-structure constraints.
- Use `docs/agent-context.md` to keep policy in rules/docs and workflow steps in skills.

## Use This Skill When

- the user pastes a brainstorm from ChatGPT or another source
- the user has strategy notes, ideas, or feature lists that are not implementation-ready
- the user wants help turning ideas into roadmap updates
- the user wants one or more new sprint docs
- the user wants the matching prompt pack and likely spec files created too

## Expected Outputs

Depending on the input, this skill may create or update:

- `docs/roadmap.md`
- one or more sprint folders under `docs/sprints/`
- one main sprint doc per sprint, named `sprint-XX-theme.md`
- one prompt pack per sprint, named `sprint-XX-prompt-pack.md`
- one or more spec files under `docs/specs/` when semantics need to be locked before implementation

## Source Of Truth Order

Read these before planning:

1. the user brainstorm or pasted planning material
2. `docs/roadmap.md`
3. `docs/README.md`
4. the latest relevant sprint in `docs/sprints/`
5. relevant locked specs in `docs/specs/`
6. `.cursor/rules/docs-sprint-planning.mdc`

Do not let a brainstorm silently override the roadmap. Reconcile it explicitly.

## Planning Workflow

### 1. Normalize the brainstorm

Classify the input into buckets:

- product goals
- user-facing features
- technical enablers
- constraints and risks
- sequencing hints
- unclear or conflicting ideas

Strip repetition, merge synonyms, and turn vague ideas into short candidate outcomes.

### 2. Reconcile with the roadmap

For each brainstorm item, decide whether it is:

- already covered by the roadmap
- a refinement of an existing epic
- a new epic or feature that should be added
- out of scope or deferred
- still too vague and should stay as an open question

If the roadmap already covers the idea, prefer updating the relevant roadmap section instead of inventing a parallel planning track.

### 3. Decide the planning granularity

Use these heuristics:

- If the brainstorm describes one coherent milestone, create one sprint.
- If it spans multiple milestones or layers, split it into multiple sprints.
- If it changes long-term direction, update the roadmap before creating sprints.
- If the input is still highly ambiguous, first tighten the roadmap/epic framing and only then create sprints.

Keep each sprint focused on one coherent milestone.

### 4. Decide which specs are required

Create a spec when implementation would otherwise guess at:

- domain semantics
- state ownership
- canonical versus derived fields
- API/read-model shapes
- fallback behavior
- deterministic invariants
- acceptance semantics that multiple sprints will rely on

Usually do not create a spec for:

- minor UI polish
- simple wiring work
- local refactors
- low-risk maintenance tasks

### 5. Slice into executable sprints

For each sprint:

- pick one milestone outcome
- define in-scope and out-of-scope explicitly
- write concrete, testable definition of done
- break work into small delegable tasks
- identify likely file targets
- state dependencies inside the sprint

Prefer backend-first or contract-first sequencing when later UI or MCP work depends on stable semantics.

### 6. Create the sprint folder and files

Use the current repo convention:

```text
docs/sprints/sprint05/
  sprint-05-ai-history-enrichment.md
  sprint-05-prompt-pack.md
```

Rules:

- folder name: `sprintXX`
- sprint doc name: `sprint-XX-theme.md`
- prompt pack name: `sprint-XX-prompt-pack.md`
- `XX` is a two-digit number
- theme must describe the sprint outcome, not a vague label

### 7. Write the sprint doc in execution order

Use this section order:

- `## Execution status`
- `## Sprint intent`
- `## Why this sprint comes next`
- `## Sprint outcome`
- `## Sprint scope`
- `## Product and technical decisions for this sprint`
- `## Deliverables`
- `## Definition of done`
- `## Suggested file targets`
- `## Features and task breakdown`
- `## Recommended implementation order`
- `## Dependencies inside the sprint`
- `## Suggested delegation`
- `## Ready-to-delegate task list`
- `## Risks`
- `## Handoff to next sprint`

The execution status block should stay compact and scan-friendly.

### 8. Write the prompt pack

Create one prompt pack per sprint.

The prompt pack should:

- tell the user to use one prompt per sub-chunk
- reference the sprint doc, roadmap, relevant specs, and relevant rules
- repeat acceptance criteria from the sprint doc
- define explicit in-scope files
- define explicit out-of-scope items
- state whether the task should modify code or only propose docs/patches
- include a hard boundary such as: `Implement only this task ID; do not expand to other sprint tasks`

Do not assume Codex can discover hidden skills. Put mandatory repo-visible references directly in the prompt text.

### 9. Cross-link and sanity-check

Before finishing:

- ensure the roadmap and sprint docs do not conflict
- ensure sprint numbering is consecutive
- ensure each sprint outcome is smaller than an epic
- ensure prompt packs match the sprint task IDs exactly
- ensure specs are referenced from the sprint when required
- ensure deferred ideas are called out instead of silently dropped

## Artifact Decision Rules

### Update `docs/roadmap.md` when

- the brainstorm introduces a new long-term capability
- epic ordering changes
- dependencies change
- MVP boundaries change
- the project direction is materially refined

### Create a new sprint when

- there is a coherent milestone with a concrete outcome
- the work can be scoped with clear in/out boundaries
- tasks can be delegated and validated

### Create a spec when

- more than one task will depend on the same semantics
- the sprint needs a semantic lock before code starts
- the user wants a "source of truth" doc for a new area

## Writing Rules

- Keep sprint docs concrete and delegable.
- Prefer small executable chunks over broad implementation waves.
- Preserve roadmap alignment unless the user explicitly wants to change direction.
- Treat specs as locked semantic anchors, not brainstorm scratchpads.
- Treat prompt packs as copy-paste delegation tools, not narrative docs.
- If a brainstorm contains unresolved choices, surface them explicitly instead of pretending they are settled.

## Recommended Response Pattern

When the user provides a brainstorm:

1. summarize the proposed direction in a few bullets
2. state whether the roadmap already covers it or needs an update
3. propose the sprint split
4. identify which specs are needed
5. create or update the roadmap, sprint docs, prompt packs, and specs
6. end with the first sprint/chunk they can start immediately

## Example Triggers

- "I pasted a ChatGPT brainstorm, turn it into sprints."
- "Take these ideas and integrate them into the roadmap."
- "Split this strategy dump into epics, specs, and prompt packs."
- "Create the planning architecture from this brainstorm."

## Execution expectations (when implementing sprints)

When the agent later executes tasks from these sprints, it must follow the chunk completion gate (`.cursor/rules/sprint-chunk-completion-gate.mdc`) and the chunk review loop (`.cursor/rules/docs-chunk-review-loop.mdc`). At the end of each task, in addition to updating execution status, the agent must **commit the chunk's changes** using the commit-message skill (`.cursor/skills/commit-message/SKILL.md`)—one commit per task, no push unless the user asks.

## Final Check

Before finishing, verify:

- the brainstorm was reconciled against the current roadmap
- roadmap changes are explicit
- sprint files follow repo naming conventions
- prompt packs exist for the created sprints
- specs were created only when they add real semantic clarity
- the resulting docs are ready to drive implementation
