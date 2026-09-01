# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## What this is

The PHP/Laravel 11 reference implementation of ONLYOFFICE Docs integration: a minimal document management
system that opens files in Document Server, receives save callbacks, and keeps version history. One of 11
sibling implementations of the same application under `../`; they share no code, and `../php` is a *separate*,
framework-less example — never treat the two as one codebase.

Deliberately unhardened: no authorization, no validation of link parameters, permissive CORS
(`Access-Control-Allow-Origin: *` on the download endpoints). That is intentional — do not "fix" it as if this
were production code. Real bugs (path traversal past the storage root, crashes, leaks) are still worth fixing.

## Commands

```bash
make install         # composer install && npm install && npm run build && php artisan key:generate
make compose-start   # docker compose build && up (nginx :80, Document Server via proxy :8080); stop: compose-stop
npm run build        # vite build -> public/build (required; the blades use @vite)

./vendor/bin/pint            # Laravel Pint — the linter (auto-fix)
./vendor/bin/pint --test     # what CI runs (.github/workflows/lint-php-laravel.yml, PHP 8.2)
./vendor/bin/phpunit                                  # whole suite (or: php artisan test)
./vendor/bin/phpunit tests/Feature/ExampleTest.php    # one file
./vendor/bin/phpunit --filter test_the_application_returns_a_successful_response   # one test
```

No `pint.json`, so Pint runs its default `laravel` preset; CI installs it globally and calls bare `pint --test`.
Lint is the only CI gate — phpunit runs in no workflow, and `tests/` holds only the two Laravel scaffold tests.
`php artisan serve` works once `.env` and `APP_KEY` exist. Submodules must be initialised or "Create new"
breaks: `git submodule update --init --recursive` — this example wires in **only**
`public/assets/document-templates`; unlike php/python/ruby/go/java it has no `document-formats` submodule.

## Configuration

There is **no `config/onlyoffice.php`**: every ONLYOFFICE setting is read straight from the environment by
`app/OnlyOffice/Managers/SettingsManager.php`, which builds one nested array in its constructor. `config/*.php`
is stock Laravel except `config/filesystems.php`, which declares the custom disks (`files`, `forcesaved`, and
an unused `formats`). Setup: copy `.env.example` to `.env`, then `php artisan key:generate` — Laravel refuses
to boot without `APP_KEY`.

Variables that matter: `DOCUMENT_SERVER_PUBLIC_URL` / `_PRIVATE_URL` (browser-facing vs server-to-server),
`DOCUMENT_STORAGE_PUBLIC_URL` / `_PRIVATE_URL` (how Document Server reaches *this* app — callback and download
URLs are built from the private one), `DOCUMENT_SERVER_JWT_{SECRET,HEADER,ALGORITHM,EXPIRATION,USE_FOR_REQUEST}`,
`DOCUMENT_STORAGE_DISPLAY_FORGOTTEN_PAGE`, and `DOCUMENT_SERVER_VERSION` (the release version, rendered as the
`server-version` meta tag; bump it with the siblings' configs). The `http://documentserver` placeholder is
**not** in a config file here — it is the fallback default of `env('DOCUMENT_SERVER_PUBLIC_URL', …)` in
`SettingsManager`; `.env.example` ships `http://localhost:8080` / `http://proxy:8080` to match `docker-compose.yml`.

## Code map

`routes/web.php` is the spine — everything sits inside one `EnsureUserDirectoryExists` group. `IndexController`
`/`; `FileController` `/files` + `/upload` `/convert` `/delete` `/saveas` `/history` `/rename` `/config`
`/download`; `VersionController` `/files/versions/{changes,previous}`; `ForgottenController` `/files/forgotten`;
`API/FormatController` `/api/formats`; `API/Files/VersionController` `PUT /api/files/versions/restore`;
`API/Files/ReferenceController` `POST /api/files/reference`; `EditorController` `/editor`, `POST /editor/track`.

**Middleware is where JWT actually lives** (`app/Http/Middleware/`) — handlers hold no inline token checks:

- `EnsureUserDirectoryExists` — `Storage::disk('files')->makeDirectory($request->ip())` on every request.
- `EnsureJWTTokenIsPresent` — guards `/files/download` and `/files/versions/*`: reads the configured header,
  strips `Bearer `, decodes; `abort(499)` when the header is missing, `abort(498)` on a bad signature.
- `CheckAndDecodeJWTPayload` — guards `/editor/track`: takes the token from the body (`token`) or the header
  and **merges the decoded payload into the request as `payload`**. `EditorController@track` reads
  `$request->input('payload')`, not the raw body, so an unsigned callback body never reaches the handler.
- `EnsureForgottenPageEnabled` — `abort(403)` unless `DOCUMENT_STORAGE_DISPLAY_FORGOTTEN_PAGE` is truthy.

All three JWT middlewares skip validation entirely when `dmode` is on the query string (embedded mode).

Below the controllers, `app/UseCases/**` holds one invokable class per operation (`__invoke(Request $dto)`);
`app/Repositories/` owns storage (`FileRepository` = the `files` disk, `VersionRepository` = the history tree,
`ForceSavedFilesRepository`, `UserRepository` = the user fixture); `app/OnlyOffice/Managers/` and
`Services/CallbackService.php` (the `/track` state machine) all extend SDK base classes;
`app/Models/Editor/Editor::open()` is the single place per-user permissions and customization are decided; and
`app/Helpers/URL/FileURL` builds every Document Server-facing URL.

## The integration model

**`/track` is the core.** Document Server POSTs a callback with a `status`: `1` editing, `2`/`3`
MustSave/Corrupted, `4` closed without changes, `6`/`7` MustForceSave/CorruptedForceSave. Dispatch lives in the
SDK's abstract `CallbackService::processCallback`; this example implements only the five `processTrackerStatus*`
hooks. Local specifics: `1`/`4` share a handler issuing a *forcesave* command when the last action's type is `0`
and that user is no longer in `users`; `6`/`7` write a force-save, or — when `forcesavetype` is `SUBMIT_FORM` —
also download `formsDataUrl` and run `SaveDocumentFormCommand`. A callback URL whose extension differs from the
stored file's goes through `ConvertCommand` first.

**Storage layout** — partitioned by `$request->ip()`, rooted at `storage/app/public/`; it does **not** match
the layout the other examples use:

```
files/<client IP>/<filename>                            current version
files/<client IP>/<filename>-history/1/                 info.json  history.json  changes.zip  1.<ext>
forcesaved/<client IP>/<filename>                       force-saved copy (separate disk)
```

`info.json` holds `key` (a fresh UUID per save), `fileType`, `version`, `created`, `user`, `serverVersion`;
there is no `prev.<ext>`/`diff.zip`/`changes.txt`/`key.txt`/`user.txt`. `N.<ext>` is the content *as of* version
N, so `/files/versions/previous` serves `version - 1`, and the current version number is the directory count
under `<filename>-history`. Force-saved copies are deleted when a real save lands.

**JWT** is the only security boundary, and there is no separate enable flag: `jwt.enabled` is derived as
`DOCUMENT_SERVER_JWT_SECRET && DOCUMENT_SERVER_JWT_USE_FOR_REQUEST`. Outgoing signing happens in
`EditorController` (config, `dataInsertImage`, `dataDocument`, `dataSpreadsheet`), `FileController@config` and
`ReferenceController`; incoming validation is entirely in middleware. **Formats** come from the SDK's
`FormatsManager`, not a `document-formats` submodule; **forgotten files** likewise are not local —
`FindAllForgottenFilesQueryHandler` calls the command service (`getForgottenList` / `getForgotten`).

**Users** — four hardcoded in `app/Repositories/UserRepository.php`: `uid-1` John Smith, `uid-2` Mark Pottato
(group-2), `uid-3` Hamish Mitchell (group-3), `uid-0` anonymous; their permission sets are what the demo
demonstrates. Special cases live in `EditorController@index` (uid-3 loses history close/restore; uid-0 gets no
mentions, protect list or user info) and `Editor::buildEditorConfig` (only uid-1 submits forms; uid-0 disables
the AI plugin through `plugins.disable`).

## What is Laravel-specific here

- `bootstrap/app.php` is the wiring point: `routes/web.php`, health route `/up`, CSRF exemptions for
  `/editor/track` and `/files/saveas`, `ulang` excluded from cookie encryption; `AppServiceProvider` binds only
  `FormatManager`, as a singleton.
- Vite (`vite.config.js`; entries `resources/js/{app,forgotten,formats}.js`) builds into the gitignored
  `public/build/`. The blades use `@vite([...])`, so a fresh checkout errors until `npm run build` has run.
- `docker/` + `docker-compose.yml` bring up `example` (php:8.2-fpm), `proxy` (nginx — `:80` for the app, `:8080`
  reverse-proxying Document Server), `documentserver` (`onlyoffice/documentserver:9.3`, `JWT_SECRET` from
  `DOCUMENT_SERVER_JWT_SECRET`) and `node`, sharing one named volume — so install dependencies *inside* the
  containers, not on the host.
- Depends on the external SDK **`onlyoffice/docs-integration-sdk` pinned to `1.4`** (1.4.0 in `composer.lock`);
  `SettingsManager`, `JWTManager`, `DocumentManager`, `FormatManager` and `CallbackService` all extend its
  classes — read the base under `vendor/onlyoffice/docs-integration-sdk/src/` before changing their behaviour.
  **No WOPI here**: `nodejs` is the only one of the 11 that implements it, so never port WOPI changes.

## Conventions

- Every source file should start with the Apache-2.0 ONLYOFFICE header (`(c) Copyright Ascensio System SIA
  <year>`). Coverage is partial — roughly a quarter of `app/**.php` has it, and the Laravel-scaffolded files
  (middleware, providers, `bootstrap/`, `routes/`) do not. New files must include it.
- Run `./vendor/bin/pint` before finishing; `pint --test` is the CI gate.
- **User-visible changes need a CHANGELOG entry at the repository root** (`../../../CHANGELOG.md`) under the top
  unreleased `##` heading, prefixed with the example name: `php-laravel: <what changed>`. Release tagging parses
  that file. Commits use Conventional Commits with the example as scope: `feat(php-laravel): …`.
- Features here are usually ported to the other 10 examples in separate commits — expect that follow-up rather
  than editing sibling directories in the same change.

## Gotchas

- `vendor/` and `node_modules/` are **gitignored, not committed**; so are `public/build/`, `public/storage/`,
  `.env` and `storage/*.key`. A fresh checkout needs `composer install`, `npm install`, `npm run build`.
- `SettingsManager` and `EnsureForgottenPageEnabled` call `env()` at runtime outside `config/`, so
  `php artisan config:cache` silently breaks every ONLYOFFICE setting — do not add it to any recipe.
- `SettingsManager` sets `'jwtPrefix' => env('DOCUMENT_SERVER_JWT_HEADER', 'Bearer ')` — the *header* variable,
  not a prefix one; with the default `.env` the prefix becomes `Authorization`. The middlewares sidestep it by
  hardcoding `Str::after($header, 'Bearer ')`.
- No `trustProxies()` is configured, so behind the bundled nginx proxy `$request->ip()` is the proxy's address
  and every visitor shares one storage directory.
- `app/Repositories/FormatRepository.php` is dead code (nothing constructs it), and the `formats` disk points at
  `public/assets/document-formats`, which has no submodule here and is never read.
- `storage/` must be writable by the web server — the php-fpm Dockerfile does
  `chgrp -R www-data ./storage && chmod -R 775 ./storage` for exactly this reason.
- `DOCUMENT_SERVER_JWT_SECRET=secret` in `.env.example` is a placeholder that `docker-compose.yml` also feeds to
  the Document Server container, so both sides must change together.
