# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this directory.

## What this is

The Go reference implementation of ONLYOFFICE Docs integration: a minimal document management system that
opens files in Document Server, receives save callbacks, and keeps version history. One of 11 sibling
implementations of the same application (see `../` for `nodejs`, `java`, `php`, `python`, `ruby`, …); they
share no code.

Locally open by design: nothing checks who calls `/callback`, and `/download` and `/remove` are open to anyone. See
*Security posture* in `../../../CLAUDE.md` — this is intentional, and not something to harden.

## Commands

```bash
go mod download
go run main.go       # port from SERVER_PORT in config/configuration.json (3000)
gofmt -l .           # .golangci.yml sets simplify: false — do not run gofmt -s
golangci-lint run    # CI installs golangci-lint@latest on Go 1.23.10
```

There are no tests in this example — no `_test.go` files anywhere — and no Makefile or Dockerfile. CI is
`lint-go.yml` (golangci-lint) and `artifact-go.yml` (rsyncs the tree into a `Go Example` artifact).

**Run from the example root**: templates, the storage root and the "create new" templates are all resolved
relative to the working directory, and the `template.Must(...)` package vars in `server/api/default/api.go`
panic at import time if the cwd is wrong. Submodules must be initialised or the app will not boot:
`git submodule update --init --recursive` (`static/assets/document-templates`, `.../document-formats`).

## Configuration

Single flat JSON file, `config/configuration.json`, read by [viper](https://github.com/spf13/viper) into
`config.ApplicationConfig` (`config/module.go`); keys are SCREAMING_SNAKE with `mapstructure` tags.
`viper.AutomaticEnv()` is on, so **any key can be overridden by an env var of the same name**
(`DOC_SERVER_HOST=… go run main.go`). The config dir is located via `runtime.Caller(0)`, so it is found
regardless of cwd — unlike everything else.

Keys that matter most: `DOC_SERVER_HOST` (the `http://documentserver/` placeholder must be replaced),
`SERVER_ADDRESS` (address this example is reachable at *from* Document Server; empty = derive from the
request), `SERVER_PORT`, `JWT_IS_ENABLED` / `JWT_SECRET` / `JWT_HEADER` / `JWT_EXPIRES_IN` (minutes),
`STORAGE_PATH` (relative to `./static`), `PLUGINS` (a JSON *string*, not an object), `FORGOTTEN_ENABLED`,
`LOGGER_DEBUG` (zap Development vs Production logger).

`README.md` step 2 describes a `configuration.env` file — it does not exist; ignore it.

## Code map

- `main.go` — the whole wiring: one `fx.New(...)` call listing every provider and module;
  `server/config/initializer.go` is the fx start/stop lifecycle hook.
- `server/server.go` — `configureRouter()`, the single list of gorilla/mux routes plus the `/static/` file
  server; also owns the `http.Server`.
- `server/api/config.go` — the `ServerEndpointsHandler` interface (FileOperations / Info / Pages);
  `server/api/default/*.go` is **one file per endpoint**, each a method on `DefaultServerEndpointsHandler`.
  `api.go` holds the shared struct, parsed templates and `generateUrl(r)` (`X-Forwarded-Proto`/TLS+`r.Host`).
- `server/managers/config.go` — every manager interface in one place: `DocumentManager`, `HistoryManager`,
  `StorageManager`, `UserManager`, `JwtManager`, `ConversionManager`, `CommandManager`, `FormatManager`.
- `server/managers/default/` — implementations. `storage.go` owns all path/URI building (`GetRootFolder`,
  `GenerateFilePath`, `GenerateFilestoreUri`, `GeneratePublicFileUri`, `GenerateVersionedFilename`,
  `GenerateFileHash` = md5 of name+mtime, used as the document key); `document.go`'s `BuildDocumentConfig`
  assembles the editor config and permissions; `history.go` owns the version tree; `format.go` loads
  the submodule's `onlyoffice-docs-formats.json`; `command.go`/`conversion.go` call Document Server.
- `server/handlers/config.go` — `CallbackRegistry`, a `map[int]CallbackHandler` keyed by callback status;
  `server/handlers/default/` has one file per handled status.
- `server/shared/` — `const.go` (`ONLYOFFICE_HISTORY_POSTFIX = "-hist"`), `cookie.go` (`uid` / `ulang`
  cookies), `response.go` (the `{"error":0}` / `{"error":1}` replies Document Server expects).

## The integration model

**Routes**: `/` index, `/editor`, `/config`, `/upload`, `/create` (GET from template, POST from a URL),
`/convert`, `/download`, `/files`, `/history`, `/historyObj`, `/restore` (PUT), `/rename`, `/reference`,
`/formats`, `/forgotten`, `/remove` (GET one, DELETE all), and `/callback`.

**The save callback is `/callback`, not `/track`** — the biggest naming difference from the siblings. It
takes `?filename=&user_address=` and dispatches on the JSON `status` via `HandleIncomingCode`.
**Only statuses 2, 3 and 4 are implemented** (`save.go`, `error.go`, `no_changes.go`); no handler for 1, and
**no force-save** — 6/7 are silently ignored and `Customization.Forcesave` is hardcoded `false`. Adding a
status = a new file in `server/handlers/default/` implementing `GetCode()`/`Handle()` and registering itself
in its constructor, plus an `fx.Invoke` line in that package's `module.go`.

**Storage layout** — unlike the general pattern, storage is **flat and global: it is not partitioned by
client IP**. Every visitor sees and edits the same files; `user_address` is accepted on the callback and
kept on the model but never used for pathing.

```
static/<STORAGE_PATH>/            (default: static/filestore)
  <filename>                      current version
  <filename>-hist/
    <filename>.json               metadata for the current version (author, created, serverVersion)
    1/  prev.<ext>  changes.json  key.txt  diff.zip
    2/  ...
```

The postfix is `-hist` and the per-version change file is `changes.json` (not `-history` / `changes.txt` /
`user.txt` as in nodejs). `/download` and `/history` do not stream bytes — they 301/303 redirect to the
`/static/...` URL, so the file server is what actually serves documents to Document Server.

**JWT** (`server/managers/default/jwt.go`) is the only security boundary, active only when `JWT_IS_ENABLED` is true
*and* `JWT_SECRET` is non-empty. Outgoing payload structs embed `jwt.RegisteredClaims` directly and are signed
HS256; incoming callbacks are read from the `JWT_HEADER` header (`Bearer ` prefix stripped by
`strings.Split(theader, " ")[1]`), falling back to the body `token`.

**Formats** come solely from the `document-formats` submodule JSON — no runtime `meta/config` / `meta/formats`
fetch (nodejs has one, this example does not). Which buttons appear per file on the index page comes from
`FormatManager.GetActions(ext)`.

**Users** — the same four-user fixture as the siblings, in `server/managers/default/user.go`:
`uid-1`…`uid-3` plus `uid-0` (anonymous), each with a `Description` slice rendered on the index page. The
current user is the `uid` cookie (default `uid-1`); `GetUserById` recurses to `uid-0` for anything unknown,
which is special-cased: strict co-editing, no mentions/protect/user info, AI plugin disabled by GUID,
feature tips on.

## What makes this example unique

- **uber-go/fx dependency injection.** Every component is an interface in a `config.go` plus a `Default…`
  implementation in a `default/` subpackage, provided through a `module.go`. Constructors return interfaces,
  so fx resolves by type — a duplicate or missing provider is a startup graph error, not a compile failure.
- **One file per endpoint** in `server/api/default/` — the root CLAUDE.md points agents here as the clearest
  map of the endpoint set in the repository — and **pluggable callback handlers** keyed by status code in a
  registry, rather than a switch inside the callback endpoint as in most siblings.

## Conventions

Repo-wide rules (licence header, commits, CHANGELOG, porting) are in `../../../CLAUDE.md`. Here, additionally:

- The licence header is a `/** … */` block above `package`.
- `.golangci.yml` is `enable-all` minus a long disable list; `lll` 120, `gofmt.simplify: false` (so do not run
  `gofmt -s`), `errcheck` excludes `(net/http.ResponseWriter).Write`. Deliberate unchecked errors carry
  `// nolint: errcheck`. Run `golangci-lint run` before finishing.
- `VERSION` in `config/configuration.json` is the release version, surfaced as `serverVersion` in history
  metadata and on the index page.

## Gotchas

- `NewDefaultFormatManager` returns an error if `static/assets/document-formats/onlyoffice-docs-formats.json`
  is missing, and fx aborts startup. An uninitialised submodule looks like a DI failure, not a missing file.
- `/convert` sends `thumbnail.first = false`, which is what produces one image per page when converting to
  JPG/PNG/BMP/GIF. Do not "simplify" it away.
