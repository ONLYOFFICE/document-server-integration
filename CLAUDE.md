# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## What this repository is

A collection of **11 independent reference implementations** of the same application — a minimal document
management system that integrates ONLYOFFICE Docs (Document Server) — each written in a different
language/framework. They live side by side under `web/documentserver-example/`:

`csharp` (ASP.NET WebForms), `csharp-mvc` (ASP.NET MVC), `go`, `java` (JavaEE/WAR), `java-spring`,
`js` (static HTML page, no backend), `nodejs` (Express), `php` (no framework), `php-laravel`,
`python` (Django), `ruby` (Rails).

There is **no shared code between examples** and no root-level build that spans them. Each directory has its
own dependency manifest, config file, linter, README and CI workflow. Work inside one example directory at a
time; changing behaviour in one does *not* change the others.

A change to a user-visible feature is normally expected to be **ported to every example**. Look at the git
log to see the pattern: one commit per language with the same subject
(`feat(python): when converting a multi-page document...`, `feat(go): ...`, …). When asked for a feature
without a named language, ask which examples are in scope rather than assuming all of them.

## Per-example commands

Run from inside the example's own directory.

| Example | Install | Run | Lint | Test |
|---|---|---|---|---|
| nodejs | `npm install` (or `make` from repo root) | `npm start` (port 3000) | `npm run lint` / `npm run lint:fix` | — |
| php | `make dev` | `make server-dev` (port 9000) | `make lint` (phpcs) | `make test` (phpunit) |
| php-laravel | `make install` | `make compose-start` | `pint --test` | `phpunit` |
| python | `make dev` | `make server-dev` (port 8000) | `make lint` (flake8), `make types` (mypy, not yet wired into CI) | `make test` |
| ruby | `make dev` | `make server-dev` (port 3000) | `make lint` (rubocop + sorbet on macOS/x86_64 only) | `make test` |
| go | `go mod download` | `go run main.go` (port 3000) | `golangci-lint run` | — |
| java | — | build WAR + deploy to a servlet container | `mvn package` (checkstyle runs in the build) | — |
| java-spring | — | `./mvnw spring-boot:run` | `mvn package` (checkstyle) | — |
| csharp, csharp-mvc | — | Visual Studio / IIS (`.sln`) | — | — |

`make help` in php/python/ruby prints the full recipe list. Docker is available via
`make compose-dev` / `make compose-prod` (python, ruby) or `docker compose up` (php, php-laravel, java).

### Running a single test

- php: `./vendor/bin/phpunit src/format/FormatManagerEditableTests.php`
- python: `python -m unittest src.format.format_tests`
- ruby: `bundle exec ruby -Itest app/format/format_tests.rb`

Test files are **colocated with the source**, not in a `tests/` tree, and are discovered by suffix:
`*Tests.php` (php), `*_tests.py` (python), `*_tests.rb` (ruby). Only php/python/ruby have tests at all.

## Submodules

`.gitmodules` wires two external repos into most examples' asset folders:
[`document-templates`](https://github.com/ONLYOFFICE/document-templates) (blank files created by "Create new")
and [`document-formats`](https://github.com/ONLYOFFICE/document-formats) (the format table driving what can be
viewed/edited/filled/converted). CI does `git submodule update --init --recursive` before anything else —
a fresh clone without it produces examples that cannot create or classify files.

## The integration architecture (shared by all examples)

Every example implements the same set of HTTP endpoints and the same on-disk storage layout. Reading
`web/documentserver-example/nodejs/app.js` plus `helpers/docManager.js` and `helpers/documentService.js` is the
fastest way to understand the whole model; `go/server/api/default/*.go` mirrors it file-per-endpoint and is the
clearest map of the endpoint set.

**Endpoints** (names vary slightly per framework): `/` index, `/editor` (builds the editor config and renders
the page), `/config`, `/upload`, `/create`, `/convert`, `/download`, `/files`, `/history`, `/historyObj`,
`/restore`, `/rename`, `/reference`, `/formats`, `/forgotten`, and `/track`.

**`/track` is the core of the integration.** Document Server POSTs a callback here with a `status`:
`1` editing, `2`/`3` MustSave/Corrupted, `4` closed with no changes, `6`/`7` MustForceSave/CorruptedForceSave.
On save the example downloads `url` from the callback, rotates the current file into the history tree, and
writes the new content in its place.

**Storage layout** — files are per-client-IP, versions are directories:

```
<storageFolder>/<user host address>/
  <filename>                       current version
  <filename>-history/
    1/  prev.<ext>  diff.zip  changes.txt  key.txt  user.txt
    2/  ...
```

The "user address" partitioning is why the examples appear to have per-visitor storage without any
authentication.

**JWT** is the only security boundary. Each example has a `JwtManager`/`jwtManager` equivalent that signs
outgoing requests to Document Server and validates the incoming `Authorization` header on `/track` and
`/download`. Config keys are `token.enable` / `token.secret` / `token.useforrequest` (or `JWT_IS_ENABLED`,
`files.docservice.secret`, …). Secret and header name must match the Document Server's.

**Users** are a hardcoded fixture, identical across examples: `uid-1`…`uid-3` plus `uid-0` (anonymous). Their
permission sets exercise different editor capabilities (review-only, comment-only, filler, etc.), so changing
one user's flags changes what the demo demonstrates.

**Formats** come from the `document-formats` submodule and are exposed through a per-example `FormatManager`
that answers "is this extension viewable / editable / fillable / auto-convertible". Newer examples
(nodejs) also fetch `meta/formats` and `meta/config` from the running Document Server at runtime and cache them.

## Configuration

The Document Server address (`http://documentserver`) is a placeholder in every example and must be replaced
to run anything. Per-example config file locations are listed in `Readme.md`. The `version` value is duplicated
into each config file (`package.json`, `pyproject.toml`, `configuration.json`, `settings.config`, …) — bump
them together.

## Conventions

- **Every source file carries the Apache-2.0 ONLYOFFICE header** (`(c) Copyright Ascensio System SIA <year>`).
  Java enforces it via checkstyle against `onlyoffice.header`; other languages rely on convention. New files
  must include it.
- **Commits** use Conventional Commits with the example as scope: `feat(nodejs): …`, `fix(php-laravel): …`.
  Older commits use the bare `nodejs: …` form; prefer the conventional form.
- **CHANGELOG.md** is release-critical: `create-tag.yml` and `release.yml` parse the first version number in
  it to name the tag and slice the release notes. Add an entry under the top unreleased `##` heading, prefixed
  with the example name (`nodejs: …`), for user-visible changes.
- CI runs lint per example, gated on `paths: ['web/documentserver-example/<name>/**']`, so a change only
  triggers its own language's workflow.

## Security posture

These are deliberately unhardened demos: no authorization, no validation of link parameters, no verification
of who calls `/track`, and permissive CORS (`Access-Control-Allow-Origin: *`). Do not "fix" these as if the
code were production — the README states the intent explicitly. Genuine bugs (path traversal beyond the
storage root, crashes, leaks) are still worth fixing.
