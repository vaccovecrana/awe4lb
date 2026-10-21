# awe4lb — Agent Notes

Read these additional files:

- ./README.md
- ./a4-test/README.md

## Architecture

`awe4lb` is a small, stateless Layer 4 load balancer. It proxies TCP, TLS and UDP
traffic to static or discovered backend pools, with health checks, TLS
termination and a REST/UI management surface.

Modules:

| Module    | Purpose                                                                                |
|-----------|----------------------------------------------------------------------------------------|
| `a4-core` | Config model, selectors, TCP/UDP proxy, discovery, health checks, validation, counters |
| `a4-ui`   | Murmux/Ronove REST API, static UI hosting, Preact SPA (`@a4ui`), rspack build          |
| `a4-lb`   | Packaging module: `a4lb` application + GraalVM native image                            |
| `a4-test` | Centralized unit and integration tests                                                 |

Configuration is stored as JSON on disk, but the UI edits a YAML document and
keeps it in sync with a structured form. The active configuration owns all
runtime state (listeners, sessions, counters, stats history).

## Frameworks (custom sources)

| Concern                  | Framework       | Path                                                       |
|--------------------------|-----------------|------------------------------------------------------------|
| Web routing / validation | murmux + ronove | `/home/jjzazuet/code/murmux`, `/home/jjzazuet/code/ronove` |
| Logging                  | shax            | `/home/jjzazuet/code/shax`                                 |

Use ronove 4.x / murmux 2.8.x. The REST controller is the source of truth for
the generated TypeScript client (`a4-ui/@a4ui/rpc.ts`); regenerate it with
`./gradlew :a4-ui:ronoveTypescriptRpc`.

## Common tasks

```bash
./gradlew build                 # full build (UI npm build + API + app)
./gradlew run                   # dev run
./gradlew :a4lb-linux-amd64:installDist   # project is renamed from a4-lb
npm --prefix a4-ui run build    # frontend only (rspack + sass)
npm --prefix a4-ui run test:unit
./a4-ui/test/run-suite.sh e2e   # start a throwaway UI, run browser tests, stop it
./a4-ui/test/run-suite.sh visual
```

Project lookup note: `a4-lb` is renamed by `settings.gradle.kts` to
`a4lb-${os}-${arch}`, so Gradle paths use `:a4lb-linux-amd64:...` on Linux.

## Testing

- Java unit + in-process proxy/stats tests: `./gradlew :a4-test:test` (no Docker).
- Docker-backed routing suite: `./a4-test/e2e.sh` (starts the test backends in
  `a4-test/src/test/resources/docker-compose.yml`, then sets
  `A4_INTEGRATION_TESTS=1`).
- UI unit tests: `npm --prefix a4-ui run test:unit` or `:a4-ui:testUi`.
- Browser suites (require a running instance):
  `A4_UI_URL=http://127.0.0.1:7070 ./gradlew :a4-ui:e2eTest` and
  `... :a4-ui:visual`. The standalone runner starts/stops the app itself:
  `./a4-ui/test/run-suite.sh e2e` or `./a4-ui/test/run-suite.sh visual`.
- Browser tests are skipped when the UI is unreachable; `e2eTest` and `visual`
  are not wired into `build`/`check`.

## UI conventions

- Preact function components + hooks only; JSX runtime is `preact/compat`.
- rspack (`builtin:swc-loader`) bundles TypeScript; Sass is compiled separately
  from `res/main.scss` into `build/ui/index.css`.
- The design system uses the `a4-*` class namespace and the Nord palette.
- The config editor must stay bidirectional: form changes regenerate YAML, and
  valid YAML edits update the form. Keep entry keys stable so inputs do not lose
  focus.
- Charts use `chart.js` + `react-chartjs-2`; history comes from the server-side
  stats ring buffer.

## Rules

- Never commit code by yourself. Human code review is a critical step to ensure
  code quality and safety. Report back to the user once a set of code changes is ready.
- Keep tests centralized in `a4-test` unless they are UI unit/E2E tests.
- Do not add unrelated project documentation or infrastructure assumptions to
  this file.
