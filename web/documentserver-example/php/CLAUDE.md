# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## What this is

The plain-PHP reference implementation of ONLYOFFICE Docs integration: a minimal document management system
that opens files in Document Server, receives save callbacks, and keeps version history. It is one of 11
sibling implementations of the same application (see `../` for `go`, `java`, `nodejs`, `php-laravel`,
`python`, `ruby`, …); they share no code. **No framework** — routing, templating and storage are hand-rolled.

Deliberately unhardened: no authorization, no validation of link parameters, no check of who calls `/track`,
`Access-Control-Allow-Origin: *` on downloads. That is intentional — do not "fix" it as if this were
production code. Real bugs (path traversal past the storage root, crashes, leaks) are still worth fixing.

## Commands

```sh
make dev              # composer install (with dev deps); make prod = --no-dev
make server-dev       # php --server $ADDRESS:$PORT  (defaults localhost:9000)
make server-prod      # php-fpm --fpm-config php-fpm.conf (defaults 0.0.0.0:9000)
make lint             # ./vendor/bin/phpcs src index.php
make test             # ./vendor/bin/phpunit --test-suffix "Tests.php" ... src
make compose-prod     # docker compose build && up --detach; make restart recreates + drops the volume
```

No `compose-dev` and no lint-fix recipe here (unlike python/ruby). Needs PHP 8.2 (`.php-version` 8.2.11) and
Composer 2.6.5 (`.composer-version`).

Tests are colocated with the sources as `src/**/*Tests.php`. There is **no `phpunit.xml`**, so a bare
`./vendor/bin/phpunit` does nothing — the suffix and the `src` path must be on the command line. Single file:
`./vendor/bin/phpunit src/format/FormatManagerEditableTests.php`; add `--filter testLoads` for one case.

Submodules must be initialised or file creation and format classification break:
`git submodule update --init --recursive` (pulls `assets/document-templates` and `assets/document-formats`;
both directories exist but are empty in a fresh clone).

## Configuration

**Environment variables only** — no config file. Everything is read lazily through `getenv()` in
`src/configuration/ConfigurationManager.php`, with the defaults hardcoded there:

- `DOCUMENT_SERVER_PUBLIC_URL` — default `http://documentserver`, **the placeholder that must be replaced**;
  the URL handed to the browser. `DOCUMENT_SERVER_PRIVATE_URL` is the server-to-server address, falling back
  to the public one. `DOCUMENT_SERVER_API_PATH` / `_PRELOADER_PATH` / `_COMMAND_PATH` / `_CONVERTER_PATH`
  override the sub-paths.
- `EXAMPLE_URL` — address this example is reachable at *from* Document Server; unset, `serverPath()` falls
  back to `$_SERVER['HTTP_HOST']`.
- `JWT_SECRET` (empty = JWT disabled), `JWT_HEADER` (`Authorization`), `JWT_EXPIRES_IN` (5, **minutes**),
  `JWT_USE_FOR_REQUEST` (true), `SSL_VERIFY_PEER_MODE_ENABLED`.
- `STORAGE_PATH` (`storage`, relative to the example root), `SINGLE_USER`, `MAXIMUM_FILE_SIZE` (100 MB),
  `CONVERSION_TIMEOUT` (120000), `ENABLE_FORGOTTEN` (true), `ADDRESS`, `PORT`.

`ConfigurationManager::$version` (`1.15.0`) is the only place the release version lives — `composer.json` has
no `version` field. `languages()` there is the hardcoded language list for the index page.

## Code map

`index.php` is the single entry point (nginx does `try_files … /index.php?$query_string`). It requires the
three legacy procedural files, calls `configure()` (installs a process-wide `stream_context_set_default` SSL
bypass when `SSL_VERIFY_PEER_MODE_ENABLED`), then `routers()` — a flat chain of
`str_starts_with($path, '/…')` checks dispatching to a function or a View and JSON-encoding the result.

**Old, procedural half** — namespace `Example`, plain functions, *not* autoloaded, `require_once`d:

- `src/functions.php` — storage paths (`getStoragePath`, `getHistoryDir`, `getVersionDir`, `getFileVersion`,
  `getForcesavePath`), URL building (`serverPath`, `fileUri`, `getCallbackUrl`, `getDownloadUrl`,
  `getHistoryDownloadUrl`, `getCreateUrl`), conversion (`sendRequestToConvertService`, `getConvertedData`),
  plus `getHistory`, `createMeta`, `getStoredFiles`, `getForgottenFiles`, `sendlog`.
- `src/ajax.php` — one function per endpoint (`upload`, `track`, `convert`, `download`, `historyDownload`,
  `reference`, `restore`, `config`, …) plus `downloadFile`/`delTree`. `src/trackmanager.php` — `readBody`
  (JWT extraction), `processSave`, `processForceSave`, `commandRequest`.

**Newer, PSR-4 half** — autoloaded per `composer.json`:

- `Example\Common\` → `src/common/`: `Path`/`URL` value objects (join, normalize) and the `HTTPStatus` enum;
  most of the test suite covers these. `Example\Configuration\` → `ConfigurationManager` (above).
- `Example\Format\` → `src/format/`: `FormatManager` deserializes
  `assets/document-formats/onlyoffice-docs-formats.json` with symfony/serializer and answers
  `all`/`viewable`/`editable`/`fillable`/`convertible` (+ `…Extensions()`) and `formatActions()`.
- `Example\Helpers\` → `src/helpers/`: `JwtManager` (firebase/php-jwt, HS256), `Users` (DTO), `ExampleUsers`
  (the hardcoded fixture plus the per-user description bullets shown on the index page).
- `Example\Proxy\` → `src/proxy/`: `ProxyManager::resolveURL()` rewrites a URL whose scheme/host/port match
  the *public* Document Server URL onto the *private* one — what the docker `proxy` service exists for.
- `Example\Views\` → `src/views/`: `View` base plus `IndexView`, `IndexStoredListView`, `DocEditorView`,
  `ForgottenFilesView`. Templating is `str_replace("{tag}", …)` over `templates/*.tpl` — no engine.
  `DocEditorView` assembles and signs the editor config object.

## The integration model

**Routes**: `/` index, `/editor`, `/config`, `/upload`, `/convert`, `/download`, `/delete`, `/files`,
`/history`, `/objhistory`, `/restore`, `/rename`, `/reference`, `/saveas`, `/formats`, `/forgotten`,
`/track`. Differences from the general pattern: history-as-object is `/objhistory` (not `/historyObj`), there
is a `/delete` route (GET one file, DELETE all) and a `/saveas` route, and there is **no `/create` route** —
`getCreateUrl()` points at `/editor?fileExt=…&user=…`, which creates the file and redirects.

**`/track` is the core.** `track()` maps status `0` NotFound, `1` Editing, `2` MustSave, `3` Corrupted,
`4` Closed, `6` MustForceSave, `7` CorruptedForceSave. `2`/`3` → `processSave`, `6`/`7` → `processForceSave`;
`1` issues a `forcesave` command request when a user finished editing; `0` and `4` do nothing. `processSave`
downloads the callback `url`, creates the next version directory, `rename()`s the current file into it as
`prev.<ext>`, writes the new content in place, and deletes any forcesave file. `processForceSave` with
`forcesavetype == 3` (SubmitForm) writes `<name>-form.<ext>` into normal storage and saves `formsdataurl` as
`<name>.txt`.

**Storage layout** — partitioned by client IP, versions are directories. Names differ from nodejs: history
suffix **`-hist`** (not `-history`), **`changes.json`** (not `changes.txt`), one **`createdInfo.json`** at the
history root (not a per-version `user.txt`), and the **forcesave copy inside the history directory**:

```
<STORAGE_PATH>/<user host address>/
  <filename>                    current version
  <filename>-hist/
    <filename>                  forcesaved copy
    createdInfo.json            {created, uid, name} for version 1
    1/  prev.<ext>  diff.zip  changes.json  key.txt
```

`getCurUserHostAddress()` returns `""` when `SINGLE_USER=true`, collapsing everything into one folder.

**JWT** is the only security boundary. `isJwtEnabled()` is simply "is `JWT_SECRET` non-empty". `/download`
and `/history` validate the `JWT_HEADER` header (`Bearer ` prefix stripped) and `die()` with 403; outgoing
config/history/command/convert payloads are signed. Unlike nodejs, the header name comes from local config
only — this example does **not** fetch `meta/config` or `meta/formats` from Document Server; formats come
solely from the `document-formats` submodule.

**Users** — four hardcoded users in `src/helpers/ExampleUsers.php`: `uid-1`…`uid-3` plus `uid-0` (anonymous,
name requested at editor open). Their permission sets are what the demo demonstrates. `uid-1` is also the
only user for whom the Submit-form button appears (`DocEditorView`).

## What is unique here

- **No framework, no router library.** A new endpoint = a `str_starts_with` branch in `index.php` plus a
  function in `src/ajax.php`. A new page = a `*.tpl` file plus a `View` subclass. Config lives in code, as
  `getenv()` defaults in `ConfigurationManager`; there is nothing to edit but the environment.
- **Production runs php-fpm behind nginx**: `php-fpm.conf` (`${ADDRESS}:${PORT}`, `clear_env = no` so the env
  config reaches workers, 100M upload limits) + `proxy/nginx.conf` (`:80` the example, `:8080` reverse-proxies
  Document Server) + `Dockerfile` (targets `example`, `proxy`) + `docker-compose.yml`. `make server-dev`
  instead uses PHP's built-in server, which serves `assets/` directly and needs no nginx.
- **No WOPI here** — nodejs is the only example implementing WOPI. Do not look for a counterpart and do not
  port WOPI changes into this directory.

## Conventions

- Every source file starts with the Apache-2.0 ONLYOFFICE header (`(c) Copyright Ascensio System SIA <year>`);
  `licenses-php.yml` checks licenses in CI. Its position relative to `namespace` varies between files.
- **phpcs**, per `phpcs.xml`: `PSR2`, plus `Generic.Files.LineLength` at 120 (no absolute limit), plus
  `Squiz.NamingConventions.ValidVariableName` with `PublicHasUnderscore` and `PrivateNoUnderscore` muted. It
  checks exactly `src` and `index.php` — `templates/` and `assets/` are not linted. `lint-php.yml` runs
  `phpcs src index.php` on changes under `web/documentserver-example/php/**`. CI runs lint, license check and
  artifact build only — **`make test` is not wired into any workflow**.
- **User-visible changes need a CHANGELOG entry at the repository root** (`../../../CHANGELOG.md`), under the
  top unreleased `##` heading, prefixed `php: `. Release tagging parses that file.
- Commits use Conventional Commits with the example as scope: `feat(php): …`, `fix(php): …`. Features here are
  usually ported to the other 10 examples in separate commits — expect that follow-up rather than editing
  sibling directories (`php-laravel` included) in the same change.

## Gotchas

- `index.php` opens with `<?php // phpcs:ignore PSR1.Files.SideEffects.FoundWithSymbols` — the file both
  declares functions and calls them; keep the comment on line 1. `routers()` also sets
  `Content-Type: application/json` for **every** route up front, HTML routes overriding it afterwards (there
  is a `TODO: delete fallback` comment on it).
- `readBody()` assigns `$data = ""` before checking `$data["token"]`, so the "JWT in body" branch is
  unreachable and a token is only ever accepted from the header. The check is also gated on
  `tokenUseForRequest()`, so `JWT_USE_FOR_REQUEST=false` disables incoming validation on `/track` entirely.
- `getCurUserHostAddress()` calls `preg_replace("[^0-9a-zA-Z.=]", '_', …)`. PHP reads `[`/`]` as the pattern
  delimiters, so the intended character class is not one and the sanitisation is a no-op — while
  `$userAddress` comes straight from the `/track` and `/download` query strings. `getClientIp()` does it
  correctly with `/…/` delimiters. `download()` likewise reads `$_GET["&dmode"]`, a key that can never be set.
- `src/configuration/ConfigurationManagerJWTUseForRequest.php` is a real `TestCase` whose name does not end in
  `Tests.php`, so `make test` silently skips it. Test classes also live in `…\Tests` sub-namespaces the PSR-4
  map does not cover; they load only because phpunit includes the file directly.
- `IndexView::getStoredListLayout()` is dead code (constructs `IndexStoredListView` with no arguments); the
  live one is `IndexStoredListView::getStoredListLayout()`.
- `View::__construct` loads templates via the relative path `./templates/<name>.tpl` and `sendlog()` writes to
  `logs/webedior-ajax.log`, both relative to cwd — run from the example root or pages render empty with no
  error. Logging happens on every request and includes full request bodies.
