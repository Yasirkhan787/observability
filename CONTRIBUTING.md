# Contributing

Thanks for helping improve this observability learning project. Keep changes
small, reproducible, and useful to someone learning by running the examples.

## Before opening a change

1. Check the existing issues and pull requests for related work.
2. Choose one stack version at a time; the versions share host ports and some
   persistent volumes.
3. Run the services and stack relevant to your change, then follow that
   version's README to verify the result.
4. Update the main README, reference, or version guide when commands, ports,
   configuration, or expected behavior change.
5. Do not commit credentials, local environment files, generated data, or
   application changes presented as documentation-only changes.

## Run locally

Use Java 21 and the Maven wrapper included in each service folder. Start the
MySQL dependency for `order-service`, then start the selected Docker Compose
stack from its own version folder, and run both services using their wrapper.
See the root README and the relevant version README for exact commands and
ports. Do not run multiple stack versions at the same time.

## Branch names

Use a short prefix and kebab-case description:

- `docs/clarify-v3-traces`
- `fix/payment-validation`
- `feat/add-new-backend`

## Commit messages

Use [Conventional Commits](https://www.conventionalcommits.org/), for example:

- `docs: add v4 Loki walkthrough`
- `fix: correct payment timeout example`
- `feat: add a new observability stack version`

## Adding a stack version or backend

1. Add a new version folder with its Compose file and only the configuration
   files required by that stack.
2. Use distinct service names where practical, document shared host ports,
   and explain whether named volumes are reused or isolated.
3. Add a version README following the existing version guide structure. State
   the required application state and how to obtain it.
4. Update the version comparison, port map, reference, and troubleshooting
   sections in the root documentation.
5. Verify the complete path from application telemetry through the Collector
   to the backend and its UI. Label any behavior that has not been tested.
6. Keep dashboards and queries aligned with metric names observed in the
   Collector or backend.

## Documentation style

- Prefer short, direct explanations, tables for reference facts, and
  copyable commands.
- Give PowerShell and bash variants where their syntax differs. In PowerShell,
  use `curl.exe` for HTTP examples.
- Use relative links and verify that each target exists. Quote paths containing
  spaces or shell metacharacters.
- Cite the repository file that supports reference facts. Mark unverified
  behavior with `⚠️ Not verified`.
- Do not claim that optional or roadmap features work unless the repository
  implements them and the behavior has been checked.
