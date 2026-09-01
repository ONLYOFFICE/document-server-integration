# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this directory.

## What this is

The ASP.NET **MVC** reference implementation of ONLYOFFICE Docs integration: a minimal document management
system that opens files in Document Server, receives save callbacks, and keeps version history. One of 11
sibling implementations of the same application (see `../` for `csharp`, `go`, `java`, `nodejs`, `php`, …);
they share no code.

Locally open by design: `Access-Control-Allow-Origin: *` from `Web.config`, and certificate validation off by
default. See *Security posture* in `../../../CLAUDE.md` — this is intentional, and not something to harden.

## Commands

There is **no CLI build, no linter and no tests** here. Do not invent `dotnet run` / `dotnet test` commands.

- `OnlineEditorsExampleMVC.sln` → `OnlineEditorsExampleMVC.csproj`: `OutputType` Library,
  `TargetFrameworkVersion` **v4.8** (.NET Framework, not .NET Core/5+). Build in Visual Studio or with MSBuild;
  run under IIS / IIS Express (`UseIISExpress` is true). The README covers the IIS site setup (CLR v4.0 pool).
- NuGet uses `PackageReference` (no `packages.config`), so `dotnet restore` works even though the .NET SDK
  cannot *build* the project — that is what `licenses-csharp-mvc.yml` does before `ONLYOFFICE/check-licenses`.
- `.github/workflows/artifact-csharp-mvc.yml` compiles nothing: it inits submodules, `rsync`s the directory
  into `deploy/DotNet (Csharp MVC) Example`, strips `assets/.git`, and uploads the source tree. **CI never
  catches a compile error here**, and there is no lint workflow for this example.
- `git submodule update --init --recursive` is required (`assets/document-templates`, `assets/document-formats`)
  or file creation and format detection break.

## Configuration

`Web.config` has `<appSettings configSource="web.appsettings.config" />`, so **all tunable keys live in
`web.appsettings.config`**; `Web.config` only holds plumbing (`maxRequestLength`, `maxAllowedContentLength`,
binding redirects, the CORS header, handler registration). The `csharp` sibling uses `settings.config` instead.

- `files.docservice.url.site` — Document Server address; ships as the `http://documentserver/` placeholder and
  must be replaced before anything works. `.url.converter` / `.url.api` / `.url.preloader` / `.url.command` are
  paths appended to it; `files.docservice.url.example` is where this example is reachable *from* Document
  Server (empty = derive from the request `Host` header).
- `files.docservice.secret` — JWT secret. **There is no `token.enable` key**: `JwtManager.Enabled` is simply
  "secret is non-empty". Companions: `files.docservice.header` (default `Authorization`),
  `files.docservice.token.useforrequest`, `files.docservice.token.expires-in` (minutes).
- `storage-path`, `filesize-max`, `filename-max` (base-name length before `[...]` truncation),
  `enable-forgotten`, `files.docservice.timeout`, `files.docservice.verify-peer-off`, `version`.
- `files.docservice.languages` is one **pipe-separated, colon-delimited** string
  (`en:English|sq-AL:Albanian (Albania)|…`) parsed by `DocManagerHelper.GetLanguages()` — unusual format, keep
  it on one line with no spaces around the separators.

## Code map

- `Global.asax.cs` — `Application_Start` only. `App_Start/RouteConfig.cs` registers **one** route, `{action}`,
  defaulting to `Home/Index` — no `{controller}` segment, so URLs are `/`, `/Editor`, `/Forgotten`, `/Sample`.
  `BundleConfig.cs` declares the script/style bundles, `FilterConfig.cs` adds `HandleErrorAttribute`, and
  `WebApiConfig.Register` is **empty** with no `Areas` folder — both inert scaffolding.
- `Controllers/HomeController.cs` — only four actions: `Index`, `Editor` (builds a `FileModel`), `Forgotten`
  (`getForgottenList`/`getForgotten` command requests), `Sample` (creates a demo file, redirects to `Editor`).
- **`WebEditor.ashx.cs` — the real API surface (~1100 lines).** One `IHttpHandler` switching on the `type` query
  parameter: `upload`, `download`, `downloadhistory`, `gethistory`, `getversiondata`, `restore`, `convert`,
  `track`, `remove`, `removeforgotten`, `assets`, `csv`, `files`, `saveas`, `rename`, `reference`, `formats`,
  `config`.
- `Helpers/DocManagerHelper.cs` — storage paths (`StoragePath`, `HistoryDir`, `VersionDir`, `ForcesavePath`),
  URL building (`GetCallback`, `GetDownloadUrl`, `GetHistoryDownloadUrl`, `GetServerUrl`, `GetCreateUrl`), plus
  `GetStoredFiles`, `GetFilesInfo`, `CreateDemo`, `CreateMeta`, `VerifySSL`, `GetLanguages`.
- `Helpers/TrackManager.cs` — `readBody` (incl. the JWT check), `processSave`, `processForceSave`,
  `commandRequest` (the `/command` service: forcesave and the forgotten-files API).
  `Helpers/DocumentConverter.cs` — `ServiceConverter.GetConvertedData`, `GenerateRevisionId` (document key).
  Plus `JwtManager.cs` (JWT nuget, HMACSHA256), `Users.cs`, `Utils.cs` (`GetOrDefault` extensions).
- `Models/FileModel.cs` — assembles the editor config JSON (`GetDocConfig`, `GetDocumentData`, `GetLogoConfig`,
  `GetSpreadsheetConfig`, `GetUsersMentions/Info/Protect`). `Models/FileUtility.cs` — file-type enum plus the
  static `FormatManager` reading `assets/document-formats/onlyoffice-docs-formats.json`.

**The controller/handler split is the main thing newcomers get wrong.** Controllers only render pages; every
AJAX endpoint, callback and file operation lives in `WebEditor.ashx.cs` — a new endpoint belongs there.

## The integration model

**`?type=track` is the core.** `TrackerStatus` (private enum in `WebEditor.ashx.cs`): `0` NotFound, `1` Editing,
`2`/`3` MustSave/Corrupted, `4` Closed, `6`/`7` MustForceSave/CorruptedForceSave. Status `1` inspects
`actions[0]` and issues a `forcesave` command request when the acting user has left. Save rotates the current
file into the history tree; force-save writes to `ForcesavePath` instead of creating a version. Every branch
answers `{"error":<0|1>}`.

**Storage layout** — partitioned by client IP (`CurUserHostAddress`, non-alphanumerics replaced with `_`),
versions are directories. The suffix is **`-hist`, not `-history`**, the changes file is `changes.json` (not
`changes.txt`), there is no `user.txt`, and the forcesaved copy sits inside the `-hist` folder:

```
<storage-path>/<user host address>/
  <filename>                  current version
  <filename>-hist/
    createdInfo.json          {created, id, name} written by CreateMeta
    <filename>                forcesaved copy, if any
    1/  prev.<ext>  diff.zip  changes.json  key.txt
```

If `storage-path` is an **absolute** path the per-IP partitioning is skipped entirely and all clients share one
folder; `Download` also stops calling `Path.GetFileName` on the requested name in that case.

**JWT** is the only security boundary. Incoming validation on `?type=track` and `?type=download` is gated on
`JwtManager.Enabled && JwtManager.SignatureUseForRequest` — `files.docservice.token.useforrequest` turns *both*
directions on, unlike examples with a separate enable flag. `?type=download` also skips validation when `dmode`
is present or `userAddress` is absent.

**Users** — four in `Helpers/Users.cs`: `uid-1`…`uid-3` plus `uid-0` (anonymous), selected by the `uid` cookie.
`uid-1` is the only user shown the "Submit form" button; `uid-0` is excluded from mentions/user info and gets a
null `referenceData.fileKey`. **Formats** come only from the `document-formats` submodule via
`FormatManager.All()` (static cache) — no `meta/formats` or `meta/config` fetch at runtime.

## What is specific to this example

- ASP.NET MVC 5 (`Microsoft.AspNet.Mvc` 5.2.7) but the views are **WebForms `.aspx` pages, not Razor** —
  `Views/Home/{Index,Editor,Forgotten}.aspx` and `Views/Shared/Error.aspx`, each
  `Inherits="System.Web.Mvc.ViewPage<T>"` with `<% %>` blocks. Razor packages are referenced; no `.cshtml`.
- The `csharp` sibling (`../csharp`, WebForms) is a near-duplicate: same `WebEditor.ashx.cs` handler and helper
  classes, but flat at the project root instead of `Helpers/`+`Models/`, with `Default.aspx` / `DocEditor.aspx`
  code-behind in place of controllers and views. A change here usually needs the same change there.
- Both C# examples wire `assets/document-formats` *and* `assets/document-templates` in the repo-root
  `.gitmodules` (several other examples get templates only). `assets/**` files are listed individually as
  `<Content Include=...>` in the `.csproj`, so a new template or format file must be added there too.

## Conventions

Repo-wide rules (licence header, commits, CHANGELOG, porting) are in `../../../CLAUDE.md`. Here, additionally:

- No linter and no analyzers — style is by convention only. Match the surrounding file: 4-space indent,
  Allman braces, `// lowercase` comments.
- The porting follow-up most often lands in `../csharp`, a near-duplicate of this example.
- The `version` key in `web.appsettings.config` is the release number.

## Gotchas

- `Web.config` declares `targetFramework="4.5"` for `httpRuntime` and `compilation`, and the README asks for
  .NET Framework 4.5, while the `.csproj` targets **v4.8**. Do not "align" these without checking.
- `bin/`, `obj/` and `packages/` are gitignored repo-wide and **not** committed; a fresh clone has no build
  output. Ignore them when exploring, as well as `Scripts/` (third-party JS).
- `files.docservice.verify-peer-off` defaults to `true`; `DocManagerHelper.VerifySSL()` then installs a
  process-wide `ServerCertificateValidationCallback` accepting every certificate and pins TLS to 1.0–1.2.
