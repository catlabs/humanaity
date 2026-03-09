---
name: modernize-legacy-angular
description: Migrate legacy Angular code to project standards. Use when refactoring older components, templates, routing, and service patterns.
---

# Modernize Legacy Angular

## Goal

Refactor existing code to current project architecture and framework conventions.

## Checklist

1. Convert feature components to standalone where applicable.
2. Replace constructor DI with `inject()`.
3. Replace `@Input`/`@Output` with `input()`/`output()`.
4. Replace `*ngIf/*ngFor/*ngSwitch` with `@if/@for/@switch`.
5. Replace component-state `BehaviorSubject` usage with signals.
6. Move API calls from components into feature services.
7. Ensure generated API services/types are used consistently.
8. Apply Material token + SCSS mixin styling conventions.
9. Split oversized files into focused units.

## Output Expectations

- Summarize what changed, what remains, and migration risks.
- Call out temporary workarounds (for example, missing generated endpoints).
