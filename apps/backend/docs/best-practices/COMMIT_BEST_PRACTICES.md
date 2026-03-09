# Commit Message Guidelines

## Format

```
<type>(<scope>): <subject>

[optional body]

[optional footer]
```

## Rules

- **Never mention "backend" or "backend features"** - this is a separate project
- Keep the subject line under 72 characters
- Use imperative mood ("Add feature" not "Added feature" or "Adds feature")
- First line should be a complete sentence
- Capitalize the first letter of the subject
- No period at the end of the subject line

## Types

### Core Types

- `feat`: New feature
- `fix`: Bug fix
- `refactor`: Code refactoring (no behavior change)
- `perf`: Performance improvement
- `test`: Adding or updating tests
- `docs`: Documentation changes
- `chore`: Maintenance tasks (dependencies, config, etc.)
- `style`: Code style changes (formatting, missing semicolons, etc.)

### Special Types

- `build`: Build system or dependencies changes
- `ci`: CI/CD configuration changes
- `security`: Security improvements
- `config`: Configuration changes

## Scopes

Use scopes to indicate the area of the codebase affected. **Scopes are flexible and should match your project structure.**

### How to Choose a Scope

1. **Feature/Module Scopes**: Use the name of the feature module or domain area
   - Examples: `auth`, `city`, `human`, `simulation`, `ai`
   - **New features automatically get their own scope** - just use the module/package name
   - Look at your package structure: `eu.catlabs.humanaity.<module>` → scope is `<module>`

2. **Technical Scopes**: Use when the change affects a technical layer across modules
   - Examples: `api`, `service`, `entity`, `dto`, `repository`, `config`, `security`
   - Use when changes span multiple features or are infrastructure-related

3. **No Scope**: Omit scope if the change affects multiple areas or is project-wide
   - Example: `chore: update dependencies` (no scope needed)

### Scope Examples (Not Exhaustive)

These are just examples - your project may have different modules:
- Feature modules: `auth`, `city`, `human`, `simulation`, `ai`, `<your-new-feature>`
- Technical layers: `api`, `service`, `entity`, `dto`, `repository`, `config`, `security`

## Examples

### Simple Feature
```
feat(city): add city search by name endpoint
```

### Simple Change (No Body Needed)
```
docs: add architecture best practices guide
```

**Note**: For simple changes (adding a file, small fixes, documentation), the subject line is sufficient. Only add a body for complex changes that need context.

### Feature with Scope
```
feat(ai): implement OpenAI adapter for human generation
```

### Bug Fix
```
fix(auth): resolve token refresh expiration issue
```

### Refactoring
```
refactor(ai): restructure AI module with provider abstraction
```

### Multiple Changes
```
feat(ai): add AiProviderPort interface for multi-provider support

- Create domain models (AiProvider, AiPrompt, AiResponse)
- Implement OpenAI adapter
- Add AiGenerationService for orchestration
```

### Breaking Change
```
feat(api): change city creation endpoint signature

BREAKING CHANGE: CityInput now requires owner field
```

### Security
```
security(auth): increase JWT token expiration time
```

### Configuration
```
config: update application properties for production
```

## Best Practices

1. **Always check changes first**: Before writing a commit message, run `git status` and `git diff` to review all changes. This ensures you don't miss any files or modifications.
2. **Be specific**: Instead of "fix bug", use "fix(auth): resolve null pointer in token validation"
3. **Keep it concise and condensed**: The subject line should be clear and complete. Avoid detailed bullet lists in the body unless absolutely necessary for complex changes. Prefer a single, well-crafted subject line that captures the essence of the change. Only add a body when the change requires explanation of the "why" or context that isn't obvious from the code.
4. **One logical change per commit**: Don't mix unrelated changes
5. **Reference issues**: Use `Closes #123` or `Fixes #456` in footer when applicable
6. **Explain why, not what**: The code shows what changed; the commit message should explain why
7. **Avoid over-detailing**: Don't list every file changed or implementation steps. Focus on what was accomplished at a high level, not the detailed mechanics

## Common Patterns

### Adding a Feature
```
feat(<module>): add <feature description>
```

### Fixing a Bug
```
fix(<module>): resolve <issue description>
```

### Refactoring
```
refactor(<module>): <refactoring description>
```

### Updating Dependencies
```
chore(deps): update Spring Boot to 3.5.0
```

### Documentation
```
docs: update README with new architecture details
```

## Footer

Optional footer can include:
- Breaking changes: `BREAKING CHANGE: <description>`
- Issue references: `Closes #123`, `Fixes #456`
- Co-authors: `Co-authored-by: Name <email>`

## Examples for Current Project

### AI Module Restructuring
```
refactor(ai): restructure AI module with provider abstraction

- Create domain models (AiProvider, AiPrompt, AiResponse)
- Add AiProviderPort interface for provider abstraction
- Implement OpenAI adapter
- Add AiGenerationService for orchestration
- Update HumanGenerationService to use new AI module
```

### New Endpoint
```
feat(city): add endpoint to get cities by owner
```

### Bug Fix
```
fix(human): resolve personality calculation edge case
```

### Security Update
```
security(auth): increase refresh token expiration to 7 days
```
