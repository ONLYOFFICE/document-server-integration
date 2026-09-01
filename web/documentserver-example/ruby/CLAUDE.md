# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this directory.

## What this is

The Ruby on Rails reference implementation of ONLYOFFICE Docs integration: a minimal document management
system that opens files in Document Server, receives save callbacks, and keeps version history. It is one of
11 sibling implementations of the same application (see `../` for `go`, `java`, `nodejs`, `php`, `python`, …);
they share no code.

Locally open by design: nothing checks who calls `/track`, `Rack::Cors` uses `origins '*'`, and
`config.hosts << /.*/` accepts any Host header. See *Security posture* in `../../../CLAUDE.md` — this is
intentional, and not something to harden.

## Commands

Ruby 3.2.2 (`.ruby-version`), Bundler 2.4.10 (`.bundler-version`); everything goes through the `Makefile`.

```sh
make dev          # bundle install (BUNDLE_WITH=development:doc:test), then bundle binstubs railties
                  # --path bin --force (creates bin/rails), then bundle exec tapioca init (Sorbet only)
make prod         # same with BUNDLE_WITHOUT=development:doc:test, no tapioca
make server-dev   # bundle exec rails server — $BINDING/$PORT, default localhost:3000
make server-prod  # bundle exec rails server --environment production
make lint         # bundle exec rubocop  (+ bundle exec srb tc where Sorbet is supported)
make test         # bundle exec rake test
make compose-dev  # docker compose -f compose-base.yml -f compose-dev.yml build && up --detach
make compose-prod # same, with compose-prod.yml
```

`rake test` (`Rakefile`) collects `FileList['app/**/*_tests.rb']`: `app/configuration/configuration_tests.rb`,
`app/format/format_tests.rb`, `app/proxy/proxy_tests.rb`. They are plain `test-unit` files with an `at_exit`
runner, so one runs directly — `bundle exec ruby app/format/format_tests.rb`, plus
`--name test_generates_extension` for a single case. CI (`.github/workflows/lint-ruby.yml`) runs **only
`bundle exec rubocop`** — not `make lint`, not `make test`.

Submodules must be initialised or nothing works: `assets/document-formats` drives every extension list and
`assets/document-templates` supplies the blank/sample files (`git submodule update --init --recursive`).
Those two are the only submodules landing here — no `plugin-aiautofill`, that is nodejs-only.

## Configuration

**There is no config file.** `app/configuration/configuration.rb` (`ConfigurationManager`) reads everything
from the environment, defaults in code: `DOCUMENT_SERVER_PUBLIC_URL` (`http://documentserver` — the
placeholder that must be replaced), `DOCUMENT_SERVER_PRIVATE_URL` (falls back to the public one),
`DOCUMENT_SERVER_{API,PRELOADER}_PATH` (joined onto the public URL), `DOCUMENT_SERVER_{COMMAND,CONVERTER}_PATH`
(onto the private one), `EXAMPLE_URL` (this example's address *as seen by* Document Server), `JWT_SECRET`,
`JWT_HEADER`, `JWT_EXPIRES_IN`, `JWT_USE_FOR_REQUEST`, `SSL_VERIFY_PEER_MODE_ENABLED`, `ENABLE_FORGOTTEN`,
`STORAGE_PATH` (`storage`, relative to the example root), `MAXIMUM_FILE_SIZE` (100 MB). `BINDING`/`PORT` are
read by Rails' own `server` command. `ConfigurationManager#version` (`1.15.0`) is the release number — bump it
with the siblings' config files.

The public/private URL split is this example's distinctive feature: URLs coming back from Document Server are
rewritten public→private by `ProxyManager#resolve_uri` before download, and private→public before reaching a
browser (`home#forgotten`, `#convert`, `#saveas`, `#restore`).

## Code map

Logic lives in `app/models/`, not in service objects, plus three newer Sorbet-typed packages
(`app/configuration/`, `app/format/`, `app/proxy/`) with colocated `*_tests.rb`.

- `config/application.rb` — the whole Rails setup: middleware, asset flags, **and every route**
  (`routes.append` block). No `config/routes.rb`, `config/environments/`, `config/environment.rb` or
  `config/initializers/`; `config.ru` boots the app directly.
- `app/controllers/home_controller.rb` (~680 lines) — every endpoint; nearly all work happens here.
- `app/models/document_helper.rb` — class-level singleton owning all path and URL building (`storage_path`,
  `history_dir`, `version_dir`, `forcesave_path`, `get_file_version`, `get_correct_name`, `get_stored_files`,
  `create_meta`, `create_demo`, `get_callback`, `get_download_url`, `get_historypath_uri`, `verify_ssl`).
- `app/models/file_model.rb` — the editor config (`#config`) and `#history`, plus the insert-image, compare,
  mail-merge and mentions/protect payloads.
- `app/models/track_helper.rb` — `/track` body reading with JWT (`read_body`), `process_save`,
  `process_force_save`, `command_request`, `download_file`, `resolve_process_save_body` (proxy rewrite of
  `url`/`changesurl`/`formsdataurl`, recursing into `home`).
- Also `app/models/`: `service_converter.rb` (`/converter`, `generate_revision_id` — the CRC32 document key),
  `jwt_helper.rb`, `file_utility.rb` (extension → `word`/`cell`/`slide`/`pdf`/`diagram`), `users.rb`.
  `app/format/format.rb` is `Format` (`T::Struct`) + `FormatManager` over
  `assets/document-formats/onlyoffice-docs-formats.json`.

## The integration model

**Routes** are all in `config/application.rb`. Names that differ from the other examples: `/sample` (this
example's "create new"), `/config` → `home#refresh_config`, `/remove` (DELETE), `/historyobj` (POST),
`/downloadhistory`, `/saveas`, `/asset`. The rest are the usual set, `/track` included.

**`/track` is the core.** `HomeController#track` reads the body via `TrackHelper.read_body` (verifying the JWT
from either `token` in the body or the configured header) and switches on `status`: `1` editing (issues a
`forcesave` command when the last editor left), `2`/`3` MustSave/Corrupted → `process_save`, `6`/`7`
MustForceSave/CorruptedForceSave → `process_force_save`; `4` and anything else return `{"error":0}`.
`forcesavetype == 3` (form submitted) is special-cased: saved as a new `<name>-form<ext>` file with the
`formsdataurl` payload as a sibling `.txt`.

**Storage layout** — partitioned by client IP (`cur_user_host_address` sanitises it), versions are
directories. Unlike the other examples the suffix is `-hist`, changes are `changes.json`, no `user.txt`:

```
<STORAGE_PATH>/<user host address>/<filename>                          current version
<STORAGE_PATH>/<user host address>/<filename>-hist/createdInfo.json    {created, uid, uname} for v1
<STORAGE_PATH>/<user host address>/<filename>-hist/<filename>          force-saved copy, if any
<STORAGE_PATH>/<user host address>/<filename>-hist/1/                  prev.<ext> diff.zip changes.json key.txt
```

`DocumentHelper.get_file_version` returns *(number of subdirectories) + 1*, so a file with no history is v1.

**JWT** is the only security boundary and is enabled purely by `JWT_SECRET` being non-empty (no separate
enable flag). HS256, `iat`/`exp` added on encode, `Bearer ` prefix stripped on decode. `/download` skips
validation when `dmode` is set or `userAddress` is absent.

**Users** — four hardcoded users in `app/models/users.rb` (`uid-1`…`uid-3` plus anonymous `uid-0`); their
permission sets are what the demo demonstrates. `uid-0` is special-cased throughout `file_model.rb` (no chat,
no mentions, no `referenceData.fileKey`, strict co-editing, AI plugin off); unknown ids fall back to `uid-1`.

**Formats** come only from the `document-formats` submodule — unlike nodejs, this example does not fetch
`meta/config`/`meta/formats` from a running Document Server.

## Rails and tooling specifics

- Intentionally skeletal Rails: four railties required, `secret_key_base` regenerated per boot with
  `SecureRandom.uuid`. `bin/`, `sorbet/`, `storage/`, `log/`, `tmp/` are gitignored and generated —
  `bin/rails` exists only after `make dev`/`make prod` runs `bundle binstubs railties`.
- Docker is three files: `compose-base.yml` (documentserver + example + nginx `proxy` publishing 80 and 8080)
  overlaid by `compose-dev.yml` or `compose-prod.yml`, which is what selects the `example-dev`/`example-prod`
  Dockerfile stage. `proxy/nginx.conf` maps `:80` → example, `:8080` → documentserver, which is what makes the
  public/private URL split meaningful.
- **Sorbet does not run on Windows.** The `Makefile` computes `SORBET_SUPPORTED` (1 only on Darwin, or Linux
  with `uname -p` = `x86_64`) and the `Gemfile` guards `sorbet`/`tapioca` with
  `install_if { RUBY_PLATFORM =~ /darwin|x86_64/ }`. On Windows `make dev` skips `tapioca init` and `make lint`
  runs rubocop only, so type errors in the `sig`-annotated files (`configuration.rb`, `format.rb`, `proxy.rb`)
  are caught neither locally nor in CI. Keep the `sig`s correct by hand.

## Conventions

Repo-wide rules (licence header, commits, CHANGELOG, porting) are in `../../../CLAUDE.md`. Here, additionally:

- The licence header sits above the `# frozen_string_literal: true` / `# typed:` sigils.
- `.rubocop.yml` is strict in unusual ways: parens on every call (`Style/MethodCallWithArgsParentheses`),
  `Style/StringHashKeys`, `brackets` symbol/word arrays, inline `private def`, one `attr_accessor` per line,
  line length 120, `Metrics` disabled. Run `make lint` before finishing.
- `ConfigurationManager#version` is the release number.

## Gotchas

- `SSL_VERIFY_PEER_MODE_ENABLED=true` **disables** certificate checking (`DocumentHelper.verify_ssl` sets
  `OpenSSL::SSL::VERIFY_NONE`). The name says the opposite of what it does.
- `DocumentHelper` keeps the current request's IP and base URL in class-level `@remote_ip`/`@base_url` set by
  `DocumentHelper.init` — shared mutable state across concurrent requests. `index` calls it from inside
  `app/views/home/index.html.erb` rather than from the controller. Do not assume thread safety here.
- Storage is not served statically: everything a client or Document Server fetches goes through `/download`.
- Go through `make compose-dev` / `make compose-prod` — `compose-base.yml` on its own does not select a
  Dockerfile stage, the overlays do.
