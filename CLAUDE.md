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
without a named language, ask which examples are in scope rather than assuming all of them. The `js` example
is the exception: it is a frozen static page with no backend, absent from CI and from the release artifacts,
and has never taken part in these sweeps.

**Every example directory has its own `CLAUDE.md`** with verified, example-specific detail — commands, code
map, storage layout, gotchas. When working inside one example, that file is the authority; this one only
orients you across the eleven.

## Per-example commands

Run from inside the example's own directory. The per-example `CLAUDE.md` files carry the exact invocations.

| Example | Install | Run | Lint | Test |
|---|---|---|---|---|
| nodejs | `npm install` (or `make` from repo root) | `npm start` (port 3000) | `npm run lint` / `npm run lint:fix` | — |
| php | `make dev` | `make server-dev` (port 9000) | `make lint` (phpcs) | `make test` (phpunit) |
| php-laravel | `make install` | `make compose-start` | `pint --test` | `phpunit` |
| python | `make dev` | `make server-dev` (port 8000) | `make lint` (flake8), `make types` (mypy, not yet wired into CI) | `make test` |
| ruby | `make dev` | `make server-dev` (port 3000) | `make lint` (rubocop + sorbet on macOS/x86_64 only) | `make test` |
| go | `go mod download` | `go run main.go` (port 3000) | `golangci-lint run` | — |
| java | — | build WAR + deploy to a servlet container | `mvn package` (checkstyle runs in the build) | — |
| java-spring | — | `mvn spring-boot:run` (`./mvnw` is broken — `.mvn/wrapper/` is not committed) | `mvn package` (checkstyle) | — |
| csharp, csharp-mvc | — | Visual Studio / IIS (`.sln`) | — | — |

`make help` in php/python/ruby prints the full recipe list. Docker is available via
`make compose-dev` / `make compose-prod` (python, ruby) or `docker compose up` (php, php-laravel, java).

### Running a single test

- php: `./vendor/bin/phpunit src/format/FormatManagerEditableTests.php`
- python: `python -m unittest src.format.format_tests`
- ruby: `bundle exec ruby -Itest app/format/format_tests.rb`

Test files are **colocated with the source**, not in a `tests/` tree, and are discovered by suffix:
`*Tests.php` (php), `*_tests.py` (python), `*_tests.rb` (ruby). Only php/python/ruby have tests at all, and
no CI workflow runs any of them — `make test` in python and ruby currently **fails out of the box**, because
both assert version `1.6.0` while their configuration reports `1.15.0`.

## Submodules

`.gitmodules` wires two external repos into most examples' asset folders:
[`document-templates`](https://github.com/ONLYOFFICE/document-templates) (blank files created by "Create new")
and [`document-formats`](https://github.com/ONLYOFFICE/document-formats) (the format table driving what can be
viewed/edited/filled/converted). CI does `git submodule update --init --recursive` before anything else —
a fresh clone without it produces examples that cannot create or classify files.

## The integration architecture

All 11 examples solve the same problem the same way, but **they are not uniform** — they were written and
evolved separately, and the differences are real. Treat what follows as the shape of the model, not as a
contract, and verify details against the example you are actually in. Each example directory has its own
`CLAUDE.md` with its verified specifics; read that first.

Reading `nodejs/app.js` plus `helpers/docManager.js` and `helpers/documentService.js` is the fastest way to
understand the model as a whole; `go/server/api/default/*.go` is the clearest map of the endpoint set, one
file per endpoint.

**Endpoints** — roughly `/` index, `/editor`, `/config`, `/upload`, `/create`, `/convert`, `/download`,
`/files`, `/history`, `/restore`, `/rename`, `/reference`, `/formats`, `/forgotten`, and the save callback.
Names diverge more than "slightly": the callback is `/track` in most, `/callback` in go, and
`IndexServlet?type=track` in java, which exposes its whole API as `?type=<action>` on one servlet rather than
as routes. php uses `/objhistory` where nodejs uses `/historyObj`, and has no `/create` at all.

**The save callback is the core of the integration.** Document Server POSTs a `status`: `1` editing, `2`/`3`
MustSave/Corrupted, `4` closed with no changes, `6`/`7` MustForceSave/CorruptedForceSave. On save the example
downloads `url` from the callback, rotates the current file into the history tree, and writes the new content
in its place. Not every example handles every status: go implements only `2`, `3`, `4` and has no force-save
at all (`Customization.Forcesave` is hardcoded `false`), while java defines `1`, `2`, `3`, `6`, `7` and never
handles `4`.

**Storage layout** — versions are numbered directories, but the details differ. The majority form is:

```
<storage root>/<user host address>/
  <filename>                    current version
  <filename>-hist/
    createdInfo.json            author of version 1
    1/  prev.<ext>  diff.zip  changes.json  key.txt
    2/  ...
```

**nodejs is the outlier**, not the reference: it uses `-history`, `changes.txt` and a per-version `user.txt`.
php-laravel differs completely (`info.json` / `history.json` / `changes.zip` / `N.<ext>`, force-saves on a
separate disk). go uses `-hist` but stores a current-version meta file at `<filename>-hist/<filename>.json`.

The per-client-IP partitioning is **not universal** either: go ignores the user address entirely and keeps one
flat folder; java-spring keys the folder on the *server's* own host IP; csharp, csharp-mvc and java drop the
partition when the configured storage path is absolute; and php-laravel loses it behind the bundled nginx
because `trustProxies()` is unset. Do not assume per-visitor isolation without checking.

**JWT** is the only security boundary — it signs outgoing requests to Document Server and validates the
incoming `Authorization` header on the callback and download routes. Most examples have **no enable flag**:
JWT is on iff the secret is non-empty (csharp, csharp-mvc, java, java-spring, php, php-laravel, python, ruby).
Only nodejs (`token.enable`) and go (`JWT_IS_ENABLED`) have an explicit switch. Secret and header name must
match the Document Server's.

**Users** are a hardcoded fixture of four: three permissioned users plus an anonymous one, whose permission
sets are what the demo actually demonstrates (review-only, comment-only, filler, protect). Most examples
identify them as `uid-1`…`uid-3` / `uid-0`; java-spring instead uses an integer database id in a `uid` cookie
(1–4, where 4 is Anonymous).

**Formats** come from the `document-formats` submodule via a per-example `FormatManager`. Exceptions:
java-spring and php-laravel have no `document-formats` submodule and get the table from the external
`docs-integration-sdk` instead. nodejs additionally fetches `meta/formats` and `meta/config` from the running
Document Server at runtime and caches them for an hour — no other example does.

**WOPI exists only in nodejs.** All ten agents verified this independently; a WOPI change is never ported.

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
