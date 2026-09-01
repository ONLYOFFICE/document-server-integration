# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this directory.

## What this is

The Python/Django reference implementation of ONLYOFFICE Docs integration: a minimal document management
system that opens files in Document Server, receives save callbacks, and keeps version history. One of 11
sibling implementations of the same application (see `../` for `go`, `java`, `nodejs`, `php`, `ruby`, …);
they share no code.

Locally open by design: nothing checks who calls `/track`, and `Access-Control-Allow-Origin: *` is set on
`/downloadhistory`. See *Security posture* in `../../../CLAUDE.md` — this is intentional, and not something to
harden.

## Commands

```sh
make dev           # pip install --editable .[development]  (flake8, mypy, types-requests); make prod for deps only
make server-dev    # python manage.py runserver     (server-prod = same, with DEBUG=false)
make lint          # flake8;  make types = mypy . — NOT run in CI, see Gotchas
make test          # python -m unittest ./src/**/*_tests.py
make compose-dev   # docker compose -f compose-base.yml -f compose-dev.yml build && up -d  (or compose-prod)
python -m unittest src.format.format_tests                                       # one test file
python -m unittest src.format.format_tests.FormatTests.test_generates_extension  # one test case
```

Port from `$PORT` (default 8000), address from `$ADDRESS` — else `127.0.0.1` when `DEBUG`, `0.0.0.0` otherwise.
Tests are `*_tests.py` colocated with sources, plain `unittest` (no pytest, no Django test runner); only
`src/common`, `src/configuration`, `src/format`, `src/memoize`, `src/proxy` have any, and `make test` needs
POSIX shell globbing, so prefer the module form above elsewhere. Submodules must be initialised or file
creation and format detection break: `git submodule update --init --recursive` (pulls
`assets/document-templates` and `assets/document-formats`; no `plugin-aiautofill` — that one is nodejs-only).

## Configuration

**Environment variables only — no config file.** Every key and default lives in `ConfigurationManager`
(`src/configuration/configuration.py`); the README documents a subset. `DOCUMENT_SERVER_PUBLIC_URL` (default
`http://documentserver` — the placeholder that must be replaced), `DOCUMENT_SERVER_PRIVATE_URL` (falls back to
the public one), `EXAMPLE_URL` (address this example is reachable at *from* Document Server), `STORAGE_PATH`
(default `<example>/storage`), `MAXIMUM_FILE_SIZE`, `DEBUG`, `ADDRESS`, `PORT`, `ENABLE_FORGOTTEN`,
`SSL_VERIFY_PEER_MODE_ENABLED`, `CONVERSION_TIMEOUT`, `DOCUMENT_SERVER_{API,PRELOADER,COMMAND,CONVERTER}_PATH`.
JWT: `JWT_SECRET` (**an empty secret is what disables JWT** — there is no separate enable flag, unlike the
nodejs example's `token.enable`), `JWT_HEADER` (`Authorization`), `JWT_EXPIRES_IN` (minutes, 5),
`JWT_USE_FOR_REQUEST` (true).

## Code map

Two generations sit side by side. `src/utils/` is the older layer: procedural, `camelCase`, module-level
functions, no type hints, license header inside a module docstring. The single-purpose packages (`src/common`,
`src/configuration`, `src/format`, `src/memoize`, `src/proxy`, `src/response`) are newer: `snake_case`, typed,
class-based, each with an `__init__.py` re-export and a colocated `*_tests.py`. Follow the newer style; do not
reformat `src/utils` wholesale.

- `manage.py` — **not a stock Django layout.** No settings module, no `urls.py`: it calls
  `settings.configure(**configuration())`, points `ROOT_URLCONF` at itself, builds `urlpatterns` in `routers()`,
  and installs one middleware, `historyManager.CorsHeaderMiddleware` (adds `Access-Control-Allow-Origin: *` to
  `/downloadhistory` only).
- `src/views/actions.py` — ~760 lines holding every endpoint except the two pages; `edit()` assembles the whole
  editor config and renders `templates/editor.html`. `index.py` is the file list; `forgotten.py` drives the
  `getForgottenList` / `getForgotten` / `deleteForgotten` commands.
- `src/utils/docManager.py` — storage paths (`getStoragePath`, `getHistoryPath`, `getForcesavePath`), URL
  building (`getServerUrl`, `getFileUri`, `getCallbackUrl`, `getDownloadUrl`), `generateFileKey`,
  `getStoredFiles`, `download()` (Content-Type via `python-magic`). `historyManager.py` — history tree paths,
  `createMeta`/`getMeta`, `getHistoryObject` (the `history` + `historyData` payload), `CorsHeaderMiddleware`.
- `src/utils/trackManager.py` — `readBody` (JWT unwrap), `processSave`, `processForceSave`, `commandRequest`,
  `resolve_process_save_body` (rewrites callback URLs through `ProxyManager`). Alongside it
  `serviceConverter.py`, `jwtManager.py` (HS256 via PyJWT), `fileUtils.py`, and `users.py` — the fixture plus
  `getUserFromReq` (`uid` cookie) and `find_user` by id, used by `/restore`.
- `src/format/format.py` — `Format` (`msgspec.Struct`) and `FormatManager` over
  `assets/document-formats/onlyoffice-docs-formats.json`; `all()` is wrapped in `src/memoize`. `src/proxy` —
  `ProxyManager.resolve_url`; `src/response` — `ErrorResponse`; `src/common` — `http.GET()`/`POST()`/`PUT()`
  method guards and `string.boolean`.

## The integration model

**Routes** (`manage.py:routers()`): `/`, `/edit`, `/config`, `/upload`, `/create`, `/convert`, `/download`,
`/downloadhistory`, `/files`, `/historyobj`, `/restore`, `/rename`, `/reference`, `/formats`, `/remove`,
`/saveas`, `/csv`, `/assets`, `/forgotten`, `/deleteforgotten`, `/track`. Names differ from nodejs: `/edit` not
`/editor`, `/historyobj` with no separate `/history`, `/remove` rather than a DELETE, no `/data` endpoint.

**`/track` is the core.** `actions.track` switches on the status: `1` editing (if `actions[0].type == 0` and
that user is gone from `users`, fire a `forcesave` command), `2`/`3` MustSave/Corrupted → `processSave`,
`6`/`7` MustForceSave/CorruptedForceSave → `processForceSave`; status `4` is not handled. The response is
always `{"error": 0}` / HTTP 200 or `{"error": 1, "message": …}` / HTTP 500. `forcesavetype == 3` (SubmitForm)
writes `<name>-form<ext>` as a new file and dumps `formsdataurl` into a sibling `.txt`.

**Storage layout** — partitioned by `REMOTE_ADDR`, and it differs from the other examples:

```
<STORAGE_PATH>/<REMOTE_ADDR>/
  <filename>                    current version
  <filename>-hist/              note: -hist, not -history
    createdInfo.json            author + creation time (history root, not per version)
    <filename>                  the forcesaved copy, if any
    1/  prev.<ext>  diff.zip  changes.json  key.txt
```

No `user.txt`, no `changes.txt`; `getFileVersion` counts subdirectories, so the tree is the source of truth.

**JWT** is the only security boundary. `/track` unwraps the body token or the `JWT_HEADER` bearer token;
`/download` and `/downloadhistory` check the header only when `dmode` is absent and `JWT_USE_FOR_REQUEST` is on.
Converter and command requests are signed twice: a header token over `{'payload': …}`, a body `token` over the
bare payload.

**Users** — `uid-1`…`uid-3` plus `uid-0` (anonymous), by the `uid` cookie; they also carry `roles`, `goback`,
`close`, `templates`, `avatar`. Only `uid-1` gets the Submit-form button. `uid-0` is special-cased throughout:
no chat, mentions, `createUrl` or `referenceData.fileKey`, strict coediting, feature tips, and the AI plugin
disabled by GUID `asc.{9DC93CDB-B576-4F0C-B55E-FCC9C48DD007}`.

## What is specific to this example

**Django, used minimally**: no apps, models, ORM, migrations or `settings.py`. Templates live in `templates/`;
`static/` is served through `django.conf.urls.static.static()`, `STATIC_URL` being the *relative* `static/`.

**Three-file compose.** `compose-base.yml` defines `documentserver`, `example` and `proxy`; `compose-dev.yml` /
`compose-prod.yml` only pick the Dockerfile target (`example-dev` vs `example-prod`), so always go through
`make compose-dev` / `make compose-prod` — a bare `docker compose up` misses the overlay and has no build
target. `compose-base.yml` also holds the `JWT_SECRET=your-256-bit-secret` placeholder and the pinned
`onlyoffice/documentserver` tag.

**`proxy/nginx.conf` is what makes compose work**: the example on `:80`, Document Server on `:8080`, so browser
and containers reach the same server by different addresses (`DOCUMENT_SERVER_PUBLIC_URL=http://localhost:8080`
vs `DOCUMENT_SERVER_PRIVATE_URL=http://proxy:8080`). **`ProxyManager.resolve_url` reconciles them**: a URL whose
scheme/host/port match the public URL is rewritten onto the private one, path and query preserved. It is applied
to `url`/`changesurl` of every `/track` callback; `actions.saveAs`, `actions.restore`, `actions.convert` and
`forgotten.getForgottenFiles` still do the same swap by hand with `str.replace` — prefer `ProxyManager`.

## Conventions

Repo-wide rules (licence header, commits, CHANGELOG, porting) are in `../../../CLAUDE.md`. Here, additionally:

- `src/utils` and `src/views` wrap the licence header in a module docstring, the newer packages use `#`
  comments — match the neighbours. `manage.py` is the one file without a header.
- `.flake8`: `max-line-length = 120`, `max-complexity = 15`, `F4*` ignored in `__init__.py` (the packages
  re-export with `import *`). Run `make lint` before finishing.
- The version is duplicated in `pyproject.toml`, `ConfigurationManager.version` and `configuration_tests.py`;
  bump all three together.

## Gotchas

- **mypy is configured but not in CI.** `.github/workflows/lint-python.yml` runs `make dev` + `make lint` only;
  `make types` is commented out behind `# TODO: Configure mypy`. Expect pre-existing errors.
- `python-magic` needs the native libmagic (`docManager.download` calls `magic.from_file`). The Dockerfile
  `apk add`s it; a bare `make dev` on Windows/macOS can import-fail without it.
- `ssl_verify_peer_mode_enabled()` defaults to `False` and goes straight into `requests(verify=…)`, so TLS
  certificates are **not** verified unless you opt in.
- `docManager.getFileUri` only feeds `generateFileKey`'s hash; use `getDownloadUrl` for anything a client or
  Document Server has to fetch. Behind a proxy set `EXAMPLE_URL` rather than relying on request headers.
