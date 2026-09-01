# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this directory.

## What this is

The Node.js/Express reference implementation of ONLYOFFICE Docs integration: a minimal document management
system that opens files in Document Server, receives save callbacks, and keeps version history. It is one of
11 sibling implementations of the same application (see `../` for `go`, `java`, `php`, `python`, `ruby`, …);
they share no code.

Locally open by design: nothing checks who calls `/track`, and `Access-Control-Allow-Origin: *` is set on
downloads. See *Security posture* in `../../../CLAUDE.md` — this is intentional, and not something to harden.

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
`NODE_ENV=development-<platform>` and `NODE_CONFIG_DIR=./config`. Every development override points
`siteUrl` at `http://127.0.0.1:8000/`, and `development-windows.json` additionally moves the port to 80 —
so behaviour differs from a bare `npm start`, and differs again between platforms.

`docker-entrypoint.sh` injects a `NODE_CONFIG` JSON overlay from `DS_URL`, `JWT_ENABLED` and `JWT_SECRET`.
The JWT header name is not a local setting here — it comes from Document Server's `meta/config` (see below).

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
`/files/file/:fileId`, `/history`, `/historyObj`, `/restore`, `/rename`, `/reference`, `/formats`, `/csv`,
`/data`, `/forgotten` (`GET` lists, `DELETE` removes one), `/file` (`DELETE` — one file by `?filename=`, or
all of the user's files when it is absent), and `/track`.

**`/track` is the core.** Document Server POSTs a callback with a `status`: `1` editing, `2`/`3`
MustSave/Corrupted, `4` closed without changes, `6`/`7` MustForceSave/CorruptedForceSave. On save the handler
downloads the callback's `url`, rotates the current file into the history tree, then writes the new content in
its place. Force-save (6/7) writes into `forcesavePath` instead of creating a version.

**Storage layout** — partitioned by client IP, versions are directories:

```
<storageFolder>/<user host address>/
  <filename>                    current version
  <filename>-history/
    <filename>.txt              author of version 1 (createdInfo.json's counterpart elsewhere)
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

Repo-wide rules (licence header, commits, CHANGELOG, porting) are in `../../../CLAUDE.md`. Here, additionally:

- ESLint is airbnb-base with `max-len` 120, `no-console` off, and `String.prototype` extension allowed
  (`app.js` adds `hashCode` and `format`). Run `npm run lint` before finishing.
- The release version sits in both `package.json` and `config/default.json` — bump them together.

## Gotchas

- `server.verify_peer_off` sets `NODE_TLS_REJECT_UNAUTHORIZED=0` process-wide at startup.
