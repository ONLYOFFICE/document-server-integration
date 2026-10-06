# Node.js integration

The Node.js integration example is a small Express application that lists files, opens them in ONLYOFFICE Docs, and saves them back through the callback handler. Run it to see a complete integration working before you write your own.

> [!WARNING]
> This example is for testing only. It has no authentication, does not validate link parameters or save requests, and accepts requests from any site, so anyone who can reach it can read and change its files. Do not run it on a public server, and disable it before going to production.

## Prerequisites

- **ONLYOFFICE Docs**: [self-hosted](https://www.onlyoffice.com/download?from=api#docs-developer) or [cloud](https://www.onlyoffice.com/docs-registration?from=api).
- **Node.js**: see the [official website](https://nodejs.org/en/download/).
- **Git**: see the [official website](https://git-scm.com/downloads).

## Step 1. Download the example

Clone the [example repository](https://github.com/ONLYOFFICE/document-server-integration/tree/main/web/documentserver-example/nodejs), go to the Node.js example, and fetch its submodules:

```sh
git clone --depth 1 https://github.com/ONLYOFFICE/document-server-integration
cd document-server-integration/web/documentserver-example/nodejs
git submodule update --init --depth 1 .
```

## Step 2. Configure the connection

Open the `config/default.json` file and edit the following keys. All of them are inside the `server` object:

```json
{
  "server": {
    "siteUrl": "http://documentserver/",
    "storageFolder": "./files",
    "exampleUrl": null,
    "token": {
      "enable": true,
      "secret": "developer-only-not-a-real-secret"
    }
  }
}
```

- `siteUrl`: the address of the server with ONLYOFFICE Docs installed. Replace `documentserver` with its name or IP address.
- `storageFolder`: the folder where the example creates and stores files. You can set an absolute path. In JSON, escape each backslash, for example, `D:\\folder`. The user running the example needs read and write permissions to this folder.
- `token.enable` and `token.secret`: must match the JWT settings of ONLYOFFICE Docs. JWT is enabled in both by default, so leave `enable` set to `true` and replace the `secret` value with the [secret key](https://api.onlyoffice.com/docs/docs-api/additional-api/signature/) of your server.
- `exampleUrl`: the address at which ONLYOFFICE Docs reaches the example. Set it if ONLYOFFICE Docs runs on another computer or in Docker, where the address you open in the browser, such as `localhost`, points somewhere else.

If you want to experiment with the editor configuration, modify the [parameters](https://api.onlyoffice.com/docs/docs-api/usage-api/advanced-parameters/) in the `views/config.ejs` file.

## Step 3. Install the dependencies and run the example

```sh
npm install
npm start
```

Open `http://localhost:3000` in your browser. You will see the example's start page, where you can upload a file or create a new document, spreadsheet, presentation, or PDF form.

## Troubleshooting

- **The document security token is not correctly formed**: JWT is disabled in the example (`token.enable` is `false`), or `token.secret` does not match the secret of ONLYOFFICE Docs.
- **Download failed**: ONLYOFFICE Docs cannot reach the example. Set `exampleUrl` in `config/default.json` to an address that ONLYOFFICE Docs can resolve.

For more information, see the [documentation](https://api.onlyoffice.com/docs/docs-api/samples/language-specific-examples/nodejs-example/).
