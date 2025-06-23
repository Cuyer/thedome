# Codex Agent Instructions

## Overview
This repository contains a Kotlin backend built with Ktor and Gradle. MongoDB is used for persistence and Docker/Docker Compose files are provided for running the application and MongoDB services.

## Code Style
- Kotlin source uses 4-space indentation and LF line endings as enforced by `.editorconfig`.
- Ensure files end with a newline.
- Prefer idiomatic Kotlin and coroutines for asynchronous logic.

## Development Workflow
- Install dependencies and run tests with the Gradle wrapper.
- Before committing, execute:
  ```bash
  ./gradlew test
  ```
  All tests must pass.
- Add or update tests when modifying application logic.
- Avoid committing build artifacts such as the `build/` directory or `.gradle/` as already ignored in `.gitignore`.

## Commit Messages
- Use concise commit messages summarizing the change (e.g. `Add new filter option`, `Fix JWT refresh logic`).
- Commit messages do not require a specific template but should be brief and meaningful.

## Additional Notes
- Dependency injection is handled with Koin (`src/main/kotlin/pl/cuyer/thedome/di`). Register new services within the Koin module when adding features.
- The GitHub CI workflow runs `./gradlew test` using JDK 21. Ensure compatibility.
- Prioritize performance when implementing database or network operations. Profile and tune slow paths to keep response times fast.

