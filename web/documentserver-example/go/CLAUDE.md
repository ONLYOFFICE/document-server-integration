# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this directory.

## What this is

The Go reference implementation of ONLYOFFICE Docs integration: a minimal document management system that
opens files in Document Server, receives save callbacks, and keeps version history. One of 11 sibling
implementations of the same application (see `../` for `nodejs`, `java`, `php`, `python`, `ruby`, …); they
share no code.

Deliberately unhardened: no authorization, no validation of link parameters, no check of who calls
`/callback`, `/download` and `/remove` open to anyone. That is intentional — do not "fix" it as if this were
production code. Real bugs (path traversal past the storage root, crashes, leaks) are still worth fixing.

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

**JWT** (`managers/default/jwt.go`) is the only security boundary, active only when `JWT_IS_ENABLED` is true
*and* `JWT_SECRET` is non-empty. Outgoing payload structs embed `jwt.RegisteredClaims` directly and are signed
HS256; incoming callbacks are read from the `JWT_HEADER` header (`Bearer ` prefix stripped by
`strings.Split(theader, " ")[1]`), falling back to the body `token`.

**Formats** come solely from the `document-formats` submodule JSON — no runtime `meta/config` / `meta/formats`
fetch (nodejs has one, this example does not). Which buttons appear per file on the index page comes from
`FormatManager.GetActions(ext)`.

**Users** — the same four-user fixture as the siblings, in `managers/default/user.go`: `uid-1`…`uid-3` plus
`uid-0` (anonymous), each with a `Description` slice rendered on the index page. The current user is the
`uid` cookie (default `uid-1`); `GetUserById` recurses to `uid-0` for anything unknown, which is
special-cased: strict co-editing, no mentions/protect/user info, AI plugin disabled by GUID, feature tips on.

## What makes this example unique

- **uber-go/fx dependency injection.** Every component is an interface in a `config.go` plus a `Default…`
  implementation in a `default/` subpackage, provided through a `module.go`. Constructors return interfaces,
  so fx resolves by type — a duplicate or missing provider is a startup graph error, not a compile failure.
- **One file per endpoint** in `server/api/default/` — the root CLAUDE.md points agents here as the clearest
  map of the endpoint set in the repository — and **pluggable callback handlers** keyed by status code in a
  registry, rather than a switch inside the callback endpoint as in most siblings.
- **There is no WOPI here** (nodejs is the only example with it) — do not look for a counterpart.

## Conventions

- Every source file starts with the Apache-2.0 ONLYOFFICE header (`(c) Copyright Ascensio System SIA <year>`)
  as a `/** … */` block above `package`. New files must include it.
- `.golangci.yml` is `enable-all` minus a long disable list; `lll` 120, `gofmt.simplify: false`, `errcheck`
  excludes `(net/http.ResponseWriter).Write`. Deliberate unchecked errors carry `// nolint: errcheck`.
- **User-visible changes need a CHANGELOG entry at the repository root** (`../../../CHANGELOG.md`), under the
  top unreleased `##` heading, prefixed `go: <what changed>`. Release tagging parses that file.
- Commits use Conventional Commits with the example as scope: `feat(go): …`, `fix(go): …`.
- Features here are usually ported to the other 10 examples in separate commits. When adding one, expect that
  follow-up rather than editing sibling directories in the same change.
- `VERSION` in `config/configuration.json` tracks the release (bump with the siblings' config files) and is
  surfaced as `serverVersion` in history metadata and on the index page.

## Gotchas

- `NewDefaultFormatManager` returns an error if `static/assets/document-formats/onlyoffice-docs-formats.json`
  is missing, and fx aborts startup. An uninitialised submodule looks like a DI failure, not a missing file.
- `SERVER_ADDRESS` is honoured **only** in `editor.go` and `convert.go`. `/config`, `/reference` and
  `/historyObj` always call `generateUrl(r)`, and `index.go` passes bare `r.Host` (no scheme) into
  `GetStoredFiles`. Behind a proxy or with a non-default external address these disagree.
- The history-metadata timestamp layout is `"2006-02-1 15:04:05"` (in `create.go`, `upload.go`, `convert.go`,
  `restore.go`). In Go's reference layout that is year-**day**-month; changing it means changing all four.
- `utils.IsInList` has no callers, and `ConversionManager.GetInternalExtension` is declared and implemented
  but never invoked — both are dead code.
- `/convert` sends `thumbnail.first = false`, which is what produces one image per page when converting to
  JPG/PNG/BMP/GIF. Do not "simplify" it away.
- `RemoveAll` deletes the entire storage root directory, `DELETE /remove` is unauthenticated, and
  `CreateDirectory` uses mode `0777`.
