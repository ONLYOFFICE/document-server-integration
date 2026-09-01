# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this directory.

## What this is

The ASP.NET **WebForms** reference implementation of ONLYOFFICE Docs integration: a minimal document management
system that opens files in Document Server, receives save callbacks, and keeps version history. One of 11 sibling
implementations of the same application (see `../` for `go`, `java`, `nodejs`, `php`, `python`, …); they share no
code. `../csharp-mvc` is ASP.NET MVC — a *different* example, not another view of this one, but a near-duplicate
in behaviour that usually receives the same change in its own commit.

Locally open by design: nothing checks who calls the track endpoint beyond JWT, `Access-Control-Allow-Origin: *`
comes from `Web.config`, and `files.docservice.verify-peer-off` installs a `ServerCertificateValidationCallback`
accepting every TLS certificate process-wide. See *Security posture* in `../../../CLAUDE.md` — this is intentional,
and not something to harden.

## Commands

There is **no CLI build, no linter and no tests** here. Do not invent `npm`/`make` targets.

- Open `OnlineEditorsExample.sln` in Visual Studio (2019+) and build/run, or
  `msbuild OnlineEditorsExample.csproj /p:Configuration=Release` (needs `WebApplication.targets` from a VS
  install). NuGet uses **PackageReference**, so `dotnet restore` works but `dotnet build` does not.
- Web Application project targeting **.NET Framework 4.8** — `Web.config` still declares `targetFramework="4.5"`
  and the README still says 4.5; the csproj wins. IIS Express is disabled in the csproj; the supported way to run
  it is the README's — point an IIS site at this folder, app pool .NET CLR v4.0.
- CI: `.github/workflows/artifact-csharp.yml` **compiles nothing** — it inits submodules and rsyncs the source
  tree into `deploy/DotNet (Csharp) Example` as a downloadable artifact. `licenses-csharp.yml` runs
  `dotnet restore` (SDK 9) plus the ONLYOFFICE license check against `.check-licenses.yml`. No lint workflow.
- Submodules must be initialised or file creation and format detection break:
  `git submodule update --init --recursive` (pulls `assets/document-templates` and `assets/document-formats`).

## Configuration

`Web.config` points `<appSettings configSource="settings.config" />` at **`settings.config`**, so every tunable
lives there; `Web.config` only holds runtime plumbing (max request length, the CORS header, request filtering,
binding redirects). Everything is read via `WebConfigurationManager.AppSettings`.

- `files.docservice.url.site` — Document Server address, ships as the `http://documentserver/` **placeholder**
  and must be replaced; joined with `files.docservice.url.api` / `.preloader` / `.converter` / `.command`.
  `files.docservice.url.example` is where Document Server reaches *this* app; empty derives it from `Host`.
- `files.docservice.secret` — JWT secret. **There is no `token.enable` key**: `JwtManager.Enabled` is just
  "secret is non-empty". Plus `files.docservice.token.useforrequest`, `.expires-in` (minutes), `.header`.
- `storage-path` (see below), `filesize-max` (bytes), `filename-max` (chars), `files.docservice.timeout`
  (conversion polling), `enable-forgotten`, `version`.
- `files.docservice.verify-peer-off` — **ships as `true`**, so `VerifySSL()` installs a process-wide
  `ServerCertificateValidationCallback` that accepts every certificate. Opt out deliberately, not by accident.
- `files.docservice.languages` — **pipe-separated `code:Label` pairs in one string**, parsed by
  `_Default.GetLanguages()` with a naive `Split('|')` / `Split(':')`; a pair missing its colon throws.

## Code map

WebForms: each page is `X.aspx` (markup + inline JS) + `X.aspx.cs` (code-behind) + `X.aspx.designer.cs`.

- `Default.aspx` / `.cs` — file-list index page and, despite the name, the utility core: path building
  (`StoragePath`, `HistoryPath`, `HistoryDir`, `VersionDir`, `ForcesavePath`, `GetFileVersion`), URL building
  (`GetServerUrl`, `FileUri`), `CurUserHostAddress`, `DocumentType`, `GetCorrectName`, `GetFilesInfo`,
  `GetLanguages`, `VerifySSL`, and `DoUpload` / `DoSaveAs` / `DoConvert`.
- `DocEditor.aspx` / `.cs` — the editor page. `Page_Load` assembles the full editor config, signs it via
  `JwtManager.Encode`, exposes it to the markup as `DocConfig`. Also `Try()` (copies a blank/sample from
  `assets/document-templates`) and `CreateMeta()`.
- `Forgotten.aspx` / `.cs` — files Document Server kept after a failed save, via `getForgottenList` /
  `getForgotten`; returns 403 when `enable-forgotten` is false.
- `WebEditor.ashx` / `.ashx.cs` — the generic handler and **where most of the backend lives**, dispatching on
  `Request["type"]`: `upload`, `download`, `downloadhistory`, `gethistory`, `getversiondata`, `restore`,
  `convert`, `track`, `remove`, `removeforgotten`, `assets`, `csv`, `files`, `saveas`, `rename`, `reference`,
  `formats`, `config`. The `TrackerStatus` enum lives here too.
- `TrackManager.cs` — `readBody` (JWT extraction/validation of the callback), `processSave`, `processForceSave`,
  `commandRequest` (`forcesave`, `getForgottenList`, `getForgotten`, `deleteForgotten`).
- `DocumentConverter.cs` — namespace `ASC.Api.DocumentConverter`; `ServiceConverter.GetConvertedData` polls the
  conversion service, `GenerateRevisionId` produces the document key.
- `FormatManager.cs` — reads `assets/document-formats/onlyoffice-docs-formats.json` once into a static cache;
  answers viewable/editable/fillable/convertible and word/cell/slide/pdf/diagram classification.

## The integration model

**Callback statuses** (`Track` in `WebEditor.ashx.cs`): `1` Editing, `2` MustSave, `3` Corrupted, `4` Closed,
`6` MustForceSave, `7` CorruptedForceSave. Status 1 inspects `actions[0]` and issues a `forcesave` command when
the user who finished editing is gone from `users`; 2/3 go to `processSave`, 6/7 to `processForceSave`. Every
branch replies `{"error":N}`.

**Storage layout** — the history suffix is **`-hist`**, not `-history`, and the metadata filenames differ from
other examples (`changes.json`, not `changes.txt`; no `user.txt`):

```
<storage-path>/<user host address>/
  <filename>                    current version
  <filename>-hist/
    createdInfo.json            who created the file (written once, at history root)
    <filename>                  the force-saved copy, when one exists (ForcesavePath)
    1/  prev.<ext>  diff.zip  changes.json  key.txt
    2/  ...
```

**`storage-path` changes that layout.** If it is a rooted path the helpers use it directly and the per-client-IP
partitioning is **dropped entirely** (every visitor shares one folder), and `Download` also stops calling
`Path.GetFileName` on the requested `fileName`, accepting sub-paths. If relative (the default, empty) it is
appended to `HttpRuntime.AppDomainAppPath` and suffixed with the sanitised `Request.UserHostAddress`.

**JWT** is the only security boundary. Outgoing configs/requests are signed by `JwtManager.Encode` (HMAC-SHA256,
`JWT` NuGet package). Incoming validation lives in `TrackManager.readBody` and `Download`, gated in both on
`JwtManager.Enabled && SignatureUseForRequest` — `token.useforrequest` switches off *incoming* checking here too,
which is not what the key name suggests.

**Formats** come only from the `document-formats` submodule via `FormatManager`; unlike `nodejs` this example
does **not** fetch `meta/config` or `meta/formats` from a running Document Server, and takes languages from the
`files.docservice.languages` string instead.

**Users** — `uid-1`…`uid-3` plus `uid-0` (anonymous) in `Users.cs`, selected by the `uid` cookie; their permission
sets are what the demo demonstrates, and `uid-0` is special-cased (no mentions, no reference data). Form submit
(`processForceSave`, `forcesavetype == 3`) writes `<name>-form.<ext>` plus a `.txt` sibling from `formsdataurl`.

## What is specific to this example

- **WebForms, not MVC**: no routing table, no controllers. URLs are physical files (`default.aspx`,
  `doceditor.aspx`, `forgotten.aspx`, `webeditor.ashx`), and `WebEditor.ashx` — one `IHttpHandler` with a `switch`
  on the `type` parameter — replaces what siblings express as routes.
- `*.aspx.designer.cs` are **generated by Visual Studio** from the markup. Never hand-edit them; add the control
  to the `.aspx` and let VS regenerate.
- The editor config is built twice — the full one in `DocEditor.aspx.cs Page_Load`, a minimal
  document/editorConfig pair in `WebEditor.ashx.cs Config()` for reopening a file with explicit permissions; key
  changes usually need both. Serialization is mixed: mostly `JavaScriptSerializer`, `Newtonsoft.Json` in places.
- `bin/`, `obj/`, `packages/`, `.vs/` and `*.user` are **all gitignored** — build output is not committed and
  `OnlineEditorsExample.csproj.user` on disk is purely local. Do not add them to a commit.

## Conventions

Repo-wide rules (licence header, commits, CHANGELOG, porting) are in `../../../CLAUDE.md`. Here, additionally:

- No linter and no analyzers — style is by convention only. Match the surrounding code: 4-space indent, `var`
  locals, explanatory `//` comments after statements (the house style is heavily commented).
- The porting follow-up most often lands in `../csharp-mvc`, a near-duplicate of this example.
- The `version` key in `settings.config` is the release number; `Properties/AssemblyInfo.cs` is unrelated and
  stays at `1.0.0.0`.

## Gotchas

- `settings.config` ships with placeholders: `files.docservice.url.site` is `http://documentserver/` and
  `files.docservice.secret` is empty, which silently disables JWT entirely.
- Without the submodules `FormatManager.All()` throws `FileNotFoundException` and "create new" cannot copy a
  template. Its static format cache lives for the AppDomain — editing that JSON needs an app pool recycle.
