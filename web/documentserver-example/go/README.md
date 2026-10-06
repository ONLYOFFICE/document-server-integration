# Go integration

The Go integration example is a small web application built on the gorilla/mux router that lists files, opens them in ONLYOFFICE Docs, and saves them back through the callback handler. Run it to see a complete integration working before you write your own.

> [!WARNING]
> This example is for testing only. It has no authentication, does not validate link parameters or save requests, and accepts requests from any site, so anyone who can reach it can read and change its files. Do not run it on a public server, and disable it before going to production.

## Prerequisites

- **ONLYOFFICE Docs**: [self-hosted](https://www.onlyoffice.com/download?from=api#docs-developer) or [cloud](https://www.onlyoffice.com/docs-registration?from=api).
- **Go**: see the [official website](https://go.dev/dl/).
- **Git**: see the [official website](https://git-scm.com/downloads).

## Step 1. Download the example

Clone the [example repository](https://github.com/ONLYOFFICE/document-server-integration/tree/main/web/documentserver-example/go), go to the Go example, and fetch its submodules:

```sh
git clone --depth 1 https://github.com/ONLYOFFICE/document-server-integration
cd document-server-integration/web/documentserver-example/go
git submodule update --init --depth 1 .
```

## Step 2. Configure the connection

Open the `config/configuration.json` file and edit the following keys:

```json
{
  "SERVER_ADDRESS": "",
  "DOC_SERVER_HOST": "http://documentserver/",
  "JWT_IS_ENABLED": false,
  "JWT_SECRET": "secret",
  "STORAGE_PATH": "filestore"
}
```

- `DOC_SERVER_HOST`: the address of the server with ONLYOFFICE Docs installed. Replace `documentserver` with its name or IP address.
- `STORAGE_PATH`: the folder where the example creates and stores files. It is created inside the `static` folder of the example, so set a relative path. The user running the example needs read and write permissions to this folder.
- `JWT_IS_ENABLED` and `JWT_SECRET`: must match the JWT settings of ONLYOFFICE Docs. JWT is enabled in ONLYOFFICE Docs by default, so set `JWT_IS_ENABLED` to `true` and replace the `JWT_SECRET` value with the [secret key](https://api.onlyoffice.com/docs/docs-api/additional-api/signature/) of your server.
- `SERVER_ADDRESS`: the address at which ONLYOFFICE Docs reaches the example, with the protocol and port and without a trailing slash, for example, `http://192.168.1.10:3000`. Set it if ONLYOFFICE Docs runs on another computer or in Docker, where the address you open in the browser, such as `localhost`, points somewhere else.

Each key can also be set as an environment variable with the same name, for example, `JWT_SECRET`. An environment variable overrides the value in the file.

If you want to experiment with the editor configuration, modify the [parameters](https://api.onlyoffice.com/docs/docs-api/usage-api/advanced-parameters/) in the `server/managers/default/document.go` file, where the example builds the configuration.

## Step 3. Install the dependencies and run the example

Run these commands from the example folder, because the example loads its templates and files from the current folder:

```sh
go mod download
go run main.go
```

Open `http://localhost:3000` in your browser. To use another port, change `SERVER_PORT` in `config/configuration.json`. You will see the example's start page, where you can upload a file or create a new document, spreadsheet, presentation, or PDF form.

## Troubleshooting

- **The document security token is not correctly formed**: JWT is disabled in the example (`JWT_IS_ENABLED` is `false`), or `JWT_SECRET` does not match the secret of ONLYOFFICE Docs.
- **Download failed**: ONLYOFFICE Docs cannot reach the example. Set `SERVER_ADDRESS` in `config/configuration.json` to an address that ONLYOFFICE Docs can resolve.

For more information, see the [documentation](https://api.onlyoffice.com/docs/docs-api/samples/language-specific-examples/go-example/).
