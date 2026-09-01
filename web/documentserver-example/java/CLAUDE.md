# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this directory.

## What this is

The JavaEE (servlets + JSP) reference implementation of ONLYOFFICE Docs integration: a minimal document
management system that opens files in Document Server, receives save callbacks, and keeps version history.
One of 11 sibling implementations of the same application (see `../` for `go`, `nodejs`, `php`, `python`,
`ruby`, …); they share no code. **`../java-spring` is a separate example, not a newer version of this one** —
never edit it as part of a change here.

Deliberately unhardened: no authorization, no validation of link parameters, no check of who calls the
`track` action, `CorsFilter` with `cors.allowed.origins = *` in `web.xml`. That is intentional — do not "fix"
it as if this were production code. Real bugs (path traversal past the storage root, crashes) still count.

## Commands

```bash
mvn package          # compiles + runs checkstyle + builds target/OnlineEditorsExampleJava-1.0.war
mvn validate         # checkstyle only — the plugin execution is bound to the validate phase
docker compose up    # builds the WAR and deploys it as Tomcat ROOT.war on :8080
```

There is no separate linter: **checkstyle is bound into the build, so `mvn package` is the lint.**
`.github/workflows/lint-java.yml` runs exactly `mvn -version && mvn package` on Temurin **Java 11**, while
`maven-compiler-plugin` targets **source/target 8** (the Dockerfile builds on JDK 8, runs on `tomcat:alpine`).
The artifact is a **WAR** — no embedded server, no `main()`; running it means deploying `target/*.war` into a
servlet container (the README walks through Tomcat's Manager App). There are **no tests** — no `src/test`, no
surefire. Submodules must be initialised or "Create new" and format classification break:
`git submodule update --init --recursive` pulls `src/main/resources/assets/document-templates` and
`.../document-formats` — under `resources/`, i.e. onto the classpath.

## Configuration

Everything lives in `src/main/resources/settings.properties`, read through `helpers/ConfigManager` (static
`Properties`, loaded once from the classpath). No environment-variable overlay, no per-environment file —
changing config means editing that file and rebuilding. Keys that matter: `files.docservice.url.site` (the
`http://documentserver/` **placeholder that must be replaced**; `.url.converter` / `.command` / `.api` /
`.preloader` are appended to it), `.url.example` (address this example is reachable at *from* Document
Server; empty = derive from the incoming request), `storage-folder` (default `app_data`), `enable-forgotten`,
and JWT `.secret` / `.header` / `.token-use-for-request` / `.token-expiration`. **There is no `token.enable`
flag** — `tokenEnabled()` is true iff `files.docservice.secret` is non-empty, so filling in the secret is
what turns JWT on.

## Code map

- `controllers/IndexServlet.java` (~1170 lines, `/IndexServlet`, `@MultipartConfig`) — almost all work.
  `processRequest` switches on the **`type` query parameter**: `upload`, `download`, `downloadhistory`,
  `convert`, `track`, `remove`, `assets`, `csv`, `files`, `saveas`, `rename`, `reference`, `restore`,
  `history`, `historydata`, `formats`, `config`. No `type` → forwards to `index.jsp`.
- `controllers/EditorServlet.java` — builds the `FileModel`, the insert-image / compare / mail-merge payloads
  and the mentions/protect/user lists, signs them, forwards to `editor.jsp`; passing `fileExt` instead
  creates a document from a template and redirects. `controllers/ForgottenServlet.java` — gated by
  `enable-forgotten`; uses `getForgottenList` / `getForgotten` / `deleteForgotten` command requests.
- `controllers/GlobalServletContextListener.java` — registered in `web.xml`; on startup installs an
  all-trusting `TrustManager` (when `files.docservice.verify-peer-off` is non-empty — any value, `FALSE`
  included) **and an all-trusting `HostnameVerifier` unconditionally**, both JVM-wide.
- `helpers/DocumentManager.java` — the hub: storage paths (`filesRootPath`, `storagePath`, `historyPath`,
  `historyDir`, `versionDir`, `forcesavePath`, `getFileVersion`), `getCorrectName`, `createMeta`,
  `createDemo`, `getStoredFiles`, URL building (`getServerUrl`, `getCallback`, `getDownloadUrl`,
  `getDownloadHistoryUrl`, `getCreateUrl`), JWT (`createToken`, `readToken`, `tokenEnabled`).
- `helpers/TrackManager.java` — `readBody` (parse + JWT-verify the callback), `processSave`,
  `processForceSave`, `commandRequest`. `helpers/ServiceConverter.java` — conversion client
  (`getConvertedData`, `generateRevisionId`, `ConvertBody`). `helpers/FileUtility.java` — name/extension/
  `getFileType` parsing. `helpers/CookieManager.java` — the `uid` and `ulang` cookies.
- `format/FormatManager.java` + `format/Format.java` load
  `assets/document-formats/onlyoffice-docs-formats.json` from the classpath and answer viewable / editable
  (`edit` + `lossy-edit`) / fillable / auto-convert; `Format`'s Lombok annotations are the only Lombok use.
- `entities/FileModel.java` is the editor config object (nested `Document`, `Permissions`, `EditorConfig`,
  `ReferenceData`, plus `changeType` / `buildToken`); `utils/` holds `Constants`, `StatusType`,
  `ConvertErrorType`. `index.jsp` calls `DocumentManager.init` itself; `editor.jsp` renders the config.

## The integration model

**URLs.** Servlets are declared with `@WebServlet` annotations, not `<servlet-mapping>` — `web.xml` only
holds the CORS filter, the session timeout and the context listener. `/` → `index.jsp`; `/EditorServlet`;
`/ForgottenServlet`; `/IndexServlet?type=<action>` for everything else. There are **no REST-style routes**:
`/track`, `/upload`, `/history` do not exist as paths, so **the callback is
`IndexServlet?type=track&fileName=…&userAddress=…`** (built by `getCallback`). `IndexServlet.track` handles
`StatusType`: `1` EDITING (issues a `forcesave` command when a user finished editing and left), `2` MUST_SAVE
/ `3` CORRUPTED → `TrackManager.processSave`, `6` MUST_FORCE_SAVE / `7` CORRUPTED_FORCE_SAVE →
`processForceSave`. **Status `4` (closed without changes) is absent from the `StatusType` enum and
unhandled.** `processForceSave` also handles `forcesavetype == 3` (SubmitForm), saving a `<name>-form.<ext>`
copy plus `formsdataurl` into a sibling `.txt`.

**Storage layout** — the `-hist` suffix and the JSON metadata files differ from other examples:

```
<webapp real path>/<storage-folder>/<user host address>/
  <filename>                     current version
  <filename>-hist/
    createdInfo.json             {created, id, name} for the document itself
    <filename>                   the force-saved copy (forcesavePath), preferred by type=download
    1/  prev.<ext>  diff.zip  changes.json  key.txt
```

`filesRootPath` resolves against `ServletContext.getRealPath("")`, i.e. **inside the deployed/exploded WAR**,
so storage is wiped by a redeploy; and if `storage-folder` is an **absolute** path the `<user host address>`
segment is dropped entirely, so all visitors share one directory.

**JWT** is the only security boundary: HS256 via `com.inversoft:prime-jwt`. `createToken` signs outgoing
payloads; `TrackManager.readBody` and the `download` / `downloadhistory` actions verify the
`files.docservice.header` header (falling back to `Authorization`), stripping a `Bearer ` prefix.

**Formats** come only from the `document-formats` submodule JSON — this example does **not** fetch
`meta/config` / `meta/formats` from a running Document Server the way `nodejs` does, and the language list is
static in `settings.properties`. **Users** are the usual `uid-1`…`uid-3` plus `uid-0` fixture, with
`descriptionUser*` lists the index page prints verbatim; `Users.getUser` falls back to `uid-1` for an unknown
id, and `uid-0` is special-cased (no mentions, no protect list, no user info).

## What is specific to this example

- **JavaEE servlets + JSP, no framework.** `javaee-web-api` 7.0 is `provided` scope — it comes from the
  container, so code compiles against `javax.servlet.*` (not `jakarta.*`) and needs a Java EE 7 / Tomcat
  9-era container. Packaging is `war` with `failOnMissingWebXml=false`.
- **Checkstyle is the whole quality gate.** Sun checks with `severity=error` and `violationSeverity=warning`,
  so *every* violation fails the build: `LineLength` 120, `FileTabCharacter`, no trailing whitespace,
  `MagicNumber`, `FinalParameters`, `HiddenField`, `VisibilityModifier`, `AvoidStarImport`, `UnusedImports`,
  `TodoComment` (a literal `TODO` fails the build). `checkstyle-suppressions.xml` disables `ParameterNumber`,
  `DesignForExtension` and the Javadoc checks.
- **No WOPI here** — `nodejs` is the only one of the 11 examples with a WOPI implementation, so do not look
  for a counterpart or assume a WOPI change needs porting here.
- `target/` is gitignored (only an empty `target/.keep` is tracked); build output is **not** committed.

## Conventions

- **Every `.java` file must start with the Apache-2.0 ONLYOFFICE header, and this is machine-enforced.**
  Checkstyle's `Header` module compares each `.java` file line-for-line against `onlyoffice.header`, so a new
  file without it — or with a different year — fails `mvn package`. Copy `onlyoffice.header` verbatim; JSPs
  carry the same text inside an HTML comment.
- **User-visible changes need a CHANGELOG entry at the repository root** (`../../../CHANGELOG.md`), under the
  top unreleased `##` heading, prefixed `java: ` — release tagging parses that file.
- Commits use Conventional Commits with the example as scope: `feat(java): …`, `fix(java): …`.
- Features here are usually ported to the other 10 examples in **separate commits** — expect that follow-up
  rather than editing sibling directories in the same change.
- `version` in `settings.properties` tracks the release; bump it together with the siblings' config files.

## Gotchas

- `DocumentManager` is entirely static, including `private static HttpServletRequest request` set by
  `DocumentManager.init(request, response)`. Every servlet **and `index.jsp`** must call `init` first, and
  concurrent requests overwrite each other's stored request. Do not assume thread safety here.
- `type` means two different things: the action selector on `IndexServlet`, and the editor type
  (`desktop` / `mobile` / `embedded`) on `EditorServlet`.
- `index.jsp` and `forgotten.jsp` link to `/ForgottenServlet` with a leading slash, so that link works only
  when the WAR is deployed at the root context (as the Dockerfile does with `ROOT.war`).
- `nb-configuration.xml` and `.project` are NetBeans/Eclipse metadata checked into the repo, not build inputs.
