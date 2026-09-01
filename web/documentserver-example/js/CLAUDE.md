# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## What this is

A single static HTML page that embeds the ONLYOFFICE Docs (Document Server) editor for a document at an
arbitrary URL. Five files, ~120 lines of hand-written code. Deliberately unhardened demo code — do not "fix"
it as if it were production; real bugs are still worth fixing.

It sits next to 10 sibling examples under `../` (`csharp`, `csharp-mvc`, `go`, `java`, `java-spring`,
`nodejs`, `php`, `php-laravel`, `python`, `ruby`), but it is **not one of them**: those are full client+server
document management systems with storage, history and a `/track` save callback. This one has no backend of any
kind — it only demonstrates instantiating `DocsAPI.DocEditor` in a browser.

**It is effectively frozen.** `git log --oneline -- .` shows 34 commits; the last functional change was
`support xlsb` (2022-02-06), everything since is the annual copyright bump. It is absent from the root
`Readme.md` and `CHANGELOG.md`, has no CI workflow (no `lint-js.yml`, no `artifact-js.yml` in
`.github/workflows/`), and `release.yml` ships zips for the other 10 examples only — not this one.

## How to run it, and configuration

Nothing to install, build or start: open `OnlineEditorsExampleJS.html` in a browser, from disk (`file://`) or
from any static web server. Two hand edits first — there is no config file:

- the `<script id="scriptApi">` tag in the HTML: replace the `http://documentserver` placeholder in
  `http://documentserver/web-apps/apps/api/documents/api.js`. Only place the address appears.
- the `#documentUrl` textarea default (`http://onlyo.co/1x5REbq?demo.docx`). Editable in the page at runtime,
  so changing the default is optional.

The document is fetched by Document Server, not the browser, so its URL must be reachable **from Document
Server**. `api.js` is loaded over plain `http://`, so serving this page over `https://` gets it blocked as
mixed content.

**JWT is impossible here.** Signing a config needs a secret and a server to sign with it; there is neither.
This page only works against a Document Server with token validation disabled.

## Code map

`OnlineEditorsExampleJS.html` loads `api.js` and `init.js`, then shows the URL textarea and three inline
`onclick` buttons: *Embedded view* (`initEditor(null, null, 'embedded')`, also adds the `embedded` class to
`#placeholder`'s parent), *View* (`initEditor()`), *Edit* (`initEditor(null, 'edit')`). Each hides the panel
and lets the editor take over. `stylesheet.css` and `logo.svg` are presentation only.

`init.js` is everything else. `initEditor(docKey, mode, type)` stashes `mode`, `type`, `docUrl`, `docKey` on
`window` via the `window.X || arg || default` idiom, so the **first** call wins and later calls reuse its
values. The extension is the substring after the last `.` of the whole URL (hence the demo URL's
`?demo.docx`), mapped by `getDocumentType` to `text` / `spreadsheet` / `presentation` through a hardcoded
`.doc.docx…`-style `indexOf` string; unknown extensions give `null`. `key(url)` is the sanitised URL plus
`Date.now()` truncated to the last 20 chars — a new key on every page load. The config handed to
`new DocsAPI.DocEditor("placeholder", …)` is only `type`, `width`/`height` (`100%`, desktop only),
`documentType`, `document: { title, url, fileType, key, permissions: { edit: true } }`, `editorConfig: { mode }`.

## What it does NOT do

Verified absent, not merely undocumented:

- **No backend**: no server process, framework, dependency manifest, `Dockerfile`/`Makefile`, `Readme.md` or
  submodules.
- **No `callbackUrl`, so edits cannot be saved.** "Edit" opens a working editor, but nothing listens for
  Document Server's save callback and changes are lost. No `/track` handler exists here.
- No storage, upload, create-new, file list, conversion, version history, forcesave, forgotten files, rename
  or reference data.
- No JWT (above), `editorConfig.user`, user fixture, permission variants, `customization`, `lang`, event
  handlers (`onDocumentReady`, `onError`, …) or plugins.
- No format manager and no `document-formats` submodule — format classification is the one hardcoded string
  in `getDocumentType`.
- No build step, linter, tests or CI.

If asked to "add feature X the way the other examples do it", say up front that X needs a server this example
does not have.

## Conventions

- `OnlineEditorsExampleJS.html`, `init.js` and `stylesheet.css` carry the Apache-2.0 ONLYOFFICE header
  (`(c) Copyright Ascensio System SIA 2026`); `logo.svg` does not. New files must include it.
- User-visible changes need a CHANGELOG entry at the repository root (`../../../CHANGELOG.md`) under the top
  unreleased `##` heading, prefixed `js: …` — though no entry has ever used this scope. Release tagging parses
  that file.
- Commits use Conventional Commits with the example as scope: `feat(js): …`, `fix(js): …`.
- The cross-example porting rule does **not** apply. The git log shows this directory has never taken part in
  the one-commit-per-language feature sweeps the other 10 share; changes here need no counterpart elsewhere.
