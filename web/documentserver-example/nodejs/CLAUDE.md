# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## What this is

The Node.js/Express reference implementation of ONLYOFFICE Docs integration: a minimal document management
system that opens files in Document Server, receives save callbacks, and keeps version history. It is one of
11 sibling implementations of the same application (see `../` for `go`, `java`, `php`, `python`, `ruby`, …);
they share no code.

Deliberately unhardened: no authorization, no validation of link parameters, no check of who calls `/track`,
`Access-Control-Allow-Origin: *`. That is intentional — do not "fix" it as if this were production code.
Real bugs (path traversal past the storage root, crashes, leaks) are still worth fixing.

## Commands

```bash
npm install          # or `make` from the repository root
npm start            # node ./bin/www — port from $PORT, else server.port in config (3000)
npm run lint         # eslint, airbnb-base
npm run lint:fix
```

There are no tests in this example.

Submodules must be initialised or file creation and the AI plugin break:
`git submodule update --init --recursive` (pulls `public/assets/document-templates` and
`public/assets/plugin-aiautofill`).

## Configuration

Uses [node-config](https://github.com/node-config/node-config). `config/default.json` holds everything;
`config/<NODE_ENV>.json` overrides it. Environments are named `development-windows` / `development-linux` /
`development-mac` / `production-windows` / `production-linux` — `run-develop.py` sets
`NODE_ENV=development-<platform>` and `NODE_CONFIG_DIR=./config`. Note that the development overrides change
the port (80) and `siteUrl`, so behaviour differs from a bare `npm start`.

`docker-entrypoint.sh` injects a `NODE_CONFIG` JSON overlay from `DS_URL`, `JWT_ENABLED`, `JWT_SECRET`,
`JWT_HEADER`.

Keys that matter most: `server.siteUrl` (Document Server address — the `http://documentserver/` placeholder
must be replaced), `server.storageFolder` / `server.storagePath`, `server.token.*` (JWT),
`server.exampleUrl` (address this example is reachable at *from* Document Server, when it differs),
`plugins.pluginsData`.

## Code map

- `app.js` — every HTTP route in one ~1500-line file. This is where nearly all work happens.
- `bin/www` — server bootstrap only.
- `helpers/docManager.js` — instantiated per request as `req.DocManager`. Owns all storage path building
  (`storagePath`, `historyPath`, `versionPath`, `diffPath`, `changesPath`, `keyPath`, `forcesavePath`),
  URL building for Document Server (`getCallback`, `getDownloadUrl`, `getServerUrl`), and the file list.
- `helpers/documentService.js` — everything that talks to Document Server: conversion
  (`getConvertedUri`), `commandRequest`, JWT sign/verify (`getToken`, `readToken`, `checkJwtHeader`,
  `fillJwtByUrl`), and `config`/`formats` meta fetching.
- `helpers/fileUtility.js` — extension/name parsing, format classification
  (`getViewExtensions`, `getEditExtensions`, `getFillExtensions`, `getConvertExtensions`), `getContentDigest`.
- `helpers/users.js` — the hardcoded user fixture.
- `helpers/wopi/` — the WOPI implementation (see below).
- `views/*.ejs` — EJS templates; `editor.ejs` is where the editor config object is assembled.
- `config/data.json` — sample data served at `/data` for the AI autofill plugin.

## The integration model

**Routes**: `/` index, `/editor`, `/config`, `/upload`, `/create`, `/convert`, `/download`, `/files`,
`/history`, `/historyObj`, `/restore`, `/rename`, `/reference`, `/formats`, `/csv`, `/data`, `/forgotten`,
and `/track`.

**`/track` is the core.** Document Server POSTs a callback with a `status`: `1` editing, `2`/`3`
MustSave/Corrupted, `4` closed without changes, `6`/`7` MustForceSave/CorruptedForceSave. On save the handler
downloads the callback's `url`, rotates the current file into the history tree, then writes the new content in
its place. Force-save (6/7) writes into `forcesavePath` instead of creating a version.

**Storage layout** — partitioned by client IP, versions are directories:

```
<storageFolder>/<user host address>/
  <filename>                    current version
  <filename>-history/
    1/  prev.<ext>  diff.zip  changes.txt  key.txt  user.txt
    2/  ...
```

**JWT** is the only security boundary. Controlled by `server.token.enable` (validate incoming),
`server.token.useforrequest` (sign outgoing), `secret`, `expiresIn`. The header name and prefix are taken from
Document Server's `meta/config` at runtime, not from local config.

**Document Server meta** — `documentService.config()` and `documentService.formats()` fetch `meta/config` and
`meta/formats` from the running Document Server to learn supported formats, languages, `maxFileSize`, service
URLs and the JWT header. Cached in module-level variables for 1 hour, with a 2s fetch timeout and in-flight
request deduplication; each returns a safe empty fallback when Document Server is unreachable. This is the
newest mechanism here and several examples still rely only on the `document-formats` submodule.

**Users** — four hardcoded users in `helpers/users.js`: `uid-1`…`uid-3` plus `uid-0` (anonymous). Their
permission sets are what the demo actually demonstrates (review-only, comment-only, filler, protect), so
changing a user's flags changes what a reviewer sees. `uid-0` is special-cased in many places: no chat, no
mentions, no AI plugin, coediting restrictions.

## WOPI

**This is the only one of the 11 examples that implements WOPI** — do not look for a counterpart elsewhere,
and do not assume a WOPI change needs porting.

Routes are registered separately by `helpers/wopi/wopiRouting.js` (`/wopi`, `/wopi-new`, `/wopi-action/:id`,
`/wopi/files/:id`, `/wopi/files/:id/contents`, `/wopi/folders/:id`, `/wopi/upload`). Supporting pieces:
`filesController.js` (CheckFileInfo, GetFile, PutFile, Lock/Unlock …), `lockManager.js`, `tokenValidator.js`
(proof-key validation), `discovery` parsing driven by `server.wopi.discovery`.

## Conventions

- Every source file starts with the Apache-2.0 ONLYOFFICE header (`(c) Copyright Ascensio System SIA <year>`).
  New files must include it.
- ESLint is airbnb-base with `max-len` 120, `no-console` off, and `String.prototype` extension allowed
  (`app.js` adds `hashCode` and `format`). Run `npm run lint` before finishing.
- **User-visible changes need a CHANGELOG entry at the repository root** (`../../../CHANGELOG.md`), under the
  top unreleased `##` heading, prefixed with the example name: `nodejs: <what changed>`. Release tagging
  parses that file.
- Commits use Conventional Commits with the example as scope: `feat(nodejs): …`, `fix(nodejs): …`.
- Features here are usually ported to the other 10 examples in separate commits. When adding one, expect that
  follow-up rather than editing sibling directories in the same change.
- The `version` in `package.json` and `config/default.json` tracks the release; bump together with the
  siblings' config files.

## Gotchas

- `helpers/cacheManager.js` is dead code — nothing requires it.
- `server.verify_peer_off` sets `NODE_TLS_REJECT_UNAUTHORIZED=0` process-wide at startup.
