# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this directory.

## What this is

The Java/Spring Boot reference implementation of ONLYOFFICE Docs integration: a minimal document management
system that opens files in Document Server, receives save callbacks, and keeps version history. One of 11
sibling implementations of the same application (`../go`, `../nodejs`, `../php`, …); they share no code.
**Not `../java`**, the JavaEE/WAR example — same vendor, same `com.onlyoffice.integration` package prefix,
different codebase.

Locally open by design: `@CrossOrigin("*")` on every controller, and nothing checks who calls `/track`. See
*Security posture* in `../../../CLAUDE.md` — this is intentional, and not something to harden.

## Commands

Java 11 (`<java.version>11</java.version>`), Spring Boot parent **2.5.1**, Maven only.

```bash
mvn package              # build + checkstyle (bound to the `validate` phase) — this IS the lint
mvn spring-boot:run      # starts on server.port = 4000
docker compose up        # docker-compose.yaml → `mvn clean spring-boot:run`, publishes 4000
```

CI (`.github/workflows/lint-spring.yml`, gated on `web/documentserver-example/java-spring/**`) installs
temurin 11 and runs `mvn -version && mvn package` — no separate lint or test step. **There are no tests
here**: no `src/test`, no `spring-boot-starter-test`. Use a system `mvn`, as the README and CI do — not
`./mvnw`. Submodules must be initialised (`git submodule update --init --recursive`) or "Create new" /
sample file creation breaks; exactly one is wired in here, `src/main/resources/assets/document-templates`.
Unlike `java`, `php`, `python`, `go`, `ruby` and `csharp*` there is **no `document-formats` submodule** — the
format table comes from the SDK.

## Configuration

Everything is in `src/main/resources/application.properties` — no profiles, no env overlay. Keys that matter:
`docservice.url` (the `http://documentserver/` placeholder, must be replaced), `docservice.security.key` /
`.header` / `.tokenValidityInMinutes` (empty key = JWT off), `files.storage` (absolute storage root; blank =
`documents/<server host IP>` under the working directory), `files.docservice.history.postfix=-hist`,
`files.docservice.url.example` (address this example is reachable at *from* Document Server), `server.port`,
`server.version` (release version — bump with the siblings' config files), `enable-forgotten`,
`spring.datasource.*` (in-memory H2, console at `/h2`), and the `url.*` mappings, injected straight into the
`@GetMapping("${url.…}")` annotations. Note that `SettingsManagerImpl` loads this file off the classpath
itself (`docservice.` prefix) rather than through the Spring environment, so the SDK reads the file, not the
Spring property sources.

## Code map

- `IntegrationApplication` (entry point); `IntegrationConfiguration` (beans for `JwtManager`,
  `DocumentServerClient`, `ConvertService`, `ModelMapper`/`ObjectMapper`/`JSONParser`, and a `@PostConstruct`
  configuring the storage root); `ExampleData` (inserts the four demo users into H2 on boot).
- `controllers/` — `IndexController` (`/`, `POST /config` handing the `url.*` mappings to the JS, `/formats`);
  `EditorController` (`/editor`: builds the `Config` via `ConfigService` plus the mentions/usersInfo/protect
  lists); `FileController` (everything else — `/upload`, `/converter`, `/delete`, `/download`,
  `/downloadhistory`, `/create`, `/assets`, `/csv`, `/files`, `/track`, `/saveas`, `/rename`, `/reference`,
  `/history`, `/historydata`, `PUT /restore`, `GET /config`); `ForgottenController` (`/forgotten` and
  `DELETE /forgotten/{filename}` via SDK forgotten-file commands).
- `documentserver/storage/LocalFileStorage` — `@Primary`, implements both `FileStoragePathBuilder`
  (`getStorageLocation`, `getFileLocation`, `getHistoryDir`, `getFileVersion`, `getForcesavePath`,
  `getServerUrl`) and `FileStorageMutator` (`createFile`, `moveFile`, `deleteFile`, `getStoredFiles`,
  `createMeta`): all path math and file IO. `documentserver/managers/` — `callback/DefaultCallbackManager`
  (`processEditing`/`processSave`/`processForceSave`) and `history/DefaultHistoryManager` (`getHistory`,
  `getHistoryData`, `versionDir`). Also `models/enums/Action`, `serializers/FilterState` +
  `SerializerFilter` (the `NULL` sentinel = "no group filter"), `documentserver/util/Constants`
  (`ANONYMOUS_USER_ID = 4`).
- `dto/` — request/response bodies. `entities/` — JPA `User`, `Group`, `Permission`, `Goback`, `Close`;
  `repositories/` — Spring Data interfaces (only custom query: `findGroupByName`); `services/` — `UserServices`
  (`createUser`, `findUserById`, `getCurrentUser` from the `uid` cookie), `Group`/`PermissionServices`.
- `sdk/` — subclasses of the SDK defaults: `DocumentMangerImpl` (sic; `getDocumentKey`, `getCorrectName`,
  `getFilesInfo`, `createDemo`), `UrlManagerImpl` (`getFileUrl`, `getCallbackUrl`, `getGobackUrl`,
  `getHistoryFileUrl`), `SettingsManagerImpl`, `ConfigServiceImpl` (maps the DB `Permission` entity onto the
  editor config), `CallbackServiceImpl` (forwards to `CallbackManager`); `sdk/configuration/` is empty.
  `templates/*.html` are Thymeleaf; `editor.html` consumes the `Config`.

## The integration model

**Two separate stores — the biggest departure from the other examples.**

*H2 + JPA holds only the demo users.* `jdbc:h2:mem:usersdb` is in-memory and stores `User`, `Group`,
`Permission`, `Goback`, `Close` and nothing else — no files, no history, no document metadata. `ExampleData`
recreates the same four users on every start, so the database is disposable by design. IDs are auto-increment
`1`…`4`, and `4` is Anonymous (`Constants.ANONYMOUS_USER_ID`). The current user is the **`uid` cookie holding
that integer id** — not the `uid-1` string form other examples use; `static/scripts/jscript.js` sets it from
the index-page selector, defaulting to `1`. `@CookieValue("uid")` is mandatory on `/editor`, `/upload`,
`/saveas` and `/restore`, so those 400 without it.

*Documents are plain files on disk*, per `LocalFileStorage`:

```
documents/<storage address>/
  <filename>                      current version
  <filename>-hist/
    createdInfo.json              {created,id,name} — author of version 1
    1/  prev.<ext>  diff.zip  changes.json  key.txt
    2/  ...
    <filename>                    force-saved copy (getForcesavePath)
```

Note `-hist` (not `-history`) and `changes.json` (not `changes.txt`). The current version is just the count of
subdirectories in the history folder (`getFileVersion`), which is why the force-saved *file* can sit beside the
numbered directories harmlessly. The storage address is `files.storage`, or when blank the **server's own**
`InetAddress.getLocalHost().getHostAddress()` — so unlike the client-IP partitioning used elsewhere, every
visitor shares one folder.

**`/track`**: `FileController.track` calls `callbackService.verifyCallback(body, authHeader)` then
`processCallback`. The status dispatch (1 editing, 2/3 save, 4 closed, 6/7 force-save) lives in the SDK's
`DefaultCallbackService`; `CallbackServiceImpl` only overrides the handlers, and `handlerClosed` deliberately
just logs "not supported yet". `processSave` renames the current file to `<n>/prev.<ext>`, downloads the
callback `url` into its place, writes `diff.zip`, `changes.json` and `key.txt`, then deletes the force-save
copy; an extension mismatch goes through `ConvertService` first. `processForceSave` with
`ForcesaveType.SUBMIT_FORM` writes `<name>-form.<ext>` plus a `.txt` of `formsdataurl`.

**JWT** is the only security boundary: `settingsManager.isSecurityEnabled()` (i.e. `docservice.security.key` is
set) gates `jwtManager.verify` on `/download` and `/downloadhistory`, and `createToken` on the editor config
and history data. **Formats** come from the SDK's bundled table, exposed at `/formats`.

## What is specific to this example

- **Spring Boot 2.5.1 + Thymeleaf** (`templates/`, static assets under `src/main/resources/static/`), and
  **`com.onlyoffice:docs-integration-sdk:1.9.0`**, which carries a large share of the behaviour: config
  building, callback dispatch, JWT, the convert/command services, the HTTP client, the format table. `sdk/`
  only *customises* it by subclassing the SDK defaults, so when something looks unimplemented here read the
  SDK superclass first — and an SDK bump can change behaviour with no diff in this directory.
- `documents/` is the runtime storage root and `target/` is Maven output — **neither is committed** (both are
  in the repo-root `.gitignore`) though both may exist locally from earlier runs. Ignore both when exploring.

## Conventions

Repo-wide rules (licence header, commits, CHANGELOG, porting) are in `../../../CLAUDE.md`. Here, additionally:

- The licence header is machine-enforced: checkstyle compares the opening lines byte-for-byte against
  `onlyoffice.header`, current year included, at the `validate` phase.
- `checkstyle.xml` is sun_checks-derived, `severity=error`: `LineLength` 120, no tabs, no trailing whitespace,
  newline at EOF, `FinalParameters` (every method parameter must be `final`), `MagicNumber` (put literals in
  `Constants`), `HiddenField`, `VisibilityModifier`, `AvoidStarImport`, `UnusedImports`, `TodoComment`.
  `checkstyle-suppressions.xml` disables `DesignForExtension`, `ParameterNumber` and the Javadoc checks.
- `server.version` in `application.properties` is the release number.

## Gotchas

- `TodoComment` matches the uppercase `TODO:` pattern only; the codebase's many lowercase `// todo:` markers
  (`DefaultCallbackManager`, `DefaultHistoryManager`, `LocalFileStorage`) pass, `TODO:` fails the build.
- Lombok is `provided` scope — without IDE annotation processing every `@Getter`/`@Setter` accessor looks
  undefined. `mvn package` is unaffected.
- The H2 database is dropped on every restart and rebuilt by `ExampleData`; entity changes are invisible until
  a restart, and nothing user-related survives one.
- `docservice.url=http://documentserver/` and the empty `docservice.security.key` are placeholders — nothing
  works until they are replaced, and JWT stays off while the key is blank.
- Two types share the simple names `DocumentManager` and `UrlManager` (the SDK's and this repo's extended
  interfaces) — `FileController` imports the SDK's `UrlManager`, `EditorController` the local one. Check the
  import before calling a method that "should exist". `spring-boot-devtools` also restarts the app on any
  classpath change.
